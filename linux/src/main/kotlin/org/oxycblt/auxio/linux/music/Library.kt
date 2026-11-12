/*
 * Copyright (c) 2026 Auxio Project
 * Library.kt is part of Auxio.
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

import java.nio.file.Path

/** A playable file, with tags where taglib could read them. */
data class Track(
    val path: Path,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val trackNumber: Int? = null,
    val year: Int? = null,
    val durationSeconds: Double? = null,
) {
    /** Stable identity for a track; the path is the only thing that cannot lie. */
    val id: String
        get() = path.toAbsolutePath().toString()
}

/** Songs sharing an album name and artist, in disc/track order. */
data class Album(
    val name: String,
    val artist: String?,
    val year: Int?,
    val tracks: List<Track>,
) {
    val durationSeconds: Double
        get() = tracks.sumOf { it.durationSeconds ?: 0.0 }
}

/** Songs sharing an artist, with the albums they appear on. */
data class Artist(val name: String, val albums: List<Album>, val tracks: List<Track>)

/** The whole library, in the three shapes Auxio presents. */
data class MusicLibrary(
    val songs: List<Track>,
    val albums: List<Album>,
    val artists: List<Artist>,
) {
    companion object {
        val EMPTY = MusicLibrary(emptyList(), emptyList(), emptyList())
    }
}

/**
 * Groups songs into albums and artists.
 *
 * Auxio does this from MediaStore; here it is derived from tags, with the containing folder
 * standing in for a missing album tag and a single placeholder for missing artists.
 */
object Library {
    const val UNKNOWN_ARTIST = "Unknown artist"

    fun build(songs: List<Track>): MusicLibrary {
        val albums =
            songs
                .groupBy { albumName(it) to (it.artist ?: UNKNOWN_ARTIST) }
                .map { (key, tracks) ->
                    val ordered =
                        tracks.sortedWith(
                            compareBy<Track> { it.trackNumber ?: Int.MAX_VALUE }
                                .thenBy { it.title.lowercase() }
                        )
                    Album(
                        name = key.first,
                        artist = key.second,
                        year = tracks.mapNotNull { it.year }.minOrNull(),
                        tracks = ordered,
                    )
                }
                .sortedWith(compareBy({ it.name.lowercase() }, { it.artist?.lowercase() }))

        val artists =
            songs
                .groupBy { it.artist ?: UNKNOWN_ARTIST }
                .map { (name, tracks) ->
                    Artist(
                        name = name,
                        albums = albums.filter { it.artist == name },
                        tracks =
                            tracks.sortedWith(
                                compareBy({ it.album ?: "" }, { it.trackNumber ?: 0 })
                            ),
                    )
                }
                .sortedBy { it.name.lowercase() }

        return MusicLibrary(songs = songs, albums = albums, artists = artists)
    }

    private fun albumName(track: Track): String =
        track.album ?: track.path.parent?.fileName?.toString() ?: "Unknown album"
}
