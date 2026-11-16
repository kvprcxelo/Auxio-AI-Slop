/*
 * Copyright (c) 2026 Auxio Project
 * SortingTest.kt is part of Auxio.
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
import kotlin.test.Test
import kotlin.test.assertEquals

class SortingTest {
    private fun track(title: String, year: Int? = null, date: String? = null) =
        Track(Path.of("$title.mp3"), title, year = year, date = date)

    @Test
    fun `sorts tracks by name in both directions`() {
        val tracks = listOf(track("Beta"), track("alpha"), track("Gamma"))

        assertEquals(
            listOf("alpha", "Beta", "Gamma"),
            Sorting.tracks(tracks, SortMode.NAME_ASC).map { it.title },
        )
        assertEquals(
            listOf("Gamma", "Beta", "alpha"),
            Sorting.tracks(tracks, SortMode.NAME_DESC).map { it.title },
        )
    }

    @Test
    fun `sorts tracks by release date with undated last`() {
        val tracks =
            listOf(
                track("Beta", year = 2020),
                track("alpha", date = "2022-04-01"),
                track("Gamma"),
            )

        assertEquals(
            listOf("Beta", "alpha", "Gamma"),
            Sorting.tracks(tracks, SortMode.DATE_ASC).map { it.title },
        )
        assertEquals(
            listOf("alpha", "Beta", "Gamma"),
            Sorting.tracks(tracks, SortMode.DATE_DESC).map { it.title },
        )
    }

    @Test
    fun `sorts albums by name and date`() {
        val albums =
            listOf(
                Album("Zebra", null, 1999, null, emptyList()),
                Album("apple", null, 2020, null, emptyList()),
                Album("Mango", null, null, null, emptyList()),
            )

        assertEquals(
            listOf("apple", "Mango", "Zebra"),
            Sorting.albums(albums, SortMode.NAME_ASC).map { it.name },
        )
        assertEquals(
            listOf("Zebra", "apple", "Mango"),
            Sorting.albums(albums, SortMode.DATE_ASC).map { it.name },
        )
        assertEquals(
            listOf("apple", "Zebra", "Mango"),
            Sorting.albums(albums, SortMode.DATE_DESC).map { it.name },
        )
    }
}
