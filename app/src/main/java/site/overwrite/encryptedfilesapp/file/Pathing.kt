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

import android.content.ContentResolver
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import java.io.File

const val APP_DIR_NAME = "Excelsior"
val DOWNLOADS_DIR: File =
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

class Pathing {
    companion object {
        /**
         * @return The application directory that is within the downloads directory.
         */
        private fun getAppDir(): String {
            return "${DOWNLOADS_DIR.path}/$APP_DIR_NAME"
        }

        /**
         * Gets the file/directory path with reference to the application directory.
         *
         * @param itemPath Path to the file/directory, with reference to the application directory.
         */
        fun getItemPath(itemPath: String): String {
            return "${getAppDir()}/$itemPath".trimEnd('/')
        }

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
         * Gets the file name from a "content://" URI.
         *
         * @param uri A URI with the "content" scheme.
         * @param contentResolver Content resolver that helps resolve the file.
         * @return File name.
         */
        fun getFileName(
            uri: Uri,
            contentResolver: ContentResolver
        ): String {
            var result = ""

            contentResolver.query(uri, null, null, null, null).use { cursor ->
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        val colIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        result = cursor.getString(colIndex)
                    }
                }
            }
            return result
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
         * Checks if an item exists at the specified path.
         *
         * @param itemPath Path to the item to check.
         * @return Boolean whether there is an item at the specified path.
         */
        fun doesItemExist(itemPath: String): Boolean {
            return File(getItemPath(itemPath)).exists()
        }

        /**
         * Checks if a file exists at the specified path.
         *
         * @param filePath Path to the file to check.
         * @return Boolean whether there is a file at the specified path.
         */
        fun doesFileExist(filePath: String): Boolean {
            val possibleFile = File(getItemPath(filePath))
            if (possibleFile.isFile) {
                return possibleFile.exists()
            }
            return false
        }

        /**
         * Recursively list the items in the directory.
         *
         * @param dirPath Path to the directory.
         * @return
         */
        fun traverseDir(dirPath: String): List<String> {
            val paths = mutableListOf<String>()
            val appDir = getAppDir()

            File(getItemPath(dirPath)).walkTopDown().forEach {
                // We only want to add files and non-empty directories
                if (it.isFile || (it.isDirectory && (it.list()?.size ?: 0) != 0)) {
                    paths.add(it.path.substring(appDir.length))
                }
            }
            // Remove the empty app directory
            paths.remove("")

            // Now sort the paths
            return paths.sorted()
        }
    }
}