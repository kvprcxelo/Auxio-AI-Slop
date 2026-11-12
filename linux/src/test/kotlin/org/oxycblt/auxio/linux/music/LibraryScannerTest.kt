/*
 * Copyright (c) 2026 Auxio Project
 * LibraryScannerTest.kt is part of Auxio.
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
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.name
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryScannerTest {
    private val root = Files.createTempDirectory("auxio-scan-test")

    @AfterTest
    fun cleanUp() {
        Files.walk(root).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    @Test
    fun `collects playable files recursively and ignores the rest`() {
        val album = root.resolve("Artist/Album").createDirectories()
        album.resolve("02 - Second.flac").createFile()
        album.resolve("01 - First.mp3").createFile()
        root.resolve("notes.txt").createFile()
        root.resolve("cover.jpg").createFile()

        val tracks = LibraryScanner.scan(listOf(root))

        assertEquals(
            listOf("01 - First", "02 - Second"),
            tracks.map { it.title },
        )
    }

    @Test
    fun `ignores paths that are not directories`() {
        val file = root.resolve("single.flac").createFile()

        assertEquals(emptyList(), LibraryScanner.scan(listOf(file)))
        assertEquals(emptyList(), LibraryScanner.scan(listOf(root.resolve("missing"))))
    }

    @Test
    fun `track keeps its path and file name as a provisional title`() {
        val dir = root.resolve("music").createDirectories()
        val file = dir.resolve("Song.opus").createFile()

        val track = LibraryScanner.scan(listOf(dir)).single()

        assertEquals("Song", track.title)
        assertEquals("Song.opus", track.path.name)
    }
}
