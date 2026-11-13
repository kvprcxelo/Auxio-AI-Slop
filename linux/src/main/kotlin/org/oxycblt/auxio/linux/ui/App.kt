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

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.ByteArrayInputStream
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.oxycblt.auxio.linux.Settings
import org.oxycblt.auxio.linux.music.Album
import org.oxycblt.auxio.linux.music.Artist
import org.oxycblt.auxio.linux.music.Artwork
import org.oxycblt.auxio.linux.music.Library
import org.oxycblt.auxio.linux.music.LibraryScanner
import org.oxycblt.auxio.linux.music.MusicLibrary
import org.oxycblt.auxio.linux.music.Track
import org.oxycblt.auxio.linux.playback.DebugLog
import org.oxycblt.auxio.linux.playback.MpvPlayer
import org.oxycblt.auxio.linux.playback.PlayQueue

private enum class Section(val label: String) {
    Songs("Songs"),
    Albums("Albums"),
    Artists("Artists"),
}

/** A short-lived line of feedback: scan results, playback failures. */
private data class Message(val text: String, val error: Boolean = false)

@Composable
fun AuxioApp(
    player: MpvPlayer,
    library: MusicLibrary,
    roots: List<Path>,
    initialSeparators: String = Settings.DEFAULT_SEPARATORS,
) {
    var songs by remember { mutableStateOf(library.songs) }
    var separators by remember { mutableStateOf(initialSeparators) }
    var section by remember { mutableStateOf(Section.Songs) }
    var openAlbum by remember { mutableStateOf<Album?>(null) }
    var openArtist by remember { mutableStateOf<Artist?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf<Track?>(null) }
    var position by remember { mutableStateOf(0.0) }
    var duration by remember { mutableStateOf(0.0) }
    var paused by remember { mutableStateOf(true) }
    var volume by remember { mutableStateOf(100.0) }
    var query by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<Message?>(null) }

    val separatorChars = remember(separators) { separators.toSet() }
    val currentLibrary = remember(songs, separatorChars) { Library.build(songs, separatorChars) }
    val queue = remember { PlayQueue(player) }
    val scope = rememberCoroutineScope()

    /** Shows a message and clears it again, so nothing stale is left on screen. */
    fun notify(text: String, error: Boolean = false) {
        val next = Message(text, error)
        message = next
        scope.launch {
            delay(if (error) 8000 else 4000)
            if (message == next) message = null
        }
    }

    fun play(tracks: List<Track>, index: Int) {
        runCatching { queue.setTracks(tracks, index) }
            .onFailure {
                notify(
                    "Could not play ${tracks.getOrNull(index)?.title ?: "track"}: ${it.message}",
                    error = true,
                )
            }
        current = queue.current
    }

    fun step(forward: Boolean) {
        runCatching { if (forward) queue.next() else queue.previous() }
            .onFailure { notify("Could not change track: ${it.message}", error = true) }
        current = queue.current
    }

    fun refresh() {
        if (scanning) return

        scanning = true
        notify("Scanning ${roots.joinToString()}…")
        scope.launch {
            val result =
                withContext(Dispatchers.IO) { runCatching { LibraryScanner.scan(roots).songs } }
            result
                .onSuccess { found ->
                    songs = found
                    notify("Found ${found.size} songs")
                }
                .onFailure { notify("Scan failed: ${it.message}", error = true) }
            scanning = false
        }
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

    val visibleSongs =
        remember(currentLibrary, query) {
            if (query.isBlank()) currentLibrary.songs
            else
                currentLibrary.songs.filter {
                    it.title.contains(query, ignoreCase = true) ||
                        it.artist?.contains(query, ignoreCase = true) == true ||
                        it.album?.contains(query, ignoreCase = true) == true
                }
        }

    val visibleAlbums =
        remember(currentLibrary, query) {
            if (query.isBlank()) currentLibrary.albums
            else
                currentLibrary.albums.filter {
                    it.name.contains(query, ignoreCase = true) ||
                        it.artist?.contains(query, ignoreCase = true) == true
                }
        }

    val visibleArtists =
        remember(currentLibrary, query) {
            if (query.isBlank()) currentLibrary.artists
            else currentLibrary.artists.filter { it.name.contains(query, ignoreCase = true) }
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
                            "${currentLibrary.songs.size} songs · " +
                                "${currentLibrary.albums.size} albums · " +
                                "${currentLibrary.artists.size} artists",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(onClick = { showSettings = !showSettings }) {
                        Text(text = "\u2699", style = MaterialTheme.typography.titleLarge)
                    }
                }

                message?.let { shown ->
                    Text(
                        text = shown.text,
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            if (shown.error) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                }

                // The content takes the space left over by the transport bar. Without the weight
                // its fillMaxSize() would consume the whole column and push the bar out of view.
                val contentModifier = Modifier.fillMaxWidth().weight(1f)

                when {
                    showSettings ->
                        SettingsPage(
                            separators = separators,
                            scanning = scanning,
                            songCount = songs.size,
                            onSeparators = {
                                separators = it
                                Settings.separators(it)
                            },
                            onRefresh = ::refresh,
                            onBack = { showSettings = false },
                            modifier = contentModifier,
                        )
                    currentLibrary.songs.isEmpty() ->
                        Box(
                            modifier = contentModifier,
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No music found in ${roots.joinToString()}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    else -> {
                        val album = openAlbum
                        val artist = openArtist
                        when {
                            album != null ->
                                AlbumDetail(
                                    album = album,
                                    current = current,
                                    onBack = { openAlbum = null },
                                    onPlay = { index -> play(album.tracks, index) },
                                    modifier = contentModifier,
                                )
                            artist != null ->
                                ArtistDetail(
                                    artist = artist,
                                    current = current,
                                    onBack = { openArtist = null },
                                    onOpenAlbum = { openAlbum = it },
                                    onPlay = { index -> play(artist.tracks, index) },
                                    modifier = contentModifier,
                                )
                            else -> {
                                Column(modifier = contentModifier) {
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
                                                tracks = visibleSongs,
                                                current = current,
                                                onPlay = { index -> play(visibleSongs, index) },
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        Section.Albums ->
                                            AlbumList(
                                                albums = visibleAlbums,
                                                onOpen = { openAlbum = it },
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        Section.Artists ->
                                            ArtistList(
                                                artists = visibleArtists,
                                                onOpen = { openArtist = it },
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                    }
                                }
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
private fun SettingsPage(
    separators: String,
    scanning: Boolean,
    songCount: Int,
    onSeparators: (String) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        DetailHeader(title = "Settings", subtitle = "$songCount songs", onBack = onBack)

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(text = "Library", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.size(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onRefresh, enabled = !scanning) {
                    Text(if (scanning) "Scanning…" else "Refresh library")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Rescan your music folders for new or removed files.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.size(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.size(16.dp))

            Text(text = "Metadata", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "Artist separators",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.size(8.dp))
            OutlinedTextField(
                value = separators,
                onValueChange = { onSeparators(it.filterNot { char -> char.isWhitespace() }) },
                singleLine = true,
                label = { Text("Characters, for example ; / , &") },
                modifier = Modifier.width(360.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text =
                    "An artist tag containing any of these characters is split into separate " +
                        "artists, so \"A; B\" lists under both A and B. Albums stay grouped by " +
                        "their raw tag. Changes apply immediately and are saved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SongList(
    tracks: List<Track>,
    current: Track?,
    onPlay: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
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
private fun AlbumList(albums: List<Album>, onOpen: (Album) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        items(albums) { album ->
            EntryRow(
                title = album.name,
                subtitle =
                    buildString {
                        album.artist?.let { append(it) }
                        append(" · ")
                        append(
                            "${album.tracks.size} " +
                                if (album.tracks.size == 1) "song" else "songs"
                        )
                        album.year?.let { append(" · $it") }
                    },
                onClick = { onOpen(album) },
            )
        }
    }
}

@Composable
private fun ArtistList(
    artists: List<Artist>,
    onOpen: (Artist) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(artists) { artist ->
            EntryRow(
                title = artist.name,
                subtitle =
                    "${artist.albums.size} " +
                        (if (artist.albums.size == 1) "album" else "albums") +
                        " · ${artist.tracks.size} songs",
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
    modifier: Modifier = Modifier,
) {
    val cover = rememberArtwork(album.tracks)

    Column(modifier = modifier) {
        DetailHeader(
            title = album.name,
            subtitle =
                buildString {
                    album.artist?.let { append(it) }
                    album.year?.let { append(" · $it") }
                    append(" · ${formatTime(album.durationSeconds)}")
                },
            cover = cover,
            onBack = onBack,
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(album.tracks) { index, track ->
                TrackRow(
                    track = track,
                    subtitle = albumTrackSubtitle(track, album),
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
    modifier: Modifier = Modifier,
) {
    val cover = rememberArtwork(artist.tracks)

    Column(modifier = modifier) {
        DetailHeader(
            title = artist.name,
            subtitle =
                "${artist.albums.size} " +
                    (if (artist.albums.size == 1) "album" else "albums") +
                    " · ${artist.tracks.size} songs",
            cover = cover,
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

/**
 * What an album row says under a title that already names the track: its position, the performing
 * artist when that differs from the album artist (compilations), and its length.
 */
private fun albumTrackSubtitle(track: Track, album: Album): String {
    val parts = mutableListOf<String>()
    track.trackNumber?.let { parts += "#$it" }
    val artist = track.artist
    if (artist != null && !artist.equals(album.artist, ignoreCase = true)) {
        parts += artist
    }
    track.durationSeconds?.takeIf { it > 0.0 }?.let { parts += formatTime(it) }
    return parts.joinToString(" · ")
}

@Composable
private fun DetailHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    cover: ImageBitmap? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) { Text("\u2039 Back") }
        Spacer(modifier = Modifier.width(8.dp))
        Cover(bitmap = cover, size = 72.dp)
        Spacer(modifier = Modifier.width(12.dp))
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

/** Cover placeholder doubles as the layout slot, so sizes stay stable with and without art. */
@Composable
private fun Cover(bitmap: ImageBitmap?, size: Dp) {
    Box(
        modifier =
            Modifier.size(size)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(text = "\u266A", style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** First cover found among [tracks], decoded once per list. */
@Composable
private fun rememberArtwork(tracks: List<Track>): ImageBitmap? {
    var image by remember(tracks) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(tracks) {
        image =
            withContext(Dispatchers.IO) {
                val found = tracks.firstNotNullOfOrNull { track ->
                    Artwork.bytes(track)?.let { decodeArtwork(it) }
                }
                if (found == null) {
                    DebugLog.write("no artwork for ${tracks.firstOrNull()?.path ?: "nothing"}")
                }
                found
            }
    }

    return image
}

private fun decodeArtwork(bytes: ByteArray): ImageBitmap? =
    runCatching { ImageIO.read(ByteArrayInputStream(bytes))?.toComposeImageBitmap() }.getOrNull()

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
    val cover = rememberArtwork(listOfNotNull(current))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Cover(bitmap = cover, size = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = current?.title ?: "Nothing playing",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(180.dp),
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
