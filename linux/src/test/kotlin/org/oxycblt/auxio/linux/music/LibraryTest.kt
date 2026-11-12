/*
 * Copyright (c) 2026 Auxio Project
 * LibraryTest.kt is part of Auxio.
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
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LibraryTest {
    private fun track(
        name: String,
        title: String,
        artist: String? = null,
        album: String? = null,
        trackNumber: Int? = null,
        year: Int? = null,
        duration: Double? = null,
        folder: String = "music",
    ) =
        Track(
            path = Path("/$folder/$name"),
            title = title,
            artist = artist,
            album = album,
            trackNumber = trackNumber,
            year = year,
            durationSeconds = duration,
        )

    @Test
    fun `groups songs into albums ordered by track number`() {
        val library =
            Library.build(
                listOf(
                    track("b.flac", "Second", "Artist", "Album", trackNumber = 2),
                    track("a.flac", "First", "Artist", "Album", trackNumber = 1),
                )
            )

        val album = library.albums.single()
        assertEquals("Album", album.name)
        assertEquals("Artist", album.artist)
        assertEquals(listOf("First", "Second"), album.tracks.map { it.title })
    }

    @Test
    fun `falls back to the folder name when a song has no album tag`() {
        val library =
            Library.build(
                listOf(track("a.flac", "Loose", "Artist", album = null, folder = "Mixtape"))
            )

        assertEquals("Mixtape", library.albums.single().name)
    }

    @Test
    fun `keeps albums by the same name apart when the artist differs`() {
        val library =
            Library.build(
                listOf(
                    track("a.flac", "One", "First", "Greatest Hits"),
                    track("b.flac", "Two", "Second", "Greatest Hits"),
                )
            )

        assertEquals(2, library.albums.size)
        assertEquals(setOf("First", "Second"), library.albums.mapNotNull { it.artist }.toSet())
    }

    @Test
    fun `groups artists, including songs without an artist tag`() {
        val library =
            Library.build(
                listOf(
                    track("a.flac", "One", "Artist", "Album"),
                    track("b.flac", "Two", "Artist", "Album"),
                    track("c.flac", "Three", artist = null, album = "Album"),
                )
            )

        assertEquals(listOf("Artist", Library.UNKNOWN_ARTIST), library.artists.map { it.name })
        assertEquals(2, library.artists.first().tracks.size)
        assertEquals(1, library.artists.last().tracks.size)
    }

    @Test
    fun `splits artists on the configured separators`() {
        val library =
            Library.build(
                listOf(
                    track("a.flac", "One", "Alpha; Beta", "Split Album"),
                    track("b.flac", "Two", "Gamma / Delta", "Split Album"),
                ),
                separators = setOf(';', '/'),
            )

        assertEquals(
            listOf("Alpha", "Beta", "Delta", "Gamma"),
            library.artists.map { it.name },
        )
        // Both split names point back at the album they appear on.
        assertEquals("Split Album", library.artists.first().albums.single().name)
    }

    @Test
    fun `keeps artist tags intact when no separators are configured`() {
        val library = Library.build(listOf(track("a.flac", "One", "Alpha; Beta", "Album")))

        assertEquals(listOf("Alpha; Beta"), library.artists.map { it.name })
    }

    @Test
    fun `does not split on characters that are not configured`() {
        val library =
            Library.build(
                listOf(track("a.flac", "One", "Simon & Garfunkel", "Album")),
                separators = setOf(';'),
            )

        assertEquals(listOf("Simon & Garfunkel"), library.artists.map { it.name })
    }

    @Test
    fun `takes the earliest year of an album and sums its duration`() {
        val library =
            Library.build(
                listOf(
                    track("a.flac", "One", "Artist", "Album", year = 1999, duration = 100.0),
                    track("b.flac", "Two", "Artist", "Album", year = 1997, duration = 50.0),
                )
            )

        val album = library.albums.single()
        assertEquals(1997, album.year)
        assertEquals(150.0, album.durationSeconds)
    }

    @Test
    fun `leaves year empty when no song has one`() {
        val library = Library.build(listOf(track("a.flac", "One", "Artist", "Album")))

        assertNull(library.albums.single().year)
    }

    @Test
    fun `uses the path as a stable track identity`() {
        val one = track("a.flac", "One", "Artist", "Album")
        val same = one.copy(title = "Renamed")

        assertEquals(one.id, same.id)
        assertEquals(Path("/music/a.flac").toAbsolutePath().toString(), one.id)
    }
}
