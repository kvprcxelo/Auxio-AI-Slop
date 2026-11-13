/*
 * Copyright (c) 2026 Auxio Project
 * Dates.kt is part of Auxio.
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

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Album dates as tags spell them ("2024", "2024-05", "2024-05-03") and as the settings ask for
 * them. Tags are inconsistent, so anything unparseable is shown verbatim rather than dropped.
 */
object Dates {
    /** The id stored in settings; [label] is what the settings screen shows. */
    data class Preset(val id: String, val label: String)

    const val SYSTEM = "system"
    const val YEAR_ONLY = "year"

    val PRESETS =
        listOf(
            Preset(SYSTEM, "System"),
            Preset(YEAR_ONLY, "Year only"),
            Preset("iso", "2024-05-03"),
            Preset("d MMM yyyy", "3 May 2024"),
            Preset("MMM d, yyyy", "May 3, 2024"),
            Preset("dd/MM/yyyy", "03/05/2024"),
            Preset("MM/dd/yyyy", "05/03/2024"),
        )

    fun format(raw: String?, presetId: String, locale: Locale = Locale.getDefault()): String? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val (year, month, day) = parts(value) ?: return value

        if (presetId == YEAR_ONLY) return year.toString()
        // A tag with only a year cannot be rendered as a full date.
        if (month <= 0 || day <= 0) return year.toString()

        val date =
            runCatching { LocalDate.of(year, month, day) }.getOrNull()
                ?: return "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

        return date.format(formatter(presetId, locale))
    }

    /** Comparable year/month/day, missing parts as zero, so "2024" sorts before "2024-05". */
    fun parts(raw: String?): Triple<Int, Int, Int>? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val match = PATTERN.find(value.replace('/', '-')) ?: return null
        val year = match.groupValues[1].toIntOrNull() ?: return null
        val month = match.groupValues[2].toIntOrNull() ?: 0
        val day = match.groupValues[3].toIntOrNull() ?: 0
        return Triple(year, month, day)
    }

    /** The earliest of the given raw dates, or null when none parse. */
    fun earliest(values: List<String?>): String? =
        values
            .filterNotNull()
            .mapNotNull { raw ->
                parts(raw)?.let { (year, month, day) -> raw to rank(year, month, day) }
            }
            .minByOrNull { it.second }
            ?.first

    private fun rank(year: Int, month: Int, day: Int): Int = year * 10_000 + month * 100 + day

    private fun formatter(presetId: String, locale: Locale): DateTimeFormatter =
        when (presetId) {
            "iso" -> DateTimeFormatter.ISO_LOCAL_DATE
            "d MMM yyyy" -> DateTimeFormatter.ofPattern("d MMM yyyy", locale)
            "MMM d, yyyy" -> DateTimeFormatter.ofPattern("MMM d, yyyy", locale)
            "dd/MM/yyyy" -> DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)
            "MM/dd/yyyy" -> DateTimeFormatter.ofPattern("MM/dd/yyyy", locale)
            else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        }

    private val PATTERN = Regex("(\\d{4})(?:-(\\d{1,2}))?(?:-(\\d{1,2}))?")
}
