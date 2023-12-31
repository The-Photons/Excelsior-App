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

import android.os.Environment
import android.util.Log
import java.io.File
import java.io.IOException

// CONSTANTS
const val APP_DIR_NAME = "Excelsior"
val DOWNLOADS_DIR: File =
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

// CLASSES
/**
 * Class that contains input/output operations.
 */
class IOMethods {
    companion object {
        // Private methods
        /**
         * Gets the file path with reference to the application directory.
         *
         * @param filePath Path to the file, with reference to the application directory.
         */
        private fun getFilePath(filePath: String): String {
            return "${DOWNLOADS_DIR.path}/$APP_DIR_NAME/$filePath"
        }

        // Public methods
        /**
         * Gets the file name from the file path.
         *
         * @param filePath Path to the file.
         * @return File name.
         */
        fun getFileName(filePath: String): String {
            return filePath.split('/').last()
        }

        /**
         * Gets the directory that contains the item.
         *
         * @param itemPath Path to the item.
         * @return Directory that contains the item.
         */
        fun getContainingDir(itemPath: String): String {
            val split = itemPath.split('/')
            return split.subList(0, split.size - 1).joinToString("/")
        }

        /**
         * Creates a directory at the specified path.
         *
         * The directory is created within the application directory that is within the `Download`
         * directory.
         *
         * @param pathToDir Path to the directory.
         * @return File object representing the directory, or `null` if the directory creation
         * failed.
         */
        fun createDirectory(pathToDir: String): File? {
            val appDirectory = File(getFilePath(pathToDir))
            if (!appDirectory.exists()) {
                val directoryCreated = appDirectory.mkdirs()
                if (!directoryCreated) {
                    // Failed to create the directory
                    return null
                } else {
                    Log.d("IO METHODS", "Created directory '$pathToDir'")
                }
            }

            return appDirectory
        }

        /**
         * Creates a file with the specified content.
         *
         * @param filePath Path to the file.
         * @param fileContent Content of the file.
         * @return File object representing the file, or `null` if the file creation fails.
         */
        fun createFile(
            filePath: String,
            fileContent: ByteArray
        ): File? {
            // Get the directory that contains the file
            val containingDir = createDirectory(getContainingDir(filePath))
            if (containingDir != null) {
                // Create the file within the directory
                val file = File(getFilePath(filePath))
                try {
                    if (!file.exists()) {
                        Log.d("IO METHODS", file.path)
                        val fileCreated = file.createNewFile()
                        if (!fileCreated) {
                            // Failed to create the file
                            Log.d("IO METHODS", "Failed to create file")
                            return null
                        } else {
                            // With the file created, fill it with the contents
                            file.writeBytes(fileContent)
                            Log.d("IO METHODS", "Created file '$filePath'")
                        }
                    }
                    return file
                } catch (e: IOException) {
                    Log.d("IO METHODS", "Failed to create file: ${e.message}")
                }
            }
            return null
        }

        /**
         * Checks if a file exists at the specified path.
         *
         * @param itemPath Path to the file to check.
         * @return A boolean; `true` if there is an item at the specified path and `false`
         * otherwise. If the path specifies a folder this always returns `false`.
         */
        fun checkIfFileExists(itemPath: String): Boolean {
            val possibleFile = File(getFilePath(itemPath))
            if (possibleFile.isFile) {
                return possibleFile.exists()
            }
            return false
        }
    }
}