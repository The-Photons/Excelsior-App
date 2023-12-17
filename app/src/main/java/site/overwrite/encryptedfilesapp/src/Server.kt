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
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

/**
 * site.overwrite.encryptedfilesapp.src was created by Guest1 on 17/12/23,19:44 in Encrypted Files App. Read the
copyright above to avoid consequences.
 */
class Server(appContext: Context, serverAddress: String) {
    // Constants
    private val LIST_DIR_PAGE = "list-dir"
    private val GET_FILE_PAGE = "get-file"

    // Properties
    private val serverURL = "http://$serverAddress"
    private val queue = Volley.newRequestQueue(appContext)

    // Helper methods
    /**
     * Helper method that sends a request to the specified page on the server.
     *
     * @param method Request method.
     * @param page   Page (and URL parameters) to send the request to.
     * @param listener Listener for a successful page request.
     * @param errorListener Listener for an page request that results in an error.
     */
    private fun sendRequest(
        method: Int,
        page: String,
        listener: Response.Listener<String>,
        errorListener: Response.ErrorListener
    ) {
        // Form the full URL
        val url = "$serverURL/$page"

        // Request a string response from the provided URL
        val stringRequest = StringRequest(method, url, listener, errorListener)

        // Add the request to the RequestQueue
        queue.add(stringRequest)
    }

    // Main methods
    /**
     * Gets the list of files in the path.
     *
     * @param path Path to the directory.
     * @param listener Listener for a successful page request.
     * @param errorListener Listener for an page request that results in an error.
     */
    fun listFiles(
        path: String,
        listener: Response.Listener<String>,
        errorListener: Response.ErrorListener
    ) {
        sendRequest(Request.Method.GET, "$LIST_DIR_PAGE/$path", listener, errorListener)
    }

    /**
     * Gets the contents of a file.
     *
     * @param path Path to the file.
     * @param listener Listener for a successful page request.
     * @param errorListener Listener for an page request that results in an error.
     */
    fun getFile(
        path: String, listener: Response.Listener<String>,
        errorListener: Response.ErrorListener
    ) {
        sendRequest(Request.Method.GET, "$GET_FILE_PAGE/$path", listener, errorListener)
    }
}
