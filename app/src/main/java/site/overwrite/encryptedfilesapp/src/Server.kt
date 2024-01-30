/*
 * Copyright (c) 2023 PhotonicGluon.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package site.overwrite.encryptedfilesapp.src

import android.util.Base64
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.delete
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

// CONSTANTS
const val LOGIN_PAGE = "auth/login"
const val LOGOUT_PAGE = "auth/logout"
const val GET_ENCRYPTION_PARAMS_PAGE = "auth/get-encryption-params"

const val LIST_DIR_PAGE = "list-dir"
const val RECURSIVE_LIST_DIR_PAGE = "recursive-list-dir"
const val PATH_EXISTS_PAGE = "path-exists"
const val GET_FILE_PAGE = "get-file"
const val CREATE_FOLDER_PAGE = "create-dir"
const val CREATE_FILE_PAGE = "create-file"
const val DELETE_ITEM_PAGE = "delete-item"

const val PING_PAGE = "ping"
const val GET_VERSION_PAGE = "version"

// CLASSES
enum class HttpMethod {
    GET, POST, DELETE
}

/**
 * Class that handles the communication with the encrypted files server.
 *
 * @property serverURL URL to the server. **Assumed to be valid**.
 */
class Server(val serverURL: String) {
    // Attributes
    private val client = HttpClient(CIO) {
        install(HttpCookies)
    }

    private val scope = CoroutineScope(Job())

    // Authentication methods
    /**
     * Checks if the provided credentials are valid and log in, if requested.
     *
     * @param username Username to check.
     * @param password Password to check.
     * @param listener Listener to process the result. The first element is whether the login was
     * successful. The second is the failure code.
     * - 0: No error
     * - 1: Invalid username
     * - 2: Invalid password
     */
    fun handleLogin(
        username: String,
        password: String,
        actuallyLogin: Boolean = true,
        listener: (Boolean, Int) -> Unit
    ) {
        // We need to ensure that a username and password are provided
        if (username.isBlank()) {
            Log.d("SERVER", "Provided username is blank")
            listener(false, 1)
            return
        } else if (password.isBlank()) {
            Log.d("SERVER", "Provided password is blank")
            listener(false, 2)
            return
        }

        // Create the POST Data
        val postData = HashMap<String, String>()
        postData["username"] = username
        postData["password"] = password

        // Otherwise we can send the request to the server
        sendRequest(
            serverURL,
            HttpMethod.POST,
            if (actuallyLogin) LOGIN_PAGE else "$LOGIN_PAGE?actually-login=false",
            scope,
            client,
            {
                Log.d("SERVER", "Login successful")
                listener(true, 0)
            },
            { _, json ->
                val message = json.getString("message")
                val errorCode = json.getInt("error_code")
                Log.d("SERVER", "Login failed: $message")
                listener(false, errorCode)
            },
            { error ->
                Log.d("SERVER", "Error when logging in: $error")
                listener(false, 1)
            },
            postData
        )
    }

    /**
     * Handle the logging out of the user from the server.
     *
     * @param listener Listener to process the result.
     */
    fun handleLogout(
        listener: (Boolean) -> Unit
    ) {
        sendRequest(
            serverURL,
            HttpMethod.GET,
            LOGOUT_PAGE,
            scope,
            client,
            {
                Log.d("SERVER", "Logout successful")
                listener(true)
            },
            { _, json ->
                val message = json.getString("message")
                Log.d("SERVER", "Logout failed: $message")
                listener(false)
            },
            { error ->
                Log.d("SERVER", "Error when logging out: $error")
                listener(false)
            }
        )
    }

    // File methods
    /**
     * Gets the encryption parameters for the logged in user.
     *
     * @param processResponse Listener for a successful page request.
     * @param failedResponse Listener for a failed page request.
     * @param errorListener Listener for an page request that results in an error.
     */
    fun getEncryptionParameters(
        processResponse: (JSONObject) -> Unit,
        failedResponse: (String, JSONObject) -> Unit,
        errorListener: (Exception) -> Unit,
    ) {
        sendRequest(
            serverURL,
            HttpMethod.GET,
            GET_ENCRYPTION_PARAMS_PAGE,
            scope,
            client,
            processResponse,
            failedResponse,
            errorListener
        )
    }

    /**
     * Gets the list of files in the path.
     *
     * @param path Path to the directory.
     * @param processResponse Listener for a successful page request.
     * @param failedResponse Listener for a failed page request.
     * @param errorListener Listener for an page request that results in an error.
     */
    fun listFiles(
        path: String,
        processResponse: (JSONObject) -> Unit,
        failedResponse: (String, JSONObject) -> Unit,
        errorListener: (Exception) -> Unit,
    ) {
        // Properly set the page
        val page: String = if (path != "") {
            "$LIST_DIR_PAGE?path=$path"
        } else {
            LIST_DIR_PAGE
        }

        // Now we can send the request
        sendRequest(
            serverURL,
            HttpMethod.GET,
            page,
            scope,
            client,
            processResponse,
            failedResponse,
            errorListener
        )
    }

    /**
     * Lists all the items in a path recursively.
     *
     * @param path Path to the directory.
     * @param processResponse Listener for a successful page request.
     * @param failedResponse Listener for a failed page request.
     * @param errorListener Listener for an page request that results in an error.
     */
    fun recursiveListFiles(
        path: String,
        processResponse: (JSONObject) -> Unit,
        failedResponse: (String, JSONObject) -> Unit,
        errorListener: (Exception) -> Unit,
    ) {
        // Properly set the page
        val page: String = if (path != "") {
            "$RECURSIVE_LIST_DIR_PAGE?path=$path"
        } else {
            RECURSIVE_LIST_DIR_PAGE
        }

        // Now we can send the request
        sendRequest(
            serverURL,
            HttpMethod.GET,
            page,
            scope,
            client,
            processResponse,
            failedResponse,
            errorListener
        )
    }

    /**
     * Checks if an item exists at the specified path.
     *
     * @param path Path to the file or folder.
     * @param listener Listener for the path check.
     * @param errorListener Listener for an page request that results in an error.
     */
    fun pathExists(
        path: String,
        listener: (Boolean) -> Unit,
        errorListener: (Exception) -> Unit
    ) {
        sendRequest(
            serverURL,
            HttpMethod.GET,
            "$PATH_EXISTS_PAGE/$path",
            scope,
            client,
            { json ->
                listener(json.getBoolean("exists"))
            },
            { _, _ -> },
            errorListener
        )
    }

    /**
     * Gets the contents of a file.
     *
     * @param path Path to the file.
     * @param processResponse Listener for a successful page request.
     * @param failedResponse Listener for a failed page request.
     * @param errorListener Listener for an page request that results in an error.
     */
    fun getFile(
        path: String,
        processResponse: (JSONObject) -> Unit,
        failedResponse: (String, JSONObject) -> Unit,
        errorListener: (Exception) -> Unit
    ) {
        sendRequest(
            serverURL,
            HttpMethod.GET,
            "$GET_FILE_PAGE/$path",
            scope,
            client,
            processResponse,
            failedResponse,
            errorListener
        )
    }

    /**
     * Creates a new folder with the specified path.
     *
     * @param path Path to the new folder.
     * @param processResponse Listener for a successful page request.
     * @param failedResponse Listener for a failed page request.
     * @param errorListener Listener for an page request that results in an error.
     */
    fun createFolder(
        path: String,
        processResponse: (JSONObject) -> Unit,
        failedResponse: (String, JSONObject) -> Unit,
        errorListener: (Exception) -> Unit,
    ) {
        sendRequest(
            serverURL,
            HttpMethod.POST,
            "$CREATE_FOLDER_PAGE/$path",
            scope,
            client,
            processResponse,
            failedResponse,
            errorListener
        )
    }

    /**
     * Creates a new file on the server.
     *
     * @param path Path to the new file.
     * @param encryptedFile Encrypted file.
     * @param processResponse Listener for a successful page request.
     * @param failedResponse Listener for a failed page request.
     * @param errorListener Listener for an page request that results in an error.
     * @param uploadHandler Function that handles uploads. Takes two parameters, the number of
     * transmitted bytes (`bytesSentTotal`) and the total bytes to upload (`contentLength`).
     */
    fun createFile(
        path: String,
        encryptedFile: File,
        processResponse: (JSONObject) -> Unit,
        failedResponse: (String, JSONObject) -> Unit,
        errorListener: (Exception) -> Unit,
        uploadHandler: suspend (bytesSentTotal: Long, contentLength: Long) -> Unit = {_, _ ->}
    ) {
        // Create the POST Data
        // TODO: Use file uploads instead of whatever this is
        val postData = HashMap<String, String>()
        postData["content"] = Base64.encodeToString(encryptedFile.readBytes(), Base64.NO_WRAP)

        // Send the POST data to the page
        sendRequest(
            serverURL,
            HttpMethod.POST,
            "$CREATE_FILE_PAGE/$path",
            scope,
            client,
            processResponse,
            failedResponse,
            errorListener,
            postData,
            uploadHandler = uploadHandler
        )
    }

    /**
     * Deletes an item from the server. **This action is irreversible.**
     *
     * @param path Path to the item.
     * @param processResponse Listener for a successful page request.
     * @param failedResponse Listener for a failed page request.
     * @param errorListener Listener for an page request that results in an error.
     */
    fun deleteItem(
        path: String,
        processResponse: (JSONObject) -> Unit,
        failedResponse: (String, JSONObject) -> Unit,
        errorListener: (Exception) -> Unit,
    ) {
        sendRequest(
            serverURL,
            HttpMethod.DELETE,
            "$DELETE_ITEM_PAGE/$path",
            scope,
            client,
            processResponse,
            failedResponse,
            errorListener
        )
    }

    // Miscellaneous methods
    /**
     * Gets the server version.
     *
     * @param processResponse Listener for a successful page request.
     * @param failedResponse Listener for a failed page request.
     * @param errorListener Listener for an page request that results in an error.
     */
    fun getServerVersion(
        processResponse: (JSONObject) -> Unit,
        failedResponse: (String, JSONObject) -> Unit,
        errorListener: (Exception) -> Unit,
    ) {
        sendRequest(
            serverURL,
            HttpMethod.GET,
            GET_VERSION_PAGE,
            scope,
            client,
            processResponse,
            failedResponse,
            errorListener
        )
    }

    // Static methods
    companion object {
        /**
         * Helper method that sends a request to the specified page on the server.
         *
         * @param serverURL Server's URL.
         * @param method Request method.
         * @param page   Page (and URL parameters) to send the request to.
         * @param scope Coroutine scope.
         * @param client HTTP client.
         * @param processResponse Listener for a successful page request.
         * @param failedResponse Listener for a failed page request.
         * @param errorListener Listener for an page request that results in an error.
         * @param postData Data to included in the POST request. Required if the request is POST.
         * @param downloadHandler Function that takes two parameters, the number of transmitted
         * bytes (`bytesSentTotal`) and the total bytes to download (`contentLength`), and processes
         * it.
         * @param uploadHandler Function that takes two parameters, the number of transmitted
         * bytes (`bytesSentTotal`) and the total bytes to upload (`contentLength`), and processes
         * it.
         */
        private fun sendRequest(
            serverURL: String,
            method: HttpMethod,
            page: String,
            scope: CoroutineScope,
            client: HttpClient,
            processResponse: (JSONObject) -> Unit,
            failedResponse: (String, JSONObject) -> Unit,
            errorListener: (Exception) -> Unit,
            postData: HashMap<String, String>? = null,
            downloadHandler: suspend (bytesSentTotal: Long, contentLength: Long) -> Unit = {_, _ ->},
            uploadHandler: suspend (bytesSentTotal: Long, contentLength: Long) -> Unit = {_, _ ->},
        ) {
            // Todo: Handle timeout of server requests

            // Form the full URL
            val url = "$serverURL/$page"
            scope.launch {
                try {
                    val response = when (method) {
                        HttpMethod.GET -> client.get(url) {
                            onDownload(downloadHandler)
                        }

                        HttpMethod.POST -> client.submitForm(url, formParameters = parameters {
                            postData?.forEach { (key, value) ->
                                append(key, value)
                            }
                        }) {
                            onUpload(uploadHandler)
                        }

                        HttpMethod.DELETE -> client.delete(url)
                    }
                    Log.d("SERVER", "Sent $method request to '$url'")

                    if (response.status.value == 200) {
                        val json = JSONObject(response.bodyAsText())
                        val status = json.getString("status")
                        if (status == "ok") {
                            processResponse(json)
                        } else {
                            failedResponse(status, json)
                        }
                    } else {
                        Log.d("SERVER", "Error ${response.status.value} for '$url'")
                    }
                } catch (e: Exception) {
                    errorListener(e)
                }
            }
        }

        /**
         * Determines whether the provided URL is a valid server URL.
         *
         * @param serverURL URL to check.
         * @param scope Coroutine scope.
         * @param client HTTP client.
         * @param listener Listener to process the result.
         */
        fun isValidURL(
            serverURL: String,
            scope: CoroutineScope,
            client: HttpClient,
            listener: (Boolean) -> Unit
        ) {
            Log.d("SERVER", "Checking if '$serverURL' is valid")
            sendRequest(
                serverURL,
                HttpMethod.GET,
                PING_PAGE,
                scope,
                client,
                { json ->
                    val response = json.get("content")
                    if (response == "pong") {
                        Log.d("SERVER", "'$serverURL' is valid")
                        listener(true)
                    } else {
                        Log.d("SERVER", "'$serverURL' is not valid")
                        listener(false)
                    }
                },
                { _, _ -> Log.d("SERVER", "'$serverURL' is not valid"); listener(false) },
                { _ -> Log.d("SERVER", "'$serverURL' is not valid"); listener(false) }
            )
        }

        /**
         * Cleans up a dirty URL to be nicer for displaying.
         *
         * @param dirtyURL Dirty URL to process.
         * @return Cleaned URL.
         */
        fun cleanUpURL(dirtyURL: String): String {
            // Strip any trailing slashes
            return dirtyURL.trimEnd('/')
        }
    }
}
