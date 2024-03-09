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

import java.io.Serializable

data class Credentials(
    var serverURL: String = "",
    var username: String = "",
    var password: String = ""
) : Serializable {
    fun isNotEmpty(): Boolean {
        return serverURL.isNotBlank()
                && username.isNotBlank()
                && password.isNotEmpty()
    }

    fun isNotEmptyWithResult(): CredentialCheckResult {
        if (serverURL.isBlank()) {
            return CredentialCheckResult.INVALID_URL
        }
        if (username.isBlank()) {
            return CredentialCheckResult.INVALID_USERNAME
        }
        if (password.isEmpty()) {
            return CredentialCheckResult.INVALID_PASSWORD
        }
        return CredentialCheckResult.VALID
    }
}

enum class CredentialCheckResult {
    PENDING,
    INVALID_URL,
    INVALID_USERNAME,
    INVALID_PASSWORD,
    VALID
}
