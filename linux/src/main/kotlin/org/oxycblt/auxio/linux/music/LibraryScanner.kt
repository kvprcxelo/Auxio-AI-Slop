package org.oxycblt.auxio.linux.music

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension
import kotlin.streams.asSequence

/** A playable file. Tags are read once the taglib bridge is wired up. */
data class Track(
    val path: Path,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val durationSeconds: Double? = null,
)

/**
 * Walks music folders and collects playable files.
 *
 * This replaces MediaStore: on Linux the library is just the filesystem, so scanning is a walk
 * plus an extension filter. Tag parsing and the SQLite cache hang off this later.
 */
object LibraryScanner {
    private val EXTENSIONS =
        setOf(
            "flac", "mp3", "ogg", "oga", "opus", "m4a", "mp4", "aac",
            "wav", "aiff", "aif", "wv", "ape", "tta", "dsf", "dff",
        )

    fun scan(roots: List<Path>): List<Track> =
        roots
            .filter { Files.isDirectory(it) }
            .flatMap { root ->
                Files.walk(root).use { stream ->
                    stream
                        .asSequence()
                        .filter { it.isRegularFile() && it.extension.lowercase() in EXTENSIONS }
                        .map { path ->
                            Track(path = path, title = path.nameWithoutExtension)
                        }
                        .toList()
                }
            }
            .sortedBy { it.title.lowercase() }
}
