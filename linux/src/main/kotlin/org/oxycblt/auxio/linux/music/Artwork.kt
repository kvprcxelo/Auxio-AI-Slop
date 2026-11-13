/*
 * Copyright (c) 2026 Auxio Project
 * Artwork.kt is part of Auxio.
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
 
package org.oxycblt.auxio.linux.music

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name

/**
 * Cover art for a track: the picture embedded in the file, or one sitting next to it.
 *
 * MediaStore gives Auxio covers for free on Android; a desktop library often only has a `cover.jpg`
 * beside the files, so both are tried.
 */
object Artwork {
    private val BASE_NAMES = listOf("cover", "folder", "front", "album", "artwork", "albumart")

    private val EXTENSIONS = listOf("jpg", "jpeg", "png", "webp")

    fun bytes(track: Track): ByteArray? =
        NativeTags.artwork(track.path) ?: folderCover(track.path.parent)

    fun folderCover(directory: Path?): ByteArray? {
        if (directory == null || !Files.isDirectory(directory)) return null

        val entries =
            runCatching { Files.list(directory).use { stream -> stream.toList() } }.getOrNull()
                ?: return null

        for (base in BASE_NAMES) {
            for (extension in EXTENSIONS) {
                val match = entries.firstOrNull {
                    it.name.equals("$base.$extension", ignoreCase = true)
                }
                if (match != null) return read(match)
            }
        }

        // Last resort: any image in the folder, so oddly named covers are still found.
        return entries
            .filter { it.extension.lowercase() in EXTENSIONS }
            .sortedBy { it.name.lowercase() }
            .firstNotNullOfOrNull { read(it) }
    }

    private fun read(file: Path): ByteArray? = runCatching { Files.readAllBytes(file) }.getOrNull()
}
