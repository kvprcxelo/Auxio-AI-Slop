/*
 * Copyright (c) 2026 Auxio Project
 * PlayQueueTest.kt is part of Auxio.
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
 
package org.oxycblt.auxio.linux.playback

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.oxycblt.auxio.linux.music.Track

class PlayQueueTest {
    private fun tone(): Path =
        Path.of(javaClass.getResource("/tone.wav")?.toURI() ?: error("tone.wav missing"))

    private fun mpvAvailable(): Boolean = runCatching { Mpv.load() }.isSuccess

    /** The same tone under several names, so the queue has distinct entries. */
    private fun tracks(dir: Path, count: Int = 2): List<Track> =
        (0 until count).map { index ->
            Track(tone().copyTo(dir.resolve("track$index.wav"), overwrite = true), "Track $index")
        }

    @Test
    fun `starts at the requested track and steps through the queue`() {
        assumeTrue(mpvAvailable(), "libmpv is not installed")

        val dir = Files.createTempDirectory("auxio-queue")
        val player = MpvPlayer(audioDriver = "null")
        try {
            val queue = PlayQueue(player)
            val songs = tracks(dir)

            queue.setTracks(songs, startAt = 1)
            assertEquals("Track 1", queue.current?.title)

            assertTrue(queue.previous())
            assertEquals("Track 0", queue.current?.title)
            assertFalse(queue.previous(), "the queue cannot go before the first track")

            assertTrue(queue.next())
            assertEquals("Track 1", queue.current?.title)
            assertFalse(queue.next(), "the queue cannot go past the last track")
        } finally {
            player.close()
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `moves to the next track when playback ends`() {
        assumeTrue(mpvAvailable(), "libmpv is not installed")

        val dir = Files.createTempDirectory("auxio-advance")
        val player = MpvPlayer(audioDriver = "null")
        try {
            val queue = PlayQueue(player)
            queue.setTracks(tracks(dir))

            val deadline = System.nanoTime() + 15_000_000_000L
            var advanced = false
            while (System.nanoTime() < deadline) {
                if (queue.advanceIfFinished()) {
                    advanced = true
                    break
                }
                Thread.sleep(100)
            }

            assertTrue(advanced, "playback ending did not advance the queue")
            assertEquals("Track 1", queue.current?.title)
        } finally {
            player.close()
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `shuffle visits every track exactly once`() {
        assumeTrue(mpvAvailable(), "libmpv is not installed")

        val dir = Files.createTempDirectory("auxio-shuffle")
        val player = MpvPlayer(audioDriver = "null")
        try {
            val queue = PlayQueue(player)
            val songs = tracks(dir, count = 6)
            queue.setShuffle(true)
            queue.setTracks(songs)

            val played = mutableListOf(queue.current?.title)
            while (queue.next()) {
                played += queue.current?.title
            }

            assertEquals(songs.map { it.title }.toSet(), played.toSet())
            assertEquals(played.size, played.toSet().size, "shuffle repeated a track: $played")
        } finally {
            player.close()
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `turning shuffle on keeps the current track`() {
        assumeTrue(mpvAvailable(), "libmpv is not installed")

        val dir = Files.createTempDirectory("auxio-shuffle-keep")
        val player = MpvPlayer(audioDriver = "null")
        try {
            val queue = PlayQueue(player)
            queue.setTracks(tracks(dir, count = 5), startAt = 3)
            val playing = queue.current?.title

            queue.setShuffle(true)

            assertEquals(playing, queue.current?.title)
        } finally {
            player.close()
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `loop all wraps from the last track to the first`() {
        assumeTrue(mpvAvailable(), "libmpv is not installed")

        val dir = Files.createTempDirectory("auxio-loop")
        val player = MpvPlayer(audioDriver = "null")
        try {
            val queue = PlayQueue(player)
            val songs = tracks(dir, count = 3)
            queue.setTracks(songs)
            queue.setLoop(LoopMode.ALL)

            assertTrue(queue.next())
            assertTrue(queue.next())
            assertEquals("Track 2", queue.current?.title)

            assertTrue(queue.next(), "loop all should wrap instead of stopping")
            assertEquals("Track 0", queue.current?.title)
        } finally {
            player.close()
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `loop one replays the same track when it ends`() {
        assumeTrue(mpvAvailable(), "libmpv is not installed")

        val dir = Files.createTempDirectory("auxio-loop-one")
        val player = MpvPlayer(audioDriver = "null")
        try {
            val queue = PlayQueue(player)
            queue.setTracks(tracks(dir, count = 2))
            queue.setLoop(LoopMode.ONE)
            val playing = queue.current?.title

            val deadline = System.nanoTime() + 15_000_000_000L
            var repeated = false
            while (System.nanoTime() < deadline) {
                if (queue.advanceIfFinished()) {
                    repeated = true
                    break
                }
                Thread.sleep(100)
            }

            assertTrue(repeated, "the track did not restart")
            assertEquals(playing, queue.current?.title)
        } finally {
            player.close()
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `has no current track before anything is queued`() {
        assumeTrue(mpvAvailable(), "libmpv is not installed")

        val player = MpvPlayer(audioDriver = "null")
        try {
            val queue = PlayQueue(player)
            assertNull(queue.current)
            assertFalse(queue.advanceIfFinished())
            assertFalse(queue.next())
        } finally {
            player.close()
        }
    }
}
