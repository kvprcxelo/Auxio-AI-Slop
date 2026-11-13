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

import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Album dates as tags spell them ("2024", "2024-05", "2024-05-03") and as the settings ask for
 * them. Settings hold a pattern of `YYYY`, `YY`, `MM` and `DD` tokens with anything else kept
 * literally, or `system` for the locale's own short date format. Tags are inconsistent, so anything
 * unparseable is shown verbatim rather than dropped.
 */
object Dates {
    /** Stored in settings when the locale's short date format should be used. */
    const val SYSTEM = "system"

    /** What the settings screen offers as tokens. */
    val TOKENS = listOf("YYYY", "YY", "MM", "DD")

    /** The pattern actually used for a setting value, resolving [SYSTEM] against [locale]. */
    fun resolvePattern(pattern: String?, locale: Locale = Locale.getDefault()): String =
        pattern?.trim()?.takeIf { it.isNotEmpty() && !it.equals(SYSTEM, true) }
            ?: systemPattern(locale)

    /** The locale's short date, written with the tokens above, e.g. `DD/MM/YYYY`. */
    fun systemPattern(locale: Locale = Locale.getDefault()): String =
        convert(localizedPattern(locale))

    /** Formats [raw] with [pattern], falling back to the bare year when the tag lacks a date. */
    fun format(raw: String?, pattern: String?, locale: Locale = Locale.getDefault()): String? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val (year, month, day) = parts(value) ?: return value

        // A tag with only a year cannot be rendered as a full date.
        if (month <= 0 || day <= 0) return year.toString()

        return render(year, month, day, resolvePattern(pattern, locale))
    }

    /** Renders one date with a token pattern; unknown characters pass through untouched. */
    fun render(
        year: Int,
        month: Int,
        day: Int,
        pattern: String = "YYYY-MM-DD",
    ): String = buildString {
        var index = 0
        while (index < pattern.length) {
            when {
                pattern.startsWith("YYYY", index, ignoreCase = true) -> {
                    append(year.toString().padStart(4, '0'))
                    index += 4
                }
                pattern.startsWith("YY", index, ignoreCase = true) -> {
                    append((year % 100).toString().padStart(2, '0'))
                    index += 2
                }
                pattern.startsWith("MM", index, ignoreCase = true) -> {
                    append(month.toString().padStart(2, '0'))
                    index += 2
                }
                pattern.startsWith("DD", index, ignoreCase = true) -> {
                    append(day.toString().padStart(2, '0'))
                    index += 2
                }
                else -> {
                    append(pattern[index])
                    index++
                }
            }
        }
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

    /** `dd/MM/y` (UK), `M/d/yy` (US) and friends, reduced to the four tokens. */
    private fun convert(pattern: String): String = buildString {
        var index = 0
        while (index < pattern.length) {
            val char = pattern[index]
            when (char) {
                'y' -> {
                    // java.time spells a two-digit year as "yy"; a lone "y" is the full year.
                    val run = runLength(pattern, index, 'y')
                    append(if (run == 2) "YY" else "YYYY")
                    index += run
                }
                'M' -> {
                    append("MM")
                    index += runLength(pattern, index, 'M')
                }
                'd' -> {
                    append("DD")
                    index += runLength(pattern, index, 'd')
                }
                '\'' -> {
                    // Quoted literal text in a pattern: copy it through.
                    index++
                    while (index < pattern.length && pattern[index] != '\'') {
                        append(pattern[index])
                        index++
                    }
                    index++
                }
                else -> {
                    append(char)
                    index++
                }
            }
        }
    }

    private fun runLength(pattern: String, start: Int, char: Char): Int {
        var end = start
        while (end < pattern.length && pattern[end] == char) end++
        return end - start
    }

    private fun localizedPattern(locale: Locale): String =
        DateTimeFormatterBuilder.getLocalizedDateTimePattern(
            FormatStyle.SHORT,
            null,
            IsoChronology.INSTANCE,
            locale,
        )

    private val PATTERN = Regex("(\\d{4})(?:-(\\d{1,2}))?(?:-(\\d{1,2}))?")
}
