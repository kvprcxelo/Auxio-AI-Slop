/*
 * Copyright (c) 2026 Auxio Project
 * MpvPlayerTest.kt is part of Auxio.
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

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

/**
 * Exercises the real libmpv binding. Skipped when libmpv is not installed, so the module still
 * builds and tests on machines without a media stack.
 */
class MpvPlayerTest {
    private fun tone(): Path =
        Path.of(javaClass.getResource("/tone.wav")?.toURI() ?: error("tone.wav missing"))

    private fun mpvAvailable(): Boolean = runCatching { Mpv.load() }.isSuccess

    @Test
    fun `plays a file and advances the clock`() {
        assumeTrue(mpvAvailable(), "libmpv is not installed")

        val player = MpvPlayer(audioOutput = "null")
        try {
            player.load(tone())

            val deadline = System.nanoTime() + 10_000_000_000L
            var position = 0.0
            while (System.nanoTime() < deadline) {
                position = player.timePosition()
                if (position > 0.2) break
                Thread.sleep(50)
            }

            assertTrue(position > 0.2, "playback did not advance, time-pos was $position")
            assertTrue(
                player.duration() in 1.5..2.5,
                "unexpected duration ${player.duration()}",
            )
        } finally {
            player.close()
        }
    }

    @Test
    fun `pause stops the clock and play resumes it`() {
        assumeTrue(mpvAvailable(), "libmpv is not installed")

        val player = MpvPlayer(audioOutput = "null")
        try {
            player.load(tone())
            Thread.sleep(400)

            player.togglePause()
            Thread.sleep(100)
            assertTrue(player.isPaused(), "player should report paused")

            val pausedAt = player.timePosition()
            Thread.sleep(400)
            assertEquals(pausedAt, player.timePosition(), 0.05, "clock moved while paused")

            player.togglePause()
            Thread.sleep(100)
            assertTrue(!player.isPaused(), "player should report playing")
        } finally {
            player.close()
        }
    }

    @Test
    fun `creates a playback context while the process locale is not C`() {
        assumeTrue(mpvAvailable(), "libmpv is not installed")

        // A desktop session makes GTK install LC_ALL from the environment, and libmpv then
        // refuses to create a context, so the player has to reset LC_NUMERIC itself.
        val locale =
            listOf("C.utf8", "C.UTF-8", "en_US.UTF-8", "de_DE.UTF-8").firstOrNull {
                ProcessLocale.setNumeric(it)
            }
        assumeTrue(locale != null, "no non-C LC_NUMERIC is installed to test with")

        try {
            // The hazard itself: a bare context cannot be created under this locale. Only
            // libmpv builds that enforce it are interesting here.
            assumeTrue(
                runCatching { Mpv.load().mpv_create() }.getOrNull() == null,
                "this libmpv tolerates a non-C LC_NUMERIC, so there is nothing to guard",
            )

            MpvPlayer(audioOutput = "null").use { player ->
                assertTrue(!player.isPaused(), "a fresh context should not be paused")
            }
        } finally {
            ProcessLocale.resetNumericToC()
        }
    }
}
