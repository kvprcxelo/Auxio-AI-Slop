/*
 * Copyright (c) 2026 Auxio Project
 * Settings.kt is part of Auxio.
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
 
package org.oxycblt.auxio.linux

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.Path
import org.oxycblt.auxio.linux.music.Dates

/**
 * Preferences, stored as a properties file under the XDG config directory.
 *
 * Auxio keeps these in DataStore behind Hilt; the desktop port needs a handful of keys, so a plain
 * file avoids the whole DI graph.
 */
object Settings {
    /** Semicolons and slashes are what most tagged libraries actually use. */
    const val DEFAULT_SEPARATORS = ";/"

    private const val SEPARATORS_KEY = "metadata.separators"
    private const val DATE_FORMAT_KEY = "metadata.dateFormat"
    private const val SORT_PUNCTUATION_KEY = "sorting.ignoreLeadingPunctuation"
    private const val COLLAGE_SEED_KEY = "artists.collageSeed"

    /** Date presets from before patterns existed, mapped to the equivalent token pattern. */
    private val LEGACY_DATE_FORMATS =
        mapOf(
            "year" to "YYYY",
            "iso" to "YYYY-MM-DD",
            "d MMM yyyy" to "YYYY-MM-DD",
            "MMM d, yyyy" to "YYYY-MM-DD",
            "dd/MM/yyyy" to "DD/MM/YYYY",
            "MM/dd/yyyy" to "MM/DD/YYYY",
        )

    /** Seed for the artist artwork collage; the same seed always gives the same arrangement. */
    const val DEFAULT_COLLAGE_SEED = "auxio"

    private val file: Path by lazy {
        val base =
            System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }?.let { Path(it) }
                ?: Path(System.getProperty("user.home"), ".config")
        base.resolve("auxio/settings.properties")
    }

    fun separators(): String = properties().getProperty(SEPARATORS_KEY) ?: DEFAULT_SEPARATORS

    fun separators(value: String) {
        put(SEPARATORS_KEY, value)
    }

    /** A [Dates] token pattern, or [Dates.SYSTEM] for the locale's short date. */
    fun dateFormat(): String {
        val stored = properties().getProperty(DATE_FORMAT_KEY) ?: return Dates.SYSTEM
        // Earlier builds stored preset ids; rewrite those into patterns once.
        val migrated = LEGACY_DATE_FORMATS[stored] ?: return stored
        dateFormat(migrated)
        return migrated
    }

    fun dateFormat(value: String) {
        put(DATE_FORMAT_KEY, value)
    }

    /** "\\*Song", "(Song)" and the like sort under the first letter when this is on. */
    fun ignoresLeadingPunctuation(): Boolean =
        properties().getProperty(SORT_PUNCTUATION_KEY)?.toBooleanStrictOrNull() ?: true

    fun ignoresLeadingPunctuation(value: Boolean) {
        put(SORT_PUNCTUATION_KEY, value.toString())
    }

    fun collageSeed(): String = properties().getProperty(COLLAGE_SEED_KEY) ?: DEFAULT_COLLAGE_SEED

    fun collageSeed(value: String) {
        put(COLLAGE_SEED_KEY, value)
    }

    private fun put(key: String, value: String) {
        val properties = properties()
        properties.setProperty(key, value)
        runCatching {
            file.parent?.let { Files.createDirectories(it) }
            Files.newOutputStream(file).use { properties.store(it, "Auxio for Linux") }
        }
    }

    private fun properties(): Properties {
        val properties = Properties()
        runCatching { Files.newInputStream(file).use { properties.load(it) } }
        return properties
    }
}
