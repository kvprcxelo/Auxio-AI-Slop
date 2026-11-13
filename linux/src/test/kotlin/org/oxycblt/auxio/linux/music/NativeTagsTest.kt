/*
 * Copyright (c) 2026 Auxio Project
 * NativeTagsTest.kt is part of Auxio.
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
import kotlin.io.path.copyTo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

class NativeTagsTest {
    private fun resource(name: String): Path =
        Path.of(javaClass.getResource("/$name")?.toURI() ?: error("$name missing"))

    @Test
    fun `reads title, artist, album and duration from a flac file`() {
        assumeTrue(NativeTags.isAvailable, "taglib shim is not built")

        val tags = assertNotNull(NativeTags.read(resource("tagged.flac")))

        assertEquals("Tagged Track", tags.title)
        assertEquals("Test Artist", tags.artist)
        assertEquals("Test Album", tags.album)
        assertEquals(3, tags.trackNumber)
        assertEquals(2024, tags.year)
        assertEquals(1, tags.durationSeconds)
    }

    @Test
    fun `reads embedded artwork`() {
        assumeTrue(NativeTags.isAvailable, "taglib shim is not built")

        val artwork = assertNotNull(NativeTags.artwork(resource("artwork.flac")))

        // The fixture has a PNG attached, so the bytes should carry the PNG signature.
        assertEquals(0x89.toByte(), artwork[0])
        assertEquals("PNG".toByteArray().toList(), artwork.slice(1..3))
    }

    @Test
    fun `reports no artwork when a file has none`() {
        assumeTrue(NativeTags.isAvailable, "taglib shim is not built")

        assertNull(NativeTags.artwork(resource("tagged.flac")))
    }

    @Test
    fun `leaves fields empty for a file with no tags`() {
        assumeTrue(NativeTags.isAvailable, "taglib shim is not built")

        val empty = Files.createTempFile("auxio-empty", ".mp3")
        try {
            // taglib opens the file and reports blank tags, so the reader either fails outright
            // or returns a record with nothing in it.
            val tags = NativeTags.read(empty)
            assertNull(tags?.title)
            assertNull(tags?.artist)
            assertNull(tags?.album)
        } finally {
            Files.deleteIfExists(empty)
        }
    }

    @Test
    fun `scanner falls back to the file name when a file has no tags`() {
        assumeTrue(NativeTags.isAvailable, "taglib shim is not built")

        val dir = Files.createTempDirectory("auxio-untagged")
        try {
            dir.resolve("09 - Filename Only.flac").also { Files.createFile(it) }

            val track = LibraryScanner.scan(listOf(dir)).songs.single()

            assertEquals("09 - Filename Only", track.title)
            // The library normalises missing artists to a single placeholder.
            assertEquals(Library.UNKNOWN_ARTIST, track.artist)
        } finally {
            Files.walk(dir).use { stream ->
                stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
            }
        }
    }

    @Test
    fun `scanner prefers tags over the file name`() {
        assumeTrue(NativeTags.isAvailable, "taglib shim is not built")

        val dir = Files.createTempDirectory("auxio-tagged")
        try {
            resource("tagged.flac").copyTo(dir.resolve("01 - ignored name.flac"))

            val track = LibraryScanner.scan(listOf(dir)).songs.single()

            assertEquals("Tagged Track", track.title)
            assertEquals("Test Artist", track.artist)
            assertEquals("Test Album", track.album)
            assertTrue(track.durationSeconds == 1.0)
        } finally {
            Files.walk(dir).use { stream ->
                stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
            }
        }
    }
}
