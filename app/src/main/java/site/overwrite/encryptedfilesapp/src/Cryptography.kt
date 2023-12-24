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

import android.util.Base64
import java.lang.IllegalArgumentException
import java.security.SecureRandom
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

// CONSTANTS
private const val AES_KEY_LENGTH = 256
private const val AES_TRANSFORMATION = "AES/CBC/PKCS5Padding"

private const val KEYGEN_ALGORITHM = "PBKDF2WithHmacSHA256"
private const val KEYGEN_ITERATIONS = 120000

/**
 * Cryptography functions.
 */
class Cryptography {
    companion object {
        /**
         * Generates the key for AES encryption based on a user's password.
         *
         * @param password User's password to generate the key for.
         * @param salt Salt for the password.
         * @return AES key.
         */
        fun genAESKey(password: String, salt: String): ByteArray {
            val factory = SecretKeyFactory.getInstance(KEYGEN_ALGORITHM)
            val spec = PBEKeySpec(password.toCharArray(), salt.toByteArray(), KEYGEN_ITERATIONS, AES_KEY_LENGTH)
            val key = factory.generateSecret(spec)
            return key.encoded
        }

        /**
         * Generate a random initialization vector for AES encryption.
         *
         * @param length Length of the IV.
         * @return IV as a hexadecimal string.
         */
        @OptIn(ExperimentalStdlibApi::class)
        fun genIV(length: Int): String {
            val rand = SecureRandom()
            val iv = ByteArray(length)
            rand.nextBytes(iv)
            return iv.toHexString()
        }

        fun encryptAES(plainText: String, key: ByteArray, iv: String): String {
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val secretKeySpec = SecretKeySpec(key, "AES")
            val ivParameterSpec = IvParameterSpec(iv.toByteArray())
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray())
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        }

        fun decryptAES(encryptedText: String, key: ByteArray, iv: String): String {
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val secretKeySpec = SecretKeySpec(key, "AES")
            val ivParameterSpec = IvParameterSpec(iv.toByteArray())
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)
            try {
                val encryptedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
                val decryptedBytes = cipher.doFinal(encryptedBytes)
                return String(decryptedBytes)
            } catch (e: IllegalArgumentException) {
                throw InvalidDecryptionException("Invalid decryption of ciphertext")
            } catch (e: BadPaddingException) {
                throw InvalidDecryptionException("Invalid decryption of ciphertext")
            }
        }
    }
}

class InvalidDecryptionException(message: String): Exception(message)
