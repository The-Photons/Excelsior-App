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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import site.overwrite.encryptedfilesapp.src.DataStoreManager
import site.overwrite.encryptedfilesapp.ui.theme.EncryptedFilesAppTheme

// ENUMS
enum class UploadBlockSize {
    BLOCK_SIZE_1024 {
        override fun toString(): String {
            return "1024 B (1 KiB)"
        }
    },
    BLOCK_SIZE_2048 {
        override fun toString(): String {
            return "2048 B (2 KiB)"
        }
    },
    BLOCK_SIZE_4096 {
        override fun toString(): String {
            return "4096 B (4 KiB)"
        }
    },
    BLOCK_SIZE_8192 {
        override fun toString(): String {
            return "8192 B (8 KiB)"
        }
    }
}

// MAIN ACTIVITY
class SettingsActivity : ComponentActivity() {
    // Properties
    private lateinit var dataStoreManager: DataStoreManager

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("SETTINGS", "Settings activity onCreate")
        super.onCreate(savedInstanceState)

        // Prevent screen rotate
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Get the data store manager
        dataStoreManager = DataStoreManager(applicationContext)

        // Then set the content
        setContent {
            EncryptedFilesAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    SettingsStuff()
                }
            }
        }
    }

    // Composables
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    @Preview
    fun SettingsStuff() {
        // Attributes
        var isUploadBlockSizeMenuExpanded by remember { mutableStateOf(false) }
        var selectedUploadBlockSize by remember { mutableStateOf(UploadBlockSize.BLOCK_SIZE_1024) }

        // Helper functions
        /**
         * Gets the settings from the data store and updates the current page with the settings
         * obtained.
         */
        fun getSettings() {
            dataStoreManager.getUploadBlockSize().asLiveData(Dispatchers.Main)
                .observe(this) { uploadBlockSize -> selectedUploadBlockSize = uploadBlockSize }
        }

        /**
         * Updates the settings in the data store using the settings on the current page.
         */
        fun setSettings() {
            runBlocking {
                dataStoreManager.setUploadBlockSize(selectedUploadBlockSize)
            }
        }

        // Main UI
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = {
                            setSettings()
                            finish()
                        }) {
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
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Upload file block size:")
                    ExposedDropdownMenuBox(
                        expanded = isUploadBlockSizeMenuExpanded,
                        onExpandedChange = {
                            isUploadBlockSizeMenuExpanded = !isUploadBlockSizeMenuExpanded
                        }
                    ) {
                        TextField(
                            value = selectedUploadBlockSize.toString(),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUploadBlockSizeMenuExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = isUploadBlockSizeMenuExpanded,
                            onDismissRequest = { isUploadBlockSizeMenuExpanded = false }
                        ) {
                            UploadBlockSize.entries.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(text = item.toString()) },
                                    onClick = {
                                        selectedUploadBlockSize = item
                                        isUploadBlockSizeMenuExpanded = false
                                        Log.d("SETTINGS", "Changed upload block size to '$item'")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Now update the UI with the settings
        getSettings()
    }
}