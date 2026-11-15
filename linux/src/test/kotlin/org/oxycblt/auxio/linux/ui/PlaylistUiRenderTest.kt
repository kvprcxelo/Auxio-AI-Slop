/*
 * Copyright (c) 2026 Auxio Project
 * PlaylistUiRenderTest.kt is part of Auxio.
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
import org.oxycblt.auxio.linux.music.Playlist
import org.oxycblt.auxio.linux.music.Track
import org.oxycblt.auxio.linux.playback.Mpv
import org.oxycblt.auxio.linux.playback.MpvPlayer

/** Renders the playlist corner button in each state, and the shuffle button it sits beside. */
class PlaylistUiRenderTest {
    private fun tracks(): List<Track> {
        val fixture =
            Path.of(javaClass.getResource("/artwork.flac")?.toURI() ?: error("fixture missing"))
        return List(4) { index ->
            Track(
                path = fixture,
                title = "Track ${index + 1}",
                artist = "Test Artist",
                album = "Test Album",
                trackNumber = index + 1,
                durationSeconds = 120.0,
            )
        }
    }

    private fun render(
        playlists: List<Playlist>,
        name: String,
        playlistTab: Boolean,
        fabY: Float?,
    ): Path {
        val songs = tracks()
        val player = MpvPlayer(audioDriver = "null")
        val output = Files.createTempDirectory("auxio-playlists").resolve("$name.png")
        try {
            val scene =
                ImageComposeScene(width = 1100, height = 720) {
                    AuxioApp(
                        player = player,
                        library = Library.build(songs, playlists = playlists),
                        roots = listOf(songs.first().path.parent),
                        initialSeparators = ";",
                    )
                }
            try {
                fun click(x: Float, y: Float) {
                    scene.sendPointerEvent(PointerEventType.Press, Offset(x, y))
                    scene.sendPointerEvent(PointerEventType.Release, Offset(x, y))
                    scene.render()
                }

                scene.render()
                if (playlistTab) click(990f, 122f)
                if (fabY != null) {
                    click(1052f, fabY)
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

        println("PLAYLISTS $output")
        return output
    }

    @Test
    fun `renders the empty playlists tab`() {
        assumeTrue(
            runCatching { Mpv.load() }.isSuccess,
            "libmpv is not installed, so the player cannot be constructed",
        )

        assertTrue(
            Files.size(render(emptyList(), "playlists-empty", playlistTab = true, fabY = null)) >
                500
        )
    }

    @Test
    fun `renders the playlist actions menu`() {
        assumeTrue(
            runCatching { Mpv.load() }.isSuccess,
            "libmpv is not installed, so the player cannot be constructed",
        )

        val songs = tracks()
        assertTrue(
            Files.size(
                render(
                    listOf(Playlist("Mix", songs)),
                    "playlists-menu",
                    playlistTab = true,
                    fabY = 566f,
                )
            ) > 500
        )
    }

    @Test
    fun `renders the corner shuffle`() {
        assumeTrue(
            runCatching { Mpv.load() }.isSuccess,
            "libmpv is not installed, so the player cannot be constructed",
        )

        assertTrue(
            Files.size(render(emptyList(), "shuffle", playlistTab = false, fabY = 566f)) > 500
        )
    }
}
