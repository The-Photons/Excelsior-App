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

package site.overwrite.encryptedfilesapp.data

import android.util.Log
import org.json.JSONObject
import site.overwrite.encryptedfilesapp.io.IOMethods

// Enums
enum class ItemType {
    FILE,
    DIRECTORY,
    PREVIOUS_DIRECTORY_MARKER
}

// Classes
abstract class RemoteItem(name: String, path: String, val size: Long, val type: ItemType) {
    // Attributes
    var name: String = name
        private set
    var path: String = path
        private set

    // Custom fields
    val dirPath: String
        get() {
            return path.split("/").dropLast(1).joinToString("/")
        }

    // Setters
    /**
     * Sets the new name of the remote item.
     *
     * @param newName New name of the file.
     * @return Status of the name update. Is `true` if successful and `false` otherwise.
     */
    fun setName(newName: String): Boolean {
        if (newName.isBlank()) {
            Log.d("REMOTE ITEMS", "Cannot set a blank file name")
            return false
        }
        name = newName

        // TODO: Handle updating name on the server

        return true
    }

    /**
     * Sets the new path of the remote item.
     *
     * @param newPath New path of the file.
     * @return Status of the path update. Is `true` if successful and `false` otherwise.
     */
    fun setPath(newPath: String): Boolean {
        if (newPath.isBlank()) {
            Log.d("REMOTE ITEMS", "Cannot set a blank file path")
            return false
        }
        path = newPath

        // TODO: Handle updating path on the server

        return true
    }

    // Methods
    /**
     * Nicely formats the file size.
     *
     * @param precision Number of decimal places to format the file size.
     * @return Formatted file size.
     */
    fun formattedSize(precision: Int = 2): String {
        return IOMethods.formatFileSize(size, precision = precision)
    }

    /**
     * Determines whether the item is synced or not.
     *
     * @return Boolean whether the item is synced or not.
     */
    abstract fun isSynced(): Boolean
}

/**
 * Represents a remote file that is present on the server.
 *
 * @property name Name of the file.
 * @property path Relative path to the file, with respect to the base directory.
 * @property size Size of the file.
 */
class RemoteFile(
    name: String = "",
    path: String = "",
    size: Long = 0
) : RemoteItem(name, path, size, ItemType.FILE) {
    override fun isSynced(): Boolean {
        TODO()
    }

    companion object {
        /**
         * Converts an obtained JSON object.
         *
         * @param json JSON object that represents the item.
         * @return Representative object.
         */
        fun fromJSON(json: JSONObject): RemoteFile {
            return RemoteFile(
                json.getString("name"),
                json.getString("path"),
                json.getLong("size")
            )
        }
    }
}


/**
 * Represents a remote folder that is present on the server.
 *
 * @property name Name of the folder.
 * @property path Relative path to the folder, with respect to the base directory.
 * @property size Total size of the folder.
 * @property subdirs Array of subfolders that this folder contains.
 * @property files Array of files that this folder contains.
 */
class RemoteDirectory(
    name: String = "",
    path: String = "",
    size: Long = 0,
    var subdirs: Array<RemoteDirectory> = emptyArray(),
    var files: Array<RemoteFile> = emptyArray()
) : RemoteItem(name, path, size, ItemType.DIRECTORY) {
    val items: Array<RemoteItem>
        get() {
            val items = ArrayList<RemoteItem>()
            for (folder in subdirs) {
                items.add(folder)
            }
            for (file in files) {
                items.add(file)
            }
            return items.toTypedArray()
        }

    override fun isSynced(): Boolean {
        // If the folder is empty then we will call it synced
        if (files.isEmpty() && subdirs.isEmpty()) {
            return true
        }

        // Check whether the files are synced
        for (file: RemoteFile in files) {
            if (!file.isSynced()) {
                return false
            }
        }

        // Then check whether the folders are synced
        for (folder: RemoteDirectory in subdirs) {
            if (!folder.isSynced()) {
                return false
            }
        }

        // All items are synced, so the folder is synced
        return true
    }

    companion object {
        /**
         * Converts an obtained JSON object.
         *
         * @param json JSON object that represents the folder.
         * @return Representative object.
         */
        fun fromJSON(json: JSONObject): RemoteDirectory {
            // Get any items that the folder may contain
            val items = json.getJSONArray("items")
            val numItems = items.length()

            val subfolders = ArrayList<RemoteDirectory>()
            val files = ArrayList<RemoteFile>()

            var item: JSONObject
            var itemType: String
            for (i in 0..<numItems) {
                item = items.getJSONObject(i)
                itemType = item.getString("type")
                if (itemType == "file") {
                    files.add(RemoteFile.fromJSON(item))
                } else {
                    subfolders.add(fromJSON(item))
                }
            }

            // Then create the final object
            return RemoteDirectory(
                json.getString("name"),
                json.getString("path"),
                json.getLong("size"),
                subfolders.toTypedArray(),
                files.toTypedArray()
            )
        }
    }
}

class RemotePreviousDirectory() : RemoteItem("", "", 0, ItemType.PREVIOUS_DIRECTORY_MARKER) {
    override fun isSynced(): Boolean {
        return false
    }
}