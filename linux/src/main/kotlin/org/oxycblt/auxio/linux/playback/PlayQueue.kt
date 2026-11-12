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

import org.oxycblt.auxio.linux.music.Track

/**
 * Which track is playing and what comes next.
 *
 * mpv is driven in idle mode, so a finished track looks like "nothing loaded" rather than an event;
 * [advanceIfFinished] is polled by the UI and only fires after playback has actually started, so
 * the gap between `loadfile` and the demuxer opening is not mistaken for the end.
 */
class PlayQueue(private val player: MpvPlayer) {
    private var tracks: List<Track> = emptyList()
    private var index = -1
    private var started = false

    val current: Track?
        get() = tracks.getOrNull(index)

    val size: Int
        get() = tracks.size

    fun setTracks(tracks: List<Track>, startAt: Int = 0) {
        this.tracks = tracks
        playAt(startAt)
    }

    fun playAt(position: Int) {
        require(position in tracks.indices) { "position $position is outside the queue" }
        index = position
        started = false
        player.load(tracks[index].path)
    }

    fun next(): Boolean {
        if (index + 1 >= tracks.size) return false
        playAt(index + 1)
        return true
    }

    fun previous(): Boolean {
        if (index <= 0) return false
        playAt(index - 1)
        return true
    }

    /** Moves on once the current track has ended. Returns whether the current track changed. */
    fun advanceIfFinished(): Boolean {
        if (current == null) return false
        if (!player.isIdle()) {
            started = true
            return false
        }
        if (!started) return false

        started = false
        if (next()) return true

        // End of the queue: stop rather than spinning on an idle player.
        player.stop()
        return false
    }
}
