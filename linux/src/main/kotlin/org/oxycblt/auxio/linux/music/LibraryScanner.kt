/*
 * Copyright (c) 2026 Auxio Project
 * LibraryScanner.kt is part of Auxio.
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
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension
import kotlin.streams.asSequence

/**
 * Walks music folders and builds the library.
 *
 * This replaces MediaStore: on Linux the library is just the filesystem, so scanning is a walk plus
 * an extension filter, with taglib supplying the metadata.
 *
 * With a [cacheFile], the tags read for each file are kept between scans and reused while the
 * file's size and modification time are unchanged. [useCache] turns that reuse on; a full scan
 * ignores the cache but still rewrites it for the next reload.
 */
object LibraryScanner {
    private val EXTENSIONS =
        setOf(
            "flac",
            "mp3",
            "ogg",
            "oga",
            "opus",
            "m4a",
            "mp4",
            "aac",
            "wav",
            "aiff",
            "aif",
            "wv",
            "ape",
            "tta",
            "dsf",
            "dff",
        )

    fun scan(
        roots: List<Path>,
        useCache: Boolean = false,
        cacheFile: Path? = null,
    ): MusicLibrary {
        val cached = if (useCache && cacheFile != null) LibraryCache.read(cacheFile) else emptyMap()
        val scanned = LinkedHashMap<Path, CachedTrack>()

        val songs =
            roots
                .filter { Files.isDirectory(it) }
                .flatMap { walk(it) }
                .map { path ->
                    val key = path.toAbsolutePath().normalize()
                    val modified =
                        runCatching { Files.getLastModifiedTime(path).toMillis() }.getOrDefault(-1)
                    val size = runCatching { Files.size(path) }.getOrDefault(-1)
                    val hit = cached[key]
                    val track =
                        if (
                            hit != null &&
                                modified >= 0 &&
                                hit.modified == modified &&
                                hit.size == size
                        ) {
                            hit.track
                        } else {
                            track(path)
                        }
                    scanned[key] = CachedTrack(modified, size, track)
                    track
                }
                .sortedBy { it.title.lowercase() }

        if (cacheFile != null) LibraryCache.write(cacheFile, scanned.values)

        return Library.build(songs, playlists = Playlists.load(roots, songs))
    }

    private fun walk(root: Path): List<Path> =
        Files.walk(root).use { stream ->
            stream
                .asSequence()
                .filter { it.isRegularFile() && it.extension.lowercase() in EXTENSIONS }
                .toList()
        }

    private fun track(path: Path): Track {
        val tags = NativeTags.read(path)
        return Track(
            path = path,
            title = tags?.title ?: path.nameWithoutExtension,
            artist = tags?.artist,
            album = tags?.album,
            genre = tags?.genre,
            trackNumber = tags?.trackNumber,
            year = tags?.year,
            date = tags?.date,
            durationSeconds = tags?.durationSeconds?.toDouble(),
        )
    }
}
