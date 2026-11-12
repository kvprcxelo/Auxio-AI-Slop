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

    /** The same tone under two names, so the queue has two distinct entries. */
    private fun tracks(dir: Path): List<Track> {
        val first = tone().copyTo(dir.resolve("one.wav"), overwrite = true)
        val second = tone().copyTo(dir.resolve("two.wav"), overwrite = true)
        return listOf(Track(first, "One"), Track(second, "Two"))
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
            assertEquals("Two", queue.current?.title)

            assertTrue(queue.previous())
            assertEquals("One", queue.current?.title)
            assertFalse(queue.previous(), "the queue cannot go before the first track")

            assertTrue(queue.next())
            assertEquals("Two", queue.current?.title)
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
            assertEquals("Two", queue.current?.title)
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
