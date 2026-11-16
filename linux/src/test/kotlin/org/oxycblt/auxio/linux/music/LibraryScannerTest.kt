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

        val songs = LibraryScanner.scan(listOf(root)).songs

        assertEquals(
            listOf("01 - First", "02 - Second"),
            songs.map { it.title },
        )
    }

    @Test
    fun `ignores paths that are not directories`() {
        val file = root.resolve("single.flac").createFile()

        assertEquals(emptyList(), LibraryScanner.scan(listOf(file)).songs)
        assertEquals(emptyList(), LibraryScanner.scan(listOf(root.resolve("missing"))).songs)
    }

    @Test
    fun `track keeps its path and file name as a provisional title`() {
        val dir = root.resolve("music").createDirectories()
        val file = dir.resolve("Song.opus").createFile()

        val track = LibraryScanner.scan(listOf(dir)).songs.single()

        assertEquals("Song", track.title)
        assertEquals("Song.opus", track.path.name)
    }

    @Test
    fun `a cached scan reuses tags for unchanged files`() {
        val dir = root.resolve("music").createDirectories()
        val file = dir.resolve("Song.flac").createFile()
        val cache = root.resolve("cache").resolve("library")

        val first = LibraryScanner.scan(listOf(dir), useCache = true, cacheFile = cache)
        assertEquals("Song", first.songs.single().title)

        // A second cached scan trusts the stored tags while the file is untouched.
        val cached =
            CachedTrack(
                Files.getLastModifiedTime(file).toMillis(),
                Files.size(file),
                first.songs.single().copy(title = "Cached title"),
            )
        LibraryCache.write(cache, listOf(cached))
        assertEquals(
            "Cached title",
            LibraryScanner.scan(listOf(dir), useCache = true, cacheFile = cache)
                .songs
                .single()
                .title,
        )

        // A full scan ignores the cache and reads the file again.
        assertEquals(
            "Song",
            LibraryScanner.scan(listOf(dir), useCache = false, cacheFile = cache)
                .songs
                .single()
                .title,
        )
    }
}
