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
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import site.overwrite.encryptedfilesapp.Server
import site.overwrite.encryptedfilesapp.data.ItemType
import site.overwrite.encryptedfilesapp.data.RemoteDirectory
import site.overwrite.encryptedfilesapp.data.RemoteFile
import site.overwrite.encryptedfilesapp.data.RemoteItem
import site.overwrite.encryptedfilesapp.data.RemotePreviousDirectory
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
    val context = LocalContext.current
    val homeViewUIState by homeViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = { HomeTopBar(homeViewModel) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = { AddItemActionButton() },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        if (!homeViewModel.loggedIn) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.width(48.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
            return@Scaffold
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            val items = homeViewUIState.activeDirectory.items.toCollection(ArrayList())
            if (homeViewUIState.activeDirectory.parentDir != null) {
                items.add(0, RemotePreviousDirectory())
            }
            for (item in items) {
                DirectoryItem(
                    item = item,
                    onClick = { homeViewModel.directoryItemOnClick(item) }
                )
            }
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

    // If there is a toast message to show, show it
    if (homeViewUIState.toastMessage != "") {
        Toast.makeText(context, homeViewUIState.toastMessage, Toast.LENGTH_LONG).show()
        homeViewModel.setToastMessage("")
    }
}

// Scaffold composables
@Composable
fun HomeTopBar(
    homeViewModel: HomeViewModel
) {
    val homeViewUIState by homeViewModel.uiState.collectAsState()

    var topBarName = homeViewUIState.activeDirectory.path
    if (topBarName == "") {
        topBarName = "Home"
    }

    HomeTopBar(
        name = topBarName,
        setShowConfirmLogoutDialog = { homeViewModel.showConfirmLogoutDialog = it }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    name: String,
    setShowConfirmLogoutDialog: (Boolean) -> Unit
) {
    // TODO: Incorporate previous directory action here

    var showExtrasMenu by remember { mutableStateOf(false) }
    TopAppBar(
        colors = topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = {
            Text(
                name,
                overflow = TextOverflow.Visible,
                maxLines = 1
            )
        },
        actions = {
            IconButton(onClick = { showExtrasMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More"
                )
            }
            DropdownMenu(
                expanded = showExtrasMenu,
                onDismissRequest = { showExtrasMenu = false }
            ) {
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.AutoMirrored.Default.Logout, "Logout") },
                    text = { Text("Logout") },
                    onClick = { setShowConfirmLogoutDialog(true) }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.Info, "About") },
                    text = { Text("About") },
                    onClick = { /*TODO*/ }
                )
            }
        }
    )
}

@Composable
fun AddItemActionButton() {
    var dropdownExpanded by remember { mutableStateOf(false) }

    FloatingActionButton(
        modifier = Modifier.padding(all = 16.dp),
        onClick = { dropdownExpanded = !dropdownExpanded },
    ) {
        Icon(Icons.Default.Add, "Add")
        DropdownMenu(
            expanded = dropdownExpanded,
            onDismissRequest = { dropdownExpanded = false }
        ) {
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.AutoMirrored.Default.NoteAdd, "Add File") },
                text = { Text("Add File") },
                onClick = {
                    /* TODO */
//                    pickFileLauncher.launch("*/*")
                }
            )
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Default.CreateNewFolder, "Create Folder") },
                text = { Text("Create Folder") },
                onClick = {
                    /* TODO */
//                    showCreateFolderInputDialog = true
                }
            )
        }
    }
}

// Main composables
/**
 * Composable that represents an item that is in the active directory.
 *
 * @param item Remote item that this composable is representing.
 * @param onClick Function to run when the item is clicked.
 */
@Composable
fun DirectoryItem(
    item: RemoteItem,
    onClick: (RemoteItem) -> Unit
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }

    TextButton(
        shape = RoundedCornerShape(0),
        modifier = Modifier.height(50.dp),
        onClick = { onClick(item) }
    ) {
        // Get the correct icon and description to display
        val icon: ImageVector
        val description: String
        when (item.type) {
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

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.type == ItemType.PREVIOUS_DIRECTORY_MARKER) {
                Spacer(Modifier.size(34.dp))
                Icon(icon, description)
                Spacer(Modifier.size(4.dp))
                Text("Previous Directory")
                Spacer(Modifier.weight(1f))
                return@TextButton
            }

            if (item.synced) {
                Icon(Icons.Filled.CloudDone, "Synced", modifier = Modifier.size(24.dp))
            } else {
                Icon(Icons.Outlined.Cloud, "Unsynced", modifier = Modifier.size(24.dp))
            }

            Spacer(Modifier.size(10.dp))
            Icon(icon, description)
            Spacer(Modifier.size(4.dp))
            Text(
                item.name,
                modifier = Modifier.width(200.dp),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
            Spacer(Modifier.weight(1f))
            Text(item.formattedSize())
            Spacer(Modifier.size(4.dp))
            Box {
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
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Sync, "Sync") },
                        text = { Text("Sync") },
                        enabled = !item.synced,
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
                        enabled = item.synced,
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
                            "Are you sure that you want to delete the ${item.type} '${item.name}' " +
                                    "from the server?"
                        )
                        if (item.type == ItemType.DIRECTORY) {
                            Text("This action also deletes all files within the directory.")
                        }
                        Text("This action is irreversible!", fontWeight = FontWeight.Bold)
                    }
                },
                onYes = {
                    /* TODO */
                    showConfirmDeleteDialog = false
                },
                onNo = { showConfirmDeleteDialog = false }
            )
        }
    }
}

// Previews
@Preview
@Composable
fun HomeTopBarPreview() {
    EncryptedFilesAppTheme {
        HomeTopBar("Testing Name") {}
    }
}

@Preview
@Composable
fun AddItemButtonPreview() {
    EncryptedFilesAppTheme {
        AddItemActionButton()
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
fun DirectoryFilePreview() {
    EncryptedFilesAppTheme {
        DirectoryItem(
            RemoteFile(
                "Test File.txt",
                "dir1/dir2/subdir3/Test File.txt",
                1234,
                null
            )
        ) {}
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
fun DirectoryFolderPreview() {
    EncryptedFilesAppTheme {
        DirectoryItem(
            RemoteDirectory(
                "Test Folder",
                "dir1/dir2/subdir3",
                7891011,
                emptyArray(),
                emptyArray(),
                null
            )
        ) {}
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
fun PreviousDirectoryItemPreview() {
    EncryptedFilesAppTheme {
        DirectoryItem(
            RemotePreviousDirectory()
        ) {}
    }
}