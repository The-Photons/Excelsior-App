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

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.QuestionMark
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.launch
import org.json.JSONArray
import site.overwrite.encryptedfilesapp.src.Cryptography
import site.overwrite.encryptedfilesapp.src.Dialogs
import site.overwrite.encryptedfilesapp.src.IOMethods
import site.overwrite.encryptedfilesapp.src.Server
import site.overwrite.encryptedfilesapp.src.decodeHex
import site.overwrite.encryptedfilesapp.ui.theme.EncryptedFilesAppTheme

// CONSTANTS
const val PREVIOUS_DIRECTORY_TEXT_LABEL = "Previous Directory"
const val PREVIOUS_DIRECTORY_TYPE = "prev-dir"

// MAIN ACTIVITY
class MainActivity : ComponentActivity() {
    // Properties
    private var loggedIn = false

    private lateinit var server: Server
    private lateinit var queue: RequestQueue
    private lateinit var loginIntent: Intent

    private lateinit var encryptionIV: String
    private lateinit var encryptionSalt: String
    private lateinit var encryptionKey: ByteArray

    // Overridden functions
    @OptIn(ExperimentalStdlibApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("MAIN", "Main activity onCreate")
        super.onCreate(savedInstanceState)

        // Create the request queue
        queue = Volley.newRequestQueue(applicationContext)

        // TODO: Remove and Uncomment Below
        server = Server(queue, "http://10.0.2.2:5000")  // 10.0.2.2 refers to localhost on PC
        encryptionIV = "encryptionIntVec"
        encryptionSalt = "someSalt12345678"
        encryptionKey = String(
            Cryptography.decryptAES(
                "UXMMpaGD1SJ3ZATBuJnt7I3MWYHzsVFURgo0tKDg5aOoP16mmDPal/8GmsqvXXkohZk" +
                        "f7SxRorWXe9qcIW+rmAA5niaqZeI2nvAuSrmztRg=",
                Cryptography.genAESKey("password", encryptionSalt),
                encryptionIV
            )
        ).decodeHex()
        loggedIn = true

//        // We first need to ask for the login details, especially the encryption key
//        loginIntent = Intent(this, LoginActivity::class.java)
//        val getLoginCredentials =
//            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//                if (result.resultCode == Activity.RESULT_OK) {
//                    val resultIntent = result.data
//                    val serverURL = resultIntent?.getStringExtra("server_url") ?: ""
//
//                    server = Server(queue, serverURL)
//                    encryptionIV = resultIntent?.getStringExtra("iv") ?: ""
//                    encryptionSalt = resultIntent?.getStringExtra("salt") ?: ""
//                    encryptionKey =
//                        resultIntent?.getByteArrayExtra("encryption_key") ?: ByteArray(0)
//
//                    loggedIn = true
//                    Log.d(
//                        "MAIN",
//                        "Got server URL '$serverURL', initialization vector '$encryptionIV'," +
//                                " salt '$encryptionSalt', and encryption key (as hex string)" +
//                                " '${encryptionKey.toHexString()}'"
//                    )
//                }
//            }
//        getLoginCredentials.launch(loginIntent)
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
    @Preview
    fun FilesList() {
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        var dirPath by remember { mutableStateOf("") }
        var prevDir by remember { mutableStateOf("") }
        var dirItems by remember { mutableStateOf(JSONArray()) }

        var isLoadingFiles by remember { mutableStateOf(false) }

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
            error: VolleyError,
            retryAction: () -> Unit,
            dismissedAction: () -> Unit
        ) {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = error.message.toString(),
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
                            error,
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
         */
        fun getFile(rawPath: String) {
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
                            error,
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
                getFile(path)
                Log.d("MAIN", "Synced file '$path'")
                if (displayResult) {
                    scope.launch {
                        snackbarHostState.showSnackbar("File Synced")
                        getItemsInDir()
                    }
                }
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

            var expanded by remember { mutableStateOf(false) }
            var showConfirmDeleteDialog by remember { mutableStateOf(false) }

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
                        // TODO: Handle checking of sync of folders
                        //       (Use a downloaded folder index?)
                        if (IOMethods.checkIfFileExists("$dirPath/$name")) {
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
                                onClick = { expanded = !expanded }
                            ) {
                                Icon(Icons.Filled.MoreVert, "More")
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
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
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Filled.Delete, "Delete") },
                                    text = { Text("Delete From Server") },
                                    onClick = { showConfirmDeleteDialog = true }
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

                        // TODO: Perhaps also filter by specific chars (e.g. [0-9A-z_-])
                        textFieldValidator = { text -> text.isNotBlank() }
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
                    title = {
                        Text("Files (${if (dirPath != "") dirPath else "/"})")
                    },
                    navigationIcon = {
                        IconButton(onClick = { showConfirmLogoutDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Logout,
                                contentDescription = "Logout"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* TODO: do something with menu */ }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Menu"
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
            // TODO: Scrolling?
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
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (dirPath != "") {
                        DirectoryItem(PREVIOUS_DIRECTORY_TEXT_LABEL, PREVIOUS_DIRECTORY_TYPE, "")
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
                            Log.d("MAIN", "Start logout process; deleting all folders")
                            IOMethods.deleteItem("")
                            Log.d("MAIN", "Logged out")
                            finish()
                        },
                        onNo = { showConfirmLogoutDialog = false }
                    )
                }
            }
        }

        // Now display the main directory
        getItemsInDir()
    }
}
