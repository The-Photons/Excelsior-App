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

import android.content.Context
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

const val LIST_DIR_PAGE = "list-dir"
const val GET_FILE_PAGE = "get-file"

class Server(appContext: Context, private val serverURL: String) {
    // Properties
    private val queue = Volley.newRequestQueue(appContext)

    // Helper methods
    /**
     * Helper method that sends a request to the specified page on the server.
     *
     * @param method Request method.
     * @param page   Page (and URL parameters) to send the request to.
     * @param processResponse Listener for a successful page request.
     * @param failedResponse Listener for a failed page request.
     * @param errorListener Listener for an page request that results in an error.
     */
    private fun sendRequest(
        method: Int,
        page: String,
        processResponse: (Any) -> Any,
        failedResponse: (String) -> Any,
        errorListener: Response.ErrorListener
    ) {
        // Form the full URL
        val url = "$serverURL/$page"

        // Request a string response from the provided URL
        val stringRequest = StringRequest(
            method,
            url,
            { response ->
                run {
                    val json = JSONObject(response)
                    val status = json.getString("status")
                    if (status == "ok") {
                        processResponse(json.get("content"))
                    } else {
                        failedResponse(status)
                    }
                }
            },
            errorListener
        )

        // Add the request to the RequestQueue
        queue.add(stringRequest)
    }

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
        processResponse: (Any) -> Any,
        failedResponse: (String) -> Any,
        errorListener: Response.ErrorListener
    ) {
        sendRequest(
            Request.Method.GET,
            "$LIST_DIR_PAGE/$path",
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
        processResponse: (Any) -> Any,
        failedResponse: (String) -> Any,
        errorListener: Response.ErrorListener
    ) {
        sendRequest(
            Request.Method.GET,
            "$GET_FILE_PAGE/$path",
            processResponse,
            failedResponse,
            errorListener
        )
    }
}
