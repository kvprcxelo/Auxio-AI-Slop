/*
 * Copyright (c) 2026 Auxio Project
 * ArtworkStoreTest.kt is part of Auxio.
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
 
package org.oxycblt.auxio.linux.music

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.exists
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Eviction only; reading covers needs real tags and is covered elsewhere. */
class ArtworkStoreTest {
    private val kilobyte = 1024L

    @Test
    fun `deletes the oldest covers until the cache fits`() {
        withCache(entries = 10, size = 100 * kilobyte) { dir ->
            val remaining = ArtworkStore.trim(dir, 500 * kilobyte)

            assertTrue(remaining <= 500 * kilobyte, "cache is still $remaining bytes")
            assertFalse(dir.resolve("entry-0.art").exists())
            assertTrue(dir.resolve("entry-9.art").exists())
        }
    }

    @Test
    fun `leaves a cache under the limit alone`() {
        withCache(entries = 3, size = 100 * kilobyte) { dir ->
            val remaining = ArtworkStore.trim(dir, 500 * kilobyte)

            assertTrue(remaining <= 500 * kilobyte)
            repeat(3) { assertTrue(dir.resolve("entry-$it.art").exists()) }
        }
    }

    private fun withCache(entries: Int, size: Long, block: (Path) -> Unit) {
        val dir = Files.createTempDirectory("auxio-artwork")
        try {
            repeat(entries) { index ->
                val file = dir.resolve("entry-$index.art")
                Files.write(file, ByteArray(size.toInt()))
                // Write order is also the eviction order.
                Files.setLastModifiedTime(file, FileTime.fromMillis(1_000L + index))
            }
            block(dir)
        } finally {
            Files.walk(dir).use { stream ->
                stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
            }
        }
    }
}
