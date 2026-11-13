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
    val genre: String? = null,
    val trackNumber: Int? = null,
    val year: Int? = null,
    /** Raw date tag, kept verbatim so the settings can decide how to render it. */
    val date: String? = null,
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
    val date: String?,
    val tracks: List<Track>,
) {
    val durationSeconds: Double
        get() = tracks.sumOf { it.durationSeconds ?: 0.0 }
}

/** Songs sharing an artist, with the albums they appear on. */
data class Artist(val name: String, val albums: List<Album>, val tracks: List<Track>)

/** Songs sharing a genre tag. */
data class Genre(val name: String, val tracks: List<Track>)

/** The whole library, in the shapes Auxio presents. */
data class MusicLibrary(
    val songs: List<Track>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val genres: List<Genre> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
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

    /**
     * @param separators characters that split a multi-artist tag into separate artists, matching
     *   Auxio's metadata separator setting. Album grouping still uses the raw tag, so a compilation
     *   stays one album.
     * @param ignoreLeadingPunctuation sort "\\*Song" under "S" instead of at the top, as if the
     *   decoration were not there.
     */
    fun build(
        songs: List<Track>,
        separators: Set<Char> = emptySet(),
        ignoreLeadingPunctuation: Boolean = false,
        playlists: List<Playlist> = emptyList(),
    ): MusicLibrary {
        val normalized = normalizeArtists(songs, separators)

        val albums =
            normalized
                .groupBy { albumName(it) to (it.artist ?: UNKNOWN_ARTIST) }
                .map { (key, tracks) ->
                    val ordered =
                        tracks.sortedWith(
                            compareBy<Track> { it.trackNumber ?: Int.MAX_VALUE }
                                .thenBy { sortKey(it.title, ignoreLeadingPunctuation) }
                        )
                    Album(
                        name = key.first,
                        artist = key.second,
                        year = tracks.mapNotNull { it.year }.minOrNull(),
                        date = Dates.earliest(tracks.map { it.date }),
                        tracks = ordered,
                    )
                }
                .sortedWith(
                    compareBy(
                        { sortKey(it.name, ignoreLeadingPunctuation) },
                        { it.artist?.lowercase() },
                    )
                )

        val artists =
            normalized
                .flatMap { track -> artistNames(track, separators).map { name -> name to track } }
                // Tags disagree about casing often; "X" and "x" are the same artist.
                .groupBy { it.first.lowercase() }
                .map { (_, entries) ->
                    val tracks = entries.map { it.second }.distinctBy { it.id }
                    val ids = tracks.map { it.id }.toSet()
                    Artist(
                        name = displayName(entries.map { it.first }),
                        albums = albums.filter { album -> album.tracks.any { it.id in ids } },
                        tracks =
                            tracks.sortedWith(
                                compareBy(
                                    { sortKey(it.album ?: "", ignoreLeadingPunctuation) },
                                    { it.trackNumber ?: 0 },
                                )
                            ),
                    )
                }
                .sortedBy { sortKey(it.name, ignoreLeadingPunctuation) }

        val genres =
            normalized
                .flatMap { track -> genreNames(track, separators).map { name -> name to track } }
                .groupBy { it.first.lowercase() }
                .map { (_, entries) ->
                    Genre(
                        name = displayName(entries.map { it.first }),
                        tracks =
                            entries
                                .map { it.second }
                                .distinctBy { it.id }
                                .sortedWith(
                                    compareBy(
                                        { sortKey(it.album ?: "", ignoreLeadingPunctuation) },
                                        { it.trackNumber ?: 0 },
                                    )
                                ),
                    )
                }
                .sortedBy { sortKey(it.name, ignoreLeadingPunctuation) }

        val sortedSongs = normalized.sortedBy { sortKey(it.title, ignoreLeadingPunctuation) }

        return MusicLibrary(
            songs = sortedSongs,
            albums = albums,
            artists = artists,
            genres = genres,
            playlists = playlists,
        )
    }

    /**
     * Rewrites every song's artist to the spellings the library settled on.
     *
     * A tag like "X; x; Y" lists the same artist twice in different casing; the duplicate is
     * dropped and both survivors take the name used by most of the library, so the songs list and
     * the artist page agree.
     */
    private fun normalizeArtists(songs: List<Track>, separators: Set<Char>): List<Track> {
        val splitByTrack = songs.associate { it.id to artistNames(it, separators) }
        val canonical =
            splitByTrack.values
                .flatten()
                .groupBy { it.lowercase() }
                .mapValues { (_, variants) -> displayName(variants) }

        return songs.map { track ->
            val names =
                splitByTrack
                    .getValue(track.id)
                    .map { canonical[it.lowercase()] ?: it }
                    .distinctBy { it.lowercase() }
            track.copy(artist = names.joinToString("; "))
        }
    }

    /**
     * The spelling to show for an artist whose tags disagree: whichever variant occurs most, with
     * ties broken alphabetically so the choice is stable between rebuilds.
     */
    private fun displayName(variants: List<String>): String =
        variants
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .first()
            .key

    private fun albumName(track: Track): String =
        track.album ?: track.path.parent?.fileName?.toString() ?: "Unknown album"

    /** Artist names for a track, split on the configured separators. */
    internal fun artistNames(track: Track, separators: Set<Char>): List<String> {
        val raw = track.artist?.takeIf { it.isNotBlank() } ?: return listOf(UNKNOWN_ARTIST)
        if (separators.isEmpty()) return listOf(raw)

        return raw.split(*separators.toCharArray())
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf(UNKNOWN_ARTIST) }
    }

    /** Genre names for a track, split on the configured separators like artists are. */
    internal fun genreNames(track: Track, separators: Set<Char>): List<String> {
        val raw = track.genre?.takeIf { it.isNotBlank() } ?: return emptyList()
        if (separators.isEmpty()) return listOf(raw.trim())

        return raw.split(*separators.toCharArray()).map { it.trim() }.filter { it.isNotEmpty() }
    }

    /** Case-insensitive sort key, optionally past leading non-alphanumeric decoration. */
    internal fun sortKey(value: String, ignoreLeadingPunctuation: Boolean): String {
        val text = value.trim()
        if (!ignoreLeadingPunctuation) return text.lowercase()

        val start = text.indexOfFirst { it.isLetterOrDigit() }
        return (if (start >= 0) text.substring(start) else text).lowercase()
    }
}
