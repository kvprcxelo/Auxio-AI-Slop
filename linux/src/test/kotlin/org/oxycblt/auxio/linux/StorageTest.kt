/*
 * Copyright (c) 2026 Auxio Project
 * StorageTest.kt is part of Auxio.
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

import kotlin.test.Test
import kotlin.test.assertEquals

/** Paths only; the directories themselves are created by the application. */
class StorageTest {
    @Test
    fun `settings live in the config directory`() {
        assertEquals("config", Storage.configFile.fileName.toString())
        assertEquals("auxio", Storage.configFile.parent.fileName.toString())
    }

    @Test
    fun `covers are cached under the cache directory`() {
        assertEquals("auxio", Storage.artworkCacheDir.parent.fileName.toString())
        assertEquals("artwork", Storage.artworkCacheDir.fileName.toString())
    }

    @Test
    fun `playlists are read from the home directory`() {
        assertEquals("playlists", Storage.playlistDir.fileName.toString())
        assertEquals("auxio", Storage.playlistDir.parent.fileName.toString())
    }
}
