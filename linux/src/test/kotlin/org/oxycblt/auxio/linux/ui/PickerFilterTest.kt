/*
 * Copyright (c) 2026 Auxio Project
 * PickerFilterTest.kt is part of Auxio.
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
 
package org.oxycblt.auxio.linux.ui

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import org.oxycblt.auxio.linux.music.Track

class PickerFilterTest {
    private val songs =
        listOf(
            Track(Path.of("alpha.mp3"), "Alpha", artist = "First Artist", album = "One"),
            Track(Path.of("beta.mp3"), "Beta", artist = "Second Artist", album = "Two"),
        )

    @Test
    fun `filters by title, artist and album`() {
        assertEquals(listOf("Alpha"), filterPickerSongs(songs, "alp").map { it.title })
        assertEquals(listOf("Beta"), filterPickerSongs(songs, "second").map { it.title })
        assertEquals(listOf("Beta"), filterPickerSongs(songs, "two").map { it.title })
        assertEquals(songs, filterPickerSongs(songs, ""))
        assertEquals(emptyList(), filterPickerSongs(songs, "missing"))
    }
}
