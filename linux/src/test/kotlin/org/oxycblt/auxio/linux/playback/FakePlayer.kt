/*
 * Copyright (c) 2026 Auxio Project
 * FakePlayer.kt is part of Auxio.
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

/** A player that makes no sound, so the window can be rendered on machines without libmpv. */
class FakePlayer : Player {
    private var paused = true

    override fun load(path: Path, autoplay: Boolean) {
        paused = !autoplay
    }

    override fun play() {
        paused = false
    }

    override fun pause() {
        paused = true
    }

    override fun togglePause() {
        paused = !paused
    }

    override fun seekTo(seconds: Double) = Unit

    override fun seekBy(delta: Double) = Unit

    override fun stop() = Unit

    override fun volume(value: Double) = Unit

    override fun timePosition(): Double = 0.0

    override fun duration(): Double = 0.0

    override fun isPaused(): Boolean = paused

    override fun isIdle(): Boolean = true

    override fun close() = Unit
}
