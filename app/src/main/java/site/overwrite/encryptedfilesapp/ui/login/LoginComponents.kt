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

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import site.overwrite.encryptedfilesapp.data.CredentialCheckResult
import site.overwrite.encryptedfilesapp.ui.theme.EncryptedFilesAppTheme
import site.overwrite.encryptedfilesapp.ui.Dialogs

// Constants
const val SERVER_FIELD_LABEL = "Server URL"
const val SERVER_FIELD_PLACEHOLDER = "https://example.com/"
const val SERVER_FIELD_ERROR_TEXT = "Invalid URL"

const val USERNAME_FIELD_LABEL = "Username"
const val USERNAME_FIELD_PLACEHOLDER = "Username"
const val USERNAME_FIELD_ERROR_TEXT = "Invalid Username"

const val PASSWORD_FIELD_LABEL = "Password"
const val PASSWORD_FIELD_PLACEHOLDER = "Password"
const val PASSWORD_FIELD_ERROR_TEXT = "Invalid Password"

// Composables
/**
 * Form to handle the login.
 *
 * @param serverURL Default value for the server URL field.
 * @param username Default value for the username field.
 * @param loginViewModel Login view model that for the login activity.
 */
@Composable
fun LoginForm(
    serverURL: String = "",
    username: String = "",
    loginViewModel: LoginViewModel = viewModel()
) {
    val loginUIState by loginViewModel.uiState.collectAsState()
    loginViewModel.updateServerURL(serverURL)
    loginViewModel.updateUsername(username)

    Surface {
        val context = LocalContext.current

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp)
        ) {
            Text(text = "Login", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.padding(vertical = 5.dp))
            ServerURLField(
                value = loginViewModel.serverURL,
                onChange = { loginViewModel.updateServerURL(it) },
                modifier = Modifier.fillMaxWidth(),
                isError = !loginViewModel.hasUpdatedValues &&
                        loginUIState.credentialCheckResult == CredentialCheckResult.INVALID_URL
            )
            UsernameField(
                value = loginViewModel.username,
                onChange = { loginViewModel.updateUsername(it) },
                modifier = Modifier.fillMaxWidth(),
                isError = !loginViewModel.hasUpdatedValues &&
                        loginUIState.credentialCheckResult == CredentialCheckResult.INVALID_USERNAME
            )
            PasswordField(
                value = loginViewModel.password,
                onChange = { loginViewModel.updatePassword(it) },
                submit = { loginViewModel.submit(context) },
                modifier = Modifier.fillMaxWidth(),
                isError = !loginViewModel.hasUpdatedValues &&
                        loginUIState.credentialCheckResult == CredentialCheckResult.INVALID_PASSWORD
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { loginViewModel.submit(context) },
                enabled = loginViewModel.allFieldsFilled(),
                shape = RoundedCornerShape(5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }
        }

        if (loginViewModel.isLoading) {
            Dialogs.LoadingIndicatorDialog()
        }
    }
}

/**
 * Field that asks for a server URL input.
 *
 * @param value Value to use for the field.
 * @param onChange Function to run upon input change.
 * @param isError Whether the value is erroneous or not.
 * @param modifier Modifier for the input field.
 * @param label Label to display for the input field.
 * @param placeholder Placeholder for the input field.
 * @param errorText Text to show if the value is erroneous.
 */
@Composable
fun ServerURLField(
    value: String,
    onChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
    label: String = SERVER_FIELD_LABEL,
    placeholder: String = SERVER_FIELD_PLACEHOLDER,
    errorText: String = SERVER_FIELD_ERROR_TEXT
) {
    val focusManager = LocalFocusManager.current
    val leadingIcon = @Composable {
        Icon(
            Icons.Default.Dns,
            contentDescription = "Server",
            tint = MaterialTheme.colorScheme.primary
        )
    }

    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        leadingIcon = leadingIcon,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) }
        ),
        placeholder = { Text(placeholder) },
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = {
            if (isError) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = errorText,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}

/**
 * Field that asks for a username input.
 *
 * @param value Value to place in the field.
 * @param onChange Function to run upon input change.
 * @param isError Whether the value is erroneous or not.
 * @param modifier Modifier for the input field.
 * @param label Label to display for the input field.
 * @param placeholder Placeholder for the input field.
 * @param errorText Text to show if the value is erroneous.
 */
@Composable
fun UsernameField(
    value: String,
    onChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
    label: String = USERNAME_FIELD_LABEL,
    placeholder: String = USERNAME_FIELD_PLACEHOLDER,
    errorText: String = USERNAME_FIELD_ERROR_TEXT
) {
    val focusManager = LocalFocusManager.current
    val leadingIcon = @Composable {
        Icon(
            Icons.Default.Person,
            contentDescription = "Username",
            tint = MaterialTheme.colorScheme.primary
        )
    }

    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        leadingIcon = leadingIcon,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) }
        ),
        placeholder = { Text(placeholder) },
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = {
            if (isError) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = errorText,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}

/**
 * Field that asks for a password input.
 *
 * @param value Value to place in the field.
 * @param onChange Function to run upon input change.
 * @param submit Function to run when the "done" button is pressed.
 * @param isError Whether the value is erroneous or not.
 * @param modifier Modifier for the input field.
 * @param label Label to display for the input field.
 * @param placeholder Placeholder for the input field.
 * @param errorText Text to show if the value is erroneous.
 */
@Composable
fun PasswordField(
    value: String,
    onChange: (String) -> Unit,
    submit: () -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
    label: String = PASSWORD_FIELD_LABEL,
    placeholder: String = PASSWORD_FIELD_PLACEHOLDER,
    errorText: String = PASSWORD_FIELD_ERROR_TEXT
) {
    var isPasswordVisible by remember { mutableStateOf(false) }

    val leadingIcon = @Composable {
        Icon(
            Icons.Default.Key,
            contentDescription = "Password",
            tint = MaterialTheme.colorScheme.primary
        )
    }
    val trailingIcon = @Composable {
        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
            Icon(
                if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Password
        ),
        keyboardActions = KeyboardActions(
            onDone = { submit() }
        ),
        placeholder = { Text(placeholder) },
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = {
            if (isError) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = errorText,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else
            PasswordVisualTransformation()
    )
}

// Previews
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun LoginFormPreviewLight() {
    EncryptedFilesAppTheme {
        LoginForm()
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LoginFormPreviewDark() {
    EncryptedFilesAppTheme {
        LoginForm()
    }
}
