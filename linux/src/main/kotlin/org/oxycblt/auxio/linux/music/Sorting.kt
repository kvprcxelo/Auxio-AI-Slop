/*
 * Copyright (c) 2026 Auxio Project
 * Sorting.kt is part of Auxio.
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

/** How a visible list is ordered; [isDate] distinguishes release-date modes. */
enum class SortMode(val label: String) {
    NAME_ASC("Name (A–Z)"),
    NAME_DESC("Name (Z–A)"),
    DATE_ASC("Release date (oldest first)"),
    DATE_DESC("Release date (newest first)");

    val isDate: Boolean
        get() = this == DATE_ASC || this == DATE_DESC
}

/**
 * Orders the library lists the way the sort control asks.
 *
 * Release-date modes fall back to name order when nothing in the list carries a date, so a shared
 * mode cannot produce an unhelpful arbitrary order. Undated items always come last.
 */
object Sorting {
    fun tracks(
        tracks: List<Track>,
        mode: SortMode,
        ignorePunctuation: Boolean = false,
    ): List<Track> =
        sort(tracks, mode, { it.title }, { releaseYear(it.year, it.date) }, ignorePunctuation)

    fun albums(
        albums: List<Album>,
        mode: SortMode,
        ignorePunctuation: Boolean = false,
    ): List<Album> =
        sort(albums, mode, { it.name }, { releaseYear(it.year, it.date) }, ignorePunctuation)

    fun artists(
        artists: List<Artist>,
        mode: SortMode,
        ignorePunctuation: Boolean = false,
    ): List<Artist> = sort(artists, mode, { it.name }, { null }, ignorePunctuation)

    fun genres(
        genres: List<Genre>,
        mode: SortMode,
        ignorePunctuation: Boolean = false,
    ): List<Genre> = sort(genres, mode, { it.name }, { null }, ignorePunctuation)

    fun playlists(
        playlists: List<Playlist>,
        mode: SortMode,
        ignorePunctuation: Boolean = false,
    ): List<Playlist> = sort(playlists, mode, { it.name }, { null }, ignorePunctuation)

    private fun releaseYear(year: Int?, date: String?): Int? =
        year ?: date?.trim()?.take(4)?.toIntOrNull()

    private fun <T> sort(
        items: List<T>,
        mode: SortMode,
        name: (T) -> String,
        date: (T) -> Int?,
        ignorePunctuation: Boolean,
    ): List<T> {
        val byName = compareBy<T> { Library.sortKey(name(it), ignorePunctuation) }
        return when (mode) {
            SortMode.NAME_ASC -> items.sortedWith(byName)
            SortMode.NAME_DESC -> items.sortedWith(byName.reversed())
            SortMode.DATE_ASC ->
                items.sortedWith(
                    compareBy<T> { date(it) == null }.thenBy { date(it) ?: 0 }.then(byName)
                )
            SortMode.DATE_DESC ->
                items.sortedWith(
                    compareBy<T> { date(it) == null }
                        .thenByDescending { date(it) ?: 0 }
                        .then(byName)
                )
        }
    }
}
