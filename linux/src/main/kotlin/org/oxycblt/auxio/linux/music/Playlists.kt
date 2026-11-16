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
import org.oxycblt.auxio.linux.Storage

/**
 * A playlist read from an `.m3u`/`.m3u8` file.
 *
 * Auxio manages playlists in its own database; the desktop port starts from the playlist files
 * people already have next to their music, which needs no schema and keeps them editable outside
 * the app.
 */
data class Playlist(val name: String, val tracks: List<Track>, val path: Path)

/**
 * A song's removed lines and their positions, so [Playlists.undoRemove] can put it back exactly.
 */
data class RemovedSong(val file: Path, val entries: List<Pair<Int, String>>)

object Playlists {
    private val EXTENSIONS = setOf("m3u", "m3u8")

    private val UNSAFE_NAME = Regex("""[/\\:*?"<>|]""")

    /**
     * Playlists found under [roots] or in the managed [directory] (`~/auxio/playlists`), with
     * entries matched against the scanned [songs].
     */
    fun load(
        roots: List<Path>,
        songs: List<Track>,
        directory: Path = Storage.playlistDir,
    ): List<Playlist> {
        if (songs.isEmpty()) return emptyList()

        val byPath = songs.associateBy { it.path.toAbsolutePath().normalize() }
        val managed = directory.toAbsolutePath().normalize()

        return buildList {
                addAll(roots)
                // Path is Iterable<Path>, so `roots + directory` would splice in its components.
                add(directory)
            }
            .distinct()
            .filter { Files.isDirectory(it) }
            .flatMap { root -> files(root) }
            // A music root may contain the managed folder, which would find its files twice.
            .distinctBy { it.toAbsolutePath().normalize() }
            .sortedBy { it.fileName.toString().lowercase() }
            .mapNotNull { file ->
                // Playlists created here may still be empty; elsewhere an empty file is not one.
                parse(
                    file,
                    byPath,
                    keepEmpty = file.toAbsolutePath().normalize().startsWith(managed),
                )
            }
    }

    /**
     * Creates an empty playlist in [directory], named after [name], and returns its file.
     *
     * The name cannot contain path separators, so a playlist cannot escape the directory; a taken
     * name gets a numeric suffix rather than overwriting the file it would have replaced.
     */
    fun create(name: String, directory: Path = Storage.playlistDir): Path? {
        val base = name.trim().replace(UNSAFE_NAME, "-").trim()
        if (base.isEmpty()) return null

        return runCatching {
                Files.createDirectories(directory)
                uniqueFile(directory, base, "m3u").also { Files.writeString(it, "#EXTM3U\n") }
            }
            .getOrNull()
    }

    /**
     * Copies a playlist file into [directory] and returns the copy.
     *
     * Entries are made absolute on the way, since relative ones would point at the source folder
     * once the copy lives somewhere else.
     */
    fun importFrom(source: Path, directory: Path = Storage.playlistDir): Path? {
        val extension = source.extension.lowercase()
        if (extension !in EXTENSIONS) return null

        return runCatching {
                Files.createDirectories(directory)
                uniqueFile(directory, source.nameWithoutExtension, extension).also {
                    Files.writeString(it, absoluteEntries(source))
                }
            }
            .getOrNull()
    }

    private fun absoluteEntries(source: Path): String {
        val lines = runCatching { Files.readAllLines(source) }.getOrNull() ?: return "#EXTM3U\n"

        return lines.joinToString("\n", postfix = "\n") { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                line
            } else {
                runCatching {
                        val path = Path.of(trimmed)
                        val resolved = if (path.isAbsolute) path else source.parent?.resolve(path)
                        (resolved ?: path).toAbsolutePath().normalize().toString()
                    }
                    .getOrDefault(trimmed)
            }
        }
    }

    /**
     * Appends [tracks] to [playlist]'s file, skipping entries it already has, and returns how many
     * were added. A negative result means the file could not be written.
     */
    fun addTracks(playlist: Playlist, tracks: List<Track>): Int =
        runCatching {
                val file = playlist.path
                val existing = entries(file)
                val additions =
                    tracks
                        .map { it.path.toAbsolutePath().normalize() }
                        .distinct()
                        .filter { it !in existing }
                if (additions.isEmpty()) return 0

                val current = runCatching { Files.readString(file) }.getOrDefault("")
                val text = buildString {
                    append(current)
                    if (isNotEmpty() && !endsWith('\n')) append('\n')
                    if (isEmpty()) append("#EXTM3U\n")
                    additions.forEach { append(it).append('\n') }
                }
                Files.writeString(file, text)
                additions.size
            }
            .getOrDefault(-1)

    /**
     * Removes every line pointing at [track] from [playlist]'s file, keeping its other entries and
     * comments, and returns those lines so the removal can be undone. Null when nothing was removed
     * or the file could not be written.
     */
    fun removeTrack(playlist: Playlist, track: Track): RemovedSong? =
        runCatching {
                val file = playlist.path
                val target = track.path.toAbsolutePath().normalize()
                val kept = mutableListOf<String>()
                val removed = mutableListOf<Pair<Int, String>>()
                Files.readAllLines(file).forEachIndexed { index, line ->
                    if (entryOf(file, line.trim()) == target) {
                        removed.add(index to line)
                    } else {
                        kept.add(line)
                    }
                }
                if (removed.isEmpty()) return null

                Files.writeString(
                    file,
                    kept.joinToString("\n", postfix = if (kept.isEmpty()) "" else "\n"),
                )
                RemovedSong(file, removed)
            }
            .getOrNull()

    /** Puts a removal back at its original lines; false when the file could not be written. */
    fun undoRemove(removal: RemovedSong): Boolean =
        runCatching {
                val lines = Files.readAllLines(removal.file).toMutableList()
                for ((index, line) in removal.entries.sortedBy { it.first }) {
                    lines.add(index.coerceAtMost(lines.size), line)
                }
                Files.writeString(
                    removal.file,
                    lines.joinToString("\n", postfix = if (lines.isEmpty()) "" else "\n"),
                )
                true
            }
            .getOrDefault(false)

    /** Deletes the playlist's file; true when it is gone. */
    fun delete(playlist: Playlist): Boolean =
        runCatching { Files.deleteIfExists(playlist.path) }.isSuccess

    /** The file's entries as absolute paths, for deciding what a playlist already has. */
    private fun entries(file: Path): Set<Path> =
        runCatching { Files.readAllLines(file) }
            .getOrDefault(emptyList())
            .map { it.trim() }
            .mapNotNull { entryOf(file, it) }
            .toSet()

    /**
     * Resolves one entry line against the playlist's folder; comments and blanks are not entries.
     */
    private fun entryOf(file: Path, line: String): Path? {
        if (line.isEmpty() || line.startsWith("#")) return null

        val path = runCatching { Path.of(line) }.getOrNull() ?: return null
        return (if (path.isAbsolute) path else file.parent?.resolve(path) ?: path).normalize()
    }

    private fun uniqueFile(directory: Path, base: String, extension: String): Path {
        var candidate = directory.resolve("$base.$extension")
        var index = 2
        while (Files.exists(candidate)) {
            candidate = directory.resolve("$base ($index).$extension")
            index++
        }
        return candidate
    }

    private fun files(root: Path): List<Path> =
        Files.walk(root).use { stream ->
            stream
                .asSequence()
                .filter { it.isRegularFile() && it.extension.lowercase() in EXTENSIONS }
                .toList()
        }

    private fun parse(file: Path, byPath: Map<Path, Track>, keepEmpty: Boolean): Playlist? {
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

        if (tracks.isEmpty() && !keepEmpty) return null

        return Playlist(file.nameWithoutExtension, tracks, file)
    }
}
