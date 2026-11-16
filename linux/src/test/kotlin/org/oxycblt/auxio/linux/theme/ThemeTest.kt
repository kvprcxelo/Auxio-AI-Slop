/*
 * Copyright (c) 2026 Auxio Project
 * ThemeTest.kt is part of Auxio.
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
import androidx.compose.ui.graphics.luminance
import java.nio.file.Files
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThemeTest {
    private val appearance = Desktop.Appearance(accent = Color(0xFF3F6AAB), dark = true)

    private val matugenDump =
        """
        {
          "colors": {
            "amoled": { "primary": "#111111", "background": "#000000" },
            "dark": {
              "primary": "#adc6ff",
              "on_primary": "#002e69",
              "background": "#111318",
              "surface": "#111318",
              "on_surface": "#e3e2e6",
              "outline": "#8e9099"
            },
            "light": { "primary": "#005ac1", "background": "#f9f9ff" }
          },
          "colors_android": { "dark": { "color_accent_primary": "#00ff00" } }
        }
        """

    private val perRoleDump =
        """
        {
          "base16": { "base00": { "dark": { "color": "#26110d" } } },
          "colors": {
            "background": { "dark": { "color": "#191110" }, "light": { "color": "#fff8f6" } },
            "error": { "dark": { "color": "#ffb4ab" } },
            "on_primary": { "dark": { "color": "#561f12" } },
            "on_surface": { "dark": { "color": "#ecd5d0" } },
            "outline": { "dark": { "color": "#9c8a86" } },
            "primary": {
              "dark": { "color": "#ffb4a4" },
              "default": { "color": "#ffb4a4" },
              "light": { "color": "#904b3c" }
            },
            "secondary": { "dark": { "color": "#e8bdb4" } },
            "surface": { "dark": { "color": "#191110" } }
          },
          "mode": "dark"
        }
        """

    @Test
    fun `reads the dark scheme out of a nested matugen dump`() {
        val roles = requireNotNull(Matugen.parse(matugenDump, dark = true))

        assertEquals("#adc6ff", roles["primary"])
        assertEquals("#e3e2e6", roles["on_surface"])
        assertEquals("#111318", roles["surface"])
        assertNull(roles["color_accent_primary"])
    }

    @Test
    fun `reads the light scheme out of a nested matugen dump`() {
        val roles = requireNotNull(Matugen.parse(matugenDump, dark = false))

        assertEquals("#005ac1", roles["primary"])
        assertEquals("#f9f9ff", roles["background"])
    }

    @Test
    fun `reads the per-role dump matugen 4 writes`() {
        val dark = requireNotNull(Matugen.parse(perRoleDump, dark = true))
        val light = requireNotNull(Matugen.parse(perRoleDump, dark = false))

        assertEquals("#ffb4a4", dark["primary"])
        assertEquals("#191110", dark["surface"])
        assertNull(dark["base00"], "base16 is not a material role")
        assertNull(dark["color"])

        // Only the roles that carry a light colour come back; the rest fall back in schemeOf.
        assertEquals("#904b3c", light["primary"])
        assertEquals("#fff8f6", light["background"])
    }

    @Test
    fun `maps matugen roles onto a material scheme`() {
        val dark = Themes.schemeOf(requireNotNull(Matugen.parse(perRoleDump, true)), dark = true)
        val light = Themes.schemeOf(requireNotNull(Matugen.parse(perRoleDump, false)), dark = false)

        assertEquals(Themes.colorOf("#ffb4a4"), dark.primary)
        assertEquals(
            Themes.colorOf("#002e69"),
            Themes.schemeOf(requireNotNull(Matugen.parse(matugenDump, true)), true).onPrimary,
        )
        assertEquals(Themes.colorOf("#904b3c"), light.primary)
        assertTrue(light.surface.luminance() > 0.8f, "a light scheme needs a light surface")
        assertTrue(dark.surface.luminance() < 0.2f, "a dark scheme needs a dark surface")
    }

    @Test
    fun `reads a colour tuple from the appearance portal`() {
        val accent =
            Desktop.parseAccentTuple(
                "((0.27058823529411763, 0.4627450980392157, 0.7411764705882353),)"
            )

        assertEquals(Color(0.27058823529411763f, 0.4627450980392157f, 0.7411764705882353f), accent)
        assertNull(Desktop.parseAccentTuple("(<uint32 1>,)"))
    }

    @Test
    fun `reads the portal's dark preference`() {
        assertEquals(true, Desktop.parsePortalDark("(<uint32 1>,)"))
        assertEquals(false, Desktop.parsePortalDark("(<uint32 2>,)"))
        assertNull(Desktop.parsePortalDark("(<uint32 0>,)"))
    }

    @Test
    fun `reads accents from gsettings, kde and gtk`() {
        assertEquals(Themes.colorOf("#3584E4"), Desktop.parseAccentName("'blue'\n"))
        assertNull(Desktop.parseAccentName("'chartreuse'"))

        val kdeglobals =
            """
            [General]
            ColorScheme=BreezeDark
            AccentColor=#ff5500
            """
                .trimIndent()
        assertEquals(Themes.colorOf("#ff5500"), Desktop.parseKdeAccent(kdeglobals))
        assertEquals(true, Desktop.parseKdeDark(kdeglobals))

        val selection =
            """
            [Colors:Selection]
            BackgroundNormal=12,34,56
            """
                .trimIndent()
        assertEquals(Color(12, 34, 56), Desktop.parseKdeAccent(selection))

        assertEquals(
            Themes.colorOf("#9141ac"),
            Desktop.parseGtkAccent(
                "@define-color accent_bg_color #9141ac;\n@define-color accent_color #000000;"
            ),
        )
        assertEquals(
            Themes.colorOf("#2190a4"),
            Desktop.parseGtkAccent("@define-color accent_color #2190a4;"),
        )
    }

    @Test
    fun `reads a wallpaper from a quoted file uri`() {
        val image = Files.createTempFile("auxio-wall", ".png")

        assertEquals(image, Desktop.parseFileUri("'${image.toUri()}'"))
        assertNull(Desktop.parseFileUri("'none'"))
        assertNull(Desktop.parseFileUri("'/does/not/exist.png'"))
    }

    @Test
    fun `desktop mode builds the scheme around the accent`() {
        val scheme =
            Themes.scheme(ThemeSettings(mode = ThemeMode.DESKTOP), appearance, null, cache())
        val hue = scheme.primary.toHsl()[0]

        assertTrue(abs(hue - 215f) < 25f, "expected a blue primary, got hue $hue")
    }

    @Test
    fun `custom mode follows its seed colour`() {
        val red =
            Themes.scheme(
                ThemeSettings(mode = ThemeMode.CUSTOM, seed = "#e62a0d"),
                appearance,
                null,
                cache(),
            )
        val green =
            Themes.scheme(
                ThemeSettings(mode = ThemeMode.CUSTOM, seed = "#1f9e3f"),
                appearance,
                null,
                cache(),
            )

        val redHue = red.primary.toHsl()[0]
        val greenHue = green.primary.toHsl()[0]
        assertTrue(redHue < 40f || redHue > 340f, "red seed gave hue $redHue")
        assertTrue(greenHue in 90f..170f, "green seed gave hue $greenHue")
        assertNotEquals(red.primary, green.primary)
    }

    @Test
    fun `parses hex seeds in the form the field accepts`() {
        assertEquals(Color(0xFFE62A0D), parseColor("#e62a0d"))
        assertEquals(Color(0xFF00FF00), parseColor("00ff00"))
        // Only the full six digits are a colour; the three-digit shorthand was removed.
        assertNull(parseColor("#f00"))
        assertNull(parseColor("#12"))
        assertNull(parseColor("potato"))
    }

    @Test
    fun `brightness decides the scheme's lightness`() {
        val light =
            ThemeSettings(mode = ThemeMode.CUSTOM, seed = "#3366cc", brightness = Brightness.LIGHT)
        val dark = light.copy(brightness = Brightness.DARK)
        val system = light.copy(brightness = Brightness.SYSTEM)

        assertTrue(Themes.scheme(light, appearance, null, cache()).surface.luminance() > 0.8f)
        assertTrue(Themes.scheme(dark, appearance, null, cache()).surface.luminance() < 0.2f)

        // The desktop here reports a dark preference, so System follows it.
        assertTrue(Themes.scheme(system, appearance, null, cache()).surface.luminance() < 0.2f)
        assertTrue(
            Themes.scheme(system, appearance.copy(dark = false), null, cache())
                .surface
                .luminance() > 0.8f
        )
    }

    @Test
    fun `palette types change the built-in scheme`() {
        val seed = Color(0xFFD94426)
        val schemes =
            SchemeType.entries.associateWith { Themes.Approximation.scheme(seed, it, true) }
        val primaries = schemes.values.map { it.primary }

        assertEquals(
            SchemeType.entries.size,
            primaries.distinct().size,
            "every palette should look different",
        )

        val vibrant = schemes.getValue(SchemeType.VIBRANT).primary.toHsl()[1]
        val neutral = schemes.getValue(SchemeType.NEUTRAL).primary.toHsl()[1]
        assertTrue(vibrant > neutral * 2f, "vibrant $vibrant should out-chroma neutral $neutral")
        assertTrue(
            schemes.getValue(SchemeType.MONOCHROME).primary.toHsl()[1] < 0.02f,
            "monochrome should have no chroma left",
        )

        val spot = schemes.getValue(SchemeType.TONAL_SPOT).primary.toHsl()[0]
        val expressive = schemes.getValue(SchemeType.EXPRESSIVE).primary.toHsl()[0]
        assertTrue(abs(expressive - spot) > 10f, "expressive hue $expressive should shift")
    }

    @Test
    fun `the cache round trips and rejects another key`() {
        val cache = ThemeCache(Files.createTempDirectory("auxio-theme-cache"))
        val roles = mapOf("primary" to "#ffb4a4", "surface" to "#191110")

        cache.store("wall|1", roles)

        assertEquals(roles, cache.roles("wall|1"))
        assertNull(cache.roles("wall|2"), "another wallpaper should not reuse the palette")
        assertNull(cache.roles("nothing"), "an empty cache reads as null")
    }

    @Test
    fun `the cache survives a corrupt file`() {
        val directory = Files.createTempDirectory("auxio-theme-corrupt")
        Files.writeString(directory.resolve("theme-roles"), "garbage\nwithout-key\n")

        assertNull(ThemeCache(directory).roles("wall|1"))
    }

    @Test
    fun `the first frame matches the settled scheme for desktop and custom`() {
        val cache = cache()

        for (mode in listOf(ThemeMode.DESKTOP, ThemeMode.CUSTOM)) {
            for (brightness in listOf(Brightness.DARK, Brightness.LIGHT)) {
                val settings = ThemeSettings(mode = mode, brightness = brightness, seed = "#cc3366")
                val settled = Themes.scheme(settings, appearance, null, cache)
                val first = Themes.startingScheme(settings, appearance, null, cache)

                assertEquals(settled.primary, first.primary, "$mode/$brightness primary")
                assertEquals(settled.surface, first.surface, "$mode/$brightness surface")
                assertEquals(
                    settled.onSurfaceVariant,
                    first.onSurfaceVariant,
                    "$mode/$brightness text",
                )
            }
        }
    }

    @Test
    fun `a cached wallpaper scheme is used for the first frame`() {
        val cache = cache()
        val wallpaper = Files.createTempFile("auxio-wall", ".png")
        val settings = ThemeSettings(mode = ThemeMode.MATUGEN, wallpaper = wallpaper.toString())

        cache.store(
            Themes.cacheKey(settings.type, wallpaper, dark = true),
            mapOf("primary" to "#123456", "surface" to "#101010"),
        )

        val first = Themes.startingScheme(settings, Desktop.Appearance(null), null, cache)
        assertEquals(Themes.colorOf("#123456"), first.primary)

        // Another palette or the other brightness is a different entry, so it falls back.
        val vibrant =
            Themes.startingScheme(settings.copy(type = SchemeType.VIBRANT), appearance, null, cache)
        assertNotEquals(Themes.colorOf("#123456"), vibrant.primary)

        val light =
            Themes.startingScheme(
                settings.copy(brightness = Brightness.LIGHT),
                appearance,
                null,
                cache,
            )
        assertNotEquals(Themes.colorOf("#123456"), light.primary)
    }

    @Test
    fun `mitugen mode falls back to the desktop accent without matugen or a wallpaper`() {
        val scheme =
            Themes.scheme(
                ThemeSettings(mode = ThemeMode.MATUGEN, wallpaper = "/does/not/exist.png"),
                appearance,
                null,
                cache(),
            )

        assertTrue(scheme.primary.toHsl()[0] in 190f..240f, "fell back to a non-blue scheme")
    }

    @Test
    fun `writes a matugen config when the user has none`() {
        val directory = Files.createTempDirectory("auxio-matugen").resolve("matugen")

        Matugen.ensureConfig(directory)

        val file = directory.resolve("config.toml")
        assertTrue(Files.isRegularFile(file), "the config was not created")
        assertTrue(Files.readString(file).contains("[config]"))

        // An existing config is left alone, since the user may have edited it.
        Files.writeString(file, "# mine\n")
        Matugen.ensureConfig(directory)
        assertEquals("# mine\n", Files.readString(file))
    }

    @Test
    fun `a grey seed stays grey in the built-in generator`() {
        val grey = ThemeSettings(seed = "#808080").seedColor

        assertTrue(
            Themes.Approximation.scheme(grey, SchemeType.TONAL_SPOT, true).primary.toHsl()[1] <
                0.05f
        )
    }

    private fun cache() = ThemeCache(Files.createTempDirectory("auxio-theme-cache"))
}
