package org.oxycblt.auxio.linux

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.oxycblt.auxio.linux.music.LibraryScanner
import org.oxycblt.auxio.linux.playback.MpvPlayer
import org.oxycblt.auxio.linux.ui.AuxioApp
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

/**
 * Entry point. Arguments are library folders; with none, the XDG music directory is used.
 * A TUI front end will share the scanner and player once the GUI is complete.
 */
fun main(args: Array<String>) {
    val roots =
        args.map { Path(it) }
            .filter { it.exists() }
            .ifEmpty { listOf(defaultMusicDir()) }

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
