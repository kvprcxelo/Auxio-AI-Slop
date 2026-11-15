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

        assertTrue(
            abs(red.primary.toHsl()[0] - 5f) < 20f,
            "red seed gave hue ${red.primary.toHsl()[0]}",
        )
        assertTrue(
            abs(green.primary.toHsl()[0] - 130f) < 20f,
            "green seed gave hue ${green.primary.toHsl()[0]}",
        )
        assertNotEquals(red.primary, green.primary)

        val grey =
            Themes.scheme(
                ThemeSettings(mode = ThemeMode.CUSTOM, hue = 5f, chroma = 0f),
                appearance,
                null,
            )
        assertTrue(grey.primary.toHsl()[1] < 0.1f, "no chroma should mean no colour")
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
