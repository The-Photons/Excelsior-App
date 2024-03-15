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

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import site.overwrite.encryptedfilesapp.Server
import site.overwrite.encryptedfilesapp.data.ItemType
import site.overwrite.encryptedfilesapp.ui.theme.EncryptedFilesAppTheme
import site.overwrite.encryptedfilesapp.ui.utils.Dialogs

// Composables
@Composable
fun HomeScreen(
    server: Server? = null,
    username: String = "",
    password: String = "",
    homeViewModel: HomeViewModel = viewModel()
) {
    val homeViewUIState by homeViewModel.uiState.collectAsState()

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp)
    ) {
        Text("Hello ${homeViewUIState.username}!")
        Text("Using server ${homeViewUIState.server.serverURL}.")
        if (homeViewModel.loggedIn) {
            Text("You are logged in!")
        } else {
            Text("Logging you in...")
        }
    }

    // On first showing, login
    LaunchedEffect(Unit) {
        if (server != null) {
            homeViewModel.loginToServer(
                server = server,
                username = username,
                password = password
            )
        }
    }
}

@Composable
fun DirectoryItem(
    dirPath: String,
    prevDir: String,
    name: String,
    type: ItemType,
    sizeString: String,
) {
    val isPreviousDirectoryItem = type == ItemType.PREVIOUS_DIRECTORY_MARKER

    var isDropdownExpanded by remember { mutableStateOf(false) }
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }

    TextButton(
        shape = RoundedCornerShape(0),
        onClick = { /*TODO*/ }
    ) {
        // Get the correct icon and description to display
        val icon: ImageVector
        val description: String
        when (type) {
            ItemType.FILE -> {
                icon = Icons.AutoMirrored.Default.InsertDriveFile
                description = "File"
            }

            ItemType.DIRECTORY -> {
                icon = Icons.Default.Folder
                description = "Folder"
            }

            ItemType.PREVIOUS_DIRECTORY_MARKER -> {
                icon = Icons.AutoMirrored.Default.ArrowBack
                description = "Back"
            }
        }

        Row {
            // TODO: Handle synced status
//            if (isPreviousDirectoryItem) {
//                Spacer(Modifier.size(24.dp))
//            } else {
//                if (isSynced) {
//                    Icon(Icons.Filled.CloudDone, "Synced", modifier = Modifier.size(24.dp))
//                } else {
//                    Icon(Icons.Outlined.Cloud, "Unsynced", modifier = Modifier.size(24.dp))
//                }
//            }
            Spacer(Modifier.size(24.dp))

            Spacer(Modifier.size(10.dp))
            Icon(icon, description)
            Spacer(Modifier.size(4.dp))
            Text(name)
            Spacer(Modifier.weight(1f))
            Text(sizeString)
            Spacer(Modifier.size(4.dp))
            Box {
                if (isPreviousDirectoryItem) {
                    Spacer(Modifier.size(24.dp))
                } else {
                    IconButton(
                        modifier = Modifier.size(24.dp),
                        onClick = { isDropdownExpanded = !isDropdownExpanded }
                    ) {
                        Icon(Icons.Default.MoreVert, "More")
                    }
                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        val path = "$dirPath/$name"
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Sync, "Sync") },
                            text = { Text("Sync") },
                            enabled = true,  // TODO: Change condition
                            onClick = {
                                /* TODO */
                                isDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    "Delete From Device"
                                )
                            },
                            text = { Text("Delete From Device") },
                            enabled = true,  // TODO: Change condition
                            onClick = {
                                /* TODO */
                                isDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Clear,
                                    "Delete From Server"
                                )
                            },
                            text = { Text("Delete From Server") },
                            onClick = {
                                showConfirmDeleteDialog = true
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        if (showConfirmDeleteDialog) {
            Dialogs.YesNoDialog(
                icon = Icons.Default.Warning,
                iconDesc = "Warning",
                dialogTitle = "Confirm Deletion",
                dialogContent = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            "Are you sure that you want to delete the $type '$name' " +
                                    "from the server?"
                        )
                        if (type == ItemType.DIRECTORY) {
                            Text("This action also deletes all files within the directory.")
                        }
                        Text("This action is irreversible!", fontWeight = FontWeight.Bold)
                    }
                },
                onYes = {
                    showConfirmDeleteDialog = false
                    /* TODO */
                },
                onNo = { showConfirmDeleteDialog = false }
            )
        }
    }
}

// Previews
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
fun DirectoryItemPreviewLight() {
    EncryptedFilesAppTheme {
        DirectoryItem(
            dirPath = "",
            prevDir = "",
            name = "Test File 1",
            ItemType.FILE,
            "1.23 kB"
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DirectoryItemPreviewDark() {
    EncryptedFilesAppTheme {
        DirectoryItem(
            dirPath = "",
            prevDir = "",
            name = "Test File 2",
            ItemType.FILE,
            "4.56 MB"
        )
    }
}