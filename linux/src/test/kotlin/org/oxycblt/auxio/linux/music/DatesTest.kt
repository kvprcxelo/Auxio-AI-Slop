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
    fun `formats with a typed pattern`() {
        assertEquals("2024-05-03", Dates.format("2024-05-03", "YYYY-MM-DD", locale))
        assertEquals("20240503", Dates.format("2024-05-03", "YYYYMMDD", locale))
        assertEquals("03/05/24", Dates.format("2024-05-03", "DD/MM/YY", locale))
        assertEquals("2024", Dates.format("2024-05-03", "YYYY", locale))
    }

    @Test
    fun `keeps the year when the tag has no month or day`() {
        assertEquals("2024", Dates.format("2024", "YYYY-MM-DD", locale))
        assertEquals("2024", Dates.format("2024-05", "DD/MM/YYYY", locale))
    }

    @Test
    fun `shows unparseable dates verbatim`() {
        assertEquals("sometime in spring", Dates.format("sometime in spring", "YYYY", locale))
    }

    @Test
    fun `treats slashes as separators`() {
        assertEquals("2024-05-03", Dates.format("2024/05/03", "YYYY-MM-DD", locale))
    }

    @Test
    fun `resolves the system setting to the locale short date`() {
        assertEquals("DD/MM/YYYY", Dates.resolvePattern(Dates.SYSTEM, Locale.UK))
        assertEquals("MM/DD/YY", Dates.resolvePattern(Dates.SYSTEM, Locale.US))
        // A typed pattern wins over the locale.
        assertEquals("YYYYMMDD", Dates.resolvePattern("YYYYMMDD", Locale.UK))
        assertEquals("DD/MM/YYYY", Dates.resolvePattern("  ", Locale.UK))
    }

    @Test
    fun `renders tokens and passes everything else through`() {
        assertEquals("2024/05/03", Dates.render(2024, 5, 3, "YYYY/MM/DD"))
        assertEquals("03 May-ish 24", Dates.render(2024, 5, 3, "DD May-ish YY"))
        assertEquals("spring", Dates.render(2024, 5, 3, "spring"))
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
