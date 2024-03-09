/*
 * Copyright (c) 2024 PhotonicGluon.
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

package site.overwrite.encryptedfilesapp.ui.home

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.runBlocking
import site.overwrite.encryptedfilesapp.data.Cryptography
import site.overwrite.encryptedfilesapp.data.DataStoreManager
import site.overwrite.encryptedfilesapp.io.Server
import site.overwrite.encryptedfilesapp.misc.serializable
import site.overwrite.encryptedfilesapp.ui.login.Credentials
import site.overwrite.encryptedfilesapp.ui.theme.EncryptedFilesAppTheme

class HomeActivity : ComponentActivity() {
    // Properties
    private var loggedIn = false
    private lateinit var dataStoreManager: DataStoreManager

    private lateinit var server: Server
    private lateinit var username: String

    private lateinit var encryptionIV: String
    private lateinit var encryptionSalt: String
    private lateinit var encryptionKey: ByteArray

    @OptIn(ExperimentalStdlibApi::class)
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent screen rotate
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Get the data passed in from the login view
        val credentials: Credentials = intent.serializable("credentials")!!
        server = Server(credentials.serverURL)
        username = credentials.username

        // Update server URL and username
        dataStoreManager = DataStoreManager(applicationContext)
        runBlocking {
            dataStoreManager.setServerURL(credentials.serverURL)
            dataStoreManager.setUsername(credentials.username)
        }

        // Actually log into the server
        server.handleLogin(
            username,
            credentials.password
        ) { _, _ ->
            server.getEncryptionParameters(
                { json ->
                    // Set the IV and salt
                    encryptionIV = json.getString("iv")
                    encryptionSalt = json.getString("salt")

                    // Convert the given password into the AES
                    val userAESKey = Cryptography.genAESKey(credentials.password, encryptionSalt)
                    encryptionKey = Cryptography.decryptAES(
                        json.getString("encrypted_key"),
                        userAESKey,
                        encryptionIV
                    )

                    // Mark that we are logged in
                    loggedIn = true
                    Log.d(
                        "MAIN",
                        "Got server URL '${credentials.serverURL}'," +
                                " initialization vector '$encryptionIV'," +
                                " salt '$encryptionSalt', and encryption key (as hex string)" +
                                " '${encryptionKey.toHexString()}'"
                    )
                },
                { _, json ->
                    Log.d(
                        "MAIN",
                        "Failed to get encryption parameters: ${json.getString("message")}"
                    )
                },
                { error ->
                    Log.d("MAIN", "Error when getting encryption parameters: $error")
                }
            )
        }

        // Then set the content
        setContent {
            EncryptedFilesAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("${credentials.username} at ${server.serverURL}")
                }
            }
        }
    }
}

// Composables
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

// Preview
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    EncryptedFilesAppTheme {
        Greeting("Android")
    }
}