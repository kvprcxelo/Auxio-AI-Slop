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

import androidx.compose.ui.graphics.Color
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import org.oxycblt.auxio.linux.music.Dates
import org.oxycblt.auxio.linux.theme.Brightness
import org.oxycblt.auxio.linux.theme.SchemeType
import org.oxycblt.auxio.linux.theme.ThemeMode
import org.oxycblt.auxio.linux.theme.ThemeSettings
import org.oxycblt.auxio.linux.theme.toHexString

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
    private const val CACHE_LIMIT_KEY = "cache.maxSizeMb"
    private const val THEME_MODE_KEY = "theme.mode"
    private const val THEME_SEED_KEY = "theme.seed"
    private const val THEME_BRIGHTNESS_KEY = "theme.brightness"

    /** Written by builds that offered hue and chroma sliders, before the seed became a colour. */
    private const val LEGACY_HUE_KEY = "theme.hue"
    private const val LEGACY_CHROMA_KEY = "theme.chroma"
    private const val THEME_WALLPAPER_KEY = "theme.wallpaper"
    private const val THEME_TYPE_KEY = "theme.schemeType"

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

    /** Ceiling for the cover cache, in megabytes. */
    const val DEFAULT_CACHE_LIMIT_MB = 500L

    /** Smallest cache worth keeping; anything less would evict covers as they are extracted. */
    const val MINIMUM_CACHE_LIMIT_MB = 16L

    private val file: Path by lazy {
        // Renames the pre-0.7 settings file once, so preferences survive the move to `config`.
        Storage.migrateLegacyConfig()
        Storage.configFile
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

    /** How large the cover cache may grow before the oldest covers are evicted. */
    fun cacheLimitMb(): Long =
        properties().getProperty(CACHE_LIMIT_KEY)?.toLongOrNull()?.takeIf {
            it >= MINIMUM_CACHE_LIMIT_MB
        } ?: DEFAULT_CACHE_LIMIT_MB

    fun cacheLimitMb(value: Long) {
        put(CACHE_LIMIT_KEY, value.coerceAtLeast(MINIMUM_CACHE_LIMIT_MB).toString())
    }

    /** Where the colour scheme comes from, and the knobs the chosen source reads. */
    fun theme(): ThemeSettings {
        val properties = properties()

        return ThemeSettings(
            mode =
                properties.getProperty(THEME_MODE_KEY)?.let { key ->
                    ThemeMode.entries.firstOrNull { it.name == key }
                } ?: ThemeMode.DESKTOP,
            brightness =
                properties.getProperty(THEME_BRIGHTNESS_KEY)?.let { key ->
                    Brightness.entries.firstOrNull { it.name == key }
                } ?: Brightness.SYSTEM,
            seed =
                properties.getProperty(THEME_SEED_KEY)
                    ?: legacySeed(properties)
                    ?: ThemeSettings.DEFAULT_SEED,
            wallpaper = properties.getProperty(THEME_WALLPAPER_KEY) ?: "",
            type =
                properties.getProperty(THEME_TYPE_KEY)?.let { key ->
                    SchemeType.entries.firstOrNull { it.name == key }
                } ?: SchemeType.TONAL_SPOT,
        )
    }

    fun theme(value: ThemeSettings) {
        put(
            THEME_MODE_KEY to value.mode.name,
            THEME_BRIGHTNESS_KEY to value.brightness.name,
            THEME_SEED_KEY to value.seed,
            THEME_WALLPAPER_KEY to value.wallpaper,
            THEME_TYPE_KEY to value.type.name,
        )
    }

    /** Turns the old hue/chroma pair into the colour those sliders were showing. */
    private fun legacySeed(properties: Properties): String? {
        val hue = properties.getProperty(LEGACY_HUE_KEY)?.toFloatOrNull() ?: return null
        val chroma = (properties.getProperty(LEGACY_CHROMA_KEY)?.toFloatOrNull() ?: 48f) / 100f
        return Color.hsl(hue.mod(360f), chroma.coerceIn(0f, 1f), 0.5f).toHexString()
    }

    private fun put(key: String, value: String) = put(key to value)

    /** One read and one write per change, since a slider reports every pixel of a drag. */
    private fun put(vararg entries: Pair<String, String>) {
        val properties = properties()
        entries.forEach { (key, value) -> properties.setProperty(key, value) }
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
