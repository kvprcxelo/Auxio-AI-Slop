/*
 * Copyright (c) 2026 Auxio Project
 * ThemeCache.kt is part of Auxio.
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
 
package org.oxycblt.auxio.linux.theme

import java.nio.file.Files
import java.nio.file.Path
import org.oxycblt.auxio.linux.Storage

/**
 * The roles matugen last produced for a wallpaper, kept so the next launch can draw them before
 * matugen has had time to run again.
 *
 * It lives in the cache directory and holds one entry: the key describes the wallpaper and palette
 * it belongs to, so a changed wallpaper or a different palette simply misses. Reading it is a plain
 * file read, which is what makes it usable while the first frame is being composed.
 */
class ThemeCache(private val directory: Path = Storage.cacheDir) {
    private val file: Path
        get() = directory.resolve("theme-roles")

    /** The stored roles for [key], or null when nothing (or something else) was stored. */
    fun roles(key: String): Map<String, String>? {
        val lines = runCatching { Files.readAllLines(file) }.getOrNull() ?: return null
        if (lines.firstOrNull() != "key=$key") return null

        return lines
            .drop(1)
            .mapNotNull { line ->
                val split = line.indexOf('=')
                if (split <= 0) null else line.substring(0, split) to line.substring(split + 1)
            }
            .toMap()
            .takeIf { it.isNotEmpty() }
    }

    fun store(key: String, roles: Map<String, String>) {
        runCatching {
            Files.createDirectories(directory)
            Files.writeString(
                file,
                buildString {
                    appendLine("key=$key")
                    roles.toSortedMap().forEach { (name, value) -> appendLine("$name=$value") }
                },
            )
        }
    }

    fun clear() {
        runCatching { Files.deleteIfExists(file) }
    }
}
