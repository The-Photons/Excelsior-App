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
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.runBlocking
import site.overwrite.encryptedfilesapp.Server
import site.overwrite.encryptedfilesapp.data.Credentials
import site.overwrite.encryptedfilesapp.data.DataStoreManager
import site.overwrite.encryptedfilesapp.serializable
import site.overwrite.encryptedfilesapp.ui.home.services.AppIsActiveService
import site.overwrite.encryptedfilesapp.ui.theme.EncryptedFilesAppTheme

class HomeActivity : ComponentActivity() {
    // Properties
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var homeViewModel: HomeViewModel

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("HOME", "onCreate called")
        super.onCreate(savedInstanceState)

        // Prevent screen rotate
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Get the data passed in from the login view
        val credentials: Credentials = intent.serializable("credentials")!!
        val server = Server(credentials.serverURL)
        val username = credentials.username
        val password = credentials.password

        // Update server URL and username
        dataStoreManager = DataStoreManager(applicationContext)
        runBlocking {
            dataStoreManager.setServerURL(credentials.serverURL)
            dataStoreManager.setUsername(credentials.username)
        }

        // Start services
        // TODO: Allow return to app on press on notification
        val appIsActiveIntent = Intent(this, AppIsActiveService::class.java)
        appIsActiveIntent.putExtra("credentials", credentials)
        startForegroundService(appIsActiveIntent)

        // Then set the content
        setContent {
            EncryptedFilesAppTheme {
                homeViewModel = viewModel()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(
                        server = server,
                        username = username,
                        password = password,
                        homeViewModel = homeViewModel
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("close_app", false)) {
            finish()
        }
    }

    override fun onDestroy() {
        Log.d("HOME", "onDestroy called")

        stopService(Intent(this, AppIsActiveService::class.java))
        homeViewModel.logout(this)

        super.onDestroy()
    }
}
