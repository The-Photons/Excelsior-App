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

import java.io.File

/**
 * Secure deletion as described in "NIST SP-800-88 Rev. 1".
 */
class SDelete {
    companion object {
        /**
         * Securely deletes the contents of the file using the method described in "NIST SP-800-88
         * Rev. 1".
         *
         * @param file File to securely delete. Assumes that
         * - the file exists; and
         * - it is indeed a file (and not a folder)
         * @return Status of the deletion: `true` if the item was deleted and `false` if not.
         */
        fun deleteFile(file: File): Boolean {
            // Get the file size so we know how many null bytes we need to write
            val fileSize = file.length()

            // We want to fill the file with blocks of null bytes
            val content = CharArray(1024)
            val numBlocks = fileSize / 1024 + 1  // Good enough approximation of the ceiling

            // Now write the blocks
            file.bufferedWriter().use { out ->
                for (i in 1..numBlocks) {
                    out.write(content)
                }
            }

            // Finally, attempt deletion
            return file.delete()
        }
    }
}