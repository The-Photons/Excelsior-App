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

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parameters
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

// CONSTANTS
const val LOGIN_PAGE = "auth/login"
const val LOGOUT_PAGE = "auth/logout"
const val GET_ENCRYPTION_PARAMS_PAGE = "auth/get-encryption-params"

const val LIST_DIR_PAGE = "list-dir"
const val RECURSIVE_LIST_DIR_PAGE = "recursive-list-dir"
const val GET_FILE_PAGE = "get-file"
const val CREATE_FOLDER_PAGE = "create-dir"
const val CREATE_FILE_PAGE = "create-file"
const val DELETE_ITEM_PAGE = "delete-item"

const val PING_PAGE = "ping"
const val GET_VERSION_PAGE = "version"

// HELPER FUNCTIONS
fun String.decodeHex(): ByteArray {
    check(length % 2 == 0) { "Must have an even length" }

    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}

// CLASSES
enum class HttpMethod {
    GET, POST
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

    // Main methods
    /**
     * Gets the encryption parameters for the logged in user.
     *
     * @param processResponse Listener for a successful page request.
     * @param failedResponse Listener for a failed page request.
     * @param errorListener Listener for an page request that results in an error.
     */
    fun getEncryptionParameters(
        processResponse: (JSONObject) -> Any,
        failedResponse: (String, JSONObject) -> Any,
        errorListener: (Exception) -> Any
    ) {
        sendRequest(
            serverURL,
            HttpMethod.GET,
            GET_ENCRYPTION_PARAMS_PAGE,
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
        processResponse: (JSONObject) -> Any,
        failedResponse: (String, JSONObject) -> Any,
        errorListener: (Exception) -> Any
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
        processResponse: (JSONObject) -> Any,
        failedResponse: (String, JSONObject) -> Any,
        errorListener: (Exception) -> Any
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
            client,
            processResponse,
            failedResponse,
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
        processResponse: (JSONObject) -> Any,
        failedResponse: (String, JSONObject) -> Any,
        errorListener: (Exception) -> Any
    ) {
        sendRequest(
            serverURL,
            HttpMethod.GET,
            "$GET_FILE_PAGE/$path",
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
        processResponse: (JSONObject) -> Any,
        failedResponse: (String, JSONObject) -> Any,
        errorListener: (Exception) -> Any
    ) {
        sendRequest(
            serverURL,
            HttpMethod.POST,
            "$CREATE_FOLDER_PAGE/$path",
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
     * @param encryptedContent Encrypted, Base64 string of the file.
     * @param processResponse Listener for a successful page request.
     * @param failedResponse Listener for a failed page request.
     * @param errorListener Listener for an page request that results in an error.
     */
    fun createFile(
        path: String,
        encryptedContent: String,
        processResponse: (JSONObject) -> Any,
        failedResponse: (String, JSONObject) -> Any,
        errorListener: (Exception) -> Any
    ) {
        // Create the POST Data
        val postData = HashMap<String, String>()
        postData["content"] = encryptedContent

        // Send the POST data to the page
        sendRequest(
            serverURL,
            HttpMethod.POST,
            "$CREATE_FILE_PAGE/$path",
            client,
            processResponse,
            failedResponse,
            errorListener,
            postData
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
        processResponse: (JSONObject) -> Any,
        failedResponse: (String, JSONObject) -> Any,
        errorListener: (Exception) -> Any
    ) {
        sendRequest(
            serverURL,
            HttpMethod.GET,  // TODO: Fix to use DELETE
            "$DELETE_ITEM_PAGE/$path",
            client,
            processResponse,
            failedResponse,
            errorListener
        )
    }

    /**
     * Gets the server version.
     *
     * @param processResponse Listener for a successful page request.
     * @param failedResponse Listener for a failed page request.
     * @param errorListener Listener for an page request that results in an error.
     */
    fun getServerVersion(
        processResponse: (JSONObject) -> Any,
        failedResponse: (String, JSONObject) -> Any,
        errorListener: (Exception) -> Any
    ) {
        sendRequest(
            serverURL,
            HttpMethod.GET,
            GET_VERSION_PAGE,
            client,
            processResponse,
            failedResponse,
            errorListener
        )
    }

    /**
     * Checks if the provided credentials are valid.
     *
     * @param username Username to check.
     * @param password Password to check.
     * @param listener Listener to process the result.
     */
    fun isValidCredentials(
        username: String,
        password: String,
        actuallyLogin: Boolean = true,
        listener: (Boolean) -> Unit
    ) {
        // We need to ensure that a username and password are provided
        if (username.isBlank() || password.isBlank()) {
            Log.d("SERVER", "Provided username or password is blank")
            listener(false)
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
            client,
            {
                run {
                    Log.d("SERVER", "Login successful")
                    listener(true)
                }
            },
            { _, json ->
                run {
                    val message = json.getString("message")
                    Log.d("SERVER", "Login failed: $message")
                    listener(false)
                }
            },
            { error ->
                run {
                    Log.d("SERVER", "Error when logging in: $error")
                    listener(false)
                }
            },
            postData
        )
    }

    // Static methods
    companion object {
        /**
         * Helper method that sends a request to the specified page on the server.
         *
         * @param method Request method.
         * @param page   Page (and URL parameters) to send the request to.
         * @param client HTTP client.
         * @param processResponse Listener for a successful page request.
         * @param failedResponse Listener for a failed page request.
         * @param errorListener Listener for an page request that results in an error.
         * @param postData Data to included in the POST request. Required if the request is POST.
         */
        private fun sendRequest(
            serverURL: String,
            method: HttpMethod,
            page: String,
            client: HttpClient,
            processResponse: (JSONObject) -> Any,
            failedResponse: (String, JSONObject) -> Any,
            errorListener: (Exception) -> Any,
            postData: HashMap<String, String>? = null,
        ) {
            // Form the full URL
            val url = "$serverURL/$page"
            runBlocking {  // Todo: use better async?
                try {
                    val response = if (method == HttpMethod.POST) {
                        client.submitForm(url, formParameters = parameters {
                            postData?.forEach { (key, value) ->
                                append(key, value)
                            }
                        })
                    } else {
                        client.get(url)
                    }
                    Log.d("SERVER", "Sent request to '$url'")

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
         * @param client HTTP client.
         * @param listener Listener to process the result.
         */
        fun isValidURL(serverURL: String, client: HttpClient, listener: (Boolean) -> Unit) {
            Log.d("SERVER", "Checking if '$serverURL' is valid")
            sendRequest(
                serverURL,
                HttpMethod.GET,
                PING_PAGE,
                client,
                { json ->
                    run {
                        val response = json.get("content")
                        if (response == "pong") {
                            Log.d("SERVER", "'$serverURL' is valid")
                            listener(true)
                        } else {
                            Log.d("SERVER", "'$serverURL' is not valid")
                            listener(false)
                        }
                    }
                },
                { _, _ -> Log.d("SERVER", "'$serverURL' is not valid"); listener(false) },
                { _ -> Log.d("SERVER", "'$serverURL' is not valid"); listener(false) }
            )
        }
    }
}
