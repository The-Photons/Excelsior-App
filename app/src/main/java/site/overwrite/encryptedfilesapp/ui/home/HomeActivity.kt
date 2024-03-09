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
import site.overwrite.encryptedfilesapp.data.DataStoreManager
import site.overwrite.encryptedfilesapp.io.Server
import site.overwrite.encryptedfilesapp.misc.serializable
import site.overwrite.encryptedfilesapp.ui.login.Credentials
import site.overwrite.encryptedfilesapp.ui.theme.EncryptedFilesAppTheme

class HomeActivity : ComponentActivity() {
    // Properties
    private lateinit var dataStoreManager: DataStoreManager

    private lateinit var server: Server
    private lateinit var username: String

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