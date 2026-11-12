/*
 * Copyright (c) 2026 Auxio Project
 * App.kt is part of Auxio.
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
 
package org.oxycblt.auxio.linux.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.nio.file.Path
import kotlinx.coroutines.delay
import org.oxycblt.auxio.linux.music.Album
import org.oxycblt.auxio.linux.music.Artist
import org.oxycblt.auxio.linux.music.MusicLibrary
import org.oxycblt.auxio.linux.music.Track
import org.oxycblt.auxio.linux.playback.MpvPlayer
import org.oxycblt.auxio.linux.playback.PlayQueue

private enum class Section(val label: String) {
    Songs("Songs"),
    Albums("Albums"),
    Artists("Artists"),
}

@Composable
fun AuxioApp(player: MpvPlayer, library: MusicLibrary, roots: List<Path>) {
    var section by remember { mutableStateOf(Section.Songs) }
    var openAlbum by remember { mutableStateOf<Album?>(null) }
    var openArtist by remember { mutableStateOf<Artist?>(null) }
    var current by remember { mutableStateOf<Track?>(null) }
    var position by remember { mutableStateOf(0.0) }
    var duration by remember { mutableStateOf(0.0) }
    var paused by remember { mutableStateOf(true) }
    var volume by remember { mutableStateOf(100.0) }
    var query by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    val queue = remember { PlayQueue(player) }

    fun play(tracks: List<Track>, index: Int) {
        status =
            runCatching { queue.setTracks(tracks, index) }
                .exceptionOrNull()
                ?.let {
                    "Could not play ${tracks.getOrNull(index)?.title ?: "track"}: ${it.message}"
                }
        current = queue.current
    }

    fun step(forward: Boolean) {
        status =
            runCatching { if (forward) queue.next() else queue.previous() }
                .exceptionOrNull()
                ?.let { "Could not change track: ${it.message}" }
        current = queue.current
    }

    LaunchedEffect(Unit) {
        while (true) {
            if (queue.advanceIfFinished()) current = queue.current
            position = player.timePosition()
            duration = player.duration()
            paused = player.isPaused()
            delay(250)
        }
    }

    val songs =
        remember(library, query) {
            if (query.isBlank()) library.songs
            else
                library.songs.filter {
                    it.title.contains(query, ignoreCase = true) ||
                        it.artist?.contains(query, ignoreCase = true) == true ||
                        it.album?.contains(query, ignoreCase = true) == true
                }
        }

    val albums =
        remember(library, query) {
            if (query.isBlank()) library.albums
            else
                library.albums.filter {
                    it.name.contains(query, ignoreCase = true) ||
                        it.artist?.contains(query, ignoreCase = true) == true
                }
        }

    val artists =
        remember(library, query) {
            if (query.isBlank()) library.artists
            else library.artists.filter { it.name.contains(query, ignoreCase = true) }
        }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Auxio", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.width(24.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        label = { Text("Search") },
                        modifier = Modifier.width(280.dp),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text =
                            "${library.songs.size} songs · ${library.albums.size} albums · " +
                                "${library.artists.size} artists",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                status?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                }

                if (library.songs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No music found in ${roots.joinToString()}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    val album = openAlbum
                    val artist = openArtist
                    when {
                        album != null ->
                            AlbumDetail(
                                album = album,
                                current = current,
                                onBack = { openAlbum = null },
                                onPlay = { index -> play(album.tracks, index) },
                            )
                        artist != null ->
                            ArtistDetail(
                                artist = artist,
                                current = current,
                                onBack = { openArtist = null },
                                onOpenAlbum = { openAlbum = it },
                                onPlay = { index -> play(artist.tracks, index) },
                            )
                        else -> {
                            PrimaryTabRow(selectedTabIndex = section.ordinal) {
                                Section.entries.forEach { entry ->
                                    Tab(
                                        selected = section == entry,
                                        onClick = { section = entry },
                                        text = { Text(entry.label) },
                                    )
                                }
                            }

                            when (section) {
                                Section.Songs ->
                                    SongList(
                                        tracks = songs,
                                        current = current,
                                        onPlay = { index -> play(songs, index) },
                                    )
                                Section.Albums ->
                                    AlbumList(albums = albums, onOpen = { openAlbum = it })
                                Section.Artists ->
                                    ArtistList(artists = artists, onOpen = { openArtist = it })
                            }
                        }
                    }
                }

                TransportBar(
                    current = current,
                    paused = paused,
                    position = position,
                    duration = duration,
                    volume = volume,
                    onPrevious = { step(forward = false) },
                    onTogglePause = {
                        current?.let {
                            player.togglePause()
                            paused = player.isPaused()
                        }
                    },
                    onNext = { step(forward = true) },
                    onSeek = { fraction ->
                        if (duration > 0.0) player.seekTo(duration * fraction)
                    },
                    onVolume = {
                        volume = it
                        player.volume(it)
                    },
                )
            }
        }
    }
}

@Composable
private fun SongList(tracks: List<Track>, current: Track?, onPlay: (Int) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(tracks) { index, track ->
            TrackRow(
                track = track,
                subtitle = track.artist ?: track.album.orEmpty(),
                playing = current?.id == track.id,
                onClick = { onPlay(index) },
            )
        }
    }
}

@Composable
private fun AlbumList(albums: List<Album>, onOpen: (Album) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(albums) { album ->
            EntryRow(
                title = album.name,
                subtitle =
                    buildString {
                        album.artist?.let { append(it) }
                        append(" · ")
                        append(
                            "${album.tracks.size} ${if (album.tracks.size == 1) "song" else "songs"}"
                        )
                        album.year?.let { append(" · $it") }
                    },
                onClick = { onOpen(album) },
            )
        }
    }
}

@Composable
private fun ArtistList(artists: List<Artist>, onOpen: (Artist) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(artists) { artist ->
            EntryRow(
                title = artist.name,
                subtitle =
                    "${artist.albums.size} ${if (artist.albums.size == 1) "album" else "albums"} · " +
                        "${artist.tracks.size} songs",
                onClick = { onOpen(artist) },
            )
        }
    }
}

@Composable
private fun AlbumDetail(
    album: Album,
    current: Track?,
    onBack: () -> Unit,
    onPlay: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        DetailHeader(
            title = album.name,
            subtitle =
                buildString {
                    album.artist?.let { append(it) }
                    album.year?.let { append(" · $it") }
                    append(" · ${formatTime(album.durationSeconds)}")
                },
            onBack = onBack,
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(album.tracks) { index, track ->
                TrackRow(
                    track = track,
                    subtitle = track.title,
                    playing = current?.id == track.id,
                    onClick = { onPlay(index) },
                )
            }
        }
    }
}

@Composable
private fun ArtistDetail(
    artist: Artist,
    current: Track?,
    onBack: () -> Unit,
    onOpenAlbum: (Album) -> Unit,
    onPlay: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        DetailHeader(
            title = artist.name,
            subtitle =
                "${artist.albums.size} ${if (artist.albums.size == 1) "album" else "albums"} · " +
                    "${artist.tracks.size} songs",
            onBack = onBack,
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(artist.albums) { album ->
                EntryRow(
                    title = album.name,
                    subtitle = "${album.tracks.size} songs",
                    onClick = { onOpenAlbum(album) },
                )
            }
            itemsIndexed(artist.tracks) { index, track ->
                TrackRow(
                    track = track,
                    subtitle = track.album.orEmpty(),
                    playing = current?.id == track.id,
                    onClick = { onPlay(index) },
                )
            }
        }
    }
}

@Composable
private fun DetailHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) { Text("\u2039 Back") }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EntryRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowContent(title = title, subtitle = subtitle)
    }
}

@Composable
private fun TrackRow(track: Track, subtitle: String, playing: Boolean, onClick: () -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .background(
                    if (playing) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surface
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowContent(title = track.title, subtitle = subtitle)
    }
}

@Composable
private fun RowScope.RowContent(title: String, subtitle: String) {
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TransportBar(
    current: Track?,
    paused: Boolean,
    position: Double,
    duration: Double,
    volume: Double,
    onPrevious: () -> Unit,
    onTogglePause: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Double) -> Unit,
    onVolume: (Double) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = current?.title ?: "Nothing playing",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(200.dp),
            )

            IconButton(onClick = onPrevious, enabled = current != null) {
                Text(text = "\u23EE", style = MaterialTheme.typography.titleMedium)
            }
            IconButton(onClick = onTogglePause, enabled = current != null) {
                Text(
                    text = if (paused) "\u25B6" else "\u23F8",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            IconButton(onClick = onNext, enabled = current != null) {
                Text(text = "\u23ED", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.width(8.dp))
            Text(text = formatTime(position), style = MaterialTheme.typography.labelSmall)
            Slider(
                value =
                    if (duration > 0.0) (position / duration).toFloat().coerceIn(0f, 1f) else 0f,
                onValueChange = { onSeek(it.toDouble()) },
                enabled = duration > 0.0,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            )
            Text(text = formatTime(duration), style = MaterialTheme.typography.labelSmall)

            Spacer(modifier = Modifier.width(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Text(text = "Vol", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = (volume / 100.0).toFloat(),
                    onValueChange = { onVolume(it.toDouble() * 100.0) },
                    modifier = Modifier.width(110.dp).padding(start = 8.dp),
                )
            }
        }
    }
}

private fun formatTime(seconds: Double): String {
    if (seconds <= 0.0 || !seconds.isFinite()) return "--:--"
    val total = seconds.toLong()
    val minutes = total / 60
    val secs = total % 60
    return if (minutes >= 60) {
        "%d:%02d:%02d".format(minutes / 60, minutes % 60, secs)
    } else {
        "%d:%02d".format(minutes, secs)
    }
}
