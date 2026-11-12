/*
 * Copyright (c) 2026 Auxio Project
 * MpvPlayer.kt is part of Auxio.
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

import com.sun.jna.Memory
import com.sun.jna.Pointer
import java.nio.file.Path

/**
 * Audio playback backed by libmpv.
 *
 * State is polled rather than observed through mpv's event loop: a music player needs time position
 * at a handful of hertz anyway, and polling keeps the binding free of the event struct.
 */
class MpvPlayer(audioOutput: String = "auto") : AutoCloseable {
    private val api = Mpv.load()

    private val ctx: Pointer = run {
        // libmpv returns NULL from mpv_create() unless LC_NUMERIC is "C", and GTK installs
        // the desktop's locale process-wide before we ever get here.
        ProcessLocale.resetNumericToC()
        requireNotNull(api.mpv_create()) { "libmpv could not create a playback context" }
    }

    init {
        checkOk(api.mpv_initialize(ctx), "initialize")
        setOption("ao", audioOutput)
        // Audio only: never spawn a video surface for embedded cover art.
        setOption("vid", "no")
        setOption("audio-display", "no")
        setOption("force-window", "no")
        setOption("idle", "yes")
        setOption("gapless-audio", "yes")
    }

    fun load(path: Path, autoplay: Boolean = true) {
        val escaped = path.toAbsolutePath().toString().replace("\\", "\\\\").replace("\"", "\\\"")
        command("loadfile \"$escaped\" ${if (autoplay) "replace" else "replace"}".trim())
    }

    fun play() = setOption("pause", "no")

    fun pause() = setOption("pause", "yes")

    fun togglePause() = command("cycle pause")

    fun seekTo(seconds: Double) = command("seek ${seconds.coerceAtLeast(0.0)} absolute")

    fun seekBy(delta: Double) = command("seek $delta relative")

    fun stop() = command("stop")

    fun volume(value: Double) = setDouble("volume", value.coerceIn(0.0, 100.0))

    fun timePosition(): Double = getDouble("time-pos") ?: 0.0

    fun duration(): Double = getDouble("duration") ?: 0.0

    fun isPaused(): Boolean = getFlag("pause")

    fun isIdle(): Boolean = getFlag("idle-active")

    override fun close() {
        runCatching { command("quit") }
        api.mpv_terminate_destroy(ctx)
    }

    private fun command(args: String) {
        checkOk(api.mpv_command_string(ctx, args), args)
    }

    private fun setOption(name: String, value: String) {
        checkOk(api.mpv_set_option_string(ctx, name, value), "set $name")
    }

    private fun setDouble(name: String, value: Double) {
        Memory(8).also { memory ->
            memory.setDouble(0, value)
            api.mpv_set_property(ctx, name, MPV_FORMAT_DOUBLE, memory)
        }
    }

    private fun getDouble(name: String): Double? {
        val memory = Memory(8)
        val result = api.mpv_get_property(ctx, name, MPV_FORMAT_DOUBLE, memory)
        // Unavailable properties (before a file is loaded) are expected, not errors.
        return if (result < 0) null else memory.getDouble(0)
    }

    private fun getFlag(name: String): Boolean {
        val memory = Memory(8)
        val result = api.mpv_get_property(ctx, name, MPV_FORMAT_FLAG, memory)
        return result >= 0 && memory.getInt(0) != 0
    }

    private fun checkOk(result: Int, what: String) {
        if (result < 0) {
            val message = api.mpv_error_string(result) ?: "error $result"
            throw IllegalStateException("libmpv failed to $what: $message")
        }
    }
}
