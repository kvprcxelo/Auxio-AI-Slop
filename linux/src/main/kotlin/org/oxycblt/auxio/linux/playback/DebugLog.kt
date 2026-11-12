/*
 * Copyright (c) 2026 Auxio Project
 * DebugLog.kt is part of Auxio.
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
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path

/**
 * Playback diagnostics, off unless `AUXIO_LOG` is set.
 *
 * `AUXIO_LOG=1` writes to stderr. `AUXIO_LOG=/some/file` also mirrors mpv's own log there, which is
 * what makes a report from a desktop launch (no terminal) possible. libmpv keeps `terminal=no` for
 * library users, so nothing is printed without this.
 */
internal object DebugLog {
    private val value = System.getenv("AUXIO_LOG")?.trim()?.takeIf { it.isNotEmpty() }

    /** Where this app appends its own lines, or null when logging to stderr only. */
    val file: Path? = value?.takeIf { it != "1" }?.let { Path(it) }

    /**
     * Where mpv writes its log. It truncates the file whenever a context is created, so it cannot
     * share the path above or the app's own lines would be lost.
     */
    val mpvFile: Path? = file?.resolveSibling("${file.fileName}.mpv")

    val enabled: Boolean = value != null

    fun write(message: String) {
        if (!enabled) return

        val line = "[auxio] $message"
        System.err.println(line)
        file?.let { path ->
            runCatching {
                path.parent?.let { Files.createDirectories(it) }
                Files.writeString(
                    path,
                    line + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND,
                )
            }
        }
    }
}
