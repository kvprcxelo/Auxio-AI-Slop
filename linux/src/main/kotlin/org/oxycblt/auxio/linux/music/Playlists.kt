/*
 * Copyright (c) 2026 Auxio Project
 * Playlists.kt is part of Auxio.
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
 * A playlist read from an `.m3u`/`.m3u8` file.
 *
 * Auxio manages playlists in its own database; the desktop port starts from the playlist files
 * people already have next to their music, which needs no schema and keeps them editable outside
 * the app.
 */
data class Playlist(val name: String, val tracks: List<Track>)

object Playlists {
    private val EXTENSIONS = setOf("m3u", "m3u8")

    /** Playlists found under [roots], with entries matched against the scanned [songs]. */
    fun load(roots: List<Path>, songs: List<Track>): List<Playlist> {
        if (songs.isEmpty()) return emptyList()

        val byPath = songs.associateBy { it.path.toAbsolutePath().normalize() }

        return roots
            .filter { Files.isDirectory(it) }
            .flatMap { root -> files(root) }
            .sortedBy { it.fileName.toString().lowercase() }
            .mapNotNull { file -> parse(file, byPath) }
    }

    private fun files(root: Path): List<Path> =
        Files.walk(root).use { stream ->
            stream
                .asSequence()
                .filter { it.isRegularFile() && it.extension.lowercase() in EXTENSIONS }
                .toList()
        }

    private fun parse(file: Path, byPath: Map<Path, Track>): Playlist? {
        val lines = runCatching { Files.readAllLines(file) }.getOrNull() ?: return null

        val tracks =
            lines
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .mapNotNull { line ->
                    // Relative entries are relative to the playlist, absolute ones are taken as is.
                    val path = runCatching { Path.of(line) }.getOrNull() ?: return@mapNotNull null
                    val resolved = if (path.isAbsolute) path else file.parent?.resolve(path) ?: path
                    byPath[resolved.normalize()]
                }

        return if (tracks.isEmpty()) null else Playlist(file.nameWithoutExtension, tracks)
    }
}
