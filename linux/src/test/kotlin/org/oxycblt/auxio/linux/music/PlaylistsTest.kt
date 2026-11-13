/*
 * Copyright (c) 2026 Auxio Project
 * PlaylistsTest.kt is part of Auxio.
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaylistsTest {
    @Test
    fun `reads m3u playlists with relative and absolute entries`() {
        val dir = Files.createTempDirectory("auxio-m3u")
        val first = Files.writeString(dir.resolve("a.mp3"), "")
        Files.createDirectory(dir.resolve("sub"))
        val second = Files.writeString(dir.resolve("sub/b.mp3"), "")

        Files.writeString(
            dir.resolve("Mix.m3u"),
            "#EXTM3U\n${first.toAbsolutePath()}\nsub/b.mp3\nmissing.mp3\n",
        )

        val playlists = Playlists.load(listOf(dir), listOf(Track(first, "A"), Track(second, "B")))

        assertEquals(1, playlists.size)
        assertEquals("Mix", playlists.single().name)
        // The absolute entry, the relative one, and nothing for the missing file.
        assertEquals(listOf("A", "B"), playlists.single().tracks.map { it.title })
    }

    @Test
    fun `skips playlists that match no scanned songs`() {
        val dir = Files.createTempDirectory("auxio-m3u-empty")
        Files.writeString(dir.resolve("Broken.m3u8"), "#EXTM3U\ngone.mp3\n")

        val playlists =
            Playlists.load(listOf(dir), listOf(Track(dir.resolve("other.mp3"), "Other")))

        assertTrue(playlists.isEmpty())
    }
}
