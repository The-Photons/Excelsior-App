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

package site.overwrite.encryptedfilesapp.cryptography

data class EncryptionParameters(
    val iv: String = "",
    val salt: String = "",
    val key: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptionParameters

        if (iv != other.iv) return false
        if (salt != other.salt) return false
        if (key != null) {
            if (other.key == null) return false
            if (!key.contentEquals(other.key)) return false
        } else if (other.key != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = iv.hashCode()
        result = 31 * result + salt.hashCode()
        result = 31 * result + (key?.contentHashCode() ?: 0)
        return result
    }

    fun isFilled(): Boolean {
        return iv.isNotEmpty() && salt.isNotEmpty() && key != null && key.isNotEmpty()
    }
}
