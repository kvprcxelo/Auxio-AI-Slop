/*
 * Copyright (c) 2026 Auxio Project
 * Main.kt is part of Auxio.
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
 
package org.oxycblt.auxio.linux

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import org.oxycblt.auxio.linux.music.LibraryScanner
import org.oxycblt.auxio.linux.playback.MpvPlayer
import org.oxycblt.auxio.linux.ui.AuxioApp

/**
 * Entry point. Arguments are library folders; with none, the XDG music directory is used. A TUI
 * front end will share the scanner and player once the GUI is complete.
 */
fun main(args: Array<String>) {
    val roots = args.map { Path(it) }.filter { it.exists() }.ifEmpty { listOf(defaultMusicDir()) }

    val tracks = LibraryScanner.scan(roots)

    val player =
        try {
            MpvPlayer()
        } catch (error: Throwable) {
            System.err.println("Could not start playback: ${error.message}")
            return
        }

    application {
        Window(
            onCloseRequest = {
                player.close()
                exitApplication()
            },
            title = "Auxio",
            state = rememberWindowState(width = 1100.dp, height = 720.dp),
        ) {
            AuxioApp(player = player, tracks = tracks, roots = roots)
        }
    }
}

private fun defaultMusicDir(): Path {
    val home = System.getProperty("user.home")
    val xdg = System.getenv("XDG_MUSIC_DIR")?.takeIf { it.isNotBlank() }
    val candidate = if (xdg != null) Path(xdg) else Path(home, "Music")
    return if (candidate.exists() && Files.isDirectory(candidate)) candidate else Path(home)
}
