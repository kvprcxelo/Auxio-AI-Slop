/*
 * Copyright (c) 2026 Auxio Project
 * Desktop.kt is part of Auxio.
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
 
package org.oxycblt.auxio.linux.theme

import androidx.compose.ui.graphics.Color
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists
import kotlin.io.path.readLines
import org.oxycblt.auxio.linux.Storage

/**
 * What the desktop is themed with.
 *
 * There is no portable API for this, so the port asks in the order the desktops provide it: the XDG
 * appearance portal (and its accent colour), then GNOME's gsettings, then the KDE and GTK theme
 * files. Every probe is allowed to fail; the result is only a starting colour.
 */
object Desktop {
    /** What the desktop reports about its own theme. */
    data class Appearance(val accent: Color?, val dark: Boolean = true)

    /** The M3 baseline colour, so a desktop that says nothing looks like Auxio on Android. */
    val DEFAULT_ACCENT = Color(0xFF6750A4)

    private val ACCENTS =
        mapOf(
            "purple" to "#9141AC",
            "blue" to "#3584E4",
            "teal" to "#2190A4",
            "green" to "#3A944A",
            "yellow" to "#C88800",
            "orange" to "#ED5B00",
            "red" to "#E62D42",
            "pink" to "#D56199",
            "slate" to "#6F8396",
        )

    private val PORTAL =
        arrayOf(
            "gdbus",
            "call",
            "--session",
            "--dest",
            "org.freedesktop.portal.Desktop",
            "--object-path",
            "/org/freedesktop/portal/desktop",
            "--method",
            "org.freedesktop.portal.Settings.Read",
            "org.freedesktop.appearance",
        )

    fun appearance(): Appearance =
        Appearance(
            accent = portalAccent() ?: settingsAccent() ?: kdeAccent() ?: gtkAccent(),
            dark = portalDark() ?: settingsDark() ?: kdeDark() ?: true,
        )

    /** The wallpaper the desktop is showing, if it can be found. */
    fun wallpaper(): Path? =
        gnomeWallpaper() ?: kdeWallpaper() ?: hyprpaperWallpaper() ?: plasmaWallpaper()

    /** An accent colour from the appearance portal, as an 0..1 RGB tuple. */
    internal fun parseAccentTuple(output: String): Color? {
        val match = TUPLE.find(output) ?: return null
        val (red, green, blue) = match.destructured
        val channels = listOf(red, green, blue).mapNotNull { it.toFloatOrNull() }
        if (channels.size != 3 || channels.any { it > 1f || it < 0f }) return null

        return Color(channels[0], channels[1], channels[2])
    }

    /** An accent name from `gsettings`, which is all GNOME exposes without the portal. */
    internal fun parseAccentName(output: String): Color? {
        val name = output.trim().trim('\'').trim('"')
        return ACCENTS[name]?.let(::parseHex)
    }

    internal fun parseHex(hex: String): Color? {
        val digits = hex.removePrefix("#").takeIf { it.length == 6 } ?: return null
        val value = digits.toLongOrNull(16) ?: return null
        return Color(0xFF000000L or value)
    }

    /** KDE keeps its accent in `kdeglobals`, either explicitly or as the selection colour. */
    internal fun parseKdeAccent(text: String): Color? {
        text
            .lineSequence()
            .firstOrNull { it.startsWith("AccentColor=") }
            ?.removePrefix("AccentColor=")
            ?.trim()
            ?.let {
                return parseHex(it)
            }

        val selection = section(text, "Colors:Selection")
        selection
            .firstOrNull { it.startsWith("BackgroundNormal=") }
            ?.let { line ->
                val parts =
                    line.removePrefix("BackgroundNormal=").split(",").mapNotNull {
                        it.toIntOrNull()
                    }
                if (parts.size >= 3) return Color(parts[0], parts[1], parts[2])
            }
        return null
    }

    internal fun parseGtkAccent(css: String): Color? {
        val match = GTK_ACCENT.find(css) ?: return null
        val hex = match.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: return null
        return parseHex(hex)
    }

    /** A `file://` URI from a settings value, which arrives quoted. */
    internal fun parseFileUri(value: String): Path? {
        val uri = value.trim().trim('\'').trim('"')
        if (uri.isEmpty() || uri == "none") return null

        return runCatching {
                if (uri.startsWith("file://")) Path.of(java.net.URI(uri)) else Path.of(uri)
            }
            .getOrNull()
            ?.takeIf { Files.isRegularFile(it) }
    }

    private fun portalAccent(): Color? = portal("accent-color")?.let(::parseAccentTuple)

    /** The appearance portal's `color-scheme`: 1 is dark, 2 is light, 0 is no preference. */
    internal fun parsePortalDark(output: String): Boolean? =
        when {
            output.contains("uint32 1") -> true
            output.contains("uint32 2") -> false
            else -> null
        }

    private fun portalDark(): Boolean? = portal("color-scheme")?.let(::parsePortalDark)

    private fun portal(key: String): String? = run(*PORTAL, key)

    private fun settingsAccent(): Color? =
        run("gsettings", "get", "org.gnome.desktop.interface", "accent-color")
            ?.let(::parseAccentName)

    private fun settingsDark(): Boolean? =
        run("gsettings", "get", "org.gnome.desktop.interface", "color-scheme")?.let {
            when {
                it.contains("prefer-dark") -> true
                it.contains("prefer-light") || it.contains("default") -> false
                else -> null
            }
        }

    private fun kdeAccent(): Color? = read(Storage.configPath("kdeglobals"))?.let(::parseKdeAccent)

    /** KDE names its scheme, so a "Dark" in it is the closest thing to a preference. */
    internal fun parseKdeDark(text: String): Boolean? {
        val scheme =
            section(text, "General").firstOrNull { it.startsWith("ColorScheme=") } ?: return null
        return when {
            scheme.contains("Dark", ignoreCase = true) -> true
            scheme.contains("Light", ignoreCase = true) -> false
            else -> null
        }
    }

    private fun kdeDark(): Boolean? = read(Storage.configPath("kdeglobals"))?.let(::parseKdeDark)

    private fun gtkAccent(): Color? =
        listOf("gtk-4.0/gtk.css", "gtk-3.0/gtk.css").firstNotNullOfOrNull { file ->
            read(Storage.configPath(file))?.let(::parseGtkAccent)
        }

    private fun gnomeWallpaper(): Path? =
        run("gsettings", "get", "org.gnome.desktop.background", "picture-uri")?.let(::parseFileUri)

    private fun kdeWallpaper(): Path? {
        val text =
            read(Storage.configPath("plasma-org.kde.plasma.desktop-appletsrc")) ?: return null
        return text
            .lineSequence()
            .firstOrNull { it.startsWith("Image=file://") }
            ?.removePrefix("Image=")
            ?.let(::parseFileUri)
    }

    private fun plasmaWallpaper(): Path? {
        val text =
            read(Storage.configPath("plasma-org.kde.plasma.desktop-appletsrc")) ?: return null
        return text
            .lineSequence()
            .firstOrNull { it.startsWith("wallpaper=") }
            ?.removePrefix("wallpaper=")
            ?.let(::parseFileUri)
    }

    private fun hyprpaperWallpaper(): Path? {
        val text = read(Storage.configPath("hypr/hyprpaper.conf")) ?: return null
        return text
            .lineSequence()
            .firstOrNull { it.trimStart().startsWith("preload") }
            ?.substringAfter("=")
            ?.let(::parseFileUri)
    }

    private fun section(text: String, name: String): List<String> {
        var inside = false
        return text
            .lineSequence()
            .map { it.trim() }
            .filter { line ->
                when {
                    line == "[$name]" -> {
                        inside = true
                        false
                    }
                    line.startsWith("[") -> {
                        inside = false
                        false
                    }
                    else -> inside
                }
            }
            .toList()
    }

    private fun read(path: Path): String? =
        runCatching { if (path.exists()) path.readLines().joinToString("\n") else null }.getOrNull()

    private fun run(vararg command: String): String? =
        runCatching {
                val process = ProcessBuilder(command.toList()).redirectErrorStream(true).start()
                val output = process.inputStream.bufferedReader().use { it.readText() }
                if (!process.waitFor(3, TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                    null
                } else if (process.exitValue() == 0) output else null
            }
            .getOrNull()

    private val TUPLE = Regex("\\(\\s*([0-9.]+)\\s*,\\s*([0-9.]+)\\s*,\\s*([0-9.]+)\\s*\\)")
    private val GTK_ACCENT =
        Regex(
            "@define-color\\s+accent_bg_color\\s+(#[0-9a-fA-F]{6})|" +
                "@define-color\\s+accent_color\\s+(#[0-9a-fA-F]{6})"
        )
}
