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
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.tooling.preview.Preview
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.launch
import site.overwrite.encryptedfilesapp.src.Server
import site.overwrite.encryptedfilesapp.ui.theme.EncryptedFilesAppTheme

class MainActivity : ComponentActivity() {
    // Properties
    private lateinit var server: Server
    private lateinit var loginIntent: Intent;

    // Overridden functions
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("MAIN", "Main activity onCreate")
        super.onCreate(savedInstanceState)

        // We first need to ask for the login details, especially the encryption key
        loginIntent = Intent(this, LoginActivity::class.java);
        val getLoginCredentials =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val resultIntent: Intent? = result.data
                    val serverURL: String = resultIntent?.getStringExtra("server_url") ?: ""
                    Log.d("MAIN", "Got server URL: $serverURL")

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
                    ListFiles()
                }
            }
        }
    }

    // Composables
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    @Preview
    fun ListFiles() {
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        var filePath by remember { mutableStateOf("") }

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
            Row(
                modifier = Modifier.padding(innerPadding),
            ) {
                OutlinedTextField(
                    value = filePath,
                    onValueChange = { filePath = it },
                    singleLine = true,
                    label = { Text("File Path") })
                Button(
                    onClick = {
                        server.getFile(
                            filePath,
                            { content -> Log.d("MAIN", "Response content: $content") },
                            { status ->
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
                ) {
                    Text("Get File")
                }
            }
        }
    }
}
