/*
 * Copyright (c) 2026 Auxio Project
 * PlayQueue.kt is part of Auxio.
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

import kotlin.random.Random
import org.oxycblt.auxio.linux.music.Track

enum class LoopMode {
    OFF,
    ALL,
    ONE,
}

/**
 * Which track is playing and what comes next.
 *
 * Playback order is a separate list of track indices, so enabling shuffle does not reorder the
 * queue's contents: the current track stays current and simply becomes the head of the new order.
 *
 * mpv is driven in idle mode, so a finished track looks like "nothing loaded" rather than an event;
 * [advanceIfFinished] is polled by the UI and only fires after playback has actually started, so
 * the gap between `loadfile` and the demuxer opening is not mistaken for the end.
 */
class PlayQueue(private val player: MpvPlayer, private val random: Random = Random.Default) {
    private var tracks: List<Track> = emptyList()
    private var order: List<Int> = emptyList()
    private var cursor = -1
    private var started = false

    var shuffle: Boolean = false
        private set

    var loop: LoopMode = LoopMode.OFF
        private set

    val current: Track?
        get() = tracks.getOrNull(order.getOrElse(cursor) { -1 })

    val size: Int
        get() = tracks.size

    fun setTracks(tracks: List<Track>, startAt: Int = 0) {
        this.tracks = tracks
        rebuildOrder(startAt)
        playCurrent()
    }

    fun setShuffle(enabled: Boolean) {
        if (shuffle == enabled) return

        val playing = current?.id
        shuffle = enabled
        val start = tracks.indexOfFirst { it.id == playing }.takeIf { it >= 0 } ?: 0
        rebuildOrder(start)
    }

    fun setLoop(mode: LoopMode) {
        loop = mode
    }

    fun cycleLoop() {
        loop =
            when (loop) {
                LoopMode.OFF -> LoopMode.ALL
                LoopMode.ALL -> LoopMode.ONE
                LoopMode.ONE -> LoopMode.OFF
            }
    }

    fun toggleShuffle() = setShuffle(!shuffle)

    fun next(): Boolean {
        if (order.isEmpty()) return false

        if (cursor + 1 < order.size) {
            cursor++
            playCurrent()
            return true
        }
        if (loop == LoopMode.ALL) {
            cursor = 0
            playCurrent()
            return true
        }
        return false
    }

    fun previous(): Boolean {
        if (order.isEmpty()) return false

        if (cursor > 0) {
            cursor--
            playCurrent()
            return true
        }
        if (loop == LoopMode.ALL) {
            cursor = order.lastIndex
            playCurrent()
            return true
        }
        return false
    }

    /** Moves on once the current track has ended. Returns whether the current track changed. */
    fun advanceIfFinished(): Boolean {
        if (current == null) return false
        if (!player.isIdle()) {
            started = true
            return false
        }
        if (!started) return false

        if (loop == LoopMode.ONE) {
            playCurrent()
            return true
        }

        started = false
        if (next()) return true

        // End of the queue: stop rather than spinning on an idle player.
        player.stop()
        return false
    }

    private fun rebuildOrder(start: Int) {
        if (tracks.isEmpty()) {
            order = emptyList()
            cursor = -1
            return
        }

        val safeStart = start.coerceIn(0, tracks.lastIndex)
        order =
            if (shuffle) {
                // The current track leads, so turning shuffle on never interrupts playback.
                listOf(safeStart) + (tracks.indices - safeStart).shuffled(random)
            } else {
                tracks.indices.toList()
            }
        cursor = if (shuffle) 0 else safeStart
    }

    private fun playCurrent() {
        val track = current ?: return
        started = false
        player.load(track.path)
    }
}
