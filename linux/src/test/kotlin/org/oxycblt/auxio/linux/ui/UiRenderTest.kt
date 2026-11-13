/*
 * Copyright (c) 2026 Auxio Project
 * UiRenderTest.kt is part of Auxio.
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

/**
 * Renders the window off-screen and clicks through its screens, so layout can be reviewed without a
 * display server. Not a screenshot test: nothing is compared, the frames are for inspection.
 */
class UiRenderTest {
    @Test
    fun `renders every screen`() {
        assumeTrue(
            runCatching { Mpv.load() }.isSuccess,
            "libmpv is not installed, so the player cannot be constructed",
        )

        val fixture =
            Path.of(javaClass.getResource("/artwork.flac")?.toURI() ?: error("fixture missing"))
        val tracks =
            List(6) { index ->
                Track(
                    path = fixture,
                    title = "Track ${index + 1}",
                    artist = "Test Artist",
                    album = "Test Album",
                    trackNumber = index + 1,
                    year = 2024,
                    durationSeconds = 120.0 + index,
                )
            }

        val player = MpvPlayer(audioDriver = "null")
        val output = Files.createTempDirectory("auxio-ui")
        try {
            val scene =
                ImageComposeScene(width = 1100, height = 720) {
                    AuxioApp(
                        player = player,
                        library = Library.build(tracks),
                        roots = listOf(fixture.parent),
                        initialSeparators = ";/",
                    )
                }
            try {
                fun shot(name: String) {
                    // The first frame lays out, the second includes what effects produced.
                    scene.render()
                    val image = scene.render()
                    val encoded =
                        requireNotNull(image.encodeToData(EncodedImageFormat.PNG)) {
                            "could not encode $name"
                        }
                    Files.write(output.resolve("$name.png"), encoded.bytes)
                }

                fun click(x: Float, y: Float) {
                    scene.sendPointerEvent(PointerEventType.Press, Offset(x, y))
                    scene.sendPointerEvent(PointerEventType.Release, Offset(x, y))
                }

                shot("songs")

                click(553f, 122f)
                shot("albums")

                click(300f, 170f)
                shot("album-detail")

                click(40f, 115f)
                shot("back")

                click(1060f, 52f)
                shot("settings")
            } finally {
                scene.close()
            }

            val rendered = Files.list(output).use { it.toList() }
            assertTrue(
                rendered.size == 5,
                "expected five frames, got ${rendered.map { it.fileName }}",
            )
            println("RENDERED $output")
        } finally {
            player.close()
        }
    }
}
