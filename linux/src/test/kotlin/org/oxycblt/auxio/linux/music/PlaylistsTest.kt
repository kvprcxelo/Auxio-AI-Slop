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
import kotlin.io.path.createDirectories
import kotlin.io.path.nameWithoutExtension
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
    fun `creates an empty playlist that shows up in the playlists tab`() {
        val directory = Files.createTempDirectory("auxio-m3u-new")
        val music = Files.createTempDirectory("auxio-m3u-new-music")
        val song = Files.writeString(music.resolve("a.mp3"), "")

        val created = Playlists.create("Late night", directory)

        assertEquals("Late night.m3u", created?.fileName?.toString())
        assertTrue(Files.exists(created!!))
        // The managed folder is searched even when it is not one of the music roots, and a
        // playlist created there is kept while it is still empty.
        val loaded = Playlists.load(listOf(music), listOf(Track(song, "A")), directory)
        assertEquals(listOf("Late night"), loaded.map { it.name })
    }

    @Test
    fun `lists a playlist once when a music root contains the managed folder`() {
        val music = Files.createTempDirectory("auxio-m3u-nested")
        val directory = Files.createDirectories(music.resolve("auxio").resolve("playlists"))
        val song = Files.writeString(music.resolve("a.mp3"), "")
        Playlists.create("Mix", directory)

        val loaded = Playlists.load(listOf(music), listOf(Track(song, "A")), directory)

        assertEquals(1, loaded.size)
        assertEquals("Mix", loaded.single().name)
    }

    @Test
    fun `adds songs to a playlist and skips the ones it already has`() {
        val directory = Files.createTempDirectory("auxio-m3u-add")
        val first = Files.writeString(directory.resolve("a.mp3"), "")
        val second = Files.writeString(directory.resolve("b.mp3"), "")
        val file = Playlists.create("Mix", directory)!!
        val playlist = Playlist("Mix", emptyList(), file)
        val tracks = listOf(Track(first, "A"), Track(second, "B"))

        assertEquals(2, Playlists.addTracks(playlist, tracks))
        assertEquals(0, Playlists.addTracks(playlist, tracks), "the songs are already listed")

        val loaded = Playlists.load(listOf(directory), tracks, directory).single()
        assertEquals(listOf("A", "B"), loaded.tracks.map { it.title })
    }

    @Test
    fun `removes a song, keeps the others, and keeps an emptied playlist listed`() {
        val directory = Files.createTempDirectory("auxio-m3u-remove")
        val first = Files.writeString(directory.resolve("a.mp3"), "")
        val second = Files.writeString(directory.resolve("b.mp3"), "")
        val file = Playlists.create("Mix", directory)!!
        val playlist = Playlist("Mix", emptyList(), file)
        val tracks = listOf(Track(first, "A"), Track(second, "B"))
        Playlists.addTracks(playlist, tracks)

        assertEquals(1, Playlists.removeTrack(playlist, tracks.first()))
        assertEquals(0, Playlists.removeTrack(playlist, tracks.first()))
        assertEquals(
            listOf("B"),
            Playlists.load(listOf(directory), tracks, directory).single().tracks.map { it.title },
        )

        assertEquals(1, Playlists.removeTrack(playlist, tracks.last()))
        assertTrue(
            Playlists.load(listOf(directory), tracks, directory).single().tracks.isEmpty(),
            "an emptied playlist in the managed folder stays listed",
        )
    }

    @Test
    fun `imports a playlist with absolute entries and keeps both copies`() {
        val source = Files.createTempDirectory("auxio-m3u-import")
        val song = Files.writeString(source.resolve("a.mp3"), "")
        val playlist = source.resolve("Mix.m3u")
        Files.writeString(playlist, "#EXTM3U\na.mp3\n")
        val target = Files.createTempDirectory("auxio-m3u-target")

        val imported = Playlists.importFrom(playlist, target)

        assertEquals("Mix.m3u", imported?.fileName?.toString())
        assertEquals(
            "#EXTM3U\n${song.toAbsolutePath()}\n",
            Files.readString(imported!!),
        )
        assertTrue(Files.exists(playlist))
        assertEquals("Mix (2)", Playlists.importFrom(playlist, target)?.nameWithoutExtension)
    }

    @Test
    fun `refuses playlist names that would leave the directory`() {
        val dir = Files.createTempDirectory("auxio-m3u-unsafe")

        assertEquals(
            "..-..-etc-passwd.m3u",
            Playlists.create("../../etc/passwd", dir)?.fileName?.toString(),
        )
        assertEquals(null, Playlists.create("   ", dir))
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
