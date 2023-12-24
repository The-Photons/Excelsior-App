/*
 * Copyright (c) 2023 Kan Onn Kit.
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

package site.overwrite.encryptedfilesapp.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.tooling.preview.Preview
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.launch
import org.json.JSONArray
import site.overwrite.encryptedfilesapp.src.Cryptography
import site.overwrite.encryptedfilesapp.src.Server
import site.overwrite.encryptedfilesapp.ui.theme.EncryptedFilesAppTheme

class MainActivity : ComponentActivity() {
    // Properties
    private lateinit var server: Server
    private lateinit var loginIntent: Intent

    private lateinit var encryptionIV: String
    private lateinit var encryptionSalt: String
    private lateinit var encryptionKey: ByteArray

    // Overridden functions
    @OptIn(ExperimentalStdlibApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("MAIN", "Main activity onCreate")
        super.onCreate(savedInstanceState)

        // We first need to ask for the login details, especially the encryption key
        loginIntent = Intent(this, LoginActivity::class.java);
        val getLoginCredentials =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    // Retrieve data from the result intent
                    val resultIntent: Intent? = result.data
                    val serverURL: String = resultIntent?.getStringExtra("server_url") ?: ""
                    encryptionIV = resultIntent?.getStringExtra("iv") ?: ""
                    encryptionSalt = resultIntent?.getStringExtra("salt") ?: ""
                    encryptionKey =
                        resultIntent?.getByteArrayExtra("encryption_key") ?: ByteArray(0)
                    Log.d(
                        "MAIN",
                        "Got URL '$serverURL', IV '$encryptionIV', salt '$encryptionSalt', " +
                                "and encryption key (as hex string) '${encryptionKey.toHexString()}'"
                    )

                    // Now initialize the things needed
                    val queue = Volley.newRequestQueue(applicationContext)
                    server = Server(queue, serverURL)
                }
            }
        getLoginCredentials.launch(loginIntent)

        setContent {
            EncryptedFilesAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FilesList()
                }
            }
        }
    }

    // Composables
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    @Preview
    fun FilesList() {
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        var filePath by remember { mutableStateOf("file1.txt") }  // FIXME: REMOVE!!!
        var dirPath by remember { mutableStateOf("") }

        var dirItems by remember { mutableStateOf(JSONArray()) }

        // Helper functions
        fun processSuccessfulResponse(encryptedFileContent: String) {
            // Decrypt the data
            val fileData =
                Cryptography.decryptAES(encryptedFileContent, encryptionKey, encryptionIV)

            // TODO: CONTINUE
            Log.d("MAIN", "File data: ${String(fileData)}")
            scope.launch { snackbarHostState.showSnackbar(String(fileData)) }
        }

        fun getFile(path: String) {
            Log.d("MAIN", "Getting file: '$path'")
            server.getFile(
                path,
                { json ->
                    run {
                        val rawBase64Content = json.getString("content")
                        val innerContent = String(Base64.decode(rawBase64Content, Base64.DEFAULT))
                        Log.d("MAIN", "File content: $innerContent")
                        processSuccessfulResponse(innerContent)
                    }
                },
                { status, _ ->
                    run {
                        Log.d("MAIN", "Failed request: $status")
                        scope.launch { snackbarHostState.showSnackbar(status) }
                    }
                },
                { error ->
                    run {
                        Log.d("MAIN", "Request had error: $error")
                        scope.launch { snackbarHostState.showSnackbar(error.toString()) }
                    }
                }
            )
        }

        fun getItemsInDir(dir: String) {
            server.listFiles(
                dir,
                {json -> run {
                    val itemsInDirStr = json.getString("content")

                    // If the items is null, then the directory does not exist
                    if (itemsInDirStr == "null") {
                        dirItems = JSONArray()
                    } else {
                        dirItems = JSONArray(itemsInDirStr)
                        Log.d("MAIN", "Num items in dir: ${dirItems.length()}")

                        // TODO: Continue
                    }
                }},
                {status, json -> run {
                    Log.d("MAIN", "Failed to list items in directory")
                }},
                {error -> run {
                    Log.d("MAIN", "Error when getting items in directory: $error")
                }}
            )
        }

        // Main UI
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        Text("Files")
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)){
                Row{
                    OutlinedTextField(
                        value = filePath,
                        onValueChange = { filePath = it },
                        singleLine = true,
                        label = { Text("File Path") })
                    Button(
                        onClick = { getFile(filePath) }
                    ) {
                        Text("Get File")
                    }
                }
                Row{
                    OutlinedTextField(
                        value = dirPath,
                        onValueChange = { dirPath = it },
                        singleLine = true,
                        label = { Text("Directory") })
                    Button(
                        onClick = { getItemsInDir(dirPath)}
                    ) {
                        Text("List Files")
                    }
                }
                Column(modifier = Modifier.padding(innerPadding)) {
                    if (dirPath != "") {
                        Text("Previous directory")
                    }
                    for (i in 0..<dirItems.length()) {
                        // Get the specific item
                        val item = dirItems.getJSONArray(i)
                        val name = item[0]
                        val type = item[1]

                        Text("$name ($type)")
                    }
                }
            }
        }
    }
}
