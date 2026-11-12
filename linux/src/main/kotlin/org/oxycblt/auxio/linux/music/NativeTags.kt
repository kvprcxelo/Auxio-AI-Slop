/*
 * Copyright (c) 2026 Auxio Project
 * NativeTags.kt is part of Auxio.
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

import java.nio.file.Path

/** Tag values as taglib reports them. Empty tags stay null rather than becoming "". */
data class Tags(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val trackNumber: Int? = null,
    val year: Int? = null,
    val durationSeconds: Int? = null,
)

/**
 * Reads tags through taglib, using the same library build the Android app bundles.
 *
 * The native library is optional at runtime: without it the library falls back to file names, so a
 * missing [libauxiotags] degrades instead of preventing playback.
 */
internal object NativeTags {
    private const val LIBRARY = "auxiotags"

    /** Overridden by the Gradle test task, and by the AppImage through AUXIO_TAGS_LIB. */
    private const val LIBRARY_PROPERTY = "auxio.tags.library"

    private val loaded: Boolean by lazy { load() }

    val isAvailable: Boolean
        get() = loaded

    fun read(path: Path): Tags? {
        if (!loaded) return null

        val fields =
            runCatching { readNative(path.toAbsolutePath().toString()) }.getOrNull() ?: return null

        fun field(index: Int): String? = fields.getOrNull(index)?.takeIf { it.isNotBlank() }

        return Tags(
            title = field(0),
            artist = field(1),
            album = field(2),
            trackNumber = field(3)?.toIntOrNull(),
            year = field(4)?.toIntOrNull(),
            durationSeconds = field(5)?.toIntOrNull(),
        )
    }

    /** Encoded embedded artwork (PNG/JPEG bytes), or null when the file has none. */
    fun artwork(path: Path): ByteArray? {
        if (!loaded) return null
        return runCatching { readArtworkNative(path.toAbsolutePath().toString()) }.getOrNull()
    }

    private fun load(): Boolean {
        System.getProperty(LIBRARY_PROPERTY)?.let { path ->
            if (runCatching { System.load(path) }.isSuccess) return true
        }

        System.getenv("AUXIO_TAGS_LIB")
            ?.takeIf { it.isNotBlank() }
            ?.let { path ->
                if (runCatching { System.load(path) }.isSuccess) return true
            }

        return runCatching { System.loadLibrary(LIBRARY) }.isSuccess
    }

    @JvmStatic private external fun readNative(path: String): Array<String?>?

    @JvmStatic private external fun readArtworkNative(path: String): ByteArray?
}
