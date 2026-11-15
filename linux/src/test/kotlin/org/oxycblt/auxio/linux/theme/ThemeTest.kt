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
import java.nio.file.Files
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThemeTest {
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

    @Test
    fun `reads the dark scheme out of a matugen dump`() {
        val roles = requireNotNull(Matugen.parse(matugenDump))

        assertEquals("#adc6ff", roles["primary"])
        assertEquals("#e3e2e6", roles["on_surface"])
        // The amoled, light and android blocks must not leak into the roles.
        assertEquals("#111318", roles["surface"])
        assertNull(roles["color_accent_primary"])
    }

    @Test
    fun `reads the per-role dump matugen 4 writes`() {
        val dump =
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
                .trimIndent()

        val roles = requireNotNull(Matugen.parse(dump))

        // The dark variant is read, not the light one, and the base16 palette is not a role.
        assertEquals("#ffb4a4", roles["primary"])
        assertEquals("#191110", roles["surface"])
        assertEquals("#ffb4ab", roles["error"])
        assertNull(roles["base00"])
        assertNull(roles["color"])
    }

    @Test
    fun `maps matugen roles onto a material scheme`() {
        val scheme = Themes.schemeOf(requireNotNull(Matugen.parse(matugenDump)))

        assertEquals(Themes.colorOf("#adc6ff"), scheme.primary)
        assertEquals(Themes.colorOf("#002e69"), scheme.onPrimary)
        assertEquals(Themes.colorOf("#111318"), scheme.background)
    }

    @Test
    fun `reads a colour tuple from the appearance portal`() {
        val accent =
            Desktop.parseAccentTuple(
                "((0.27058823529411763, 0.4627450980392157, 0.7411764705882353),)"
            )

        assertEquals(Color(0.27058823529411763f, 0.4627450980392157f, 0.7411764705882353f), accent)
        // gdbus prints a uint32 for the colour-scheme key, which is not an accent.
        assertNull(Desktop.parseAccentTuple("(<uint32 1>,)"))
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
        val appearance = Desktop.Appearance(accent = Color(0xFF3F6AAB))
        val scheme = Themes.scheme(ThemeSettings(mode = ThemeMode.DESKTOP), appearance, null)
        val blue = scheme.primary.toHsl()

        // The desktop's hue survives; how saturated it ends up is the engine's business.
        assertTrue(abs(blue[0] - 215f) < 25f, "expected a blue primary, got hue ${blue[0]}")
    }

    @Test
    fun `custom mode follows its hue and chroma`() {
        val appearance = Desktop.Appearance(accent = Color(0xFF3F6AAB))
        val red =
            Themes.scheme(
                ThemeSettings(mode = ThemeMode.CUSTOM, hue = 5f, chroma = 80f),
                appearance,
                null,
            )
        val green =
            Themes.scheme(
                ThemeSettings(mode = ThemeMode.CUSTOM, hue = 130f, chroma = 80f),
                appearance,
                null,
            )

        // Every engine nudges the hue (matugen's HCT quantises it); the direction has to hold.
        val redHue = red.primary.toHsl()[0]
        val greenHue = green.primary.toHsl()[0]
        assertTrue(abs(redHue - 5f) < 30f, "red seed gave hue $redHue")
        assertTrue(abs(greenHue - 130f) < 30f, "green seed gave hue $greenHue")
        assertTrue(abs(redHue - greenHue) > 60f, "the two seeds should not meet in the middle")
        assertNotEquals(red.primary, green.primary)

        // matugen answers a chroma-less seed with its own default hue, so greyscale is a property
        // of the built-in generator rather than of the theme mode.
        val grey = ThemeSettings(mode = ThemeMode.CUSTOM, hue = 5f, chroma = 0f).seed
        assertTrue(
            Themes.Approximation.scheme(grey, SchemeType.TONAL_SPOT).primary.toHsl()[1] < 0.05f,
            "no chroma should mean no colour",
        )
    }

    @Test
    fun `palette types change the built-in scheme`() {
        val seed = Color(0xFFD94426)
        val schemes = SchemeType.entries.associateWith { Themes.Approximation.scheme(seed, it) }
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

        // A hue shift is what separates expressive from the spot palettes.
        val spot = schemes.getValue(SchemeType.TONAL_SPOT).primary.toHsl()[0]
        val expressive = schemes.getValue(SchemeType.EXPRESSIVE).primary.toHsl()[0]
        assertTrue(abs(expressive - spot) > 10f, "expressive hue $expressive should shift")
    }

    @Test
    fun `mitugen mode falls back to the desktop accent without matugen or a wallpaper`() {
        val appearance = Desktop.Appearance(accent = Color(0xFF3F6AAB))
        val scheme =
            Themes.scheme(
                ThemeSettings(mode = ThemeMode.MATUGEN, wallpaper = "/does/not/exist.png"),
                appearance,
                null,
            )

        assertTrue(scheme.primary.toHsl()[0] in 190f..240f, "fell back to a non-blue scheme")
    }
}
