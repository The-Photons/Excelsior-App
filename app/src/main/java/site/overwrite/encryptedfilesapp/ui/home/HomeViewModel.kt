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

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import site.overwrite.encryptedfilesapp.Server
import site.overwrite.encryptedfilesapp.cryptography.Cryptography
import site.overwrite.encryptedfilesapp.cryptography.EncryptionParameters
import site.overwrite.encryptedfilesapp.data.ItemType
import site.overwrite.encryptedfilesapp.data.RemoteDirectory
import site.overwrite.encryptedfilesapp.data.RemoteFile
import site.overwrite.encryptedfilesapp.data.RemoteItem
import site.overwrite.encryptedfilesapp.file.CRUDOperations
import site.overwrite.encryptedfilesapp.file.Pathing
import site.overwrite.encryptedfilesapp.ui.SnackbarData
import site.overwrite.encryptedfilesapp.ui.ToastData

data class HomeViewUIState(
    // TODO: Add snackbar reports for all errors

    // Main fields
    val server: Server = Server(""),
    val username: String = "",
    val password: String = "",
    val encryptionParameters: EncryptionParameters = EncryptionParameters(),
    val rootDirectory: RemoteDirectory = RemoteDirectory(
        "",
        "",
        0,
        emptyArray(),
        emptyArray(),
        null
    ),
    val activeDirectory: RemoteDirectory = RemoteDirectory(
        "",
        "",
        0,
        emptyArray(),
        emptyArray(),
        null
    ),

    // Displayables' fields
    val toastData: ToastData = ToastData(),
    val snackbarData: SnackbarData = SnackbarData()
) {
    val parentDirectory: RemoteDirectory?
        get() = activeDirectory.parentDir
    val atRootDirectory: Boolean
        get() = parentDirectory == null
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(HomeViewUIState())
    val uiState: StateFlow<HomeViewUIState> = _uiState.asStateFlow()

    init {
        _uiState.value = HomeViewUIState()
    }

    // Mutable values
    var loggedIn by mutableStateOf(false)
        private set

    var showLogoutDialog by mutableStateOf(false)
    var showCreateFolderDialog by mutableStateOf(false)

    var showProcessingDialog by mutableStateOf(false)
        private set
    var processingDialogTitle by mutableStateOf("")
        private set
    var processingDialogSubtitle by mutableStateOf("")
        private set
    var processingDialogProgress: Float? by mutableStateOf(null)
        private set

    // Auth methods
    @OptIn(ExperimentalStdlibApi::class)
    fun login(
        server: Server,
        username: String,
        password: String
    ) {
        Log.d("HOME", "Start login process")

        // Update the UI state's version of the parameters
        _uiState.update {
            it.copy(
                server = server,
                username = username,
                password = password
            )
        }

        // Then set the local copy of the required variables
        var encryptionIV: String
        var encryptionSalt: String
        var encryptionKey: ByteArray

        // Now actually perform the login operation
        _uiState.value.server.handleLogin(username, password) { _ ->
            _uiState.value.server.getEncryptionParameters(
                { json ->
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

                    // Update the UI state's version of the parameters
                    _uiState.update {
                        it.copy(
                            encryptionParameters = EncryptionParameters(
                                iv = encryptionIV,
                                salt = encryptionSalt,
                                key = encryptionKey
                            )
                        )
                    }

                    // Get the root folder's items
                    getRootFolderItems()

                    // Mark that we are logged in
                    loggedIn = true
                    Log.d(
                        "HOME",
                        "Logged in as '$username' into ${_uiState.value.server.serverURL}"
                    )
                    Log.d(
                        "HOME",
                        "Got initialization vector '$encryptionIV'," +
                                " salt '$encryptionSalt', and encryption key (as hex string)" +
                                " '${encryptionKey.toHexString()}'"
                    )
                },
                { _, json ->
                    Log.d(
                        "HOME",
                        "Failed to get encryption parameters: ${json.getString("message")}"
                    )
                },
                { error ->
                    Log.d("HOME", "Error when getting encryption parameters: $error")
                }
            )
        }
    }

    fun logout(
        server: Server,
        context: Context
    ) {
        Log.d("HOME", "Start logout process")
        server.handleLogout {
            loggedIn = false
            Log.d("HOME", "Logged out; deleting synced local files")
            initProcessingDialog("Deleting local files")

            val filesToDelete = _uiState.value.rootDirectory.syncedConstituentFiles
            val numFilesToDelete = filesToDelete.size
            var file: RemoteFile
            for (fileNum in 1..numFilesToDelete) {
                file = filesToDelete[fileNum - 1]
                CRUDOperations.deleteItem(file.path)
                processingDialogProgress = fileNum.toFloat() / numFilesToDelete
            }
            processingDialogProgress = 1f

            Log.d("HOME", "Removing directories")
            if (Pathing.doesItemExist("")) {
                CRUDOperations.deleteItem("")
            }

            Log.d("HOME", "Cleanup complete")
            (context as Activity).finish()
        }
    }

    // Directory item methods
    private fun changeActiveDirectory(newActiveDirectory: RemoteDirectory) {
        _uiState.update {
            it.copy(
                activeDirectory = newActiveDirectory
            )
        }
    }

    fun goToPreviousDirectory() {
        if (!_uiState.value.atRootDirectory) {
            changeActiveDirectory(_uiState.value.parentDirectory!!)
        } else {
            Log.d("HOME", "Cannot go back to previous directory")
            showToast("Cannot go back to previous directory")
        }
    }

    fun directoryItemOnClick(item: RemoteItem) {
        if (item.type == ItemType.DIRECTORY) {
            changeActiveDirectory(item as RemoteDirectory)
            return
        }

        val file = CRUDOperations.getFile(item.path)
        if (file == null) {
            Log.d("HOME", "File '${item.name}' not synced, so cannot open")
            showSnackbar("File not synced")
            return
        }

        // Get URI and MIME type of file
        val context = getApplication<Application>().applicationContext
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val mime = context.contentResolver.getType(uri)

        // Open file with user selected app
        val intent = Intent()
        intent.setAction(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, mime)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            context.startActivity(intent)
        } catch (error: ActivityNotFoundException) {
            Log.d(
                "HOME",
                "Cannot open file, activity not found: ${error.message}"
            )
            showSnackbar("$error.message")
        }
    }

    // CRUD methods
    private fun getRootFolderItems() {
        _uiState.value.server.listDir(
            "",
            { json ->
                val rootDirectory = RemoteDirectory.fromJSON(json)
                _uiState.update {
                    it.copy(
                        rootDirectory = rootDirectory
                    )
                }
                changeActiveDirectory(rootDirectory)
            },
            { _, json ->
                Log.d(
                    "HOME",
                    "Failed to get root folder items: ${json.getString("message")}"
                )
            },
            { error -> Log.d("HOME", "Error when getting folder items: $error") }
        )
    }

    private fun syncFile(
        file: RemoteFile,
        fileNum: Int? = null,
        totalNumFiles: Int? = null,
        onComplete: () -> Unit
    ) {
        if (file.synced) return

        val processingDialogSubtitle = if (fileNum == null) "" else "$fileNum of $totalNumFiles"
        initProcessingDialog(
            "Downloading '${file.name}'",
            processingDialogSubtitle
        )

        _uiState.value.server.getFile(
            file.path,
            { channel ->
                // Create a temporary file to store the encrypted content
                val encryptedFile = CRUDOperations.createFile("${file.path}.encrypted")
                if (encryptedFile == null) {
                    Log.d(
                        "HOME",
                        "Error when making file: failed to create temporary file"
                    )
                    showSnackbar("Failed to create temporary file")
                    hideProcessingDialog()
                    return@getFile
                }

                // Copy the channel into the encrypted file
                runBlocking {
                    channel.copyAndClose(encryptedFile.writeChannel())
                }

                // Decrypt the file
                initProcessingDialog(
                    "Decrypting '${file.name}'",
                    processingDialogSubtitle
                )

                val decryptedFile = CRUDOperations.createFile(file.path)
                if (decryptedFile == null) {
                    Log.d(
                        "HOME",
                        "Error when making file: failed to create output file"
                    )
                    showSnackbar("Failed to create output file")
                    hideProcessingDialog()
                    return@getFile
                }

                Cryptography.decryptAES(
                    encryptedFile.inputStream(),
                    decryptedFile.outputStream(),
                    _uiState.value.encryptionParameters.key!!,
                    _uiState.value.encryptionParameters.iv
                ) { numBytesDecrypted ->
                    processingDialogProgress = numBytesDecrypted.toFloat() / encryptedFile.length()
                }

                CRUDOperations.deleteItem(encryptedFile)
                hideProcessingDialog()
                onComplete()
            },
            { error ->
                Log.d("HOME", "File request had error: $error")
                hideProcessingDialog()
                showSnackbar(
                    message = "$error",
                    actionLabel = "Retry",
                    duration = SnackbarDuration.Long,
                    onAction = {
                        Log.d("HOME", "Retry sync '${file.path}'")
                        syncFile(
                            file,
                            fileNum,
                            totalNumFiles,
                            onComplete
                        )
                    }
                )
            },
            { bytesSentTotal: Long, contentLength: Long ->
                processingDialogProgress = bytesSentTotal.toFloat() / contentLength
            }
        )
    }

    fun syncItem(item: RemoteItem) {
        if (item.markedForLocalDeletion) {
            showToast("Cannot sync when attempting to delete")
            return
        }

        if (item.type == ItemType.FILE) {
            val theFile = item as RemoteFile
            Log.d("HOME", "Syncing file '${theFile.path}'")
            syncFile(theFile) {
                Log.d("HOME", "Synced file '${theFile.path}'")
                showSnackbar("File synced")
            }
            return
        }

        val theDirectory = item as RemoteDirectory
        Log.d("HOME", "Syncing directory '${theDirectory.path}'")

        val filesToSync = theDirectory.constituentFiles
        val numFilesToSync = filesToSync.size
        var numSyncedFiles = 0

        // This is kind of an ugly workaround... but it works
        fun helper() {
            if (numSyncedFiles == numFilesToSync) {
                Log.d("HOME", "Synced directory '${theDirectory.path}'")
                showSnackbar("Directory synced")
                return
            }
            syncFile(
                filesToSync[numSyncedFiles],
                numSyncedFiles + 1,
                numFilesToSync
            ) {
                numSyncedFiles += 1
                helper()
            }
        }
        helper()
    }

    fun createFileOnServer(uri: Uri) {
        val context = getApplication<Application>().applicationContext
        val name = Pathing.getFileName(uri, context.contentResolver)

        Log.d("HOME", "Attempting to upload file '$name' to server")

        val parentDir = _uiState.value.activeDirectory.path
        val path: String = if (parentDir.isEmpty()) {
            name
        } else {
            "$parentDir/$name"
        }

        // FIXME: Fix URL encoding of paths

        _uiState.value.server.doesItemExist(
            path,
            { exists ->
                if (exists) {
                    Log.d("HOME", "File already exists on server; not creating")
                    showSnackbar("File already exists on server")
                    return@doesItemExist
                }

                // FIXME: Surely we can move the following long code into another file?

                // Get the file size
                val fileDescriptor = context.contentResolver.openAssetFileDescriptor(uri, "r")
                val fileSize: Long
                if (fileDescriptor != null) {
                    fileSize = fileDescriptor.length
                    fileDescriptor.close()
                } else {
                    fileSize = -1
                }

                // Get the input stream of the file
                val inputStream = context.contentResolver.openInputStream(uri)
                val mimeType =
                    context.contentResolver.getType(uri) ?: "application/octet-stream"
                if (inputStream == null) {
                    Log.d("HOME", "Failed to read file '$path'")
                    showSnackbar("Failed to read file '$name'")
                    return@doesItemExist
                }

                initProcessingDialog("Encrypting '$name'")

                // Create a temporary file to store the encrypted content
                val encryptedFile = CRUDOperations.createFile("$path.encrypted")
                if (encryptedFile == null) {
                    Log.d(
                        "HOME",
                        "Error when making file: failed to create temporary file"
                    )
                    showSnackbar("Failed to create temporary file for uploading")
                    hideProcessingDialog()
                    return@doesItemExist
                }

                // Now encrypt the original file, storing the result in the temporary file
                Cryptography.encryptAES(
                    inputStream,
                    encryptedFile.outputStream(),
                    _uiState.value.encryptionParameters.key!!,
                    _uiState.value.encryptionParameters.iv
                ) { numBytesEncrypted ->
                    processingDialogProgress = if (fileSize != -1L) {
                        numBytesEncrypted.toFloat() / fileSize
                    } else {
                        null
                    }
                }
                Log.d("HOME", "Encrypted file; attempting upload")

                // Once encrypted, send file to server
                initProcessingDialog("Uploading '$name'")
                _uiState.value.server.createFile(
                    path,
                    encryptedFile,
                    mimeType,
                    {
                        Log.d("HOME", "New file uploaded to server: $path")

                        /*
                         * Note that the `fileSize` here is *not* exactly the uploaded file's size,
                         * but since we are using AES encryption, the difference in the uploaded
                         * file size and the file size of the unencrypted file should be small
                         * enough to not matter when displaying.
                         */
                        _uiState.value.activeDirectory.addFile(name, path, fileSize)

                        showSnackbar("Uploaded '$name'")
                        CRUDOperations.deleteItem(encryptedFile)
                    },
                    { _, json ->
                        val reason = json.getString("message")
                        Log.d(
                            "HOME",
                            "Failed to create file: $reason"
                        )
                        showSnackbar("Failed to upload file: $reason")
                        CRUDOperations.deleteItem(encryptedFile)
                        hideProcessingDialog()
                    },
                    { error ->
                        Log.d(
                            "HOME",
                            "Error when making file: $error"
                        )
                        CRUDOperations.deleteItem(encryptedFile)
                        hideProcessingDialog()
                    }
                ) { bytesSentTotal, contentLength ->
                    processingDialogProgress = bytesSentTotal.toFloat() / contentLength
                    if (bytesSentTotal == contentLength) {
                        hideProcessingDialog()
                    }
                }
                inputStream.close()
            },
            { error ->
                Log.d("HOME", "Error when checking path existence: $error")
            }
        )
    }

    fun createFolderOnServer(name: String) {
        Log.d("HOME", "Attempting to create new folder '$name' on server")

        val parentDir = _uiState.value.activeDirectory.path
        val path: String = if (parentDir.isEmpty()) {
            name
        } else {
            "$parentDir/$name"
        }

        // FIXME: Fix URL encoding of paths

        _uiState.value.server.doesItemExist(
            path,
            { exists ->
                if (exists) {
                    Log.d("HOME", "Folder already exists on server; not creating")
                    showSnackbar("Folder already exists on server")
                    return@doesItemExist
                }

                _uiState.value.server.createFolder(
                    path,
                    {
                        Log.d("HOME", "New folder created on server: $path")
                        _uiState.value.activeDirectory.addFolder(name, path)
                        showSnackbar("Folder created")
                    },
                    { _, json ->
                        val reason = json.getString("message")
                        Log.d("HOME", "Failed to create folder: $reason")
                        showSnackbar("Failed to create folder: $reason")
                    },
                    { error ->
                        Log.d("HOME", "Error when making folder: $error")
                    }
                )
            },
            { error ->
                Log.d("HOME", "Error when checking path existence: $error")
            }
        )
    }

    fun deleteItemLocally(item: RemoteItem) {
        item.markForLocalDeletion()
        Log.d("HOME", "Marked '${item.path}' for local deletion")

        showSnackbar(
            "Deleted '${item.name}' locally",
            "Undo",
            duration = SnackbarDuration.Short,
            onAction = {
                item.unmarkForLocalDeletion()
                Log.d("HOME", "Unmarked '${item.path}' for local deletion")
                showSnackbar("Restored '${item.name}'", duration = SnackbarDuration.Short)
            },
            onDismiss = {
                if (!item.markedForLocalDeletion) return@showSnackbar
                if (CRUDOperations.deleteItem(item.path)) {
                    item.unmarkForLocalDeletion()
                    return@showSnackbar
                }

                Log.d("HOME", "Failed to delete '${item.path}' locally")
                showToast("Failed to delete '${item.name}' locally")
            }
        )
    }

    fun deleteItemFromServer(item: RemoteItem) {
        item.markForServerDeletion()
        Log.d("HOME", "Marked '${item.path}' for server deletion")

        showSnackbar(
            "Deleted '${item.name}' on server",
            "Undo",
            duration = SnackbarDuration.Short,
            onAction = {
                item.unmarkForServerDeletion()
                Log.d("HOME", "Unmarked '${item.path}' for server deletion")
                showSnackbar("Restored '${item.name}' on server", duration = SnackbarDuration.Short)
            },
            onDismiss = {
                if (!item.markedForServerDeletion) return@showSnackbar
                _uiState.value.server.deleteItem(
                    item.path,
                    {
                        if (item.type == ItemType.FILE) {
                            _uiState.value.activeDirectory.removeFile(item as RemoteFile)
                        } else {
                            _uiState.value.activeDirectory.removeFolder(item as RemoteDirectory)
                        }
                        Log.d("HOME", "Deleted '${item.path}' on server")
                    },
                    { _, json ->
                        val reason = json.getString("message")
                        Log.d("HOME", "Failed to delete '${item.path}' on server: $reason")
                        showSnackbar("Failed to delete '${item.name}' on server: $reason")
                    },
                    { error ->
                        Log.d("HOME", "Error when delete '${item.path}' on server: $error")
                    }
                )
            }
        )
    }

    // Displayables methods
    /**
     * Shows a toast message to the screen.
     * @param message Message of the toast.
     * @param duration How long the toast should show on the screen.
     */
    private fun showToast(
        message: String,
        duration: Int = Toast.LENGTH_LONG
    ) {
        _uiState.update {
            it.copy(
                toastData = ToastData(message, duration)
            )
        }
    }

    /**
     * Clears the toast data.
     */
    fun clearToast() {
        showToast("")
    }

    /**
     * Shows a snackbar.
     *
     * @param message Message of the snackbar.
     * @param actionLabel Action label to shown as a button in the snackbar.
     * @param withDismissAction Whether to show a dismiss action in the snackbar.
     * @param duration How long the snackbar will be shown.
     * @param onAction Action to take when the action button is pressed.
     * @param onDismiss Action to take when the snackbar is dismissed.
     */
    private fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        withDismissAction: Boolean = false,
        duration: SnackbarDuration = if (actionLabel == null) SnackbarDuration.Short else
            SnackbarDuration.Indefinite,
        onAction: (() -> Unit)? = null,
        onDismiss: (() -> Unit)? = null,
        snackbarFree: Boolean = false
    ) {
        // If snackbar is not free yet (and not going to be free), run the current dismiss action
        if (!_uiState.value.snackbarData.snackbarFree && !snackbarFree) {
            if (_uiState.value.snackbarData.onDismiss != null) {
                _uiState.value.snackbarData.onDismiss!!()
            }
        }

        // Then we can safely update the snackbar
        _uiState.update {
            it.copy(
                snackbarData = SnackbarData(
                    message = message,
                    actionLabel = actionLabel,
                    withDismissAction = withDismissAction,
                    duration = duration,
                    onAction = onAction,
                    onDismiss = onDismiss,
                    snackbarFree = snackbarFree
                )
            )
        }
    }

    /**
     * Clears the snackbar data.
     */
    fun clearSnackbar() {
        showSnackbar(
            message = "",
            snackbarFree = true
        )
    }

    // Other methods
    /**
     * Initializes the processing dialog.
     *
     * @param newTitle New title for the processing dialog.
     * @param newSubtitle New subtitle for the processing dialog.
     * @param newProgress New progress value for the processing dialog. Put `null` if the dialog is
     * meant to be indefinite.
     */
    private fun initProcessingDialog(
        newTitle: String,
        newSubtitle: String = "",
        newProgress: Float? = 0f
    ) {
        if (showProcessingDialog) {
            hideProcessingDialog()
        }
        showProcessingDialog = true
        processingDialogTitle = newTitle
        processingDialogSubtitle = newSubtitle
        processingDialogProgress = newProgress
    }

    /**
     * Hides the processing dialog.
     */
    private fun hideProcessingDialog() {
        showProcessingDialog = false
        processingDialogTitle = ""
        processingDialogSubtitle = ""
        processingDialogProgress = null
        runBlocking {
            delay(100)
            // FIXME: This is quite hacky to fix the issue of the non-updating of the title.
            //        Is there a better way?
        }
    }
}