/*
 * Copyright (c) 2024 Kan Onn Kit.
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

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import site.overwrite.encryptedfilesapp.src.Server
import site.overwrite.encryptedfilesapp.ui.theme.EncryptedFilesAppTheme

/**
 * About page for the application.
 */
class AboutActivity : ComponentActivity() {
    // Attributes
    private lateinit var server: Server
    private lateinit var username: String

    private var appVersion = ""

    // Overridden methods
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("ABOUT", "About activity onCreate")
        super.onCreate(savedInstanceState)

        // Prevent screen rotate
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Set the server and username
        server = Server(intent?.getStringExtra("server_url") ?: "")
        username = intent?.getStringExtra("username") ?: ""

        // Get app version
        appVersion = packageManager
            .getPackageInfo(packageName, 0)
            .versionName

        // Then set the content
        setContent {
            EncryptedFilesAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AboutInfo(
                        username = username,
                        serverURL = server.serverURL,
                        appVersion = appVersion
                    )
                }
            }
        }
    }

    // Composables
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AboutInfo(
        username: String,
        serverURL: String,
        appVersion: String,
        serVersion: String = ""
    ) {
        // Attributes
        var serverVersion by remember { mutableStateOf(serVersion) }

        // Main UI
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = { Text("About") },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text("Logged in as $username into server $serverURL")
                Spacer(modifier = Modifier.padding(vertical = 8.dp))
                Text("Application version: $appVersion")
                Text("Server version: $serverVersion")
                Spacer(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    "Source code for Excelsior is licensed under GNU General Public Licence " +
                            "Version 3."
                )
            }
        }

        // Get the server version
        LaunchedEffect(Unit) {
            server.getServerVersion(
                { json ->
                    serverVersion = json.getString("version")
                    Log.d("ABOUT", "Server version: $serverVersion")
                },
                { _, _ -> Log.d("ABOUT", "Failed to get version of server") },
                { error -> Log.d("ABOUT", "Error when getting version of server: $error") }
            )
        }
    }

    // Previews
    @Composable
    @Preview
    fun AboutInfoPreview() {
        EncryptedFilesAppTheme {
            AboutInfo(
                "Username123",
                "https://example.com/",
                "A.B.C",
                "D.E.F"
            )
        }
    }
}
