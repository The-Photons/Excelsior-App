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

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.URLUtil
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.asLiveData
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import site.overwrite.encryptedfilesapp.src.DataStoreManager
import site.overwrite.encryptedfilesapp.src.Server
import site.overwrite.encryptedfilesapp.ui.theme.EncryptedFilesAppTheme

class LoginActivity : ComponentActivity() {
    // Properties
    private var serverURL by mutableStateOf("http://127.0.0.1:5000")
    private lateinit var queue: RequestQueue

    private lateinit var dataStoreManager: DataStoreManager

    // Overridden methods
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("LOGIN", "Login activity onCreate")
        super.onCreate(savedInstanceState)

        // Update properties
        queue = Volley.newRequestQueue(applicationContext)

        // Get things from the data store
        dataStoreManager = DataStoreManager(applicationContext)
        dataStoreManager.getServerURL().asLiveData(Dispatchers.Main)
            .observe(this) { url -> serverURL = url }

        setContent {
            EncryptedFilesAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ServerAddress()
                }
            }
        }
    }

    // Composables
    @OptIn(ExperimentalMaterial3Api::class)
    @Preview
    @Composable
    fun ServerAddress() {
        var isErrorServerURL by remember { mutableStateOf(false) }

        var userPassword by remember { mutableStateOf("password") }  // FIXME: REMOVE!!!
        var isErrorPassword by remember { mutableStateOf(false) }
        var isPasswordVisible by remember { mutableStateOf(false) }

        var isLoading by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    colors = topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        Text("Login")
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                OutlinedTextField(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    value = serverURL,
                    singleLine = true,
                    onValueChange = {
                        serverURL = it
                        isErrorServerURL = !URLUtil.isValidUrl(serverURL)
                    },
                    isError = isErrorServerURL,
                    supportingText = {
                        if (isErrorServerURL) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = "Invalid URL",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    label = { Text("Server Address") }
                )
                OutlinedTextField(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    value = userPassword,
                    singleLine = true,
                    placeholder = { Text("Password") },
                    onValueChange = { userPassword = it },
                    supportingText = {
                        if (isErrorPassword) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = "Incorrect Password",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    label = { Text("Encryption Key") },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val image =
                            if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val description =
                            if (isPasswordVisible) "Hide Password" else "Show Password"

                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(imageVector = image, description)
                        }
                    }
                )
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Button(
                        onClick = {
                            Log.d("LOGIN", "Login button clicked")
                            isLoading = true
                            Server.isValidURL(serverURL, queue) { isValid ->
                                run {
                                    if (!isValid) {
                                        isErrorServerURL = true
                                        Log.d("LOGIN", "Invalid server URL: $serverURL")
                                        isLoading = false
                                    } else {
                                        Log.d("LOGIN", "Good URL: $serverURL")

                                        // Update the saved URL
                                        runBlocking {
                                            dataStoreManager.setServerURL(serverURL)
                                        }

                                        // Now check the password
                                        Server.isValidEncryptionPassword(
                                            serverURL,
                                            userPassword,
                                            queue
                                        ) { isValid, encryptionParameters ->
                                            run {
                                                if (!isValid || encryptionParameters == null) {
                                                    Log.d("LOGIN", "Password is invalid")
                                                    isErrorPassword = true
                                                    isLoading = false
                                                } else {
                                                    Log.d("LOGIN", "Password is valid")

                                                    // Return needed things
                                                    val resultIntent = Intent()
                                                    resultIntent.putExtra("server_url", serverURL)
                                                    resultIntent.putExtra(
                                                        "iv",
                                                        encryptionParameters.iv
                                                    )
                                                    resultIntent.putExtra(
                                                        "salt",
                                                        encryptionParameters.salt
                                                    )
                                                    resultIntent.putExtra(
                                                        "encryption_key",
                                                        encryptionParameters.encryptionKey
                                                    )

                                                    setResult(RESULT_OK, resultIntent)
                                                    isLoading = false
                                                    finish()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Login")
                    }

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(32.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
