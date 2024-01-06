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
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import org.json.JSONObject

// CONSTANTS
const val GET_ENCRYPTION_PARAMS_PAGE = "get-encryption-params"
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
/**
 * Class that handles the communication with the encrypted files server.
 *
 * @property queue Volley `RequestQueue` for processing HTTP requests.
 * @property serverURL URL to the server. **Assumed to be valid**.
 */
class Server(private val queue: RequestQueue, val serverURL: String) {
    // Main methods
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
        errorListener: Response.ErrorListener
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
            Request.Method.GET,
            page,
            queue,
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
        errorListener: Response.ErrorListener
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
            Request.Method.GET,
            page,
            queue,
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
        errorListener: Response.ErrorListener
    ) {
        sendRequest(
            serverURL,
            Request.Method.GET,
            "$GET_FILE_PAGE/$path",
            queue,
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
        errorListener: Response.ErrorListener
    ) {
        sendRequest(
            serverURL,
            Request.Method.POST,
            "$CREATE_FOLDER_PAGE/$path",
            queue,
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
        errorListener: Response.ErrorListener
    ) {
        // Create the POST Data
        val postData = HashMap<String, String>()
        postData["content"] = encryptedContent

        // Send the POST data to the page
        sendRequest(
            serverURL,
            Request.Method.POST,
            "$CREATE_FILE_PAGE/$path",
            queue,
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
        errorListener: Response.ErrorListener
    ) {
        sendRequest(
            serverURL,
            Request.Method.DELETE,
            "$DELETE_ITEM_PAGE/$path",
            queue,
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
        errorListener: Response.ErrorListener
    ) {
        sendRequest(
            serverURL,
            Request.Method.GET,
            GET_VERSION_PAGE,
            queue,
            processResponse,
            failedResponse,
            errorListener
        )
    }

    // 'Static'/Class methods
    companion object {
        /**
         * Helper method that sends a request to the specified page on the server.
         *
         * @param method Request method.
         * @param page   Page (and URL parameters) to send the request to.
         * @param processResponse Listener for a successful page request.
         * @param failedResponse Listener for a failed page request.
         * @param errorListener Listener for an page request that results in an error.
         * @param postData Data to included in the POST request. Required if the request is POST.
         */
        private fun sendRequest(
            serverURL: String,
            method: Int,
            page: String,
            queue: RequestQueue,
            processResponse: (JSONObject) -> Any,
            failedResponse: (String, JSONObject) -> Any,
            errorListener: Response.ErrorListener,
            postData: HashMap<String, String>? = null,
        ) {
            // Form the full URL
            val url = "$serverURL/$page"

            // Request a string response from the provided URL
            val stringRequest: StringRequest
            if (method == Request.Method.POST) {
                stringRequest = object : StringRequest(
                    method,
                    url,
                    { response ->
                        run {
                            val json = JSONObject(response)
                            val status = json.getString("status")
                            if (status == "ok") {
                                processResponse(json)
                            } else {
                                failedResponse(status, json)
                            }
                        }
                    },
                    errorListener
                ) {
                    override fun getParams(): MutableMap<String, String>? {
                        return postData
                    }
                }
            } else {
                stringRequest = StringRequest(
                    method,
                    url,
                    { response ->
                        run {
                            val json = JSONObject(response)
                            val status = json.getString("status")
                            if (status == "ok") {
                                processResponse(json)
                            } else {
                                failedResponse(status, json)
                            }
                        }
                    },
                    errorListener
                )
            }

            // Add the request to the RequestQueue
            queue.add(stringRequest)
            Log.d("SERVER", "Sent request to $url")
        }

        /**
         * Determines whether the provided URL is a valid server URL.
         *
         * @param serverURL URL to check.
         * @param queue Request Volley queue.
         * @param listener Listener to process the result.
         */
        fun isValidURL(serverURL: String, queue: RequestQueue, listener: (Boolean) -> Unit) {
            Log.d("SERVER", "Checking if '$serverURL' is valid")
            sendRequest(
                serverURL,
                Request.Method.GET,
                PING_PAGE,
                queue,
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

        /**
         * Checks if the provided encryption password is valid.
         *
         * @param serverURL **Verified** server URL.
         * @param password Password to check.
         * @param queue Request Volley queue.
         * @param listener Listener to process the result.
         */
        fun isValidEncryptionPassword(
            serverURL: String,
            password: String,
            queue: RequestQueue,
            listener: (Boolean, EncryptionParameters?) -> Unit
        ) {
            // We need to ensure that a password is provided
            if (password.isBlank()) {
                Log.d("SERVER", "Provided password is blank")
                listener(false, null)
                return
            }

            // Otherwise we can send the request to the server
            sendRequest(
                serverURL,
                Request.Method.GET,
                GET_ENCRYPTION_PARAMS_PAGE,
                queue,
                { json ->
                    run {
                        // Split the response into the IV, the test string, and encrypted AES key
                        val iv = json.getString("iv")
                        val salt = json.getString("salt")
                        val encryptedTestString = json.getString("test_str")
                        val encryptedEncryptionKey = json.getString("encrypted_key")
                        Log.d("SERVER", "IV: $iv")
                        Log.d("SERVER", "Salt: $salt")
                        Log.d("SERVER", "Encrypted test string: $encryptedTestString")
                        Log.d("SERVER", "Encrypted encryption key: $encryptedEncryptionKey")

                        // Convert the given password into the AES
                        val userAESKey = Cryptography.genAESKey(password, salt)

                        // Attempt to decrypt the test string
                        try {
                            val attemptedDecrypt =
                                String(Cryptography.decryptAES(encryptedTestString, userAESKey, iv))
                            for (element in attemptedDecrypt) {
                                if (!element.isUpperCase()) {
                                    Log.d("SERVER", "Decryption of test text failed")
                                    listener(false, null)
                                }
                            }
                            Log.d("SERVER", "Decryption of test text successful")

                            // Since successful, decrypt the actual key that is used to encrypt all
                            // the data
                            val encryptionKey =
                                Cryptography.decryptAES(encryptedEncryptionKey, userAESKey, iv)
                            Log.d("SERVER", "Retrieved file encryption key")

                            // Create the encryption parameters object to return
                            val encryptionParameters = EncryptionParameters(
                                iv,
                                salt,
                                encryptionKey
                            )

                            listener(true, encryptionParameters)
                        } catch (e: InvalidDecryptionException) {
                            Log.d("SERVER", "Decryption failed: $e")
                            listener(false, null)
                        }
                    }
                },
                { _, _ ->
                    run {
                        Log.d("SERVER", "Invalid encryption password response")
                        listener(false, null)
                    }
                },
                { _ ->
                    run {
                        Log.d("SERVER", "Error in encryption password response")
                        listener(false, null)
                    }
                }
            )
        }
    }
}
