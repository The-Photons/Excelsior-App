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

import site.overwrite.encryptedfilesapp.io.IOMethods
import java.io.File
import java.math.RoundingMode

enum class FileUnit(val symbol: String, val value: Long) {
    UNIT("B", 1),
    KILOBYTE("KB", 1_000),
    MEGABYTE("MB", 1_000_000),
    GIGABYTE("GB", 1_000_000_000),
    KIBIBYTE("KiB", 1_024),
    MEBIBYTE("MiB", 1_048_576),      // 1024^2
    GIBIBYTE("GiB", 1_073_741_824);  // 1024^3

    companion object {
        /**
         * Chooses the appropriate file unit for formatting the file size.
         *
         * @param rawSize Raw file size.
         * @param altUnits Use IEC 80000-13:2008 format instead of SI format (i.e., kibibytes,
         * mebibytes, gibibytes instead of kilobytes, megabytes, gigabytes)
         */
        fun chooseUnit(rawSize: Long, altUnits: Boolean = false): FileUnit {
            if (altUnits) {
                if (rawSize >= GIBIBYTE.value) {
                    return GIBIBYTE
                }
                if (rawSize >= MEBIBYTE.value) {
                    return MEBIBYTE
                }
                if (rawSize >= KIBIBYTE.value) {
                    return KIBIBYTE
                }
                return UNIT
            } else {
                if (rawSize >= GIGABYTE.value) {
                    return GIGABYTE
                }
                if (rawSize >= MEGABYTE.value) {
                    return MEGABYTE
                }
                if (rawSize >= KILOBYTE.value) {
                    return KILOBYTE
                }
                return UNIT
            }
        }
    }
}

class FileSizeUtils {
    companion object {
        /**
         * Gets the size of the item at the specified path.
         *
         * @param itemPath Path to the item. Assumed to be valid.
         * @return Size of the item.
         */
        fun getItemSize(itemPath: String): Long {
            return File(IOMethods.getItemPath(itemPath)).length()
        }


        /**
         * Nicely formats the file size.
         *
         * @param rawSize Size of the item as a long.
         * @param precision Number of decimal places to format the file size.
         * @return Formatted file size.
         */
        fun formatFileSize(rawSize: Long, precision: Int = 2): String {
            val unit = FileUnit.chooseUnit(rawSize, altUnits = false)
            val roundedSize = if (unit == FileUnit.UNIT) {
                rawSize.toBigDecimal()
            } else {
                val reducedSize = rawSize.toBigDecimal().divide(unit.value.toBigDecimal())
                reducedSize.setScale(precision, RoundingMode.HALF_EVEN)
            }
            return "$roundedSize ${unit.symbol}"
        }
    }
}
