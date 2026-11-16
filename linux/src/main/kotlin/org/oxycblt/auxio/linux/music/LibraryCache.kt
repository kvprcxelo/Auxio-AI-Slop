/*
 * Copyright (c) 2026 Auxio Project
 * LibraryCache.kt is part of Auxio.
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

import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Base64

/** A track read on an earlier scan, valid while the file's size and modification time match. */
internal data class CachedTrack(val modified: Long, val size: Long, val track: Track)

/**
 * The tags read by the last scan, so a reload only parses files that changed.
 *
 * One line per track: the file's metadata first, then the text fields base64-encoded, which keeps
 * tabs and newlines inside tags from breaking the format. The cache is disposable — a missing,
 * truncated or unreadable file just means the next scan reads every tag again.
 */
internal object LibraryCache {
    private const val HEADER = "auxio-library 1"
    private const val FIELDS = 11

    fun read(file: Path): Map<Path, CachedTrack> =
        runCatching {
                val lines = Files.readAllLines(file)
                if (lines.firstOrNull() != HEADER) {
                    emptyMap()
                } else {
                    lines.drop(1).mapNotNull(::entry).associateBy { it.track.path }
                }
            }
            .getOrDefault(emptyMap())

    fun write(file: Path, entries: Collection<CachedTrack>) {
        runCatching {
            Files.createDirectories(file.parent)
            val temporary = Files.createTempFile(file.parent, "library", ".tmp")
            try {
                Files.writeString(
                    temporary,
                    buildString {
                        appendLine(HEADER)
                        entries.forEach { appendLine(line(it)) }
                    },
                )
                try {
                    Files.move(
                        temporary,
                        file,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE,
                    )
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING)
                }
            } finally {
                Files.deleteIfExists(temporary)
            }
        }
    }

    private fun line(entry: CachedTrack): String {
        val track = entry.track
        return listOf(
                entry.modified.toString(),
                entry.size.toString(),
                encode(track.path.toString()),
                encode(track.title),
                encode(track.artist),
                encode(track.album),
                encode(track.genre),
                track.trackNumber?.toString().orEmpty(),
                track.year?.toString().orEmpty(),
                encode(track.date),
                track.durationSeconds?.toString().orEmpty(),
            )
            .joinToString("\t")
    }

    private fun entry(line: String): CachedTrack? {
        val fields = line.split('\t')
        if (fields.size != FIELDS) return null

        val modified = fields[0].toLongOrNull() ?: return null
        val size = fields[1].toLongOrNull() ?: return null
        val path = decode(fields[2]) ?: return null
        val title = decode(fields[3]) ?: return null

        return CachedTrack(
            modified,
            size,
            Track(
                path = Path.of(path).toAbsolutePath().normalize(),
                title = title,
                artist = decode(fields[4]),
                album = decode(fields[5]),
                genre = decode(fields[6]),
                trackNumber = fields[7].toIntOrNull(),
                year = fields[8].toIntOrNull(),
                date = decode(fields[9]),
                durationSeconds = fields[10].toDoubleOrNull(),
            ),
        )
    }

    private fun encode(value: String?): String =
        value
            ?.let { Base64.getEncoder().encodeToString(it.toByteArray(StandardCharsets.UTF_8)) }
            .orEmpty()

    private fun decode(value: String): String? =
        value
            .takeIf { it.isNotEmpty() }
            ?.let {
                runCatching { String(Base64.getDecoder().decode(it), StandardCharsets.UTF_8) }
                    .getOrNull()
            }
}
