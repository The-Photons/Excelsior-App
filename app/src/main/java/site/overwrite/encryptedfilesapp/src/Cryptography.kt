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
import android.util.Log
import site.overwrite.encryptedfilesapp.activities.EncryptionBufferSize
import java.io.FileOutputStream
import java.io.InputStream
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
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
        fun genAESKey(
            password: String,
            salt: String
        ): ByteArray {
            val factory = SecretKeyFactory.getInstance(KEYGEN_ALGORITHM)
            val spec = PBEKeySpec(
                password.toCharArray(),
                salt.toByteArray(),
                KEYGEN_ITERATIONS,
                AES_KEY_LENGTH
            )
            val key = factory.generateSecret(spec)
            return key.encoded
        }

        /**
         * Encrypts bytes using AES.
         *
         * @param plainText Bytes to encrypt
         * @param key AES encryption/decryption key.
         * @param iv Initialization vector used to encrypt the data.
         * @return Encrypted text. This is a Base64 string.
         */
        fun encryptAES(
            plainText: ByteArray,
            key: ByteArray,
            iv: String
        ): String {
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val secretKeySpec = SecretKeySpec(key, "AES")
            val ivParameterSpec = IvParameterSpec(iv.toByteArray())
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec)
            val encryptedBytes = cipher.doFinal(plainText)
            return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        }

        /**
         * Encrypts bytes using AES.
         *
         * @param inputStream Input stream for the bytes to encrypt.
         * @param outputStream Output stream for encrypted bytes.
         * @param bufferSize Encryption buffer size value.
         * @param key AES encryption/decryption key.
         * @param iv Initialization vector used to encrypt the data.
         * @param listener Listener for changes in the number of bytes encrypted.
         */
        fun encryptAES(
            inputStream: InputStream,
            outputStream: FileOutputStream,
            bufferSize: EncryptionBufferSize,
            key: ByteArray,
            iv: String,
            listener: (numBytesEncrypted: Int) -> Unit = {_->}
        ) {
            // Set up cipher
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val secretKeySpec = SecretKeySpec(key, "AES")
            val ivParameterSpec = IvParameterSpec(iv.toByteArray())
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec)

            // Then process the encryption
            val cipherOutputStream = CipherOutputStream(outputStream, cipher)

            val buffer = ByteArray(bufferSize.size)
            var numBytesDecrypted = 0  // FIXME: Shouldn't this be long?
            var numReadBytes: Int
            inputStream.use { input ->
                cipherOutputStream.use { output ->
                    while (true) {
                        // Read bytes from input and decrypt them
                        numReadBytes = input.read(buffer)
                        if (numReadBytes == -1) {
                            break
                        }

                        Log.d("CRYPTO", "Bytes Dec: $numBytesDecrypted, Bytes Read: $numReadBytes")
                        output.write(buffer, 0, numReadBytes)

                        // Update the statuses
                        numBytesDecrypted += numReadBytes
                        listener(numBytesDecrypted)
                    }
                }
            }
        }

        /**
         * Decrypts encrypted AES text.
         *
         * @param encryptedText Text to decrypt. This text *should* be a Base64 string.
         * @param key AES encryption/decryption key.
         * @param iv Initialization vector used to encrypt the data.
         * @return Original plaintext.
         */
        fun decryptAES(
            encryptedText: String,
            key: ByteArray,
            iv: String
        ): ByteArray {
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val secretKeySpec = SecretKeySpec(key, "AES")
            val ivParameterSpec = IvParameterSpec(iv.toByteArray())
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)
            try {
                val encryptedBytes = Base64.decode(encryptedText, Base64.NO_WRAP)
                return cipher.doFinal(encryptedBytes)
            } catch (e: IllegalArgumentException) {
                throw InvalidDecryptionException("Invalid decryption of ciphertext")
            } catch (e: BadPaddingException) {
                throw InvalidDecryptionException("Invalid decryption of ciphertext")
            }
        }

        /**
         * Decrypts encrypted AES text.
         *
         * @param inputStream Input stream for the bytes to decrypt. This should *not* be a Base64
         * encrypted string.
         * @param outputStream Output stream for decrypted bytes.
         * @param bufferSize Encryption buffer size value.
         * @param key AES encryption/decryption key.
         * @param iv Initialization vector used to encrypt the data.
         * @param listener Listener for changes in the number of bytes encrypted.
         */
        fun decryptAES(
            inputStream: InputStream,
            outputStream: FileOutputStream,
            bufferSize: EncryptionBufferSize,
            key: ByteArray,
            iv: String,
            listener: (numBytesDecrypted: Int) -> Unit = { _->}
        ) {
            // Set up cipher
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val secretKeySpec = SecretKeySpec(key, "AES")
            val ivParameterSpec = IvParameterSpec(iv.toByteArray())
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)

            // Then process the decryption
            val cipherInputStream = CipherInputStream(inputStream, cipher)

            val buffer = ByteArray(bufferSize.size)
            var numBytesDecrypted = 0  // FIXME: Shouldn't this be long?
            var numReadBytes: Int
            cipherInputStream.use { input ->
                outputStream.use { output ->
                    while (true) {
                        // Read bytes from input and decrypt them
                        numReadBytes = input.read(buffer)
                        if (numReadBytes == -1) {
                            break
                        }

                        Log.d("CRYPTO", "Bytes Dec: $numBytesDecrypted, Bytes Read: $numReadBytes")
                        output.write(buffer, 0, numReadBytes)

                        // Update the statuses
                        numBytesDecrypted += numReadBytes
                        listener(numBytesDecrypted)
                    }
                }
            }
        }
    }
}

// EXCEPTIONS
class InvalidDecryptionException(message: String) : Exception(message)
