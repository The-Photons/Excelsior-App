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

// Enums
enum class ItemType {
    FILE,
    DIRECTORY,
    PREVIOUS_DIRECTORY_MARKER
}

// Classes
abstract class RemoteItem(name: String, path: String) {
    // Attributes
    var name: String = name
        private set
    var path: String = path
        private set

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

        // TODO: Handle updating name on the server

        return true
    }

    // Methods
    /**
     * Determines whether the item is synced or not.
     *
     * @return Boolean whether the item is synced or not.
     */
    abstract fun isSynced(): Boolean
}

/**
 * Represents a remote folder that is present on the server.
 *
 * @property name Name of the folder.
 * @property path Relative path to the folder, with respect to the base directory.
 * @property subfolders Array of subfolders that this folder contains.
 * @property files Array of files that this folder contains.
 */
class RemoteFolder(
    name: String = "",
    path: String = "",
    var subfolders: Array<RemoteFolder> = emptyArray(),
    var files: Array<RemoteFile> = emptyArray()
) : RemoteItem(name, path) {
    override fun isSynced(): Boolean {
        // If the folder is empty then we will call it synced
        if (files.isEmpty() && subfolders.isEmpty()) {
            return true
        }

        // Check whether the files are synced
        for (file: RemoteFile in files) {
            if (!file.isSynced()) {
                return false
            }
        }

        // Then check whether the folders are synced
        for (folder: RemoteFolder in subfolders) {
            if (!folder.isSynced()) {
                return false
            }
        }

        // All items are synced, so the folder is synced
        return true
    }
}

/**
 * Represents a remote file that is present on the server.
 *
 * @property name Name of the file.
 * @property path Relative path to the file, with respect to the base directory.
 */
class RemoteFile(
    name: String = "",
    path: String = ""
) : RemoteItem(name, path) {
    // Methods
    override fun isSynced(): Boolean {
        // TODO: Implement
        return true
    }
}