/*
 * Copyright (c) 2023-2024 PhotonicGluon.
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

package site.overwrite.encryptedfilesapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Class that contains dialogs.
 */
class Dialogs {
    // Composables
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
         * @param textFieldValidator Validation function that validates the input for the text
         * field.
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

        /**
         * Shows a dialog that looks like a circular progress indicator. Indicates some process
         * that may take an indefinite amount of time to complete.
         */
        @Composable
        fun LoadingIndicatorDialog() {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        @Composable
        private fun ProgressBar(
            progress: Float?,
            onCancel: (() -> Unit)?
        ) {
            val progressModifier = if (onCancel == null) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.fillMaxWidth(0.9f)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = progressModifier,
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = progressModifier,
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                if (onCancel != null) {
                    IconButton(
                        onClick = { onCancel() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Cancel"
                        )
                    }
                }
            }
            if (progress != null) {
                Text(
                    "${
                        String.format(
                            "%.02f",
                            progress * 100
                        )
                    }%",
                    modifier = Modifier.offset(y = (-8).dp),
                    fontSize = 12.sp
                )
            }
        }

        /**
         * Creates a new progress indicator dialog.
         *
         * @param dialogTitle Title of the dialog.
         * @param dialogSubtitle Subtitle of the dialog.
         * @param progress Progress to show on the progress indicator. If `null` then the progress
         * indicator will be indeterminate.
         * @param onCancel Function to run when the cancel button is pressed.
         */
        @Composable
        fun ProgressIndicatorDialog(
            dialogTitle: String,
            dialogSubtitle: String = "",
            progress: Float?,
            onCancel: (() -> Unit)? = null
        ) {
            Dialog(
                onDismissRequest = {},
                DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = dialogTitle,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        if (dialogSubtitle.isNotEmpty()) {
                            Text(
                                text = dialogSubtitle,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        ProgressBar(
                            progress = progress,
                            onCancel = onCancel
                        )
                    }
                }
            }
        }
    }
}

// Previews
@Preview
@Composable
fun ConfirmDialogPreview1() {
    Dialogs.ConfirmDialog(
        dialogTitle = "Test Confirm Dialog 1",
        dialogContent = {
            Column {
                Text("Test 1")
                Text("Test 2")
            }
        },
        confirmText = "Confirm",
        dismissText = "Dismiss",
        onConfirmation = {},
        onDismissal = {}
    )
}

@Preview
@Composable
fun ConfirmDialogPreview2() {
    Dialogs.ConfirmDialog(
        dialogTitle = "Test Confirm Dialog 2",
        dialogContent = {
            Column {
                Text("Test 1")
                Text("Test 2")
            }
        },
        confirmText = "Confirm",
        dismissText = "Dismiss",
        onConfirmation = {},
        onDismissal = {},
        icon = Icons.Filled.Tablet,
        iconDesc = "Tablet"
    )
}

@Preview
@Composable
fun YesNoDialogPreview1() {
    Dialogs.YesNoDialog(
        dialogTitle = "Test Yes No Dialog 1",
        dialogContent = {
            Column {
                Text("Test 1")
                Text("Test 2")
            }
        },
        onYes = {},
        onNo = {}
    )
}

@Preview
@Composable
fun YesNoDialogPreview2() {
    Dialogs.YesNoDialog(
        dialogTitle = "Test Yes No Dialog 2",
        dialogContent = {
            Column {
                Text("Test 1")
                Text("Test 2")
            }
        },
        onYes = {},
        onNo = {},
        icon = Icons.Filled.Tablet,
        iconDesc = "Tablet"
    )
}

@Preview
@Composable
fun TextInputDialogPreview1() {
    Dialogs.TextInputDialog(
        dialogTitle = "Test Text Input Dialog",
        textFieldLabel = "Test Text Field",
        onConfirmation = { _ -> },
        onDismissal = { },
        textFieldValidator = { _ -> false }
    )
}

@Preview
@Composable
fun TextInputDialogPreview2() {
    Dialogs.TextInputDialog(
        dialogTitle = "Test Text Input Dialog",
        textFieldLabel = "Test Text Field",
        onConfirmation = { _ -> },
        onDismissal = { },
        textFieldValidator = { _ -> false },
        textFieldPlaceholder = "Placeholder",
        textFieldErrorText = "Error text",
        icon = Icons.Filled.Tablet,
        iconDesc = "Tablet"
    )
}

@Preview
@Composable
fun LoadingIndicatorDialogPreview() {
    Dialogs.LoadingIndicatorDialog()
}

@Preview
@Composable
fun ProgressIndicatorDialogPreview1() {
    Dialogs.ProgressIndicatorDialog(
        dialogTitle = "Test Progress Indicator Dialog 1",
        progress = 0.5678f
    )
}

@Preview
@Composable
fun ProgressIndicatorDialogPreview2() {
    Dialogs.ProgressIndicatorDialog(
        dialogTitle = "Test Progress Indicator Dialog 2",
        dialogSubtitle = "Some subtitle",
        progress = 0.6789f
    )
}

@Preview
@Composable
fun ProgressIndicatorDialogPreview3() {
    Dialogs.ProgressIndicatorDialog(
        dialogTitle = "Test Progress Indicator Dialog 3",
        progress = null
    )
}

@Preview
@Composable
fun ProgressIndicatorDialogPreview4() {
    Dialogs.ProgressIndicatorDialog(
        dialogTitle = "Test Progress Indicator Dialog 4",
        dialogSubtitle = "Some subtitle",
        progress = 0.2222f,
        onCancel = {}
    )
}