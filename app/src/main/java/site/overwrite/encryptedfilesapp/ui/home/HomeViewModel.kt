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
import site.overwrite.encryptedfilesapp.data.Cryptography
import site.overwrite.encryptedfilesapp.Server
import site.overwrite.encryptedfilesapp.data.EncryptionParameters
import site.overwrite.encryptedfilesapp.data.ItemType
import site.overwrite.encryptedfilesapp.data.RemoteDirectory
import site.overwrite.encryptedfilesapp.data.RemoteItem
import site.overwrite.encryptedfilesapp.data.RemotePreviousDirectory

data class HomeViewUIState(
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
    )
)

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeViewUIState())
    val uiState: StateFlow<HomeViewUIState> = _uiState.asStateFlow()

    init {
        _uiState.value = HomeViewUIState()
    }

    // Mutable values
    var loggedIn by mutableStateOf(false)
        private set

    // Setters

    // Directory item methods
    fun directoryItemOnClick(item: RemoteItem) {
        when (item.type) {
            ItemType.FILE -> {
                // TODO: Implement
                // FIXME: Use better toasts...
                //Toast.makeText(null, "To be implemented", Toast.LENGTH_LONG).show()
            }

            ItemType.DIRECTORY -> {
                _uiState.update {
                    it.copy(
                        activeDirectory = (item as RemoteDirectory)
                    )
                }
            }

            ItemType.PREVIOUS_DIRECTORY_MARKER -> {
                if (_uiState.value.activeDirectory.parentDir != null) {
                    _uiState.update {
                        it.copy(
                            activeDirectory = _uiState.value.activeDirectory.parentDir!!
                        )
                    }
                } else {
                    // FIXME: Use better toasts...
                    Log.d("HOME", "Cannot go back to previous directory")
//                    Toast.makeText(null, "Cannot go back to previous directory", Toast.LENGTH_LONG)
//                        .show()
                }
            }
        }
    }

    // Other methods
    @OptIn(ExperimentalStdlibApi::class)
    fun loginToServer(
        server: Server,
        username: String,
        password: String
    ) {
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
                        "Got server URL '${_uiState.value.server.serverURL}'," +
                                " initialization vector '$encryptionIV'," +
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
}