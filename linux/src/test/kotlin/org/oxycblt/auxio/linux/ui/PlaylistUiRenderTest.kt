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
import androidx.compose.ui.input.pointer.PointerButtons
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

    @OptIn(androidx.compose.ui.InternalComposeUiApi::class)
    private fun render(
        playlists: List<Playlist>,
        name: String,
        playlistTab: Boolean,
        fabY: Float?,
        emptyMenu: Boolean = false,
        dismissAt: Pair<Float, Float>? = null,
        escape: Boolean = false,
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

                if (escape) {
                    scene.sendKeyEvent(KeyEvent(key = Key.Escape, type = KeyEventType.KeyDown))
                    scene.sendKeyEvent(KeyEvent(key = Key.Escape, type = KeyEventType.KeyUp))
                    repeat(4) { scene.render() }
                    val escaped = scene.render()
                    val escapedPng = requireNotNull(escaped.encodeToData(EncodedImageFormat.PNG))
                    Files.write(output.resolveSibling("$name-escaped.png"), escapedPng.bytes)
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
    fun `playlist rows collage the first four songs`() {
        val songs = tracks()
        val tone = Path.of(javaClass.getResource("/tone.wav")?.toURI() ?: error("tone missing"))
        val directory = Files.createTempDirectory("auxio-playlist-collage")
        val bare =
            List(3) { index ->
                Track(Files.copy(tone, directory.resolve("bare$index.wav")), "Bare $index")
            }
        val playlist =
            Playlist(
                "Collage",
                listOf(songs.first(), bare[0], bare[1], bare[2]),
                Path.of("Collage.m3u"),
            )

        val path = render(listOf(playlist), "playlist-collage", playlistTab = true, fabY = null)
        val image = javax.imageio.ImageIO.read(path.toFile())
        val withArt = java.awt.Color(image.getRGB(25, 160))
        val withoutArt = java.awt.Color(image.getRGB(45, 160))
        assertTrue(
            withArt.red > 150,
            "the first song's cover was not in the collage (${withArt.red})",
        )
        assertTrue(
            withoutArt.red < 100,
            "the collage filled a slot that has no cover (${withoutArt.red})",
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
                click(400f, 306f)
                // The checkbox ticks over a short animation, so let it finish.
                repeat(6) {
                    Thread.sleep(50)
                    scene.render()
                }
                shot("playlist-add-selected")

                // Add the song; this writes the playlist and reloads it.
                click(775f, 533f)
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
                click(1012f, 315f)
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

                click(662f, 570f)
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
    fun `escape closes the playlist menu`() {
        val songs = tracks()
        val path =
            render(
                listOf(Playlist("Mix", songs, Path.of("Mix.m3u"))),
                "menu-escape",
                playlistTab = true,
                fabY = 566f,
                escape = true,
            )
        val opened = javax.imageio.ImageIO.read(path.toFile())
        val openedPixel = java.awt.Color(opened.getRGB(970, 470))
        assertTrue(openedPixel.red > 50, "the playlist menu did not open")
        val escaped =
            javax.imageio.ImageIO.read(path.resolveSibling("menu-escape-escaped.png").toFile())
        val pixel = java.awt.Color(escaped.getRGB(970, 470))
        assertTrue(
            pixel.red < 50 && pixel.green < 50,
            "the menu stayed open after Escape (${pixel.red}, ${pixel.green})",
        )
    }

    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    @Test
    fun `drags a sorted playlist page and saves that order`() {
        val songs = tracks()
        val directory = songs.first().path.parent
        val file = directory.resolve("Sorted.m3u")
        val zulu = songs[0].copy(title = "Zulu", year = 2020)
        val alpha = songs[1].copy(title = "Alpha", year = 2010)
        val mike = songs[2].copy(title = "Mike", year = 2015)
        fun entries(tracks: List<Track>) =
            tracks.joinToString("\n", postfix = "\n") { it.path.toAbsolutePath().toString() }

        val stored = entries(listOf(zulu, alpha, mike))
        Files.writeString(file, stored)
        val playlist = Playlist("Sorted", listOf(zulu, alpha, mike), file)
        val player = FakePlayer()
        val output = Files.createTempDirectory("auxio-playlist-sort").resolve("sort.png")
        try {
            val scene =
                ImageComposeScene(width = 1100, height = 720) {
                    AuxioApp(
                        player = player,
                        library = Library.build(songs, playlists = listOf(playlist)),
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
                        scene.render(System.nanoTime())
                    }
                }

                fun shot(name: String) {
                    val image = scene.render(System.nanoTime())
                    val encoded = requireNotNull(image.encodeToData(EncodedImageFormat.PNG))
                    Files.write(output.resolveSibling("$name.png"), encoded.bytes)
                }

                scene.render()
                click(990f, 122f)
                click(300f, 170f)
                settle()

                // Sort only the page: manual opens a menu that starts with Playlist order.
                click(1011f, 48f)
                settle()
                shot("playlist-sort-menu")
                click(950f, 160f)
                settle()
                shot("playlist-sorted")
                assertTrue(
                    Files.readString(file) == stored,
                    "sorting changed the playlist file",
                )

                // Grab the whole first card (Alpha under Name A-Z) and drop it one slot down.
                scene.sendPointerEvent(PointerEventType.Press, Offset(500f, 315f))
                repeat(8) { step ->
                    scene.sendPointerEvent(
                        PointerEventType.Move,
                        Offset(500f, 315f + 10f * (step + 1)),
                        buttons = PointerButtons(isPrimaryPressed = true),
                    )
                    scene.render()
                }
                // Let the other cards finish sliding into the gap before the shot.
                repeat(4) {
                    Thread.sleep(50)
                    scene.render(System.nanoTime())
                }
                shot("playlist-sorted-dragging")
                scene.sendPointerEvent(PointerEventType.Release, Offset(500f, 395f))
                settle(20)
                scene.sendPointerEvent(PointerEventType.Move, Offset(10f, 650f))
                settle(2)
                shot("playlist-sorted-dropped")

                val expected = entries(listOf(mike, alpha, zulu))
                assertTrue(
                    Files.readString(file) == expected,
                    "the order dropped while sorted was not written to the playlist " +
                        "(${Files.readAllLines(file)})",
                )
            } finally {
                scene.close()
            }
        } finally {
            player.close()
        }

        println("PLAYLIST_SORT $output")
    }

    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    @Test
    fun `drops use the visible order after changing the sort`() {
        val songs = tracks()
        val directory = songs.first().path.parent
        val file = directory.resolve("Kept.m3u")
        val alpha = songs[0].copy(title = "Alpha")
        val zulu = songs[1].copy(title = "Zulu")
        val mike = songs[2].copy(title = "Mike")
        val stored = listOf(alpha, zulu, mike)
        Files.writeString(
            file,
            stored.joinToString("\n", postfix = "\n") {
                it.path.toAbsolutePath().toString()
            },
        )
        val playlist = Playlist("Kept", stored, file)
        val player = FakePlayer()
        val output = Files.createTempDirectory("auxio-playlist-sort-change")
        try {
            val scene =
                ImageComposeScene(width = 1100, height = 720) {
                    AuxioApp(
                        player = player,
                        library = Library.build(songs, playlists = listOf(playlist)),
                        roots = listOf(directory),
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

                scene.render()
                click(990f, 122f)
                click(300f, 170f)
                settle()

                // Name A-Z puts Alpha at index 0, where it already was. The row therefore keeps
                // its pointer handler while the list around it changes.
                click(1011f, 48f)
                settle()
                click(950f, 160f)
                settle()

                // Drag that same row down one; the visible order is Alpha, Mike, Zulu.
                scene.sendPointerEvent(PointerEventType.Press, Offset(500f, 315f))
                repeat(8) { step ->
                    scene.sendPointerEvent(
                        PointerEventType.Move,
                        Offset(500f, 315f + 10f * (step + 1)),
                        buttons = PointerButtons(isPrimaryPressed = true),
                    )
                    scene.render()
                }
                scene.sendPointerEvent(PointerEventType.Release, Offset(500f, 395f))
                settle(20)

                val entries = Files.readAllLines(file).map { it.trim() }.filter { it.isNotEmpty() }
                val expected = listOf(mike, alpha, zulu).map { it.path.toAbsolutePath().toString() }
                assertTrue(
                    entries == expected,
                    "the drop used a stale order instead of the visible one ($entries)",
                )
            } finally {
                scene.close()
            }
        } finally {
            player.close()
        }

        println("PLAYLIST_SORT_CHANGE $output")
    }

    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    @Test
    fun `drops matching the stored order leave the sorted view`() {
        val songs = tracks()
        val directory = songs.first().path.parent
        val bare = Files.createTempDirectory("auxio-playlist-bare")
        val tone = Path.of(javaClass.getResource("/tone.wav")?.toURI() ?: error("tone missing"))
        val mike = Track(Files.copy(tone, bare.resolve("mike.wav")), "Mike")
        val zulu = Track(Files.copy(tone, bare.resolve("zulu.wav")), "Zulu")
        // Alpha carries artwork; Mike and Zulu do not, so the first visible row identifies the
        // order without needing to read text out of the rendered page.
        val alpha = songs[0].copy(title = "Alpha")
        val stored = listOf(mike, alpha, zulu)
        val file = directory.resolve("SortedBack.m3u")
        Files.writeString(
            file,
            stored.joinToString("\n", postfix = "\n") {
                it.path.toAbsolutePath().toString()
            },
        )
        val playlist = Playlist("SortedBack", stored, file)
        val player = FakePlayer()
        val output = Files.createTempDirectory("auxio-playlist-sorted-back")
        try {
            val scene =
                ImageComposeScene(width = 1100, height = 720) {
                    AuxioApp(
                        player = player,
                        library = Library.build(songs, playlists = listOf(playlist)),
                        roots = listOf(directory),
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
                        scene.render(System.nanoTime())
                    }
                }

                scene.render()
                click(990f, 122f)
                click(300f, 170f)
                settle()

                // Name A-Z is Alpha, Mike, Zulu. Drag Alpha down one, which lands on the stored
                // Mike, Alpha, Zulu order.
                click(1011f, 48f)
                settle()
                click(950f, 160f)
                settle()
                scene.sendPointerEvent(PointerEventType.Press, Offset(500f, 315f))
                repeat(8) { step ->
                    scene.sendPointerEvent(
                        PointerEventType.Move,
                        Offset(500f, 315f + 10f * (step + 1)),
                        buttons = PointerButtons(isPrimaryPressed = true),
                    )
                    scene.render()
                }
                scene.sendPointerEvent(PointerEventType.Release, Offset(500f, 395f))
                settle(20)

                val entries = Files.readAllLines(file).map { it.trim() }.filter { it.isNotEmpty() }
                assertTrue(
                    entries == stored.map { it.path.toAbsolutePath().toString() },
                    "the stored order was unexpectedly rewritten ($entries)",
                )

                val image = scene.render(System.nanoTime())
                val rendered = output.resolve("after.png")
                Files.write(
                    rendered,
                    requireNotNull(image.encodeToData(EncodedImageFormat.PNG)).bytes,
                )
                val shot = javax.imageio.ImageIO.read(rendered.toFile())
                val firstRow = java.awt.Color(shot.getRGB(30, 298))
                val secondRow = java.awt.Color(shot.getRGB(30, 370))
                assertTrue(
                    firstRow.red < 100,
                    "the page stayed sorted instead of showing Mike first (${firstRow.red})",
                )
                assertTrue(
                    secondRow.red > 150,
                    "Alpha's cover was not in the second row (${secondRow.red})",
                )
            } finally {
                scene.close()
            }
        } finally {
            player.close()
        }

        println("PLAYLIST_SORTED_BACK $output")
    }

    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    @Test
    fun `drags a playlist song into a new order and saves it`() {
        val songs = tracks()
        val directory = songs.first().path.parent
        val file = directory.resolve("Mix.m3u")
        Files.writeString(
            file,
            songs[0].path.toAbsolutePath().toString() +
                "\n" +
                songs[1].path.toAbsolutePath().toString() +
                "\n",
        )
        val playlist = Playlist("Mix", songs.take(2), file)
        val player = FakePlayer()
        val output = Files.createTempDirectory("auxio-playlist-drag").resolve("drag.png")
        try {
            val scene =
                ImageComposeScene(width = 1100, height = 720) {
                    AuxioApp(
                        player = player,
                        library = Library.build(songs, playlists = listOf(playlist)),
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
                click(990f, 122f)
                click(300f, 170f)
                repeat(6) {
                    Thread.sleep(50)
                    scene.render()
                }

                // Clicking a card still plays it, even though the whole row is now draggable.
                click(500f, 315f)
                repeat(6) { scene.render() }
                assertTrue(!player.isPaused(), "clicking a playlist card did not play it")

                // Grab the first card anywhere and drag it past the second row.
                scene.sendPointerEvent(PointerEventType.Press, Offset(500f, 315f))
                repeat(8) { step ->
                    scene.sendPointerEvent(
                        PointerEventType.Move,
                        Offset(500f, 315f + 10f * (step + 1)),
                        buttons = PointerButtons(isPrimaryPressed = true),
                    )
                    scene.render()
                }
                scene.sendPointerEvent(PointerEventType.Release, Offset(500f, 395f))
                repeat(20) {
                    Thread.sleep(50)
                    scene.render()
                }
                scene.sendPointerEvent(PointerEventType.Move, Offset(10f, 650f))
                repeat(2) { scene.render() }

                val image = scene.render()
                val encoded = requireNotNull(image.encodeToData(EncodedImageFormat.PNG))
                Files.write(output, encoded.bytes)

                val entries = Files.readAllLines(file).map { it.trim() }.filter { it.isNotEmpty() }
                assertTrue(
                    entries ==
                        listOf(
                            songs[1].path.toAbsolutePath().toString(),
                            songs[0].path.toAbsolutePath().toString(),
                        ),
                    "the new order was not saved to the playlist ($entries)",
                )
            } finally {
                scene.close()
            }
        } finally {
            player.close()
        }

        println("PLAYLIST_DRAG $output")
    }

    @Test
    fun `opens the sort menu`() {
        val songs = tracks()
        val player = FakePlayer()
        val output = Files.createTempDirectory("auxio-sort").resolve("sort-menu.png")
        try {
            val scene =
                ImageComposeScene(width = 1100, height = 720) {
                    AuxioApp(
                        player = player,
                        library = Library.build(songs),
                        roots = listOf(songs.first().path.parent),
                        initialSeparators = ";",
                    )
                }
            try {
                scene.render()
                scene.sendPointerEvent(PointerEventType.Press, Offset(1011f, 48f))
                scene.sendPointerEvent(PointerEventType.Release, Offset(1011f, 48f))
                repeat(4) { scene.render() }
                val image = scene.render()
                val encoded = requireNotNull(image.encodeToData(EncodedImageFormat.PNG))
                Files.write(output, encoded.bytes)

                // Choose Name (Z–A), then close the menu; the songs should reverse.
                scene.sendPointerEvent(PointerEventType.Press, Offset(950f, 162f))
                scene.sendPointerEvent(PointerEventType.Release, Offset(950f, 162f))
                repeat(4) { scene.render() }
                val selected = scene.render()
                val selectedPng = requireNotNull(selected.encodeToData(EncodedImageFormat.PNG))
                Files.write(output.resolveSibling("sort-applied.png"), selectedPng.bytes)
            } finally {
                scene.close()
            }
        } finally {
            player.close()
        }

        println("SORT_MENU $output")
    }

    @Test
    fun `renders the corner shuffle`() {
        assertTrue(
            Files.size(render(emptyList(), "shuffle", playlistTab = false, fabY = 566f)) > 500
        )
    }
}
