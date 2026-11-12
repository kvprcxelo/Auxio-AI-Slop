package org.oxycblt.auxio.linux.playback

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

/**
 * Exercises the real libmpv binding. Skipped when libmpv is not installed, so the module
 * still builds and tests on machines without a media stack.
 */
class MpvPlayerTest {
    private fun tone(): Path =
        Path.of(javaClass.getResource("/tone.wav")?.toURI() ?: error("tone.wav missing"))

    private fun mpvAvailable(): Boolean =
        runCatching { Mpv.load() }.isSuccess

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
}
