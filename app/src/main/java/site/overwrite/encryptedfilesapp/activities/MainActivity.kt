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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import org.json.JSONArray
import site.overwrite.encryptedfilesapp.src.Cryptography
import site.overwrite.encryptedfilesapp.src.Dialogs
import site.overwrite.encryptedfilesapp.src.IOMethods
import site.overwrite.encryptedfilesapp.src.Server
import site.overwrite.encryptedfilesapp.ui.theme.EncryptedFilesAppTheme

// CONSTANTS
const val PREVIOUS_DIRECTORY_TEXT_LABEL = "Previous Directory"
const val PREVIOUS_DIRECTORY_TYPE = "prev-dir"

val FOLDER_NAME_REGEX = Regex("[0-9A-z+\\-_= ]+")

// MAIN ACTIVITY
class MainActivity : ComponentActivity() {
    // Properties
    private var initialized = false
    private var loggedIn = false

    private lateinit var server: Server
    private lateinit var loginIntent: Intent

    private lateinit var encryptionIV: String
    private lateinit var encryptionSalt: String
    private lateinit var encryptionKey: ByteArray

    // Overridden functions
    @SuppressLint("SourceLockedOrientationActivity")
    @OptIn(ExperimentalStdlibApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("MAIN", "Main activity onCreate")
        super.onCreate(savedInstanceState)

        // Prevent screen rotate
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // We first need to ask for the login details, especially the encryption key
        loginIntent = Intent(this, LoginActivity::class.java)
        val getLoginCredentials =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val resultIntent = result.data
                    val serverURL = resultIntent?.getStringExtra("server_url") ?: ""
                    val username = resultIntent?.getStringExtra("username") ?: ""
                    val password = resultIntent?.getStringExtra("password") ?: ""

                    server = Server(serverURL)
                    server.handleLogin(username, password) {}
                    server.getEncryptionParameters(
                        { json ->
                            run {
                                // Set the IV and salt
                                encryptionIV = json.getString("iv")
                                encryptionSalt = json.getString("salt")

                                // Convert the given password into the AES
                                val userAESKey = Cryptography.genAESKey(password, encryptionSalt)
                                encryptionKey = Cryptography.decryptAES(
                                    json.getString("encrypted_key"),
                                    userAESKey,
                                    encryptionIV
                                )

                                // Mark that we are logged in
                                loggedIn = true
                                Log.d(
                                    "MAIN",
                                    "Got server URL '$serverURL', initialization vector '$encryptionIV'," +
                                            " salt '$encryptionSalt', and encryption key (as hex string)" +
                                            " '${encryptionKey.toHexString()}'"
                                )
                            }
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
            }
        getLoginCredentials.launch(loginIntent)
    }

    override fun onStart() {
        Log.d("MAIN", "Main activity onStart")
        super.onStart()

        setContent {
            EncryptedFilesAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (loggedIn) {
                        FilesList()
                    }
                }
            }
        }
    }

    // Composables
    /**
     * List of files.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun FilesList() {
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        var dirPath by remember { mutableStateOf("") }
        var prevDir by remember { mutableStateOf("") }
        var dirItems by remember { mutableStateOf(JSONArray()) }

        var isLoadingFiles by remember { mutableStateOf(false) }

        var showExtrasMenu by remember { mutableStateOf(false) }
        var showConfirmLogoutDialog by remember { mutableStateOf(false) }

        // Helper functions
        /**
         * Handle a failed connection.
         *
         * @param snackbarHostState Snackbar host state.
         * @param error Error thrown when attempting network connection.
         * @param retryAction Action to take when retry is selected.
         * @param dismissedAction Action to take when dismissal is selected.
         */
        fun handleFailedConnection(
            snackbarHostState: SnackbarHostState,
            error: String,
            retryAction: () -> Unit,
            dismissedAction: () -> Unit
        ) {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = error,
                    actionLabel = "Retry",
                    duration = SnackbarDuration.Indefinite
                )
                when (result) {
                    SnackbarResult.ActionPerformed -> retryAction()
                    SnackbarResult.Dismissed -> dismissedAction()
                }
            }
        }

        /**
         * Gets all the items in the current directory.
         */
        fun getItemsInDir() {
            isLoadingFiles = true
            val dir = dirPath.trimStart('/')
            Log.d("MAIN", "Getting items in directory: '$dir'")
            server.listFiles(
                dir,
                { json ->
                    run {
                        val itemsInDirStr = json.getString("content")

                        // If the items is null, then the directory does not exist
                        if (itemsInDirStr == "null") {
                            dirItems = JSONArray()
                        } else {
                            dirItems = JSONArray(itemsInDirStr)
                            Log.d("MAIN", "Found ${dirItems.length()} items in directory")
                        }
                        isLoadingFiles = false
                    }
                },
                { status, _ ->
                    run {
                        Log.d("MAIN", "Failed to list items in directory")
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Failed to get things in directory: $status"
                            )
                        }
                        isLoadingFiles = false
                    }
                },
                { error ->
                    run {
                        Log.d("MAIN", "Error when getting items in directory: $error")
                        dirItems = JSONArray()
                        handleFailedConnection(
                            snackbarHostState,
                            error.message.toString(),
                            {
                                Log.d("MAIN", "Attempting retry of directory listing")
                                getItemsInDir()
                            },
                            {}
                        )
                        isLoadingFiles = false
                    }
                }
            )
        }

        /**
         * Gets the file at the specified path.
         *
         * @param rawPath Path to the file.
         * @param displayResult Whether to display the result of the syncing.
         */
        fun getFile(rawPath: String, displayResult: Boolean = true) {
            val path = rawPath.trimStart('/')
            Log.d("MAIN", "Getting file: '$path'")
            server.getFile(
                path,
                { json ->
                    run {
                        val encryptedContent = json.getString("content")
                        val fileData = Cryptography.decryptAES(
                            encryptedContent, encryptionKey, encryptionIV
                        )
                        IOMethods.createFile(rawPath, fileData)
                        Log.d("MAIN", "Downloaded '$rawPath'")
                        if (displayResult) {
                            scope.launch {
                                snackbarHostState.showSnackbar("File Synced")
                                getItemsInDir()
                            }
                        }
                    }
                },
                { status, _ ->
                    run {
                        Log.d("MAIN", "Failed file request: $status")
                        scope.launch { snackbarHostState.showSnackbar(status) }
                    }
                },
                { error ->
                    run {
                        Log.d("MAIN", "File request had error: $error")
                        handleFailedConnection(
                            snackbarHostState,
                            error.message.toString(),
                            {
                                Log.d("MAIN", "Attempting retry of file retrieval")
                                getFile(rawPath)
                            },
                            {}
                        )
                    }
                }
            )
        }

        /**
         * Deletes the specified item from the server.
         *
         * @param path Path to the item.
         * @param type Item type.
         */
        fun deleteItem(path: String, type: String) {
            Log.d("MAIN", "Deleting $type '$path'")
            server.deleteItem(
                path.trimStart('/'),
                { _ ->
                    run {
                        Log.d("MAIN", "Deleted $type '$path' from server")
                        val name = IOMethods.getFileName(path)
                        scope.launch { snackbarHostState.showSnackbar("Deleted $type '$name' from server") }
                        getItemsInDir()
                    }
                },
                { _, json ->
                    run {
                        val reason = json.getString("message")
                        Log.d("MAIN", "Failed to delete $type: $reason")
                        scope.launch {
                            snackbarHostState.showSnackbar("Failed to delete $type: $reason")
                        }
                    }
                },
                { error ->
                    run {
                        Log.d("MAIN", "Error when deleting $type: $error")
                        scope.launch { snackbarHostState.showSnackbar(error.message.toString()) }
                    }
                }
            )
        }

        /**
         * Gets the file name from a `content://` URI.
         *
         * @param uri A URI with the `content` scheme.
         * @return File name.
         */
        fun getFileName(uri: Uri): String {
            var result = ""

            contentResolver.query(uri, null, null, null, null).use { cursor ->
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        val colIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        result = cursor.getString(colIndex)
                    }
                }
            }
            return result
        }

        /**
         * Handles the sync of the item at the specified path.
         *
         * @param path Path to the file or folder.
         * @param type Type, file or folder.
         * @param displayResult Whether to display the results of syncing.
         */
        fun handleSync(path: String, type: String, displayResult: Boolean = true) {
            if (type == "file") {
                Log.d("MAIN", "Syncing file '$path'")
                getFile(path, displayResult)
            } else {
                // Get all items within the folder
                Log.d("MAIN", "Syncing directory '$path'")
                server.listFiles(
                    path.trimStart('/'),
                    { json ->
                        run {
                            val itemsInDirStr = json.getString("content")

                            // If the items is null, then the directory does not exist
                            val items: JSONArray = if (itemsInDirStr == "null") {
                                JSONArray()
                            } else {
                                JSONArray(itemsInDirStr)
                            }

                            for (i in 0..<items.length()) {
                                val item = items.getJSONObject(i)
                                val itemName = item.getString("name")
                                val itemPath = "$path/$itemName"
                                val itemType = item.getString("type")

                                handleSync(itemPath, itemType, false)
                            }

                            Log.d("MAIN", "Synced directory '$path'")
                            if (displayResult) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Folder Synced")
                                    getItemsInDir()
                                }
                            }
                        }
                    },
                    { status, _ ->
                        run {
                            Log.d("MAIN", "Failed to get items in directory")
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Failed to get things in directory: $status"
                                )
                            }
                        }
                    },
                    { error ->
                        run {
                            Log.d("MAIN", "Error when getting items in directory: $error")
                            scope.launch { snackbarHostState.showSnackbar(error.message.toString()) }
                        }
                    }
                )
            }
        }

        /**
         * Checks if the item at the specified path has been synced.
         *
         * @param path Path to the file or folder.
         * @param type Type, file or folder.
         * @param listener Listener to the result of the check.
         */
        fun checkSync(path: String, type: String, listener: (Boolean) -> Unit) {
            if (type == "file") {
                listener(IOMethods.checkIfFileExists(path))
            } else {
                // Get directory contents on the phone
                val localDirContents = IOMethods.traverseDir(path)

                // Then check the server's copy
                server.recursiveListFiles(
                    path.trimStart('/'),
                    { json ->
                        run {
                            val serverDirContents = json.getJSONArray("content")

                            // Check if everything on the server copy is on the local copy
                            val numItems = serverDirContents.length()
                            var item: String
                            for (i in 0..<numItems) {
                                item = serverDirContents.getString(i)
                                if (!localDirContents.contains(item)) {
                                    listener(false)
                                    return@run
                                }
                            }

                            // If reached here then everything is synced
                            listener(true)
                        }
                    },
                    { _, _ ->
                        run {
                            listener(false)
                        }
                    },
                    { error ->
                        run {
                            Log.d("MAIN", "Error when traversing server copy of '$path': $error")
                            listener(false)
                        }
                    }
                )
            }
        }

        // Helper composables
        /**
         * Creates a directory item on the screen.
         *
         * @param name Name of the item in the directory.
         * @param type Type of the item in the directory.
         * @param sizeString (Nicely processed) size of the item.
         */
        @Composable
        fun DirectoryItem(name: String, type: String, sizeString: String) {
            // Attributes
            val isPreviousDirectoryItem = type == PREVIOUS_DIRECTORY_TYPE

            var isDropdownExpanded by remember { mutableStateOf(false) }
            var showConfirmDeleteDialog by remember { mutableStateOf(false) }

            var isSynced by remember { mutableStateOf(false) }

            TextButton(
                shape = RoundedCornerShape(0),
                onClick = {
                    Log.d("MAIN", "Clicked on $type named '$name'")
                    if (type == "file") {
                        // Check if the file exists on the phone (i.e., synced)
                        val filePath = "$dirPath/$name"
                        val file = IOMethods.getFile(filePath)
                        if (file != null) {
                            // Get URI and MIME type of file
                            val uri = FileProvider.getUriForFile(
                                this,
                                "${applicationContext.packageName}.provider",
                                file
                            )
                            val mime = contentResolver.getType(uri)

                            // Open file with user selected app
                            val intent = Intent()
                            intent.setAction(Intent.ACTION_VIEW)
                            intent.setDataAndType(uri, mime)
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            try {
                                startActivity(intent)
                            } catch (error: ActivityNotFoundException) {
                                Log.d(
                                    "MAIN",
                                    "Cannot open file, activity not found: ${error.message}"
                                )
                            }
                        } else {
                            Log.d("MAIN", "File '$filePath' not synced")
                            scope.launch { snackbarHostState.showSnackbar("File not synced") }
                        }
                    } else {
                        if (isPreviousDirectoryItem) {
                            dirPath = prevDir
                            prevDir = IOMethods.getContainingDir(prevDir)
                        } else {
                            prevDir = dirPath
                            dirPath = "$dirPath/$name"
                        }
                        Log.d("MAIN", "Dir path: '$dirPath'; Prev dir: '$prevDir'")
                        getItemsInDir()
                    }
                }
            ) {
                // Get the correct icon and description to display
                val icon: ImageVector
                val description: String
                when (type) {
                    "file" -> {
                        icon = Icons.Filled.InsertDriveFile
                        description = "File"
                    }

                    "directory" -> {
                        icon = Icons.Filled.Folder
                        description = "Folder"
                    }

                    PREVIOUS_DIRECTORY_TYPE -> {
                        icon = Icons.Filled.ArrowBack
                        description = "Back"
                    }

                    else -> {
                        icon = Icons.Filled.QuestionMark
                        description = "Unknown"
                    }
                }

                Row {
                    if (isPreviousDirectoryItem) {
                        Spacer(Modifier.size(24.dp))
                    } else {
                        checkSync("$dirPath/$name", type) { synced -> isSynced = synced }

                        if (isSynced) {
                            Icon(Icons.Filled.CloudDone, "Synced", modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Outlined.Cloud, "Unsynced", modifier = Modifier.size(24.dp))
                        }
                    }
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
                                Icon(Icons.Filled.MoreVert, "More")
                            }
                            DropdownMenu(
                                expanded = isDropdownExpanded,
                                onDismissRequest = { isDropdownExpanded = false }
                            ) {
                                val path = "$dirPath/$name"
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Filled.Sync, "Sync") },
                                    text = { Text("Sync") },
                                    enabled = !IOMethods.checkIfFileExists(path),
                                    onClick = {
                                        Toast.makeText(
                                            applicationContext,
                                            "Starting sync of '$name'",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        handleSync(path, type)
                                        isDropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.Delete,
                                            "Delete From Device"
                                        )
                                    },
                                    text = { Text("Delete From Device") },
                                    enabled = IOMethods.checkIfExists(path),
                                    onClick = {
                                        if (IOMethods.deleteItem(path)) {
                                            scope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    "Deleted item",
                                                    "Undo",
                                                    duration = SnackbarDuration.Short
                                                )
                                                when (result) {
                                                    SnackbarResult.ActionPerformed -> {
                                                        handleSync(path, type)
                                                    }

                                                    SnackbarResult.Dismissed -> {
                                                        // Don't need to do anything
                                                    }
                                                }
                                            }
                                        } else {
                                            scope.launch { snackbarHostState.showSnackbar("Failed to delete item") }
                                        }
                                        isDropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.Clear,
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
            }

            if (showConfirmDeleteDialog) {
                Dialogs.YesNoDialog(
                    icon = Icons.Filled.Warning,
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
                            if (type == "directory") {
                                Text("This action also deletes all files within the directory.")
                            }
                            Text("This action is irreversible!", fontWeight = FontWeight.Bold)
                        }
                    },
                    onYes = {
                        showConfirmDeleteDialog = false
                        deleteItem("$dirPath/$name", type)
                    },
                    onNo = { showConfirmDeleteDialog = false }
                )
            }
        }

        /**
         * Adds the item action button.
         */
        @Composable
        fun AddItemActionButton() {
            // Attributes
            var expanded by remember { mutableStateOf(false) }

            var showCreateFolderInputDialog by remember { mutableStateOf(false) }

            val pickFileLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri ->
                if (uri != null) {
                    val fileName = getFileName(uri)
                    val filePath = "$dirPath/$fileName".trimStart('/')

                    Toast.makeText(
                        applicationContext,
                        "Uploading '$fileName'",
                        Toast.LENGTH_SHORT
                    ).show()

                    // TODO: Progress bar for uploading?
                    val inputStream = contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val content = inputStream.readBytes()
                        Log.d("MAIN", "Got content of '$fileName'")
                        val encrypted =
                            Cryptography.encryptAES(content, key = encryptionKey, iv = encryptionIV)
                        server.createFile(
                            filePath,
                            encrypted,
                            { _ ->
                                run {
                                    Log.d("MAIN", "New file created: $filePath")
                                    scope.launch { snackbarHostState.showSnackbar("Added file") }
                                    getItemsInDir()
                                }
                            },
                            { _, json ->
                                run {
                                    val reason = json.getString("message")
                                    Log.d("MAIN", "Failed to create file: $reason")
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Failed to create file: $reason")
                                    }
                                }
                            },
                            { error ->
                                run {
                                    Log.d("MAIN", "Error when making file: $error")
                                    scope.launch { snackbarHostState.showSnackbar(error.message.toString()) }
                                }
                            }
                        )
                        inputStream.close()
                    }
                }
            }

            // Helper functions
            /**
             * Code to run once the user confirms the folder name to create.
             *
             * @param folderName Name of the folder to create.
             */
            fun onConfirmFolderName(folderName: String) {
                Log.d("MAIN", "Request for new folder: $folderName")
                showCreateFolderInputDialog = false

                val fullFolderPath = "$dirPath/$folderName".trimStart('/')

                server.createFolder(
                    fullFolderPath,
                    { _ ->
                        run {
                            Log.d("MAIN", "New folder created: $fullFolderPath")
                            scope.launch { snackbarHostState.showSnackbar("Directory created") }
                            getItemsInDir()
                        }
                    },
                    { _, json ->
                        run {
                            val reason = json.getString("message")
                            Log.d("MAIN", "Failed to create folder: $reason")
                            scope.launch {
                                snackbarHostState.showSnackbar("Failed to create folder: $reason")
                            }
                        }
                    },
                    { error ->
                        run {
                            Log.d("MAIN", "Error when making folder: $error")
                            scope.launch { snackbarHostState.showSnackbar(error.message.toString()) }
                        }
                    }
                )
            }

            // Main UI
            if (!isLoadingFiles) {
                FloatingActionButton(
                    modifier = Modifier.padding(all = 16.dp),
                    onClick = { expanded = !expanded },
                ) {
                    Icon(Icons.Filled.Add, "Add")
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.NoteAdd, "Add File") },
                            text = { Text("Add File") },
                            onClick = { pickFileLauncher.launch("*/*") }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.CreateNewFolder, "Create Folder") },
                            text = { Text("Create Folder") },
                            onClick = { showCreateFolderInputDialog = true }
                        )
                    }
                }

                if (showCreateFolderInputDialog) {
                    Dialogs.TextInputDialog(
                        dialogTitle = "Enter Folder Name",
                        textFieldLabel = "Name",
                        textFieldPlaceholder = "Name of the folder",
                        textFieldErrorText = "Invalid folder name",
                        onConfirmation = { folderName -> onConfirmFolderName(folderName) },
                        onDismissal = { showCreateFolderInputDialog = false },
                        textFieldValidator = { text ->
                            text.isNotBlank() && FOLDER_NAME_REGEX.matches(
                                text
                            )
                        }
                    )
                }
            }
        }

        // Main UI
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    colors = topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = { Text("Files") },
                    navigationIcon = {
                        IconButton(onClick = { showConfirmLogoutDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Logout,
                                contentDescription = "Logout"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showExtrasMenu = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More"
                            )
                        }
                        DropdownMenu(
                            expanded = showExtrasMenu,
                            onDismissRequest = { showExtrasMenu = false }
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Filled.Settings, "Settings") },
                                text = { Text("Settings") },
                                onClick = {
                                    Log.d("MAIN", "Showing settings page")
                                    val settingsIntent =
                                        Intent(applicationContext, SettingsActivity::class.java)
                                    try {
                                        startActivity(settingsIntent)
                                    } catch (e: ActivityNotFoundException) {
                                        Log.d("MAIN", "Failed to show settings: ${e.message}")
                                    }
                                    showExtrasMenu = false
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Filled.Info, "About") },
                                text = { Text("About") },
                                onClick = {
                                    Log.d("MAIN", "Showing about page")
                                    val aboutIntent =
                                        Intent(applicationContext, AboutActivity::class.java)
                                    aboutIntent.putExtra("server_url", server.serverURL)
                                    try {
                                        startActivity(aboutIntent)
                                    } catch (e: ActivityNotFoundException) {
                                        Log.d("MAIN", "Failed to show about: ${e.message}")
                                    }
                                    showExtrasMenu = false
                                }
                            )
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = { AddItemActionButton() },
            floatingActionButtonPosition = FabPosition.End
        )
        { innerPadding ->
            if (isLoadingFiles) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(48.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            } else {
                Column(modifier = Modifier.padding(innerPadding)) {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        text = if (dirPath != "") dirPath else "/"
                    )
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        if (dirPath != "") {
                            DirectoryItem(
                                PREVIOUS_DIRECTORY_TEXT_LABEL,
                                PREVIOUS_DIRECTORY_TYPE,
                                ""
                            )
                        }
                        for (i in 0..<dirItems.length()) {
                            // Get the specific item
                            val item = dirItems.getJSONObject(i)
                            val name = item.getString("name")
                            val type = item.getString("type")
                            val size = item.getString("size")

                            // Create a button with that icon
                            DirectoryItem(name, type, size)
                        }
                    }
                    if (showConfirmLogoutDialog) {
                        Dialogs.YesNoDialog(
                            icon = Icons.Filled.Logout,
                            iconDesc = "Logout",
                            dialogTitle = "Confirm Logout",
                            dialogContent = {
                                Text("Are you sure that you want to log out?")
                            },
                            onYes = {
                                showConfirmLogoutDialog = false
                                Log.d("MAIN", "Start logout process")
                                server.handleLogout { success ->
                                    if (success) {
                                        loggedIn = false
                                        Log.d("MAIN", "Deleting all folders")
                                        IOMethods.deleteItem("")
                                        Log.d("MAIN", "Logged out")
                                        finish()
                                    } else {
                                        Log.d("MAIN", "Failed to log out")
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Failed to log out")
                                        }
                                    }
                                }
                            },
                            onNo = { showConfirmLogoutDialog = false }
                        )
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            // Now display the main directory
            getItemsInDir()

            // Show logout question when the back button is pressed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                onBackInvokedDispatcher.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT
                ) {
                    showConfirmLogoutDialog = true
                }
            } else {
                onBackPressedDispatcher.addCallback(
                    this@MainActivity,
                    object : OnBackPressedCallback(true) {
                        override fun handleOnBackPressed() {
                            showConfirmLogoutDialog = true
                        }
                    })
            }
        }
    }
}
