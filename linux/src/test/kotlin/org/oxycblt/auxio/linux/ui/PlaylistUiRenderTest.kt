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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerEventType
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue
import org.jetbrains.skia.EncodedImageFormat
import org.oxycblt.auxio.linux.music.Library
import org.oxycblt.auxio.linux.music.Playlist
import org.oxycblt.auxio.linux.music.Track
import org.oxycblt.auxio.linux.playback.FakePlayer

/** Renders the playlist corner button in each state, and the shuffle button it sits beside. */
class PlaylistUiRenderTest {
    private fun tracks(): List<Track> {
        val fixture =
            Path.of(javaClass.getResource("/artwork.flac")?.toURI() ?: error("fixture missing"))
        val directory = Files.createTempDirectory("auxio-playlist-tracks")
        return List(4) { index ->
            Track(
                // Distinct paths, so each song has its own id in the picker.
                path = Files.copy(fixture, directory.resolve("track${index + 1}.flac")),
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
        emptyMenu: Boolean = false,
        dismissAt: Pair<Float, Float>? = null,
    ): Path {
        val songs = tracks()
        val player = FakePlayer()
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
                if (emptyMenu) {
                    click(550f, 435f)
                    scene.render()
                }

                scene.render()
                val image = scene.render()
                val encoded = requireNotNull(image.encodeToData(EncodedImageFormat.PNG))
                Files.write(output, encoded.bytes)

                if (dismissAt != null) {
                    click(dismissAt.first, dismissAt.second)
                    scene.render()
                    val dismissed = scene.render()
                    val dismissedPng =
                        requireNotNull(dismissed.encodeToData(EncodedImageFormat.PNG))
                    Files.write(output.resolveSibling("$name-dismissed.png"), dismissedPng.bytes)
                }
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
        assertTrue(
            Files.size(render(emptyList(), "playlists-empty", playlistTab = true, fabY = null)) >
                500
        )
    }

    @Test
    fun `renders the playlist actions menu`() {
        val songs = tracks()
        assertTrue(
            Files.size(
                render(
                    listOf(Playlist("Mix", songs, Path.of("Mix.m3u"))),
                    "playlists-menu",
                    playlistTab = true,
                    fabY = 566f,
                )
            ) > 500
        )
    }

    @OptIn(androidx.compose.ui.InternalComposeUiApi::class)
    @Test
    fun `renders an empty playlist and its add songs dialog`() {
        val songs = tracks()
        val root = Files.createTempDirectory("auxio-playlist-root")
        val playlistFile = Files.writeString(root.resolve("Empty.m3u"), "#EXTM3U\n")
        val playlist = Playlist("Empty", emptyList(), playlistFile)
        val player = FakePlayer()
        val output = Files.createTempDirectory("auxio-playlist-add")
        try {
            val scene =
                ImageComposeScene(width = 1100, height = 720) {
                    AuxioApp(
                        player = player,
                        library = Library.build(songs, playlists = listOf(playlist)),
                        roots = listOf(root),
                        initialSeparators = ";",
                    )
                }
            try {
                fun click(x: Float, y: Float) {
                    scene.sendPointerEvent(PointerEventType.Press, Offset(x, y))
                    scene.sendPointerEvent(PointerEventType.Release, Offset(x, y))
                    scene.render()
                }

                fun shot(name: String) {
                    scene.render()
                    val image = scene.render()
                    val encoded = requireNotNull(image.encodeToData(EncodedImageFormat.PNG))
                    Files.write(output.resolve("$name.png"), encoded.bytes)
                }

                scene.render()
                click(990f, 122f)
                click(300f, 170f)
                shot("playlist-empty")

                // The empty page's Add songs button, below the centred message.
                click(550f, 500f)
                // The dialog fades in; give it time to reach full opacity before the shot.
                repeat(10) {
                    Thread.sleep(50)
                    scene.render()
                }
                shot("playlist-add-dialog")

                // Escape closes the picker without changing anything.
                scene.sendKeyEvent(KeyEvent(key = Key.Escape, type = KeyEventType.KeyDown))
                repeat(6) { scene.render() }
                shot("playlist-escape")
                val escaped =
                    javax.imageio.ImageIO.read(output.resolve("playlist-escape.png").toFile())
                val pixel = java.awt.Color(escaped.getRGB(300, 300))
                assertTrue(
                    pixel.red < 40 && pixel.green < 40,
                    "the picker stayed open after Escape (${pixel.red}, ${pixel.green}, ${pixel.blue})",
                )

                // Open it again and pick the first song, so the shot shows a chosen state.
                click(550f, 500f)
                repeat(10) {
                    Thread.sleep(50)
                    scene.render()
                }
                click(400f, 270f)
                // The checkbox ticks over a short animation, so let it finish.
                repeat(6) {
                    Thread.sleep(50)
                    scene.render()
                }
                shot("playlist-add-selected")

                // Add the song; this writes the playlist and reloads it.
                click(775f, 500f)
                repeat(20) {
                    Thread.sleep(50)
                    scene.render()
                }
                shot("playlist-added")

                assertTrue(
                    Files.readString(playlistFile)
                        .contains(songs.first().path.toAbsolutePath().toString()),
                    "the song was not written to the playlist",
                )

                // Remove it with the row's minus button, then put it back from the notice.
                click(1060f, 315f)
                repeat(20) {
                    Thread.sleep(50)
                    scene.render()
                }
                shot("playlist-removed")
                assertTrue(
                    !Files.readString(playlistFile)
                        .contains(songs.first().path.toAbsolutePath().toString()),
                    "the song was not removed from the playlist",
                )

                click(1055f, 104f)
                repeat(20) {
                    Thread.sleep(50)
                    scene.render()
                }
                shot("playlist-restored")
                assertTrue(
                    Files.readString(playlistFile)
                        .contains(songs.first().path.toAbsolutePath().toString()),
                    "undo did not restore the song",
                )
            } finally {
                scene.close()
            }
        } finally {
            player.close()
        }

        println("PLAYLIST_ADD $output")
    }

    @Test
    fun `clicking the status message row closes the playlist menu`() {
        val songs = tracks()
        // Keep the playlists in a scanned root, so they survive the reload after removal.
        val directory = songs.first().path.parent
        val mixFile = directory.resolve("Mix.m3u")
        val otherFile = directory.resolve("Other.m3u")
        Files.writeString(mixFile, songs[0].path.toAbsolutePath().toString() + "\n")
        Files.writeString(otherFile, songs[1].path.toAbsolutePath().toString() + "\n")
        val playlists =
            listOf(
                Playlist("Mix", listOf(songs[0]), mixFile),
                Playlist("Other", listOf(songs[1]), otherFile),
            )
        val player = FakePlayer()
        val output = Files.createTempDirectory("auxio-playlist-message").resolve("menu-message.png")
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

                fun settle(frames: Int = 6) {
                    repeat(frames) {
                        Thread.sleep(50)
                        scene.render()
                    }
                }

                fun shot(name: String) {
                    scene.render()
                    val image = scene.render()
                    val encoded = requireNotNull(image.encodeToData(EncodedImageFormat.PNG))
                    Files.write(output.resolveSibling("$name.png"), encoded.bytes)
                }

                scene.render()
                click(990f, 122f)
                click(300f, 170f)
                settle()

                // Removing Mix's only song leaves a status row with Undo above the content.
                click(1060f, 315f)
                settle(20)
                shot("menu-message-removed")

                // Back to the list; the status row and its notice stay put.
                click(51f, 190f)
                settle()
                shot("menu-message-back")

                // Open the corner playlist menu over the list.
                click(1052f, 566f)
                settle()
                shot("menu-message-open")
                val open =
                    javax.imageio.ImageIO.read(
                        output.resolveSibling("menu-message-open.png").toFile()
                    )
                val openPixel = java.awt.Color(open.getRGB(970, 470))
                assertTrue(openPixel.red > 50, "the playlist menu did not open")

                // Clicking the status row dismisses it.
                click(300f, 121f)
                settle()
                shot("menu-message-dismissed")
                val dismissed =
                    javax.imageio.ImageIO.read(
                        output.resolveSibling("menu-message-dismissed.png").toFile()
                    )
                val pixel = java.awt.Color(dismissed.getRGB(970, 470))
                assertTrue(
                    pixel.red < 50 && pixel.green < 50,
                    "the menu stayed open after a status row click " +
                        "(${pixel.red}, ${pixel.green})",
                )
            } finally {
                scene.close()
            }
        } finally {
            player.close()
        }

        println("PLAYLIST_MESSAGE $output")
    }

    @Test
    fun `renders the add songs picker with a choice`() {
        val songs = tracks()
        val scheme = androidx.compose.material3.darkColorScheme()
        val output = Files.createTempDirectory("auxio-add-picker").resolve("picker-selected.png")
        val scene =
            ImageComposeScene(width = 1100, height = 720) {
                androidx.compose.material3.MaterialTheme(colorScheme = scheme) {
                    androidx.compose.material3.Surface(
                        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                        color = scheme.background,
                    ) {
                        AddSongsDialog(
                            songs = songs,
                            selected = setOf(songs.first().id),
                            scheme = scheme,
                            onToggle = { _, _ -> },
                            onAdd = {},
                            onDismiss = {},
                        )
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

        println("ADD_PICKER $output")
    }

    @Test
    fun `clicking outside the playlist menu closes it`() {
        val songs = tracks()
        val path =
            render(
                listOf(Playlist("Mix", songs, Path.of("Mix.m3u"))),
                "menu-dismiss",
                playlistTab = true,
                fabY = 566f,
                dismissAt = 500f to 300f,
            )
        val image =
            javax.imageio.ImageIO.read(path.resolveSibling("menu-dismiss-dismissed.png").toFile())
        val pixel = java.awt.Color(image.getRGB(970, 470))
        assertTrue(
            pixel.red < 50 && pixel.green < 50,
            "the menu stayed open (${pixel.red}, ${pixel.green}, ${pixel.blue})",
        )
    }

    @Test
    fun `clicking outside the empty playlists menu closes it`() {
        val path =
            render(
                emptyList(),
                "empty-menu-dismiss",
                playlistTab = true,
                fabY = null,
                emptyMenu = true,
                dismissAt = 300f to 300f,
            )
        val opened = javax.imageio.ImageIO.read(path.toFile())
        val dismissed =
            javax.imageio.ImageIO.read(
                path.resolveSibling("empty-menu-dismiss-dismissed.png").toFile()
            )
        val openPixel = java.awt.Color(opened.getRGB(550, 480))
        val closedPixel = java.awt.Color(dismissed.getRGB(550, 480))
        assertTrue(openPixel.red > 50, "the empty playlists menu did not open")
        assertTrue(
            closedPixel.red < 50,
            "the empty playlists menu stayed open (${closedPixel.red})",
        )
    }

    @Test
    fun `clicking the top bar closes the playlist menu`() {
        val songs = tracks()
        val path =
            render(
                listOf(Playlist("Mix", songs, Path.of("Mix.m3u"))),
                "menu-top-bar",
                playlistTab = true,
                fabY = 566f,
                dismissAt = 550f to 20f,
            )
        val image =
            javax.imageio.ImageIO.read(path.resolveSibling("menu-top-bar-dismissed.png").toFile())
        val pixel = java.awt.Color(image.getRGB(970, 470))
        assertTrue(
            pixel.red < 50 && pixel.green < 50,
            "the menu stayed open after a top bar click (${pixel.red}, ${pixel.green})",
        )
    }

    @Test
    fun `clicking the player bar closes the playlist menu`() {
        val songs = tracks()
        val path =
            render(
                listOf(Playlist("Mix", songs, Path.of("Mix.m3u"))),
                "menu-player-bar",
                playlistTab = true,
                fabY = 566f,
                dismissAt = 550f to 650f,
            )
        val image =
            javax.imageio.ImageIO.read(
                path.resolveSibling("menu-player-bar-dismissed.png").toFile()
            )
        val pixel = java.awt.Color(image.getRGB(970, 470))
        assertTrue(
            pixel.red < 50 && pixel.green < 50,
            "the menu stayed open after a player bar click (${pixel.red}, ${pixel.green})",
        )
    }

    @Test
    fun `renders the corner shuffle`() {
        assertTrue(
            Files.size(render(emptyList(), "shuffle", playlistTab = false, fabY = 566f)) > 500
        )
    }
}
