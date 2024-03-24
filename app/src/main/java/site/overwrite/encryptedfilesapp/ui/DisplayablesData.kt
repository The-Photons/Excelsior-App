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

package site.overwrite.encryptedfilesapp.ui

import android.widget.Toast
import androidx.compose.material3.SnackbarDuration

data class ToastData(
    val message: String = "",
    val duration: Int = Toast.LENGTH_LONG
) {
    val isEmpty: Boolean
        get() = message.isBlank()
}

data class SnackbarData(
    val message: String = "",
    val actionLabel: String? = null,
    val withDismissAction: Boolean = false,
    val duration: SnackbarDuration = if (actionLabel == null) SnackbarDuration.Short else
        SnackbarDuration.Indefinite,
    val onAction: (() -> Unit)? = null,
    val onDismiss: (() -> Unit)? = null,
    val snackbarFree: Boolean = true
) {
    val isEmpty: Boolean
        get() = message.isBlank()
                || (actionLabel != null && onAction == null && onDismiss == null)
}
