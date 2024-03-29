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

package site.overwrite.encryptedfilesapp.file

import android.util.Log
import java.io.File
import java.io.IOException

class CRUDOperations {
    companion object {
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
            val appDirectory = File(Pathing.getItemPath(pathToDir))
            if (!appDirectory.exists()) {
                val directoryCreated = appDirectory.mkdirs()
                if (!directoryCreated) {
                    // Failed to create the directory
                    return null
                } else {
                    Log.d("CRUD-OPERATIONS", "Created directory '$pathToDir'")
                }
            }

            return appDirectory
        }

        /**
         * Creates a file with the specified content.
         *
         * @param filePath Path to the file.
         * @return File object representing the file, or `null` if the file creation fails.
         */
        fun createFile(
            filePath: String
        ): File? {
            val containingDir = createDirectory(Pathing.getContainingDir(filePath))
            if (containingDir != null) {
                // Create the file within the directory
                val file = File(Pathing.getItemPath(filePath))
                try {
                    if (!file.exists()) {
                        val fileCreated = file.createNewFile()
                        if (!fileCreated) {
                            // Failed to create the file
                            Log.d("CRUD-OPERATIONS", "Failed to create file")
                            return null
                        }
                    }
                    return file
                } catch (e: IOException) {
                    Log.d("CRUD-OPERATIONS", "Failed to create file: ${e.message}")
                }
            }
            return null
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
            val containingDir = createDirectory(Pathing.getContainingDir(filePath))
            if (containingDir != null) {
                // Create the file within the directory
                val file = File(Pathing.getItemPath(filePath))
                try {
                    if (!file.exists()) {
                        val fileCreated = file.createNewFile()
                        if (!fileCreated) {
                            // Failed to create the file
                            Log.d("CRUD-OPERATIONS", "Failed to create file")
                            return null
                        } else {
                            // With the file created, fill it with the contents
                            file.writeBytes(fileContent)
                            Log.d("CRUD-OPERATIONS", "Created file '$filePath'")
                        }
                    }
                    return file
                } catch (e: IOException) {
                    Log.d("CRUD-OPERATIONS", "Failed to create file: ${e.message}")
                }
            }
            return null
        }

        /**
         * Gets the file at the specified file path.
         *
         * @param filePath Path to the file, with respect to the application directory.
         * @return The file object, or `null` if the file does not exist.
         */
        fun getFile(filePath: String): File? {
            if (Pathing.doesFileExist(filePath)) {
                return File(Pathing.getItemPath(filePath))
            }
            return null
        }

        /**
         * Delete a item on the phone.
         *
         * If the item is a directory, then this function also deletes its contents.
         *
         * @param fileOrDirectory File or directory to delete.
         * @return Status of the deletion: `true` if the item was deleted and `false` if not.
         */
        fun deleteItem(fileOrDirectory: File): Boolean {
            var allDeleted = true
            if (fileOrDirectory.isDirectory) {
                for (child in fileOrDirectory.listFiles()!!) {
                    if (!deleteItem(child)) {
                        allDeleted = false
                    }
                }

                if (allDeleted) {
                    // The `allDeleted` flag now depends on if we can delete the directory
                    allDeleted = fileOrDirectory.delete()
                }
            } else {
                // Just try to delete the file using secure deletion
                allDeleted = SDelete.deleteFile(fileOrDirectory)
            }

            if (allDeleted) {
                Log.d("CRUD-OPERATIONS", "Deleted '${fileOrDirectory.path}'")
            } else {
                Log.d("CRUD-OPERATIONS", "Failed to delete '${fileOrDirectory.path}'")
            }
            return allDeleted
        }

        /**
         * Delete a item on the phone.
         *
         * If the path points to a directory, then this function also deletes its contents.
         *
         * @param itemPath Path to the file/folder.
         * @return Status of the deletion: `true` if the item was deleted and `false` if not.
         */
        fun deleteItem(itemPath: String): Boolean {
            val fileOrDirectory = File(Pathing.getItemPath(itemPath))
            return deleteItem(fileOrDirectory)
        }
    }
}