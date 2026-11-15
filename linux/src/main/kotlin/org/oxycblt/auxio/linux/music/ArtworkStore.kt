/*
 * Copyright (c) 2026 Auxio Project
 * ArtworkStore.kt is part of Auxio.
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
import java.security.MessageDigest
import kotlin.io.path.absolute
import org.oxycblt.auxio.linux.Storage

/**
 * Cover art on disk, so covers are extracted from their tags once rather than on every launch.
 *
 * Only the picture bytes are cached: decoding is cheap and the decoded bitmap would be an order of
 * magnitude larger. The key includes the file's size and modification time, so editing a file's
 * tags misses the old entry instead of showing the old cover.
 */
object ArtworkStore {
    private const val NO_ARTWORK_SUFFIX = ".none"

    fun bytes(track: Track): ByteArray? = bytesFor(track.path)

    fun bytesFor(path: Path): ByteArray? {
        val key = key(path) ?: return Artwork.bytesFor(path)
        val cacheDir = Storage.artworkCacheDir
        val cached = cacheDir.resolve("$key.art")

        if (Files.exists(cached)) {
            val bytes = runCatching { Files.readAllBytes(cached) }.getOrNull()
            if (bytes != null) return bytes
        }

        // Covers are commonly missing, and looking for one costs a taglib parse per file.
        if (Files.exists(cacheDir.resolve(key + NO_ARTWORK_SUFFIX))) return null

        val found = Artwork.bytesFor(path)
        write(cacheDir, key, found)
        return found
    }

    private fun write(cacheDir: Path, key: String, bytes: ByteArray?) {
        runCatching {
            Files.createDirectories(cacheDir)
            if (bytes == null) {
                Files.createFile(cacheDir.resolve(key + NO_ARTWORK_SUFFIX))
            } else {
                Files.write(cacheDir.resolve("$key.art"), bytes)
            }
        }
    }

    /** Null when the file is gone, in which case there is nothing worth caching. */
    private fun key(path: Path): String? {
        val absolute = path.absolute()
        if (!Files.isRegularFile(absolute)) return null

        val size = runCatching { Files.size(absolute) }.getOrDefault(0L)
        val modified =
            runCatching { Files.getLastModifiedTime(absolute).toMillis() }.getOrDefault(0L)
        val digest =
            MessageDigest.getInstance("SHA-256").digest("$absolute|$size|$modified".toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
