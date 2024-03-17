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
import site.overwrite.encryptedfilesapp.data.RemoteFolder

data class HomeViewUIState(
    val server: Server = Server(""),
    val username: String = "",
    val password: String = "",
    val encryptionParameters: EncryptionParameters = EncryptionParameters(),
    val rootFolder: RemoteFolder = RemoteFolder()
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
}