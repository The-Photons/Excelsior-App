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
class Server(private val appContext: Context, serverAddress: String) {
    private val serverURL = "http://$serverAddress"

    fun listFiles() {
        // Instantiate the RequestQueue
        val queue = Volley.newRequestQueue(appContext)

        // Form the full URL
        val url = "$serverURL/list-dir"
        Log.d("STATE", url)

        // Request a string response from the provided URL.
        val stringRequest = StringRequest(
            Request.Method.GET,
            url,
            { response -> Log.d("STATE", "Response is: $response") },
            { error -> Log.d("ERROR", "Error: $error") })

        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }
}
