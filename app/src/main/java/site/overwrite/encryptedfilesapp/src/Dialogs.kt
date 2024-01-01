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

package site.overwrite.encryptedfilesapp.src

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Class that contains dialogs.
 */
class Dialogs {
    companion object {
        /**
         * Creates a new confirmation dialog.
         *
         * @param dialogTitle Title of the dialog.
         * @param dialogContent Content of the dialog.
         * @param confirmText Text to show for the "confirm" button.
         * @param dismissText Text to show for the "dismiss" button.
         * @param onDismissal Function that handles dismissal requests.
         * @param onConfirmation Function that handles confirmation requests.
         * @param icon Optional icon to display on the dialog.
         * @param iconDesc Optional description for the icon. If an icon is provided, then this must
         * be present.
         */
        @Composable
        fun ConfirmDialog(
            dialogTitle: String,
            dialogContent: @Composable (() -> Unit)?,
            confirmText: String,
            dismissText: String,
            onConfirmation: () -> Unit,
            onDismissal: () -> Unit,
            icon: ImageVector? = null,
            iconDesc: String? = null
        ) {
            AlertDialog(
                icon = {
                    if (icon != null) {
                        Icon(icon, iconDesc)
                    }
                },
                title = { Text(text = dialogTitle) },
                text = dialogContent,
                onDismissRequest = { onDismissal() },
                confirmButton = {
                    TextButton(
                        onClick = { onConfirmation() }
                    ) {
                        Text(confirmText)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { onDismissal() }
                    ) {
                        Text(dismissText)
                    }
                }
            )
        }

        /**
         * Creates a new yes/no dialog.
         *
         * @param dialogTitle Title of the dialog.
         * @param dialogContent Content of the dialog.
         * @param onYes Function that handles the "yes" request.
         * @param onNo Function that handles the "no" request and dismissals.
         * @param icon Optional icon to display on the dialog.
         * @param iconDesc Optional description for the icon. If an icon is provided, then this must
         * be present.
         */
        @Composable
        fun YesNoDialog(
            dialogTitle: String,
            dialogContent: @Composable (() -> Unit)?,
            onYes: () -> Unit,
            onNo: () -> Unit,
            icon: ImageVector? = null,
            iconDesc: String? = null
        ) {
            ConfirmDialog(
                dialogTitle = dialogTitle,
                dialogContent = dialogContent,
                confirmText = "Yes",
                dismissText = "No",
                onConfirmation = { onYes() },
                onDismissal = { onNo() },
                icon = icon,
                iconDesc = iconDesc
            )
        }

        /**
         * Creates a new text input dialog.
         *
         * @param dialogTitle Title of the dialog.
         * @param textFieldLabel Label for the text field.
         * @param textFieldPlaceholder Placeholder for the text field.
         * @param textFieldErrorText Text to show if the validation fails.
         * @param onConfirmation Function that handles confirmation requests.
         * @param onDismissal Function that handles dismissal requests.
         * @param textFieldValidator Validation function that validates the input for the text field.
         * @param icon Optional icon to display on the dialog.
         * @param iconDesc Optional description for the icon. If an icon is provided, then this must
         * be present.
         * @param singleLine Whether the text field accepts only one line of text.
         */
        @Composable
        fun TextInputDialog(
            dialogTitle: String,
            textFieldLabel: String,
            textFieldPlaceholder: String = "",
            textFieldErrorText: String = "Invalid input",
            onConfirmation: (String) -> Unit,
            onDismissal: () -> Unit,
            textFieldValidator: (String) -> Boolean,
            icon: ImageVector? = null,
            iconDesc: String? = null,
            singleLine: Boolean = true,
        ) {
            // Attributes
            var text by remember { mutableStateOf("") }
            var isInvalidText by remember { mutableStateOf(false) }

            val focusRequester = remember { FocusRequester() }

            // Dialog
            AlertDialog(
                icon = {
                    if (icon != null) {
                        Icon(icon, iconDesc)
                    }
                },
                title = { Text(text = dialogTitle) },
                text = {
                    TextField(
                        modifier = Modifier.focusRequester(focusRequester),
                        value = text,
                        onValueChange = {
                            text = it
                            isInvalidText = !textFieldValidator(text)
                        },
                        label = { Text(textFieldLabel) },
                        placeholder = { Text(textFieldPlaceholder) },
                        isError = isInvalidText,
                        supportingText = {
                            if (isInvalidText) {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = textFieldErrorText,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        singleLine = singleLine
                    )
                },
                onDismissRequest = { onDismissal() },
                confirmButton = {
                    TextButton(
                        enabled = !(text.isBlank() || isInvalidText),
                        onClick = { onConfirmation(text) }
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { onDismissal() }
                    ) {
                        Text("Cancel")
                    }
                }
            )

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
    }
}