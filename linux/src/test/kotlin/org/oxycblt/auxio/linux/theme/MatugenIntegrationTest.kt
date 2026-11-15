/*
 * Copyright (c) 2026 Auxio Project
 * MatugenIntegrationTest.kt is part of Auxio.
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
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

/**
 * Runs the real `matugen` binary, so the argument list and both JSON shapes are checked against
 * something other than a fixture. Skipped when matugen is not installed, which is the norm in CI.
 */
class MatugenIntegrationTest {
    @Test
    fun `generates a scheme from a colour`() {
        assumeTrue(Matugen.isAvailable(), "matugen is not installed")

        val roles = requireNotNull(Matugen.fromColor(Color(0xFFD94426), SchemeType.TONAL_SPOT.flag))

        assertTrue(roles.size >= 20, "expected a full palette, got ${roles.keys}")
        assertNotNull(Themes.colorOf(requireNotNull(roles["primary"])))
        assertNotNull(Themes.colorOf(requireNotNull(roles["on_primary"])))
        assertNotNull(Themes.colorOf(requireNotNull(roles["surface"])))
    }

    @Test
    fun `palette types reach matugen`() {
        assumeTrue(Matugen.isAvailable(), "matugen is not installed")

        val seed = Color(0xFFD94426)
        val vibrant = requireNotNull(Matugen.fromColor(seed, SchemeType.VIBRANT.flag))
        val neutral = requireNotNull(Matugen.fromColor(seed, SchemeType.NEUTRAL.flag))

        assertNotEquals(vibrant["primary"], neutral["primary"])
    }

    @Test
    fun `generates a scheme from an image`() {
        assumeTrue(Matugen.isAvailable(), "matugen is not installed")

        val wallpaper = gradient()
        val roles = requireNotNull(Matugen.fromImage(wallpaper, SchemeType.TONAL_SPOT.flag))
        assertTrue(roles.size >= 20)

        // The wallpaper mode hands the image to matugen rather than the desktop accent.
        val scheme =
            Themes.scheme(
                ThemeSettings(mode = ThemeMode.MATUGEN, wallpaper = wallpaper.toString()),
                Desktop.Appearance(accent = Color(0xFF6750A4)),
                wallpaper,
            )
        assertEquals(Themes.colorOf(requireNotNull(roles["primary"])), scheme.primary)
    }

    @Test
    fun `a missing wallpaper falls back instead of failing`() {
        assumeTrue(Matugen.isAvailable(), "matugen is not installed")

        val scheme =
            Themes.scheme(
                ThemeSettings(mode = ThemeMode.MATUGEN, wallpaper = "/does/not/exist.png"),
                Desktop.Appearance(accent = Color(0xFF3F6AAB)),
                null,
            )

        assertTrue(scheme.primary.toHsl()[0] in 190f..240f, "expected the accent's blue back")
    }

    /** A green-to-orange gradient, whose palette is nowhere near the default purple. */
    private fun gradient(): Path {
        val image = BufferedImage(240, 135, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                val red = 20 + x
                val green = 150 - y
                val blue = 90 + (x * y) / 400
                image.setRGB(x, y, (red shl 16) or (green shl 8) or blue)
            }
        }
        val file = Files.createTempFile("auxio-wallpaper", ".png")
        ImageIO.write(image, "png", file.toFile())
        return file
    }
}
