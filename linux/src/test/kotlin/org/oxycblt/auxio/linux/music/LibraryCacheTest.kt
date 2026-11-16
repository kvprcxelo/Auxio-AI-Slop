/*
 * Copyright (c) 2026 Auxio Project
 * LibraryCacheTest.kt is part of Auxio.
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LibraryCacheTest {
    @Test
    fun `round trips tracks through the cache`() {
        val file = Files.createTempDirectory("auxio-library-cache").resolve("library")
        val track =
            Track(
                path = Path.of("/music/Artist/Album/01 - Song.flac"),
                title = "Song\twith a tab",
                artist = "Artist",
                album = "Album",
                genre = "Rock; Pop",
                trackNumber = 1,
                year = 2024,
                date = "2024-03-01",
                durationSeconds = 123.5,
            )
        val entry = CachedTrack(1_700_000_000_000, 4096, track)

        LibraryCache.write(file, listOf(entry))

        assertEquals(entry, LibraryCache.read(file)[track.path])
    }

    @Test
    fun `a missing or corrupt cache reads as empty`() {
        val dir = Files.createTempDirectory("auxio-library-corrupt")

        assertTrue(LibraryCache.read(dir.resolve("missing")).isEmpty())

        Files.writeString(dir.resolve("garbage"), "garbage\nnot\ta\ttrack\n")
        assertTrue(LibraryCache.read(dir.resolve("garbage")).isEmpty())
    }
}
