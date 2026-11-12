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

/**
 * Preferences, stored as a properties file under the XDG config directory.
 *
 * Auxio keeps these in DataStore behind Hilt; the desktop port needs two keys, so a plain file
 * avoids the whole DI graph.
 */
object Settings {
    /** Semicolons and slashes are what most tagged libraries actually use. */
    const val DEFAULT_SEPARATORS = ";/"

    private const val SEPARATORS_KEY = "metadata.separators"

    private val file: Path by lazy {
        val base =
            System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }?.let { Path(it) }
                ?: Path(System.getProperty("user.home"), ".config")
        base.resolve("auxio/settings.properties")
    }

    fun separators(): String = properties().getProperty(SEPARATORS_KEY) ?: DEFAULT_SEPARATORS

    fun separators(value: String) {
        val properties = properties()
        properties.setProperty(SEPARATORS_KEY, value)
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
