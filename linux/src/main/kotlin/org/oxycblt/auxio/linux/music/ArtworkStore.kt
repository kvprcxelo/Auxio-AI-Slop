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
import org.oxycblt.auxio.linux.Settings
import org.oxycblt.auxio.linux.Storage

/**
 * Cover art on disk, so covers are extracted from their tags once rather than on every launch.
 *
 * Only the picture bytes are cached: decoding is cheap and the decoded bitmap would be an order of
 * magnitude larger. The key includes the file's size and modification time, so editing a file's
 * tags misses the old entry instead of showing the old cover.
 *
 * The cache is capped, evicting the least recently written covers first, because a large library
 * would otherwise fill the disk a few hundred kilobytes at a time.
 */
object ArtworkStore {
    private const val NO_ARTWORK_SUFFIX = ".none"
    private const val BYTES_PER_MB = 1024L * 1024L
    private const val TRIM_TARGET = 0.9
    private const val BLOCK_SIZE = 512L

    /**
     * "No artwork here" markers are empty, so they never free themselves through the size limit. A
     * large library without covers would otherwise leave one per song forever.
     */
    internal const val MAX_MARKERS = 2_000

    private val lock = Any()

    /** Size of everything in the cache, or -1 while it has not been measured yet. */
    private var knownBytes = -1L

    /** Number of "no artwork" markers, or -1 while it has not been counted yet. */
    private var knownMarkers = -1L

    /** The limit is read from the settings file once, then reused on every write. */
    private var cachedLimit = -1L

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
        enforceLimitIfNeeded()
        return found
    }

    /** Applies the configured limit, for when it is changed from the settings page. */
    fun enforceLimit() {
        synchronized(lock) { cachedLimit = Settings.cacheLimitMb() * BYTES_PER_MB }
        trim(Storage.artworkCacheDir, limitBytes())
    }

    private fun enforceLimitIfNeeded() {
        val limit = limitBytes()
        val (size, markers) = synchronized(lock) { knownBytes to knownMarkers }
        // Markers are empty, so only the count can tell that they are piling up.
        if (size < 0 || markers < 0 || size > limit || markers > MAX_MARKERS) {
            trim(Storage.artworkCacheDir, limit)
        }
    }

    private fun limitBytes(): Long {
        val cached = cachedLimit
        if (cached > 0) return cached

        val limit = Settings.cacheLimitMb() * BYTES_PER_MB
        synchronized(lock) { cachedLimit = limit }
        return limit
    }

    private fun write(cacheDir: Path, key: String, bytes: ByteArray?): Long {
        val written =
            runCatching {
                    Files.createDirectories(cacheDir)
                    if (bytes == null) {
                        val marker = cacheDir.resolve(key + NO_ARTWORK_SUFFIX)
                        Files.write(marker, ByteArray(0))
                        0L
                    } else {
                        val file = cacheDir.resolve("$key.art")
                        Files.write(file, bytes)
                        bytes.size.toLong()
                    }
                }
                .getOrDefault(0L)

        synchronized(lock) {
            if (knownBytes >= 0) knownBytes += written
            if (bytes == null && knownMarkers >= 0) knownMarkers++
        }
        return written
    }

    /**
     * Deletes the oldest entries until [directory] holds at most [limit] bytes, and returns what is
     * left. Entries are trimmed a little below the limit so the next few covers do not trigger
     * another walk of the cache.
     */
    internal fun trim(directory: Path, limit: Long, maxMarkers: Int = MAX_MARKERS): Long {
        synchronized(lock) {
            val entries =
                runCatching {
                        Files.list(directory).use { stream -> stream.toList() }
                    }
                    .getOrNull()
                    ?: run {
                        knownBytes = 0
                        return 0
                    }

            val files = entries.mapNotNull { file ->
                val size = diskUsage(file) ?: return@mapNotNull null
                val used =
                    runCatching { Files.getLastModifiedTime(file).toMillis() }.getOrDefault(0L)
                CacheEntry(file, size, used, file.fileName.toString().endsWith(NO_ARTWORK_SUFFIX))
            }

            var total = files.sumOf { it.size }

            var markers = files.count { it.marker }
            if (markers > maxMarkers) {
                for (entry in files.filter { it.marker }.sortedBy { it.used }) {
                    if (markers <= maxMarkers) break
                    if (delete(entry)) {
                        total -= entry.size
                        markers--
                    }
                }
            }

            if (total > limit) {
                val target = (limit * TRIM_TARGET).toLong()
                for (entry in files.sortedBy { it.used }) {
                    if (total <= target) break
                    // Entries already removed above no longer exist, so this is a no-op for them.
                    if (delete(entry)) {
                        total -= entry.size
                        if (entry.marker) markers--
                    }
                }
            }

            knownBytes = total
            knownMarkers = markers.toLong()
            return total
        }
    }

    private fun delete(entry: CacheEntry): Boolean =
        runCatching { Files.deleteIfExists(entry.path) }.getOrDefault(false)

    /**
     * Blocks actually allocated on disk, which is what the cache limit is about: a 1 KB cover still
     * takes a 4 KB block, and directory overhead is not free either.
     */
    private fun diskUsage(file: Path): Long? =
        runCatching { Files.size(file) }
            .map { size ->
                // The attribute is missing on filesystems that do not report allocations.
                val blocks =
                    runCatching { Files.getAttribute(file, "unix:blocks") as? Number }
                        .getOrNull()
                        ?.toLong() ?: 0L
                if (blocks > 0) blocks * BLOCK_SIZE else size
            }
            .getOrNull()

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

    private data class CacheEntry(
        val path: Path,
        val size: Long,
        val used: Long,
        val marker: Boolean,
    )
}
