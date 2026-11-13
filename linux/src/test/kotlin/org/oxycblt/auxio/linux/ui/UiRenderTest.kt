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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.dp
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue
import org.jetbrains.skia.EncodedImageFormat
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.oxycblt.auxio.linux.music.Album
import org.oxycblt.auxio.linux.music.Artist
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

                click(60f, 141f)
                shot("back")

                click(921f, 122f)
                shot("artists")

                click(300f, 170f)
                shot("artist")

                click(1060f, 52f)
                shot("settings")
            } finally {
                scene.close()
            }

            val rendered = Files.list(output).use { it.toList() }
            assertTrue(
                rendered.size == 7,
                "expected seven frames, got ${rendered.map { it.fileName }}",
            )
            println("RENDERED $output")
        } finally {
            player.close()
        }
    }

    /** A taller window, because the settings page is longer than the normal one. */
    @Test
    fun `renders the whole settings page`() {
        assumeTrue(
            runCatching { Mpv.load() }.isSuccess,
            "libmpv is not installed, so the player cannot be constructed",
        )

        val fixture =
            Path.of(javaClass.getResource("/artwork.flac")?.toURI() ?: error("fixture missing"))
        val tracks =
            List(3) { index ->
                Track(
                    path = fixture,
                    title = "Track ${index + 1}",
                    artist = "Test Artist",
                    album = "Test Album",
                    trackNumber = index + 1,
                    year = 2024,
                    durationSeconds = 120.0,
                )
            }

        val player = MpvPlayer(audioDriver = "null")
        val output = Files.createTempDirectory("auxio-settings").resolve("settings-full.png")
        try {
            val scene =
                ImageComposeScene(width = 1100, height = 1100) {
                    AuxioApp(
                        player = player,
                        library = Library.build(tracks),
                        roots = listOf(fixture.parent),
                        initialSeparators = ";/",
                    )
                }
            try {
                scene.render()
                scene.sendPointerEvent(PointerEventType.Press, Offset(1060f, 52f))
                scene.sendPointerEvent(PointerEventType.Release, Offset(1060f, 52f))
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

        println("SETTINGS $output")
    }

    /** Distinct folder covers, so the collage grid is visible rather than one flat colour. */
    @Test
    fun `renders an artist collage`() {
        val root = Files.createTempDirectory("auxio-collage")
        val colors =
            listOf(
                java.awt.Color(220, 60, 60),
                java.awt.Color(60, 180, 90),
                java.awt.Color(60, 110, 220),
            )
        val albums = colors.mapIndexed { index, color ->
            val dir = Files.createDirectory(root.resolve("album$index"))
            val cover =
                java.awt.image.BufferedImage(64, 64, java.awt.image.BufferedImage.TYPE_INT_RGB)
            cover.createGraphics().apply {
                this.color = color
                fillRect(0, 0, 64, 64)
                dispose()
            }
            javax.imageio.ImageIO.write(cover, "png", dir.resolve("cover.png").toFile())
            val audio =
                Files.copy(
                    Path.of(javaClass.getResource("/tone.wav")!!.toURI()),
                    dir.resolve("track.wav"),
                )
            Album(
                name = "Album $index",
                artist = "Test Artist",
                year = 2024,
                date = null,
                tracks =
                    listOf(
                        Track(
                            path = audio,
                            title = "Track $index",
                            artist = "Test Artist",
                            album = "Album $index",
                        )
                    ),
            )
        }
        val artist = Artist("Test Artist", albums, albums.flatMap { it.tracks })
        val output = root.resolve("collage.png")

        val scene =
            ImageComposeScene(width = 560, height = 280) {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    Surface {
                        Row(modifier = Modifier.padding(20.dp)) {
                            ArtistCollage(artist = artist, seed = "auxio", size = 200.dp)
                            Spacer(Modifier.width(20.dp))
                            ArtistCollage(
                                artist = artist,
                                seed = "auxio",
                                size = 200.dp,
                                circle = true,
                            )
                        }
                    }
                }
            }
        try {
            scene.render()
            val image = scene.render()
            val encoded = requireNotNull(image.encodeToData(EncodedImageFormat.PNG))
            Files.write(output, encoded.bytes)
        } finally {
            scene.close()
        }

        println("COLLAGE $output")
    }

    @Test
    fun `renders the wave progress bar`() {
        val output = Files.createTempDirectory("auxio-wave").resolve("wave.png")

        val scene =
            ImageComposeScene(width = 640, height = 180) {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    Surface {
                        Column(modifier = Modifier.padding(24.dp)) {
                            WaveProgress(
                                progress = 0.62f,
                                enabled = true,
                                playing = true,
                                onSeek = {},
                                modifier = Modifier.fillMaxWidth(),
                            )
                            WaveProgress(
                                progress = 0.62f,
                                enabled = true,
                                playing = false,
                                onSeek = {},
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        try {
            scene.render()
            val image = scene.render()
            val encoded = requireNotNull(image.encodeToData(EncodedImageFormat.PNG))
            Files.write(output, encoded.bytes)
        } finally {
            scene.close()
        }

        assertTrue(Files.size(output) > 500, "the bar did not draw")
        println("WAVE $output")
    }
}
