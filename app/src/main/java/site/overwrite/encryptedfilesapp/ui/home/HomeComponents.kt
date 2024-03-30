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
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.SnackbarResult
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
import site.overwrite.encryptedfilesapp.file.Pathing
import site.overwrite.encryptedfilesapp.ui.Dialogs
import site.overwrite.encryptedfilesapp.ui.theme.EncryptedFilesAppTheme

// Main composable
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

    var topBarName = homeViewUIState.activeDirectory.name
    if (topBarName == "") {
        topBarName = "Home"
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                name = topBarName,
                hasPreviousDirectory = !homeViewUIState.atRootDirectory,
                previousDirectoryOnClick = { homeViewModel.goToPreviousDirectory() },
                setShowConfirmLogoutDialog = { homeViewModel.showLogoutDialog = it },
                onClickSyncCurrentDirectory = { homeViewModel.syncItem(homeViewUIState.activeDirectory) }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            AddItemActionButton(
                onClickCreateFile = { homeViewModel.createFileOnServer(it) },
                onClickCreateFolder = { homeViewModel.showCreateFolderDialog = true }
            )
        },
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
            for (item in homeViewUIState.activeDirectory.items) {
                if (!item.markedForServerDeletion) {
                    DirectoryItem(
                        item = item,
                        onClick = { homeViewModel.directoryItemOnClick(item) },
                        onSyncRequest = { homeViewModel.syncItem(item) },
                        onLocalDeleteRequest = { homeViewModel.deleteItemLocally(item) },
                        onServerDeleteRequest = { homeViewModel.deleteItemFromServer(item) }
                    )
                }
            }
        }
    }

    // Dialogs
    if (homeViewModel.showCreateFolderDialog) {
        CreateFolderDialog(
            textFieldValidator = { Pathing.isValidFolderName(it) },
            onDismiss = { homeViewModel.showCreateFolderDialog = false },
            onConfirmFolderName = {
                homeViewModel.showCreateFolderDialog = false
                homeViewModel.createFolderOnServer(it)
            }
        )
    }

    if (homeViewModel.showProcessingDialog) {
        ProcessingDialog(
            title = homeViewModel.processingDialogTitle,
            subtitle = homeViewModel.processingDialogSubtitle,
            progress = homeViewModel.processingDialogProgress
        )
    }

    if (homeViewModel.showLogoutDialog) {
        LogoutDialog(
            hideDialog = { homeViewModel.showLogoutDialog = false }
        ) {
            homeViewModel.logout(homeViewUIState.server, context)
        }
    }

    // Set up back button handling
    BackHandler {
        if (homeViewUIState.atRootDirectory) {
            homeViewModel.showLogoutDialog = true
            // TODO: Enforce logout. Perhaps through persistent notification?
        } else {
            homeViewModel.goToPreviousDirectory()
        }
    }

    // On first showing, login
    LaunchedEffect(Unit) {
        if (server != null) {
            homeViewModel.login(
                server = server,
                username = username,
                password = password
            )
        }
    }

    // Show any displayables
    LaunchedEffect(homeViewUIState.toastData) {
        if (homeViewUIState.toastData.isEmpty) return@LaunchedEffect
        Toast.makeText(
            context,
            homeViewUIState.toastData.message,
            homeViewUIState.toastData.duration
        ).show()
        homeViewModel.clearToast()
    }

    LaunchedEffect(homeViewUIState.snackbarData) {
        if (homeViewUIState.snackbarData.isEmpty) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            homeViewUIState.snackbarData.message,
            homeViewUIState.snackbarData.actionLabel,
            homeViewUIState.snackbarData.withDismissAction,
            homeViewUIState.snackbarData.duration
        )
        when (result) {
            SnackbarResult.Dismissed -> {
                if (homeViewUIState.snackbarData.onDismiss != null) {
                    homeViewUIState.snackbarData.onDismiss!!()
                }
            }

            SnackbarResult.ActionPerformed -> {
                if (homeViewUIState.snackbarData.onAction != null) {
                    homeViewUIState.snackbarData.onAction!!()
                }
            }
        }
        homeViewModel.clearSnackbar()
    }
}

// Scaffold composables
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    name: String,
    hasPreviousDirectory: Boolean,
    previousDirectoryOnClick: () -> Unit,
    setShowConfirmLogoutDialog: (Boolean) -> Unit,
    onClickSyncCurrentDirectory: () -> Unit
) {
    var showExtrasMenu by remember { mutableStateOf(false) }
    TopAppBar(
        colors = topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        navigationIcon = {
            if (!hasPreviousDirectory) return@TopAppBar
            IconButton(
                onClick = previousDirectoryOnClick
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
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
                    leadingIcon = { Icon(Icons.Default.Sync, "Sync") },
                    text = { Text("Sync Directory") },
                    onClick = {
                        onClickSyncCurrentDirectory()
                        showExtrasMenu = false
                    }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.Info, "About") },
                    text = { Text("About") },
                    onClick = {
                        /* TODO: Show about page */
                        showExtrasMenu = false
                    }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.AutoMirrored.Default.Logout, "Logout") },
                    text = { Text("Logout") },
                    onClick = {
                        setShowConfirmLogoutDialog(true)
                        showExtrasMenu = false
                    }
                )
            }
        }
    )
}

@Composable
fun AddItemActionButton(
    onClickCreateFile: (Uri) -> Unit,
    onClickCreateFolder: () -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    val pickFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) onClickCreateFile(uri) }

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
                    // TODO: Allow to upload multiple files at once
                    pickFileLauncher.launch("*/*")
                    dropdownExpanded = false
                }
            )
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Default.CreateNewFolder, "Create Folder") },
                text = { Text("Create Folder") },
                onClick = {
                    onClickCreateFolder()
                    dropdownExpanded = false
                }
            )
        }
    }
}

// Dialogs
@Composable
fun CreateFolderDialog(
    textFieldValidator: (String) -> Boolean,
    onDismiss: () -> Unit,
    onConfirmFolderName: (String) -> Unit,
) {
    Dialogs.TextInputDialog(
        dialogTitle = "Enter Folder Name",
        textFieldLabel = "Name",
        textFieldPlaceholder = "Name of the folder",
        textFieldErrorText = "Invalid folder name",
        onConfirmation = { onConfirmFolderName(it) },
        onDismissal = { onDismiss() },
        textFieldValidator = { textFieldValidator(it) }
    )
}

@Composable
fun ProcessingDialog(
    title: String,
    subtitle: String = "",
    progress: Float?
) {
    Dialogs.ProgressIndicatorDialog(
        dialogTitle = title,
        dialogSubtitle = subtitle,
        progress = progress
    )
}

@Composable
fun LogoutDialog(
    hideDialog: () -> Unit,
    handleLogout: () -> Unit
) {
    Dialogs.YesNoDialog(
        icon = Icons.AutoMirrored.Default.Logout,
        iconDesc = "Logout",
        dialogTitle = "Confirm Logout",
        dialogContent = {
            Text("Are you sure that you want to log out?")
        },
        onYes = {
            hideDialog()
            handleLogout()
        },
        onNo = { hideDialog() }
    )
}

// Main composables
/**
 * Composable that represents an item that is in the active directory.
 *
 * @param item Remote item that this composable is representing.
 * @param onClick Function to run when the item is clicked.
 * @param onSyncRequest Function to run when the sync button is clicked.
 * @param onLocalDeleteRequest Function to run when the delete from device button is clicked.
 * @param onServerDeleteRequest Function to run when the delete from server button is clicked.
 */
@Composable
fun DirectoryItem(
    item: RemoteItem,
    onClick: (RemoteItem) -> Unit,
    onSyncRequest: () -> Unit,
    onLocalDeleteRequest: () -> Unit,
    onServerDeleteRequest: () -> Unit
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
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                            onSyncRequest()
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
                            onLocalDeleteRequest()
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

                    // TODO: Add renaming of items
                    // TODO: Add moving of items
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
                            "Are you sure that you want to delete the " +
                                    "${item.type.toString().lowercase()} '${item.name}' from the " +
                                    "server?"
                        )
                        if (item.type == ItemType.DIRECTORY) {
                            Text("This action also deletes all files within the directory.")
                        }
                        Text("This action is irreversible!", fontWeight = FontWeight.Bold)
                    }
                },
                onYes = {
                    onServerDeleteRequest()
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
        HomeTopBar("Testing Name", true, {}, {}, {})
    }
}

@Preview
@Composable
fun AddItemButtonPreview() {
    EncryptedFilesAppTheme {
        AddItemActionButton({}, {})
    }
}

@Preview
@Composable
fun CreateFolderDialogPreview() {
    EncryptedFilesAppTheme {
        CreateFolderDialog({ false }, {}, {})
    }
}

@Preview
@Composable
fun LogoutDialogPreview() {
    EncryptedFilesAppTheme {
        LogoutDialog({}, {})
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
fun DirectoryFilePreview() {
    EncryptedFilesAppTheme {
        DirectoryItem(
            RemoteFile(
                "Test File.txt",
                "",  // Need to be blank for testing
                1234,
                null
            ),
            {},
            {},
            {},
            {}
        )
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
            ),
            {},
            {},
            {},
            {}
        )
    }
}
