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
        date: String? = null,
        genre: String? = null,
    ) =
        Track(
            path = Path("/$folder/$name"),
            title = title,
            artist = artist,
            album = album,
            genre = genre,
            trackNumber = trackNumber,
            year = year,
            date = date,
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
    fun `drops repeated artist entries and adopts the library spelling`() {
        val library =
            Library.build(
                listOf(
                    track("a.flac", "One", "alpha; Alpha; Beta", "Album"),
                    track("b.flac", "Two", "Alpha", "Album"),
                ),
                separators = setOf(';'),
            )

        assertEquals(listOf("Alpha", "Beta"), library.artists.map { it.name })
        // The duplicate survives once, spelled the way the rest of the library spells it.
        assertEquals("Alpha; Beta", library.songs.first { it.title == "One" }.artist)
    }

    @Test
    fun `sorts past leading punctuation when the setting is on`() {
        val songs =
            listOf(
                track("a.flac", "*Zebra", "Artist", "Album"),
                track("b.flac", "Apple", "Artist", "Album"),
            )

        assertEquals(
            listOf("Apple", "*Zebra"),
            Library.build(songs, ignoreLeadingPunctuation = true).songs.map { it.title },
        )
        assertEquals(
            listOf("*Zebra", "Apple"),
            Library.build(songs, ignoreLeadingPunctuation = false).songs.map { it.title },
        )
    }

    @Test
    fun `groups genres case-insensitively and splits them like artists`() {
        val library =
            Library.build(
                listOf(
                    track("a.flac", "One", "Artist", "Album", genre = "Rock; rock"),
                    track("b.flac", "Two", "Artist", "Album", genre = "ROCK"),
                    track("c.flac", "Three", "Artist", "Album"),
                ),
                separators = setOf(';'),
            )

        val genre = library.genres.single()
        assertEquals("ROCK", genre.name)
        // The duplicate genre inside the first tag is dropped, and the untagged song is left out.
        assertEquals(listOf("One", "Two"), genre.tracks.map { it.title })
    }

    @Test
    fun `takes the earliest album date`() {
        val library =
            Library.build(
                listOf(
                    track("a.flac", "One", "Artist", "Album", date = "2001-05-03"),
                    track("b.flac", "Two", "Artist", "Album", date = "1999"),
                )
            )

        assertEquals("1999", library.albums.single().date)
    }

    @Test
    fun `groups artist names case-insensitively and shows the most common spelling`() {
        val library =
            Library.build(
                listOf(
                    track("a.flac", "One", "x", "Album"),
                    track("b.flac", "Two", "X", "Album"),
                    track("c.flac", "Three", "X", "Album"),
                )
            )

        assertEquals(listOf("X"), library.artists.map { it.name })
        assertEquals(3, library.artists.single().tracks.size)
    }

    @Test
    fun `prefers the uppercase spelling when both are equally common`() {
        val library =
            Library.build(
                listOf(
                    track("a.flac", "One", "x", "Album"),
                    track("b.flac", "Two", "X", "Album"),
                )
            )

        assertEquals(listOf("X"), library.artists.map { it.name })
    }

    @Test
    fun `sorts artists case-insensitively`() {
        val library =
            Library.build(
                listOf(
                    track("a.flac", "One", "beta", "Album"),
                    track("b.flac", "Two", "Alpha", "Album"),
                    track("c.flac", "Three", "gamma", "Album"),
                )
            )

        assertEquals(listOf("Alpha", "beta", "gamma"), library.artists.map { it.name })
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
