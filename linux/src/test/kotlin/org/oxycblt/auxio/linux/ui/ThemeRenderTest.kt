/*
 * Copyright (c) 2026 Auxio Project
 * ThemeRenderTest.kt is part of Auxio.
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
 
package org.oxycblt.auxio.linux.ui

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue
import org.jetbrains.skia.EncodedImageFormat
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.oxycblt.auxio.linux.music.Library
import org.oxycblt.auxio.linux.music.Track
import org.oxycblt.auxio.linux.playback.Mpv
import org.oxycblt.auxio.linux.playback.MpvPlayer
import org.oxycblt.auxio.linux.theme.SchemeType
import org.oxycblt.auxio.linux.theme.ThemeMode
import org.oxycblt.auxio.linux.theme.ThemeSettings

/** Renders the window under a custom scheme and the appearance settings that chose it. */
class ThemeRenderTest {
    private fun tracks(): List<Track> {
        val fixture =
            Path.of(javaClass.getResource("/artwork.flac")?.toURI() ?: error("fixture missing"))
        return List(5) { index ->
            Track(
                path = fixture,
                title = "Track ${index + 1}",
                artist = "Test Artist",
                album = "Test Album",
                genre = "Rock",
                trackNumber = index + 1,
                durationSeconds = 120.0,
            )
        }
    }

    /** A green-to-magenta gradient, so a wallpaper scheme cannot be mistaken for the default. */
    private fun gradient(): Path {
        val image =
            java.awt.image.BufferedImage(240, 135, java.awt.image.BufferedImage.TYPE_INT_RGB)
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                image.setRGB(x, y, ((20 + x) shl 16) or ((150 - y) shl 8) or (90 + (x * y) / 400))
            }
        }
        val file = Files.createTempFile("auxio-wallpaper", ".png")
        javax.imageio.ImageIO.write(image, "png", file.toFile())
        return file
    }

    private fun render(theme: ThemeSettings, name: String, height: Int, settings: Boolean): Path {
        val songs = tracks()
        val player = MpvPlayer(audioDriver = "null")
        val output = Files.createTempDirectory("auxio-theme").resolve("$name.png")
        try {
            val scene =
                ImageComposeScene(width = 1100, height = height) {
                    AuxioApp(
                        player = player,
                        library = Library.build(songs),
                        roots = listOf(songs.first().path.parent),
                        initialSeparators = ";",
                        initialTheme = theme,
                    )
                }
            try {
                scene.render()
                // The scheme is built off the composition after a short debounce, so drive a few
                // frames to let it land before taking the shot.
                repeat(6) {
                    Thread.sleep(60)
                    scene.render()
                }
                if (settings) {
                    scene.sendPointerEvent(PointerEventType.Press, Offset(1060f, 52f))
                    scene.sendPointerEvent(PointerEventType.Release, Offset(1060f, 52f))
                    scene.render()
                }

                scene.render()
                val image = scene.render()
                val encoded = requireNotNull(image.encodeToData(EncodedImageFormat.PNG))
                Files.write(output, encoded.bytes)
            } finally {
                scene.close()
            }
        } finally {
            player.close()
        }

        println("THEME $output")
        return output
    }

    @Test
    fun `renders a custom scheme`() {
        assumeTrue(
            runCatching { Mpv.load() }.isSuccess,
            "libmpv is not installed, so the player cannot be constructed",
        )

        val theme = ThemeSettings(mode = ThemeMode.CUSTOM, hue = 10f, chroma = 70f)
        assertTrue(Files.size(render(theme, "custom", height = 720, settings = false)) > 500)
    }

    @Test
    fun `renders every theme mode`() {
        assumeTrue(
            runCatching { Mpv.load() }.isSuccess,
            "libmpv is not installed, so the player cannot be constructed",
        )

        val wallpaper = gradient()
        val modes =
            listOf(
                ThemeSettings(mode = ThemeMode.DESKTOP) to "mode-desktop",
                ThemeSettings(mode = ThemeMode.MATUGEN, wallpaper = wallpaper.toString()) to
                    "mode-matugen",
                ThemeSettings(
                    mode = ThemeMode.CUSTOM,
                    hue = 190f,
                    chroma = 90f,
                    type = SchemeType.VIBRANT,
                ) to "mode-custom",
            )

        modes.forEach { (theme, name) ->
            assertTrue(Files.size(render(theme, name, height = 720, settings = false)) > 500)
        }
    }

    @Test
    fun `renders the appearance settings`() {
        assumeTrue(
            runCatching { Mpv.load() }.isSuccess,
            "libmpv is not installed, so the player cannot be constructed",
        )

        val theme = ThemeSettings(mode = ThemeMode.CUSTOM, hue = 10f, chroma = 70f)
        assertTrue(Files.size(render(theme, "appearance", height = 1400, settings = true)) > 500)
    }
}
