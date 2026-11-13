/*
 * Copyright (c) 2026 Auxio Project
 * ArtworkTest.kt is part of Auxio.
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
import kotlin.io.path.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.Assumptions.assumeTrue

class ArtworkTest {
    private fun resource(name: String): Path =
        Path.of(javaClass.getResource("/$name")?.toURI() ?: error("$name missing"))

    private val cover = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 1, 2, 3)

    @Test
    fun `falls back to a folder cover`() {
        val dir = Files.createTempDirectory("auxio-art")
        try {
            dir.resolve("cover.jpg").writeBytes(cover)
            val track = Track(dir.resolve("song.opus"), "Song")

            assertContentEquals(cover, Artwork.bytes(track))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `finds oddly named images in the folder`() {
        val dir = Files.createTempDirectory("auxio-art")
        try {
            dir.resolve("some-scan.png").writeBytes(cover)
            val track = Track(dir.resolve("song.opus"), "Song")

            assertContentEquals(cover, Artwork.bytes(track))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `prefers embedded artwork over a folder cover`() {
        assumeTrue(NativeTags.isAvailable, "taglib shim is not built")

        val dir = Files.createTempDirectory("auxio-art")
        try {
            val song = resource("artwork.flac").copyTo(dir.resolve("song.flac"))
            dir.resolve("cover.jpg").writeBytes(cover)

            val artwork = assertNotNull(Artwork.bytes(Track(song, "Song")))

            // The embedded picture is a PNG, unlike the sentinel bytes used for the folder file.
            assertContentEquals(listOf(0x89.toByte(), 'P'.code.toByte()), artwork.take(2))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `reports nothing when neither the file nor the folder has art`() {
        val dir = Files.createTempDirectory("auxio-art").createDirectories()
        try {
            assertNull(Artwork.bytes(Track(dir.resolve("song.opus"), "Song")))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }
}
