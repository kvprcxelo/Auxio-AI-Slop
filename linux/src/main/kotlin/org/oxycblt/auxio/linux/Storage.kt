/*
 * Copyright (c) 2026 Auxio Project
 * Storage.kt is part of Auxio.
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

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Where Auxio keeps its own files.
 *
 * The XDG base directory spec covers settings and cache: configuration is small and worth keeping,
 * artwork can always be extracted again, so it belongs in the cache where it is free to be cleared.
 * Playlists are the exception, because they are files people edit and share by hand; they live in
 * `~/auxio/playlists`, which is also where Auxio's own exports end up on Android.
 */
object Storage {
    private const val APP_DIR = "auxio"

    /** Settings, one `key=value` per line. */
    val configFile: Path by lazy { configDir.resolve("config") }

    private val configDir: Path by lazy { xdg("XDG_CONFIG_HOME", ".config").resolve(APP_DIR) }

    /** Where the pre-XDG-named settings file lived, before it was moved to [configFile]. */
    private val legacyConfigFile: Path by lazy { configDir.resolve("settings.properties") }

    val cacheDir: Path by lazy { xdg("XDG_CACHE_HOME", ".cache").resolve(APP_DIR) }

    /** Covers pulled out of tags or folders, so a rescan does not parse every file again. */
    val artworkCacheDir: Path by lazy { cacheDir.resolve("artwork") }

    /** Tags read by the last scan, so a reload only parses files that changed. */
    val libraryCache: Path by lazy { cacheDir.resolve("library") }

    /** `.m3u`/`.m3u8` files, in addition to any found next to the music itself. */
    val playlistDir: Path by lazy { home.resolve(APP_DIR).resolve("playlists") }

    private val home: Path
        get() = Path(System.getProperty("user.home") ?: ".").toAbsolutePath()

    private fun xdg(variable: String, fallback: String): Path =
        System.getenv(variable)?.takeIf { it.isNotBlank() }?.let { Path(it) }
            ?: home.resolve(fallback)

    /** A file under the XDG config home itself, where desktops keep their themes and wallpapers. */
    fun configPath(name: String): Path = configDir.parent.resolve(name)

    /** Creates the directories Auxio writes to, so they exist to drop playlists into. */
    fun ensureDirectories() {
        runCatching {
            Files.createDirectories(configDir)
            Files.createDirectories(artworkCacheDir)
            Files.createDirectories(playlistDir)
        }
    }

    /** Moves `auxio/settings.properties` to `auxio/config` once, keeping existing preferences. */
    fun migrateLegacyConfig() {
        if (Files.exists(configFile) || !Files.exists(legacyConfigFile)) return

        runCatching {
            Files.createDirectories(configDir)
            Files.move(legacyConfigFile, configFile)
        }
    }
}
