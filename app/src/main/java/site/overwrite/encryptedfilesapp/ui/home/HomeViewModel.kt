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
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import site.overwrite.encryptedfilesapp.Server
import site.overwrite.encryptedfilesapp.data.Cryptography
import site.overwrite.encryptedfilesapp.data.EncryptionParameters
import site.overwrite.encryptedfilesapp.data.ItemType
import site.overwrite.encryptedfilesapp.data.RemoteDirectory
import site.overwrite.encryptedfilesapp.data.RemoteItem
import site.overwrite.encryptedfilesapp.io.IOMethods

data class HomeViewUIState(
    // Main fields
    val server: Server = Server(""),
    val username: String = "",
    val password: String = "",
    val encryptionParameters: EncryptionParameters = EncryptionParameters(),
    val activeDirectory: RemoteDirectory = RemoteDirectory(
        "",
        "",
        0,
        emptyArray(),
        emptyArray(),
        null
    ),

    // Toast message fields
    val toastMessage: String = "",
    val toastDuration: Int = Toast.LENGTH_LONG
) {
    val parentDirectory: RemoteDirectory?
        get() = activeDirectory.parentDir
    val atRootDirectory: Boolean
        get() = parentDirectory == null
}

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeViewUIState())
    val uiState: StateFlow<HomeViewUIState> = _uiState.asStateFlow()

    init {
        _uiState.value = HomeViewUIState()
    }

    // Mutable values
    var loggedIn by mutableStateOf(false)
        private set
    var showConfirmLogoutDialog by mutableStateOf(false)

    // Auth methods
    @OptIn(ExperimentalStdlibApi::class)
    fun login(
        server: Server,
        username: String,
        password: String
    ) {
        Log.d("MAIN", "Start login process")

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
                        "MAIN",
                        "Logged in as '$username' into ${_uiState.value.server.serverURL}"
                    )
                    Log.d(
                        "MAIN",
                        "Got initialization vector '$encryptionIV'," +
                                " salt '$encryptionSalt', and encryption key (as hex string)" +
                                " '${encryptionKey.toHexString()}'"
                    )
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

    fun logout(
        server: Server,
        context: Context
    ) {
        Log.d("MAIN", "Start logout process")
        server.handleLogout {
            loggedIn = false
            Log.d("MAIN", "Deleting all folders")
            IOMethods.deleteItem("")  // TODO: Use a progress bar for deletion
            Log.d("MAIN", "Logged out")
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
            setToastMessage("Cannot go back to previous directory")
        }
    }

    fun directoryItemOnClick(item: RemoteItem) {
        if (item.type == ItemType.DIRECTORY) {
            changeActiveDirectory(item as RemoteDirectory)
            return
        }

        // TODO: Implement file click
        setToastMessage("To be implemented", Toast.LENGTH_SHORT)
    }

    // CRUD methods
    private fun getRootFolderItems() {
        _uiState.value.server.listDir(
            "",
            { json ->
                // Get the content as a JSON array
                val rootFolder = RemoteDirectory.fromJSON(json)
                _uiState.update {
                    it.copy(
                        activeDirectory = rootFolder
                    )
                }
            },
            { _, json ->
                Log.d(
                    "MAIN",
                    "Failed to get root folder items: ${json.getString("message")}"
                )
            },
            { error -> Log.d("MAIN", "Error when getting folder items: $error") }
        )
    }

    // Other methods
    /**
     * Shows a toast message to the screen.
     * @param message Message of the toast.
     * @param duration How long the toast should show on the screen.
     */
    fun setToastMessage(
        message: String,
        duration: Int = Toast.LENGTH_LONG
    ) {
        _uiState.update {
            it.copy(
                toastMessage = message,
                toastDuration = duration
            )
        }
    }
}