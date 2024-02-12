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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import site.overwrite.encryptedfilesapp.src.Cryptography
import site.overwrite.encryptedfilesapp.src.DataStoreManager
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
    private var loggedIn = false
    private lateinit var dataStoreManager: DataStoreManager

    private lateinit var server: Server
    private lateinit var username: String
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

        // Get the data store manager
        dataStoreManager = DataStoreManager(applicationContext)

        // We first need to ask for the login details, especially the encryption key
        loginIntent = Intent(this, LoginActivity::class.java)
        val getLoginCredentials =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val resultIntent = result.data
                    val serverURL = resultIntent?.getStringExtra("server_url") ?: ""
                    username = resultIntent?.getStringExtra("username") ?: ""
                    val password = resultIntent?.getStringExtra("password") ?: ""

                    server = Server(serverURL)
                    server.handleLogin(username, password) { _, _ ->
                        server.getEncryptionParameters(
                            { json ->
                                // Set the IV and salt
                                encryptionIV = json.getString("iv")
                                encryptionSalt = json.getString("salt")

                                // Convert the given password into the AES
                                val userAESKey =
                                    Cryptography.genAESKey(password, encryptionSalt)
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

                                // Call the `onStart()` method again to load the GUI properly
                                onStart()
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
            }
        getLoginCredentials.launch(loginIntent)
    }

    override fun onStart() {
        Log.d("MAIN", "Main activity onStart")
        super.onStart()

        setContent {
            EncryptedFilesAppTheme {
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

    // Helper functions
    /**
     * Gets the file name from a "content://" URI.
     *
     * @param uri A URI with the "content" scheme.
     * @return File name.
     */
    private fun getFileName(uri: Uri): String {
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
     * Checks if the item at the specified path has been synced.
     *
     * @param path Path to the file or folder.
     * @param type Type, file or folder.
     * @param listener Listener to the result of the check.
     */
    private fun checkSync(
        path: String,
        type: String,
        listener: (Boolean) -> Unit
    ) {
        if (type == "file") {
            listener(IOMethods.checkIfFileExists(path))
        } else {
            // Get directory contents on the phone
            val localDirContents = IOMethods.traverseDir(path)

            // Then check the server's copy
            server.recursiveListFiles(
                path.trimStart('/'),
                { json ->
                    val serverDirContents = json.getJSONArray("content")

                    // Check if everything on the server copy is on the local copy
                    val numItems = serverDirContents.length()
                    var item: String
                    for (i in 0..<numItems) {
                        item = serverDirContents.getString(i)
                        if (!localDirContents.contains(item)) {
                            listener(false)
                            return@recursiveListFiles
                        }
                    }

                    // If reached here then everything is synced
                    listener(true)
                },
                { _, _ -> listener(false) },
                { error ->
                    Log.d("MAIN", "Error when traversing server copy of '$path': $error")
                    listener(false)
                }
            )
        }
    }

    /**
     * Deletes the specified item from the server.
     *
     * @param dirPath Path of the directory that contains the item.
     * @param name Name of the item.
     * @param type Type of the item.
     * @param scope Coroutine scope.
     * @param snackbarHostState Snackbar host state.
     * @param setLoadingFiles Setter for the loading files flag.
     * @param setDirItems Setter for the directory items array.
     */
    private fun deleteItemFromServer(
        dirPath: String,
        name: String,
        type: String,
        scope: CoroutineScope,
        snackbarHostState: SnackbarHostState,
        setLoadingFiles: (Boolean) -> Unit,
        setDirItems: (JSONArray) -> Unit
    ) {
        val path = "$dirPath/$name"

        Log.d("MAIN", "Deleting $type '$path'")
        server.deleteItem(
            path.trimStart('/'),
            { _ ->
                Log.d("MAIN", "Deleted $type '$path' from server")
                scope.launch { snackbarHostState.showSnackbar("Deleted $type '$name' from server") }
                getItemsInDir(
                    dirPath,
                    scope,
                    snackbarHostState,
                    setLoadingFiles,
                    setDirItems
                )
            },
            { _, json ->
                val reason = json.getString("message")
                Log.d("MAIN", "Failed to delete $type: $reason")
                scope.launch {
                    snackbarHostState.showSnackbar("Failed to delete $type: $reason")
                }
            },
            { error ->
                Log.d("MAIN", "Error when deleting $type: $error")
                scope.launch { snackbarHostState.showSnackbar(error.message.toString()) }
            }
        )
    }

    /**
     * Gets all the items in the current directory.
     *
     * @param dirPath Path to the directory.
     * @param scope Coroutine scope.
     * @param snackbarHostState Snackbar host state.
     * @param setLoadingFiles Setter for the loading files flag.
     * @param setDirItems Setter for the directory items array.
     */
    private fun getItemsInDir(
        dirPath: String,
        scope: CoroutineScope,
        snackbarHostState: SnackbarHostState,
        setLoadingFiles: (Boolean) -> Unit,
        setDirItems: (JSONArray) -> Unit,
    ) {
        setLoadingFiles(true)
        val dir = dirPath.trimStart('/')
        Log.d("MAIN", "Getting items in directory: '$dir'")
        server.listFiles(
            dir,
            { json ->
                val itemsInDirStr = json.getString("content")

                // If the items is null, then the directory does not exist
                if (itemsInDirStr == "null") {
                    setDirItems(JSONArray())
                } else {
                    setDirItems(JSONArray(itemsInDirStr))
                }
                setLoadingFiles(false)
            },
            { status, _ ->
                Log.d("MAIN", "Failed to list items in directory")
                scope.launch {
                    snackbarHostState.showSnackbar(
                        "Failed to get things in directory: $status"
                    )
                }
                setLoadingFiles(false)
            },
            { error ->
                Log.d("MAIN", "Error when getting items in directory: $error")
                setDirItems(JSONArray())
                handleFailedConnection(
                    error,
                    scope = scope,
                    snackbarHostState = snackbarHostState,
                    retryAction = {
                        Log.d("MAIN", "Attempting retry of directory listing")
                        getItemsInDir(
                            dirPath, scope, snackbarHostState, setLoadingFiles, setDirItems
                        )
                    },
                )
                setLoadingFiles(false)
            }
        )
    }

    /**
     * Handle a failed connection.
     *
     * @param error Exception thrown when attempting network connection.
     * @param actionLabel What to show as the action.
     * @param scope Coroutine scope.
     * @param snackbarHostState Snackbar host state.
     * @param retryAction Action to take when retry is selected.
     * @param dismissedAction Action to take when dismissal is selected.
     */
    private fun handleFailedConnection(
        error: Exception,
        actionLabel: String = "Retry",
        scope: CoroutineScope,
        snackbarHostState: SnackbarHostState,
        retryAction: () -> Unit = {},
        dismissedAction: () -> Unit = {}
    ) {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = error.message.toString(),
                actionLabel = actionLabel,
                duration = SnackbarDuration.Indefinite
            )
            when (result) {
                SnackbarResult.ActionPerformed -> retryAction()
                SnackbarResult.Dismissed -> dismissedAction()
            }
        }
    }

    /**
     * Handles a click on a item.
     *
     * @param dirPath Path to the directory.
     * @param prevDir Previous directory.
     * @param name Name of the item.
     * @param type Type of the item.
     * @param scope Coroutine scope.
     * @param snackbarHostState Snackbar host state.
     * @param setDirPath Setter for the current directory's path.
     * @param setPrevDirPath Setter for the previous directory path.
     * @param setLoadingFiles Setter for the loading files flag.
     * @param setDirItems Setter for the directory items array.
     */
    private fun handleItemClick(
        dirPath: String,
        prevDir: String,
        name: String,
        type: String,
        scope: CoroutineScope?,
        snackbarHostState: SnackbarHostState?,
        setDirPath: (String) -> Unit,
        setPrevDirPath: (String) -> Unit,
        setLoadingFiles: (Boolean) -> Unit,
        setDirItems: (JSONArray) -> Unit
    ) {
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
                    scope?.launch { snackbarHostState?.showSnackbar(error.message.toString()) }
                }
            } else {
                Log.d("MAIN", "File '$filePath' not synced")
                scope?.launch { snackbarHostState?.showSnackbar("File not synced") }
            }
        } else {
            val newDirPath: String
            val newPrevDir: String
            if (type == PREVIOUS_DIRECTORY_TYPE) {
                newDirPath = prevDir
                newPrevDir = IOMethods.getContainingDir(prevDir)
            } else {
                newDirPath = "$dirPath/$name"
                newPrevDir = dirPath
            }

            setDirPath(newDirPath)
            setPrevDirPath(newPrevDir)

            Log.d("MAIN", "Dir path: '$newDirPath'; Prev dir: '$newPrevDir'")
            if (scope != null && snackbarHostState != null) {
                getItemsInDir(
                    newDirPath,
                    scope,
                    snackbarHostState,
                    setLoadingFiles,
                    setDirItems
                )
            }
        }
    }

    /**
     * Gets the file at the specified path.
     *
     * @param dirPath Path to the directory.
     * @param name Name of the item.
     * @param displayResult Whether to display the result of the syncing.
     * @param scope Coroutine scope.
     * @param snackbarHostState Snackbar host state.
     * @param setLoadingFiles Setter for the loading files flag.
     * @param setDirItems Setter for the directory items array.
     * @param setShowDataTransferDialog Setter for the flag for showing the data transfer dialog.
     * @param setDataTransferDialogTitle Setter for the title for the data transfer dialog.
     * @param setDataTransferProgress Setter for the progress on the data transfer dialog.
     */
    private fun syncFile(
        dirPath: String,
        name: String,
        displayResult: Boolean,
        scope: CoroutineScope,
        snackbarHostState: SnackbarHostState,
        setLoadingFiles: (Boolean) -> Unit,
        setDirItems: (JSONArray) -> Unit,
        setShowDataTransferDialog: (Boolean) -> Unit,
        setDataTransferDialogTitle: (String) -> Unit,
        setDataTransferProgress: (Float) -> Unit
    ) {
        val path = "$dirPath/$name"
        Log.d("MAIN", "Getting file: '$path'")

        setShowDataTransferDialog(true)
        setDataTransferDialogTitle("Downloading File")
        setDataTransferProgress(0f)

        server.getFile(
            path,
            { channel ->
                // Create a temporary file to store the encrypted content
                val encryptedFile = IOMethods.createFile("$path.encrypted")
                if (encryptedFile == null) {
                    Log.d(
                        "MAIN",
                        "Error when making file: failed to create temporary file"
                    )
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Failed to create temporary file"
                        )
                    }
                    setShowDataTransferDialog(false)
                    return@getFile
                }

                // Copy the channel into the encrypted file
                runBlocking {
                    channel.copyAndClose(encryptedFile.writeChannel())
                }

                // Decrypt the file
                setDataTransferDialogTitle("Decrypting File")
                setDataTransferProgress(0f)

                val decryptedFile = IOMethods.createFile(path)
                if (decryptedFile == null) {
                    Log.d(
                        "MAIN",
                        "Error when making file: failed to create output file"
                    )
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Failed to create output file"
                        )
                    }
                    setShowDataTransferDialog(false)
                    return@getFile
                }

                Cryptography.decryptAES(
                    encryptedFile.inputStream(),
                    decryptedFile.outputStream(),
                    encryptionKey,
                    encryptionIV
                ) { numBytesDecrypted ->
                    setDataTransferProgress(numBytesDecrypted.toFloat() / encryptedFile.length())
                }

                IOMethods.deleteItem(encryptedFile)

                Log.d("MAIN", "Downloaded '$path'")
                if (displayResult) {
                    scope.launch {
                        snackbarHostState.showSnackbar("File Synced")
                        getItemsInDir(
                            dirPath,
                            scope,
                            snackbarHostState,
                            setLoadingFiles,
                            setDirItems
                        )
                    }
                }
                setShowDataTransferDialog(false)
            },
            { error ->
                Log.d("MAIN", "File request had error: $error")
                handleFailedConnection(
                    error,
                    scope = scope,
                    snackbarHostState = snackbarHostState,
                    retryAction = {
                        Log.d("MAIN", "Attempting retry of file retrieval")
                        syncFile(
                            dirPath,
                            name,
                            displayResult,
                            scope,
                            snackbarHostState,
                            setLoadingFiles,
                            setDirItems,
                            setShowDataTransferDialog,
                            setDataTransferDialogTitle,
                            setDataTransferProgress
                        )
                    }
                )
            },
            { bytesSentTotal, contentLength ->
                setDataTransferProgress(bytesSentTotal.toFloat() / contentLength)
            }
        )
    }

    /**
     * Handles the syncing of the item.
     *
     * @param dirPath Path to the directory.
     * @param name Name of the item.
     * @param type Item type.
     * @param displayResult Whether to display the result of the syncing.
     * @param scope Coroutine scope.
     * @param snackbarHostState Snackbar host state.
     * @param setLoadingFiles Setter for the loading files flag.
     * @param setDirItems Setter for the directory items array.
     * @param setShowDataTransferDialog Setter for the flag for showing the data transfer dialog.
     * @param setDataTransferDialogTitle Setter for the title for the data transfer dialog.
     * @param setDataTransferProgress Setter for the progress on the data transfer dialog.
     */
    private fun handleSync(
        dirPath: String,
        name: String,
        type: String,
        displayResult: Boolean = true,
        scope: CoroutineScope,
        snackbarHostState: SnackbarHostState,
        setLoadingFiles: (Boolean) -> Unit,
        setDirItems: (JSONArray) -> Unit,
        setShowDataTransferDialog: (Boolean) -> Unit,
        setDataTransferDialogTitle: (String) -> Unit,
        setDataTransferProgress: (Float) -> Unit
    ) {
        val path = "$dirPath/$name"
        if (type == "file") {
            Log.d("MAIN", "Syncing file '$path'")
            syncFile(
                dirPath,
                name,
                displayResult,
                scope,
                snackbarHostState,
                setLoadingFiles,
                setDirItems,
                setShowDataTransferDialog,
                setDataTransferDialogTitle,
                setDataTransferProgress
            )
        } else {
            // Get all items within the folder
            Log.d("MAIN", "Syncing directory '$path'")
            server.listFiles(
                path.trimStart('/'),
                { json ->
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
                        val itemType = item.getString("type")

                        handleSync(
                            path,
                            itemName,
                            itemType,
                            false,
                            scope,
                            snackbarHostState,
                            setLoadingFiles,
                            setDirItems,
                            setShowDataTransferDialog,
                            setDataTransferDialogTitle,
                            setDataTransferProgress
                        )
                    }

                    Log.d("MAIN", "Synced directory '$path'")
                    if (displayResult) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Folder Synced")
                            getItemsInDir(
                                dirPath,
                                scope,
                                snackbarHostState,
                                setLoadingFiles,
                                setDirItems
                            )
                        }
                    }
                },
                { status, _ ->
                    Log.d("MAIN", "Failed to get items in directory")
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Failed to get things in directory: $status"
                        )
                    }
                },
                { error ->
                    Log.d("MAIN", "Error when getting items in directory: $error")
                    scope.launch { snackbarHostState.showSnackbar(error.message.toString()) }
                }
            )
        }
    }

    // Composables
    /**
     * Creates a directory item on the screen.
     *
     * @param dirPath Directory that the item is in.
     * @param prevDir Previous directory.
     * @param name Name of the item in the directory.
     * @param type Item type.
     * @param sizeString (Nicely processed) size of the item.
     * @param scope Coroutine scope.
     * @param snackbarHostState Snackbar host state.
     * @param setDirPath Setter for the current directory path.
     * @param setPrevDirPath Setter for the previous directory path.
     * @param setLoadingFiles Setter for the loading files flag.
     * @param setDirItems Setter for the directory items array.
     * @param setShowDataTransferDialog Setter for the flag for showing the data transfer dialog.
     * @param setDataTransferDialogTitle Setter for the title for the data transfer dialog.
     * @param setDataTransferProgress Setter for the progress on the data transfer dialog.
     */
    @Composable
    fun DirectoryItem(
        dirPath: String,
        prevDir: String,
        name: String,
        type: String,
        sizeString: String,
        scope: CoroutineScope?,
        snackbarHostState: SnackbarHostState?,
        setDirPath: (String) -> Unit,
        setPrevDirPath: (String) -> Unit,
        setLoadingFiles: (Boolean) -> Unit,
        setDirItems: (JSONArray) -> Unit,
        setShowDataTransferDialog: (Boolean) -> Unit,
        setDataTransferDialogTitle: (String) -> Unit,
        setDataTransferProgress: (Float) -> Unit
    ) {
        // Attributes
        val isPreviousDirectoryItem = type == PREVIOUS_DIRECTORY_TYPE

        var isDropdownExpanded by remember { mutableStateOf(false) }
        var showConfirmDeleteDialog by remember { mutableStateOf(false) }

        var hasChangeInSyncStatus by remember { mutableStateOf(false) }
        var isSynced by remember { mutableStateOf(false) }

        // Prematurely end if the current directory is the root directory and the type is "previous
        // directory"
        if (dirPath == "" && isPreviousDirectoryItem) return

        // Otherwise create the item
        TextButton(
            shape = RoundedCornerShape(0),
            onClick = {
                Log.d("MAIN", "Clicked on $type named '$name'")
                handleItemClick(
                    dirPath,
                    prevDir,
                    name,
                    type,
                    scope,
                    snackbarHostState,
                    setDirPath,
                    setPrevDirPath,
                    setLoadingFiles,
                    setDirItems
                )
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
                                    hasChangeInSyncStatus = true
                                    if (scope != null && snackbarHostState != null) {
                                        handleSync(
                                            dirPath,
                                            name,
                                            type,
                                            true,
                                            scope,
                                            snackbarHostState,
                                            setLoadingFiles,
                                            setDirItems,
                                            setShowDataTransferDialog,
                                            setDataTransferDialogTitle,
                                            setDataTransferProgress
                                        )
                                    }
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
                                        hasChangeInSyncStatus = true
                                        scope?.launch {
                                            val result = snackbarHostState?.showSnackbar(
                                                "Deleted item",
                                                "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            when (result) {
                                                SnackbarResult.ActionPerformed -> {
                                                    hasChangeInSyncStatus = true
                                                    handleSync(
                                                        dirPath,
                                                        name,
                                                        type,
                                                        true,
                                                        scope,
                                                        snackbarHostState,
                                                        setLoadingFiles,
                                                        setDirItems,
                                                        setShowDataTransferDialog,
                                                        setDataTransferDialogTitle,
                                                        setDataTransferProgress
                                                    )
                                                }

                                                else -> {
                                                    // Don't need to do anything
                                                }
                                            }
                                        }
                                    } else {
                                        scope?.launch {
                                            snackbarHostState?.showSnackbar("Failed to delete item")
                                        }
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
                    if (scope != null && snackbarHostState != null) {
                        deleteItemFromServer(
                            dirPath,
                            name,
                            type,
                            scope,
                            snackbarHostState,
                            setLoadingFiles,
                            setDirItems
                        )
                    }
                },
                onNo = { showConfirmDeleteDialog = false }
            )
        }

        // Check the sync status of the item
        LaunchedEffect(hasChangeInSyncStatus) {
            checkSync("$dirPath/$name", type) { synced ->
                isSynced = synced
            }
            hasChangeInSyncStatus = false
        }
    }

    /**
     * The main files list.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun FilesList(
        initialDirItems: JSONArray = JSONArray()
    ) {
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        var dirPath by remember { mutableStateOf("") }
        var prevDir by remember { mutableStateOf("") }

        var isLoadingFiles by remember { mutableStateOf(false) }

        var showDataTransferProgressDialog by remember { mutableStateOf(false) }
        var dataTransferDialogTitle by remember { mutableStateOf("") }
        var dataTransferProgress: Float? by remember { mutableStateOf(0f) }

        var showExtrasMenu by remember { mutableStateOf(false) }
        var showConfirmLogoutDialog by remember { mutableStateOf(false) }

        var dirItems by remember { mutableStateOf(initialDirItems) }

        // Helper composables
        /**
         * Adds the item action button.
         */
        // TODO: Rewrite
        @Composable
        fun AddItemActionButton() {
            // Attributes
            var dropdownExpanded by remember { mutableStateOf(false) }

            var showCreateFolderInputDialog by remember { mutableStateOf(false) }

            fun uploadFile(uri: Uri) {
                val fileName = getFileName(uri)
                val filePath = "$dirPath/$fileName".trimStart('/')

                Toast.makeText(
                    applicationContext,
                    "Uploading '$fileName'",
                    Toast.LENGTH_SHORT
                ).show()

                // First check if the file exists already
                server.pathExists(
                    filePath,
                    { exists ->
                        if (!exists) {
                            // Get the file size
                            val fileDescriptor = contentResolver.openAssetFileDescriptor(uri, "r")
                            val fileSize: Long
                            if (fileDescriptor != null) {
                                fileSize = fileDescriptor.length
                                fileDescriptor.close()
                            } else {
                                fileSize = -1
                            }

                            // Get the input stream of the file
                            val inputStream = contentResolver.openInputStream(uri)
                            val mimeType =
                                contentResolver.getType(uri) ?: "application/octet-stream"
                            if (inputStream != null) {
                                showDataTransferProgressDialog = true
                                dataTransferDialogTitle = "Encrypting File"
                                dataTransferProgress = 0f

                                // Create a temporary file to store the encrypted content
                                val encryptedFile = IOMethods.createFile("$filePath.encrypted")
                                if (encryptedFile == null) {
                                    Log.d(
                                        "MAIN",
                                        "Error when making file: failed to create temporary file"
                                    )
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Failed to create temporary file"
                                        )
                                    }
                                    showDataTransferProgressDialog = false
                                    return@pathExists
                                }

                                // Now encrypt the original file, storing the result in the temporary file
                                Cryptography.encryptAES(
                                    inputStream,
                                    encryptedFile.outputStream(),
                                    encryptionKey,
                                    encryptionIV
                                ) { numBytesEncrypted ->
                                    dataTransferProgress = if (fileSize != -1L) {
                                        numBytesEncrypted.toFloat() / fileSize
                                    } else {
                                        null
                                    }
                                }
                                Log.d("MAIN", "Encrypted file; attempting upload")

                                // Once encrypted, send file to server
                                dataTransferDialogTitle = "Uploading File"
                                dataTransferProgress = 0f
                                server.createFile(
                                    filePath,
                                    encryptedFile,
                                    mimeType,
                                    { _ ->
                                        Log.d("MAIN", "New file created: $filePath")
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "Added file"
                                            )
                                        }
                                        IOMethods.deleteItem(encryptedFile)
                                        getItemsInDir(
                                            dirPath,
                                            scope,
                                            snackbarHostState,
                                            { isLoadingFiles = it }
                                        ) { dirItems = it }
                                    },
                                    { _, json ->
                                        val reason = json.getString("message")
                                        Log.d(
                                            "MAIN",
                                            "Failed to create file: $reason"
                                        )
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Failed to create file: $reason")
                                        }
                                        IOMethods.deleteItem(encryptedFile)
                                        showDataTransferProgressDialog = false
                                    },
                                    { error ->
                                        Log.d(
                                            "MAIN",
                                            "Error when making file: $error"
                                        )
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                error.message.toString()
                                            )
                                        }
                                        IOMethods.deleteItem(encryptedFile)
                                        showDataTransferProgressDialog = false
                                    }
                                ) { bytesSentTotal, contentLength ->
                                    dataTransferProgress = bytesSentTotal.toFloat() / contentLength
                                    if (bytesSentTotal == contentLength) {
                                        showDataTransferProgressDialog = false
                                    }
                                }
                                inputStream.close()
                            } else {
                                Log.d("MAIN", "Failed to read file '$filePath'")
                                scope.launch { snackbarHostState.showSnackbar("Failed to read file '$fileName'") }
                            }
                        } else {
                            Log.d("MAIN", "File already exists, not uploading")
                            scope.launch { snackbarHostState.showSnackbar("File already exists on server") }
                        }
                    },
                    { error ->
                        Log.d("MAIN", "Error when checking path existence: $error")
                        scope.launch { snackbarHostState.showSnackbar(error.message.toString()) }
                    }
                )
            }

            val pickFileLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri -> if (uri != null) uploadFile(uri) }

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

                server.pathExists(
                    fullFolderPath,
                    { exists ->
                        if (!exists) {
                            server.createFolder(
                                fullFolderPath,
                                { _ ->
                                    Log.d("MAIN", "New folder created: $fullFolderPath")
                                    scope.launch { snackbarHostState.showSnackbar("Directory created") }
                                    getItemsInDir(
                                        dirPath,
                                        scope,
                                        snackbarHostState,
                                        { isLoadingFiles = it }
                                    ) { dirItems = it }
                                },
                                { _, json ->
                                    val reason = json.getString("message")
                                    Log.d("MAIN", "Failed to create folder: $reason")
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Failed to create folder: $reason")
                                    }
                                },
                                { error ->
                                    Log.d("MAIN", "Error when making folder: $error")
                                    scope.launch { snackbarHostState.showSnackbar(error.message.toString()) }
                                }
                            )
                        } else {
                            Log.d("MAIN", "Folder already exists, not creating")
                            scope.launch { snackbarHostState.showSnackbar("Folder already exists on server") }
                        }
                    },
                    { error ->
                        Log.d("MAIN", "Error when checking path existence: $error")
                        scope.launch { snackbarHostState.showSnackbar(error.message.toString()) }
                    }
                )
            }

            // Main UI
            if (!isLoadingFiles) {
                FloatingActionButton(
                    modifier = Modifier.padding(all = 16.dp),
                    onClick = { dropdownExpanded = !dropdownExpanded },
                ) {
                    Icon(Icons.Filled.Add, "Add")
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
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
//                            DropdownMenuItem(
//                                leadingIcon = { Icon(Icons.Filled.Settings, "Settings") },
//                                text = { Text("Settings") },
//                                onClick = {
//                                    Log.d("MAIN", "Showing settings page")
//                                    val settingsIntent =
//                                        Intent(applicationContext, SettingsActivity::class.java)
//                                    try {
//                                        startActivity(settingsIntent)
//                                    } catch (e: ActivityNotFoundException) {
//                                        Log.d("MAIN", "Failed to show settings: ${e.message}")
//                                    }
//                                    showExtrasMenu = false
//                                }
//                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Filled.Info, "About") },
                                text = { Text("About") },
                                onClick = {
                                    Log.d("MAIN", "Showing about page")
                                    val aboutIntent =
                                        Intent(applicationContext, AboutActivity::class.java)
                                    aboutIntent.putExtra("server_url", server.serverURL)
                                    aboutIntent.putExtra("username", username)

                                    try {
                                        startActivity(aboutIntent)
                                    } catch (e: ActivityNotFoundException) {
                                        Log.d("MAIN", "Failed to show about view: ${e.message}")
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
                        for (i in -1..<dirItems.length()) {
                            val name: String
                            val type: String
                            val size: String

                            if (i == -1) {
                                name = PREVIOUS_DIRECTORY_TEXT_LABEL
                                type = PREVIOUS_DIRECTORY_TYPE
                                size = ""
                            } else {
                                // Get the specific item
                                val item = dirItems.getJSONObject(i)
                                name = item.getString("name")
                                type = item.getString("type")
                                size = item.getString("size")
                            }

                            // Create a button with that icon
                            DirectoryItem(
                                dirPath,
                                prevDir,
                                name,
                                type,
                                size,
                                scope,
                                snackbarHostState,
                                { dirPath = it },
                                { prevDir = it },
                                { isLoadingFiles = it },
                                { dirItems = it },
                                { showDataTransferProgressDialog = it },
                                { dataTransferDialogTitle = it },
                                { dataTransferProgress = it }
                            )
                        }
                    }

                    if (showDataTransferProgressDialog) {
                        Dialogs.ProgressIndicatorDialog(
                            dialogTitle = dataTransferDialogTitle,
                            progress = dataTransferProgress
                        )
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
                                server.handleLogout { _ ->
                                    loggedIn = false
                                    Log.d("MAIN", "Deleting all folders")
                                    IOMethods.deleteItem("")
                                    Log.d("MAIN", "Logged out")
                                    finish()
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
            getItemsInDir(
                dirPath,
                scope,
                snackbarHostState,
                { isLoadingFiles = it }
            ) { dirItems = it }

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

    // Previews
    @Preview
    @Composable
    fun DirectoryItemPreview() {
        EncryptedFilesAppTheme {
            Surface(
                color = MaterialTheme.colorScheme.background
            ) {
                DirectoryItem(
                    "test-dir",
                    "prev-dir",
                    "Testing File.txt",
                    "file",
                    "123.45 kB",
                    null,
                    null,
                    { _ -> },
                    { _ -> },
                    { _ -> },
                    { _ -> },
                    { _ -> },
                    { _ -> },
                    { _ -> }
                )
            }
        }
    }

    @Preview
    @Composable
    fun FilesListPreview() {
        EncryptedFilesAppTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                FilesList(
                    JSONArray(
                        "[{'name': 'test-dir1', 'type': 'directory', 'size': '1.23 kB'}, " +
                                "{'name': 'test-dir2', 'type': 'directory', 'size': '4.56 MB'}, " +
                                "{'name': 'test-file1', 'type': 'file', 'size': '7.89 GB'}, " +
                                "{'name': 'test-file2', 'type': 'file', 'size': '10.11 TB'}]"
                    )
                )
            }
        }
    }
}
