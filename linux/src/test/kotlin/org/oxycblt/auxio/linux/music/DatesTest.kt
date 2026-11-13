/*
 * Copyright (c) 2026 Auxio Project
 * DatesTest.kt is part of Auxio.
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

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DatesTest {
    private val locale = Locale.UK

    @Test
    fun `formats a full date with the chosen preset`() {
        assertEquals("2024-05-03", Dates.format("2024-05-03", "iso", locale))
        assertEquals("3 May 2024", Dates.format("2024-05-03", "d MMM yyyy", locale))
        assertEquals("May 3, 2024", Dates.format("2024-05-03", "MMM d, yyyy", locale))
        assertEquals("03/05/2024", Dates.format("2024-05-03", "dd/MM/yyyy", locale))
        assertEquals("05/03/2024", Dates.format("2024-05-03", "MM/dd/yyyy", locale))
        assertEquals("2024", Dates.format("2024-05-03", Dates.YEAR_ONLY, locale))
    }

    @Test
    fun `keeps the year when the tag has no month or day`() {
        assertEquals("2024", Dates.format("2024", "iso", locale))
        assertEquals("2024", Dates.format("2024-05", "d MMM yyyy", locale))
    }

    @Test
    fun `shows unparseable dates verbatim`() {
        assertEquals("sometime in spring", Dates.format("sometime in spring", "iso", locale))
    }

    @Test
    fun `treats slashes as separators`() {
        assertEquals("2024-05-03", Dates.format("2024/05/03", "iso", locale))
    }

    @Test
    fun `parses year, month and day with missing parts as zero`() {
        assertEquals(Triple(2024, 5, 3), Dates.parts("2024-05-03"))
        assertEquals(Triple(2024, 5, 0), Dates.parts("2024-05"))
        assertEquals(Triple(2024, 0, 0), Dates.parts("2024"))
        assertNull(Dates.parts(""))
        assertNull(Dates.parts(null))
    }

    @Test
    fun `picks the earliest date regardless of how precise each tag is`() {
        assertEquals("1999", Dates.earliest(listOf("2001-05-03", "1999", null)))
        assertEquals("2001-01-05", Dates.earliest(listOf("2001-05-03", "2001-01-05")))
        assertNull(Dates.earliest(listOf(null, "not a date")))
    }
}
