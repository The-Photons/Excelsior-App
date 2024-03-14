/*
 * Copyright (c) 2023-2024 PhotonicGluon.
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

package site.overwrite.encryptedfilesapp

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.parameters
import io.ktor.utils.io.ByteReadChannel
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
        // Create the POST Data
        val postData = HashMap<String, String>()
        postData["username"] = username
        postData["password"] = password

        // Send the request to the server
        sendRequest(
            url = serverURL,
            method = HttpMethod.POST,
            page = if (actuallyLogin) LOGIN_PAGE else "$LOGIN_PAGE?actually-login=false",
            scope = scope,
            client = client,
            processJSONResponse = {
                Log.d("SERVER", "Login successful")
                listener(true, 0)
            },
            failedResponse = { _, json ->
                val message = json.getString("message")
                val errorCode = json.getInt("error_code")
                Log.d("SERVER", "Login failed: $message")
                listener(false, errorCode)
            },
            errorListener = { error ->
                Log.d("SERVER", "Error when logging in: $error")
                listener(false, 1)
            },
            postData = postData
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
            url = serverURL,
            method = HttpMethod.GET,
            page = LOGOUT_PAGE,
            scope = scope,
            client = client,
            processJSONResponse = {
                Log.d("SERVER", "Logout successful")
                listener(true)
            },
            failedResponse = { _, json ->
                val message = json.getString("message")
                Log.d("SERVER", "Logout failed: $message")
                listener(false)
            },
            errorListener = { error ->
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
            url = serverURL,
            method = HttpMethod.GET,
            page = GET_ENCRYPTION_PARAMS_PAGE,
            scope = scope,
            client = client,
            processJSONResponse = processResponse,
            failedResponse = failedResponse,
            errorListener = errorListener
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
            url = serverURL,
            method = HttpMethod.GET,
            page = page,
            scope = scope,
            client = client,
            processJSONResponse = processResponse,
            failedResponse = failedResponse,
            errorListener = errorListener
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
            url = serverURL,
            method = HttpMethod.GET,
            page = page,
            scope = scope,
            client = client,
            processJSONResponse = processResponse,
            failedResponse = failedResponse,
            errorListener = errorListener
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
            url = serverURL,
            method = HttpMethod.GET,
            page = "$PATH_EXISTS_PAGE/$path",
            scope = scope,
            client = client,
            processJSONResponse = { json ->
                listener(json.getBoolean("exists"))
            },
            failedResponse = { _, _ -> },
            errorListener = errorListener
        )
    }

    /**
     * Gets the contents of a file.
     *
     * @param path Path to the file.
     * @param processResponse Listener for a successful page request.
     * @param errorListener Listener for an page request that results in an error.
     * @param downloadHandler Function that takes two parameters, the number of transmitted
     * bytes (`bytesSentTotal`) and the total bytes to download (`contentLength`), and processes
     * it.
     */
    fun getFile(
        path: String,
        processResponse: (ByteReadChannel) -> Unit,
        errorListener: (Exception) -> Unit,
        downloadHandler: suspend (bytesSentTotal: Long, contentLength: Long) -> Unit = { _, _ -> }
    ) {
        sendRequest(
            url = serverURL,
            method = HttpMethod.GET,
            page = "$GET_FILE_PAGE/$path",
            scope = scope,
            client = client,
            responseIsJSON = false,
            processRawResponse = processResponse,
            errorListener = errorListener,
            downloadHandler = downloadHandler
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
            url = serverURL,
            method = HttpMethod.POST,
            page = "$CREATE_FOLDER_PAGE/$path",
            scope = scope,
            client = client,
            processJSONResponse = processResponse,
            failedResponse = failedResponse,
            errorListener = errorListener
        )
    }

    /**
     * Creates a new file on the server.
     *
     * @param path Path to the new file.
     * @param encryptedFile Encrypted file.
     * @param mimeType MIME type of the original unencrypted file.
     * @param processResponse Listener for a successful page request.
     * @param failedResponse Listener for a failed page request.
     * @param errorListener Listener for an page request that results in an error.
     * @param uploadHandler Function that handles uploads. Takes two parameters, the number of
     * transmitted bytes (`bytesSentTotal`) and the total bytes to upload (`contentLength`).
     */
    fun createFile(
        path: String,
        encryptedFile: File,
        mimeType: String,
        processResponse: (JSONObject) -> Unit,
        failedResponse: (String, JSONObject) -> Unit,
        errorListener: (Exception) -> Unit,
        uploadHandler: suspend (bytesSentTotal: Long, contentLength: Long) -> Unit = { _, _ -> }
    ) {
        sendRequest(
            url = serverURL,
            method = HttpMethod.POST,
            page = "$CREATE_FILE_PAGE/$path",
            scope = scope,
            client = client,
            processJSONResponse = processResponse,
            failedResponse = failedResponse,
            errorListener = errorListener,
            postFile = encryptedFile,
            postFileMimeType = mimeType,
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
            url = serverURL,
            method = HttpMethod.DELETE,
            page = "$DELETE_ITEM_PAGE/$path",
            scope = scope,
            client = client,
            processJSONResponse = processResponse,
            failedResponse = failedResponse,
            errorListener = errorListener
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
            url = serverURL,
            method = HttpMethod.GET,
            page = GET_VERSION_PAGE,
            scope = scope,
            client = client,
            processJSONResponse = processResponse,
            failedResponse = failedResponse,
            errorListener = errorListener
        )
    }

    // Static methods
    companion object {
        /**
         * Helper method that sends a request to the specified page on the server.
         *
         * @param url Server's URL.
         * @param method Request method.
         * @param page   Page (and URL parameters) to send the request to.
         * @param scope Coroutine scope.
         * @param client HTTP client.
         * @param responseIsJSON Whether the response from the server is in JSON format.
         * @param processRawResponse Processes a raw successful response from the server. Required
         * if [responseIsJSON] is `false`.
         * @param processJSONResponse Processes the JSON response from the server. Required if
         * [responseIsJSON] is `true`.
         * @param failedResponse Listener for a failed page request.
         * @param errorListener Listener for an page request that results in an error.
         * @param postData Data to included in the POST request. Required if [postFile] is not
         * provided and if the request is a POST request.
         * @param postFile File to be included in the POST request. Required if [postData] is not
         * provided and if the request is a POST request.
         * @param postFileMimeType MIME type of the file included in the POST request. Required if
         * [postFile] is provided.
         * @param downloadHandler Function that takes two parameters, the number of transmitted
         * bytes (`bytesSentTotal`) and the total bytes to download (`contentLength`), and processes
         * it.
         * @param uploadHandler Function that takes two parameters, the number of transmitted
         * bytes (`bytesSentTotal`) and the total bytes to upload (`contentLength`), and processes
         * it.
         */
        private fun sendRequest(
            url: String,
            method: HttpMethod,
            page: String,
            scope: CoroutineScope,
            client: HttpClient,
            responseIsJSON: Boolean = true,
            processRawResponse: (channel: ByteReadChannel) -> Unit = { _ -> },
            processJSONResponse: (json: JSONObject) -> Unit = { _ -> },
            failedResponse: (status: String, json: JSONObject) -> Unit = { _, _ -> },
            errorListener: (error: Exception) -> Unit = { _ -> },
            postData: HashMap<String, String>? = null,
            postFile: File? = null,
            postFileMimeType: String? = null,
            downloadHandler: suspend (bytesSentTotal: Long, contentLength: Long) -> Unit = { _, _ -> },
            uploadHandler: suspend (bytesSentTotal: Long, contentLength: Long) -> Unit = { _, _ -> },
        ) {
            // Todo: Handle timeout of server requests

            // Form the full URL
            val fullURL = "$url/$page"
            scope.launch {
                try {
                    val response = when (method) {
                        HttpMethod.GET -> client.get(fullURL) {
                            onDownload(downloadHandler)
                        }

                        HttpMethod.POST ->
                            if (postFile != null && postFileMimeType != null) {
                                client.submitFormWithBinaryData(
                                    url = fullURL,
                                    formData = formData {
                                        // FIXME: Is `readBytes()` the best method?
                                        append("file", postFile.readBytes(), Headers.build {
                                            append(HttpHeaders.ContentType, postFileMimeType)
                                            append(
                                                HttpHeaders.ContentDisposition,
                                                "filename=\"${postFile.name}\""
                                            )
                                        })
                                    }
                                ) {
                                    onUpload(uploadHandler)
                                }
                            } else {
                                client.submitForm(
                                    url = fullURL,
                                    formParameters = parameters {
                                        postData?.forEach { (key, value) ->
                                            append(key, value)
                                        }
                                    }) {
                                    onUpload(uploadHandler)
                                }
                            }

                        HttpMethod.DELETE -> client.delete(fullURL)
                    }
                    Log.d("SERVER", "Sent $method request to '$fullURL'")

                    if (response.status.value == 200) {
                        if (responseIsJSON) {
                            val json = JSONObject(response.bodyAsText())
                            val status = json.getString("status")
                            if (status == "ok") {
                                processJSONResponse(json)
                            } else {
                                failedResponse(status, json)
                            }
                        } else {
                            processRawResponse(response.bodyAsChannel())
                        }
                    } else {
                        Log.d("SERVER", "Error ${response.status.value} for '$fullURL'")
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
                url = serverURL,
                method = HttpMethod.GET,
                page = PING_PAGE,
                scope = scope,
                client = client,
                processJSONResponse = { json ->
                    val response = json.get("content")
                    if (response == "pong") {
                        Log.d("SERVER", "'$serverURL' is valid")
                        listener(true)
                    } else {
                        Log.d("SERVER", "'$serverURL' is not valid")
                        listener(false)
                    }
                },
                failedResponse = { _, _ ->
                    Log.d("SERVER", "'$serverURL' is not valid"); listener(
                    false
                )
                },
                errorListener = { _ ->
                    Log.d(
                        "SERVER",
                        "'$serverURL' is not valid"
                    ); listener(false)
                }
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
