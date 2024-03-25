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
import site.overwrite.encryptedfilesapp.file.Pathing

// Enums
enum class ItemType {
    FILE,
    DIRECTORY
}

// Classes
abstract class RemoteItem(
    name: String,
    path: String,
    val size: Long,
    val type: ItemType,
    var parentDir: RemoteDirectory?,

    markedForLocalDeletion: Boolean = false,
    markedForServerDeletion: Boolean = false
) {
    // Fields
    var name: String = name
        private set
    var path: String = path
        private set
    val synced: Boolean
        get() = !markedForLocalDeletion && isSynced()

    var markedForLocalDeletion: Boolean = markedForLocalDeletion
        private set
    var markedForServerDeletion: Boolean = markedForServerDeletion
        private set

    val dirPath: String
        get() {
            return Pathing.getContainingDir(path)
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
    open fun markForLocalDeletion(state: Boolean = true) {
        markedForLocalDeletion = state
    }

    fun unmarkForLocalDeletion() {
        markForLocalDeletion(false)
    }

    open fun markForServerDeletion(state: Boolean = true) {
        markedForServerDeletion = state
    }

    fun unmarkForServerDeletion() {
        markForServerDeletion(false)
    }

    // Methods
    /**
     * Nicely formats the file size.
     *
     * @param precision Number of decimal places to format the file size.
     * @return Formatted file size.
     */
    fun formattedSize(precision: Int = 2): String {
        return FileSizeUtils.formatFileSize(size, precision = precision)
    }

    /**
     * Determines whether the item is synced or not.
     *
     * @return Boolean whether the item is synced or not.
     */
    protected abstract fun isSynced(): Boolean
}

/**
 * Represents a remote file that is present on the server.
 *
 * @property name Name of the file.
 * @property path Relative path to the file, with respect to the base directory.
 * @property size Size of the file.
 * @property parentDir Directory that contains this file.
 */
class RemoteFile(
    name: String,
    path: String,
    size: Long,
    parentDir: RemoteDirectory?
) : RemoteItem(name, path, size, ItemType.FILE, parentDir) {
    // Methods
    override fun isSynced(): Boolean {
        return path.isNotEmpty() && Pathing.doesFileExist(path)
    }

    companion object {
        /**
         * Converts an obtained JSON object.
         *
         * @param json JSON object that represents the item.
         * @return Representative object.
         */
        fun fromJSON(json: JSONObject): RemoteFile {
            val path = json.getString("path")
            return RemoteFile(
                json.getString("name"),
                path,
                json.getLong("size"),
                null  // Will update when the file is placed in a directory
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
 * @property parentDir Directory that contains this folder.
 */
open class RemoteDirectory(
    name: String,
    path: String,
    size: Long,
    var subdirs: Array<RemoteDirectory>,
    var files: Array<RemoteFile>,
    parentDir: RemoteDirectory?
) : RemoteItem(name, path, size, ItemType.DIRECTORY, parentDir) {
    // Fields
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

    /**
     * Files that belong to this directory or any subdirectory.
     */
    val constituentFiles: Array<RemoteFile>
        get() {
            val files = ArrayList<RemoteFile>()
            for (folder in subdirs) {
                files.addAll(folder.constituentFiles)
            }
            for (file in this.files) {
                files.add(file)
            }
            return files.toTypedArray()
        }

    /**
     * Synced files that belong to this directory or any subdirectory.
     */
    val syncedConstituentFiles: Array<RemoteFile>
        get() {
            val allFiles = constituentFiles
            val syncedFiles = ArrayList<RemoteFile>()
            for (file in allFiles) {
                if (Pathing.doesFileExist(file.path)) {
                    syncedFiles.add(file)
                }
            }
            return syncedFiles.toTypedArray()
        }

    // Methods
    override fun isSynced(): Boolean {
        // If the folder is empty then we will call it synced
        if (files.isEmpty() && subdirs.isEmpty()) {
            return true
        }

        // Check whether the files are synced
        for (file: RemoteFile in files) {
            if (!file.synced) {
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

    override fun markForLocalDeletion(state: Boolean) {
        super.markForLocalDeletion(state)
        for (file in files) {
            file.markForLocalDeletion(state)
        }
        for (folder in subdirs) {
            folder.markForLocalDeletion(state)
        }
    }

    override fun markForServerDeletion(state: Boolean) {
        super.markForServerDeletion(state)
        for (file in files) {
            file.markForServerDeletion(state)
        }
        for (folder in subdirs) {
            folder.markForServerDeletion(state)
        }
    }

    private fun addFile(file: RemoteFile) {
        val filesList = files.toMutableList()
        filesList.add(file)
        files = filesList.toTypedArray()
    }

    fun addFile(
        name: String,
        path: String,
        size: Long
    ) {
        addFile(
            RemoteFile(
                name,
                path,
                size,
                this
            )
        )
    }

    private fun addFolder(directory: RemoteDirectory) {
        val subdirList = subdirs.toMutableList()
        subdirList.add(directory)
        subdirs = subdirList.toTypedArray()
    }

    fun addFolder(
        name: String,
        path: String
    ) {
        addFolder(
            RemoteDirectory(
                name,
                path,
                0,
                emptyArray(),
                emptyArray(),
                this
            )
        )
    }

    fun removeFile(file: RemoteFile) {
        val filesList = files.toMutableList()
        filesList.remove(file)
        files = filesList.toTypedArray()
    }

    fun removeFolder(dir: RemoteDirectory) {
        val subdirList = subdirs.toMutableList()
        subdirList.remove(dir)
        subdirs = subdirList.toTypedArray()
    }

    companion object {
        /**
         * Converts an obtained JSON object.
         *
         * @param json JSON object that represents the folder.
         * @return Representative object.
         */
        fun fromJSON(json: JSONObject): RemoteDirectory {
            // First create the directory object that we will return
            val directory = RemoteDirectory(
                json.getString("name"),
                json.getString("path"),
                json.getLong("size"),
                emptyArray(),
                emptyArray(),
                null
            )

            // Get any items that the folder may contain
            val items = json.getJSONArray("items")
            val numItems = items.length()

            val subdirs = ArrayList<RemoteDirectory>()
            val files = ArrayList<RemoteFile>()

            var item: JSONObject
            var itemType: String
            for (i in 0..<numItems) {
                item = items.getJSONObject(i)
                itemType = item.getString("type")
                if (itemType == "file") {
                    val file = RemoteFile.fromJSON(item)
                    file.parentDir = directory
                    files.add(file)
                } else {
                    val subdir = fromJSON(item)
                    subdir.parentDir = directory
                    subdirs.add(subdir)
                }
            }

            // Finally we can update the arrays for the files and subdirectories
            directory.files = files.toTypedArray()
            directory.subdirs = subdirs.toTypedArray()
            return directory
        }
    }
}
