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

package site.overwrite.encryptedfilesapp.ui.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import site.overwrite.encryptedfilesapp.DEFAULT_TIMEOUT_MILLIS
import site.overwrite.encryptedfilesapp.LoginResult
import site.overwrite.encryptedfilesapp.Server
import site.overwrite.encryptedfilesapp.data.CredentialCheckResult
import site.overwrite.encryptedfilesapp.data.Credentials
import site.overwrite.encryptedfilesapp.ui.home.HomeActivity

data class LoginViewUIState(
    val credentials: Credentials = Credentials(),
    val credentialCheckResult: CredentialCheckResult = CredentialCheckResult.PENDING
) {
    fun checkCredentials(onResult: (CredentialCheckResult) -> Unit) {
        // Check that all fields are non-empty
        val emptyCheckResult = credentials.checkCredentialsIsEmpty()
        if (emptyCheckResult != CredentialCheckResult.VALID) {
            Log.d("LOGIN", "A field is empty")
            onResult(emptyCheckResult)
            return
        }

        // Initialize the HTTP client to use
        val client = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = DEFAULT_TIMEOUT_MILLIS
            }
        }

        Server.isValidURL(
            credentials.serverURL,
            CoroutineScope(Job()),
            client
        ) { isValidURL ->
            if (!isValidURL) {
                Log.d("LOGIN", "Invalid server URL: ${credentials.serverURL}")
                onResult(CredentialCheckResult.INVALID_URL)
                return@isValidURL
            }

            Log.d("LOGIN", "Good URL: ${credentials.serverURL}")

            // Now check the username and password
            val server = Server(credentials.serverURL)
            server.handleLogin(
                credentials.username,
                credentials.password,
                false
            ) { loginResult ->
                when (loginResult) {
                    LoginResult.SUCCESS -> {
                        Log.d("LOGIN", "Credentials valid; logged in as '${credentials.username}'")
                        onResult(CredentialCheckResult.VALID)
                    }

                    LoginResult.TIMEOUT -> {
                        Log.d("LOGIN", "Connection timed out")
                        onResult(CredentialCheckResult.TIMEOUT)
                    }

                    LoginResult.INVALID_USERNAME -> {
                        Log.d("LOGIN", "Invalid username: ${credentials.username}")
                        onResult(CredentialCheckResult.INVALID_USERNAME)
                    }

                    LoginResult.INVALID_PASSWORD -> {
                        Log.d("LOGIN", "Invalid password")
                        onResult(CredentialCheckResult.INVALID_PASSWORD)
                    }
                }
            }
        }
    }
}

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LoginViewUIState())
    val uiState: StateFlow<LoginViewUIState> = _uiState.asStateFlow()  // Read-only state flow

    init {
        _uiState.value = LoginViewUIState()
    }

    // Mutable values
    var hasUpdatedValues by mutableStateOf(true)
    var isLoading by mutableStateOf(false)

    var serverURL by mutableStateOf("")
        private set
    var username by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set

    // Setters
    fun updateServerURL(newURL: String) {
        hasUpdatedValues = true
        serverURL = newURL
    }

    fun updateUsername(newUsername: String) {
        hasUpdatedValues = true
        username = newUsername
    }

    fun updatePassword(newPassword: String) {
        hasUpdatedValues = true
        password = newPassword
    }

    // Other methods
    fun allFieldsFilled(): Boolean {
        return serverURL.isNotBlank() && username.isNotBlank() && password.isNotEmpty()
    }

    fun submit(context: Context) {
        isLoading = true

        // Update the login UI state
        _uiState.update {
            it.copy(
                credentials = Credentials(
                    serverURL, username, password
                ),
                credentialCheckResult = CredentialCheckResult.PENDING
            )
        }
        hasUpdatedValues = false

        // Then check the credentials
        _uiState.value.checkCredentials { result ->
            isLoading = false
            _uiState.update {
                it.copy(credentialCheckResult = result)
            }

            if (result != CredentialCheckResult.VALID) {
                password = ""
            } else {
                // Send the credentials onwards
                val intent = Intent(context, HomeActivity::class.java)
                intent.putExtra("credentials", _uiState.value.credentials)
                context.startActivity(intent)
                (context as Activity).finish()
            }
        }
    }
}