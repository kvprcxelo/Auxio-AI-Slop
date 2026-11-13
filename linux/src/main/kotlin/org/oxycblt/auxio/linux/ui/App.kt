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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.ByteArrayInputStream
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.oxycblt.auxio.linux.Settings
import org.oxycblt.auxio.linux.music.Album
import org.oxycblt.auxio.linux.music.Artist
import org.oxycblt.auxio.linux.music.Artwork
import org.oxycblt.auxio.linux.music.Dates
import org.oxycblt.auxio.linux.music.Library
import org.oxycblt.auxio.linux.music.LibraryScanner
import org.oxycblt.auxio.linux.music.MusicLibrary
import org.oxycblt.auxio.linux.music.Track
import org.oxycblt.auxio.linux.playback.DebugLog
import org.oxycblt.auxio.linux.playback.LoopMode
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
    var dateFormat by remember { mutableStateOf(Settings.dateFormat()) }
    var ignorePunctuation by remember { mutableStateOf(Settings.ignoresLeadingPunctuation()) }
    var collageSeed by remember { mutableStateOf(Settings.collageSeed()) }
    var section by remember { mutableStateOf(Section.Songs) }
    var openAlbum by remember { mutableStateOf<Album?>(null) }
    var openArtist by remember { mutableStateOf<Artist?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var shuffle by remember { mutableStateOf(false) }
    var loop by remember { mutableStateOf(LoopMode.OFF) }
    var current by remember { mutableStateOf<Track?>(null) }
    var position by remember { mutableStateOf(0.0) }
    var duration by remember { mutableStateOf(0.0) }
    var paused by remember { mutableStateOf(true) }
    var volume by remember { mutableStateOf(100.0) }
    var query by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<Message?>(null) }

    val separatorChars = remember(separators) { separators.toSet() }
    val currentLibrary =
        remember(songs, separatorChars, ignorePunctuation) {
            Library.build(songs, separatorChars, ignorePunctuation)
        }
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

    fun toggleShuffle() {
        shuffle = !shuffle
        queue.setShuffle(shuffle)
    }

    fun cycleLoop() {
        queue.cycleLoop()
        loop = queue.loop
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
            // mpv reports pause=false while idle, so an empty queue is paused by definition.
            paused = queue.current == null || player.isPaused()
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
                            count(currentLibrary.songs.size, "song") +
                                " · " +
                                count(currentLibrary.albums.size, "album") +
                                " · " +
                                count(currentLibrary.artists.size, "artist"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(onClick = { showSettings = !showSettings }) {
                        Icon(imageVector = AuxioIcons.Settings, contentDescription = "Settings")
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
                            dateFormat = dateFormat,
                            ignorePunctuation = ignorePunctuation,
                            collageSeed = collageSeed,
                            scanning = scanning,
                            songCount = songs.size,
                            onSeparators = {
                                separators = it
                                Settings.separators(it)
                            },
                            onDateFormat = {
                                dateFormat = it
                                Settings.dateFormat(it)
                            },
                            onIgnorePunctuation = {
                                ignorePunctuation = it
                                Settings.ignoresLeadingPunctuation(it)
                            },
                            onCollageSeed = {
                                collageSeed = it
                                Settings.collageSeed(it)
                            },
                            onRefresh = ::refresh,
                            onBack = { showSettings = false },
                            modifier = contentModifier,
                        )
                    currentLibrary.songs.isEmpty() ->
                        Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
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
                                    dateFormat = dateFormat,
                                    onBack = { openAlbum = null },
                                    onPlay = { index -> play(album.tracks, index) },
                                    modifier = contentModifier,
                                )
                            artist != null ->
                                ArtistDetail(
                                    artist = artist,
                                    collageSeed = collageSeed,
                                    dateFormat = dateFormat,
                                    onBack = { openArtist = null },
                                    onOpenAlbum = { openAlbum = it },
                                    onPlay = { index -> play(artist.tracks, index) },
                                    onShuffle = {
                                        shuffle = true
                                        queue.setShuffle(true)
                                        play(artist.tracks, 0)
                                    },
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
                                                dateFormat = dateFormat,
                                                onOpen = { openAlbum = it },
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        Section.Artists ->
                                            ArtistList(
                                                artists = visibleArtists,
                                                collageSeed = collageSeed,
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
                    shuffle = shuffle,
                    loop = loop,
                    onToggleShuffle = ::toggleShuffle,
                    onCycleLoop = ::cycleLoop,
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
    dateFormat: String,
    ignorePunctuation: Boolean,
    collageSeed: String,
    scanning: Boolean,
    songCount: Int,
    onSeparators: (String) -> Unit,
    onDateFormat: (String) -> Unit,
    onIgnorePunctuation: (Boolean) -> Unit,
    onCollageSeed: (String) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        DetailHeader(title = "Settings", subtitle = "$songCount songs", onBack = onBack)

        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(text = "Library", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.size(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onRefresh, enabled = !scanning) {
                    Icon(
                        imageVector = AuxioIcons.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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
                        "artists, so \"A; B\" lists under both A and B. Repeated names are " +
                        "dropped regardless of case. Changes apply immediately and are saved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(720.dp),
            )

            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = "Album date format",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.size(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = Dates.resolvePattern(dateFormat),
                    onValueChange = { typed ->
                        onDateFormat(typed.trim().ifEmpty { Dates.SYSTEM })
                    },
                    singleLine = true,
                    label = { Text("Pattern, for example DD/MM/YYYY") },
                    modifier = Modifier.width(360.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                TextButton(onClick = { onDateFormat(Dates.SYSTEM) }) { Text("Use system format") }
            }
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text =
                    "Tokens: YYYY (2024), YY (24), MM (05), DD (03). Anything else is kept " +
                        "literally, so YYYYMMDD and DD.MM.YY both work. Dates come from the " +
                        "album's date tag; a tag with only a year always shows the year.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(720.dp),
            )

            Spacer(modifier = Modifier.size(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = ignorePunctuation, onCheckedChange = onIgnorePunctuation)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Ignore leading punctuation",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text =
                            "Sort \"*Song\" or \"(Song)\" by the first letter or digit instead of " +
                                "the symbol.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(700.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.size(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.size(16.dp))

            Text(text = "Advanced", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "Artist artwork seed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.size(8.dp))
            OutlinedTextField(
                value = collageSeed,
                onValueChange = onCollageSeed,
                singleLine = true,
                label = { Text("Seed") },
                modifier = Modifier.width(360.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text =
                    "Artist rows and headers show a collage of the artist's album covers, " +
                        "shuffled with this seed and the artist's name. The same seed always " +
                        "gives the same arrangement; change it to reshuffle every artist.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(720.dp),
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
                artwork = rememberArtwork(listOf(track)),
                onClick = { onPlay(index) },
            )
        }
    }
}

@Composable
private fun AlbumList(
    albums: List<Album>,
    dateFormat: String,
    onOpen: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(albums) { album ->
            EntryRow(
                title = album.name,
                subtitle = albumSubtitle(album, dateFormat),
                artwork = rememberArtwork(album.tracks),
                onClick = { onOpen(album) },
            )
        }
    }
}

@Composable
private fun ArtistList(
    artists: List<Artist>,
    collageSeed: String,
    onOpen: (Artist) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(artists) { artist ->
            ArtistRow(
                artist = artist,
                collageSeed = collageSeed,
                subtitle =
                    count(artist.albums.size, "album") + " · " + count(artist.tracks.size, "song"),
                onClick = { onOpen(artist) },
            )
        }
    }
}

@Composable
private fun ArtistRow(
    artist: Artist,
    collageSeed: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtistCollage(artist = artist, seed = collageSeed, size = 40.dp, circle = true)
        Spacer(modifier = Modifier.width(12.dp))
        RowContent(title = artist.name, subtitle = subtitle)
    }
}

private fun albumSubtitle(album: Album, dateFormat: String): String = buildString {
    album.artist?.let { append(it) }
    append(" · ")
    append(count(album.tracks.size, "song"))
    Dates.format(album.date ?: album.year?.toString(), dateFormat)?.let { append(" · $it") }
}

@Composable
private fun AlbumDetail(
    album: Album,
    current: Track?,
    dateFormat: String,
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
                    Dates.format(album.date ?: album.year?.toString(), dateFormat)?.let {
                        append(" · $it")
                    }
                    append(" · ${formatTime(album.durationSeconds)}")
                },
            cover = cover,
            reserveCover = true,
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

/**
 * An artist profile: a large circular collage of their albums, the counts, play and shuffle for the
 * whole artist, and the albums themselves.
 */
@Composable
private fun ArtistDetail(
    artist: Artist,
    collageSeed: String,
    dateFormat: String,
    onBack: () -> Unit,
    onOpenAlbum: (Album) -> Unit,
    onPlay: (Int) -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = AuxioIcons.ArrowBack, contentDescription = "Back")
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ArtistCollage(
                        artist = artist,
                        seed = collageSeed,
                        size = 200.dp,
                        circle = true,
                    )
                    Spacer(modifier = Modifier.size(20.dp))
                    Text(
                        text = "Artist",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Music",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text =
                            count(artist.albums.size, "album") +
                                " • " +
                                count(artist.tracks.size, "song"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = { onPlay(0) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 12.dp),
                        ) {
                            Icon(imageVector = AuxioIcons.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play")
                        }
                        OutlinedButton(
                            onClick = onShuffle,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 12.dp),
                        ) {
                            Icon(imageVector = AuxioIcons.Shuffle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Shuffle")
                        }
                    }
                }

                Text(
                    text = "Albums",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            items(artist.albums) { album ->
                EntryRow(
                    title = album.name,
                    subtitle =
                        Dates.format(album.date ?: album.year?.toString(), dateFormat).orEmpty(),
                    artwork = rememberArtwork(album.tracks),
                    onClick = { onOpenAlbum(album) },
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
    reserveCover: Boolean = cover != null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) {
            Icon(imageVector = AuxioIcons.ArrowBack, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Back")
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (reserveCover) {
            Cover(bitmap = cover, size = 72.dp)
            Spacer(modifier = Modifier.width(12.dp))
        }
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
            Icon(
                imageVector = AuxioIcons.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Artist artwork: up to four album covers in a grid, ordered by a shuffle seeded with the setting's
 * seed and the artist's name, so the arrangement looks random but is stable between launches.
 */
@Composable
internal fun ArtistCollage(
    artist: Artist,
    seed: String,
    size: Dp,
    circle: Boolean = false,
) {
    val albums = remember(artist.name, seed) { collageAlbums(artist, seed) }
    val covers = albums.take(4).mapNotNull { rememberArtwork(it.tracks) }
    val shape = if (circle) CircleShape else RoundedCornerShape(6.dp)

    // One album (or one cover): fill the whole shape rather than half of a 2x2 grid.
    if (covers.size <= 1) {
        Box(
            modifier =
                Modifier.size(size)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            val image = covers.firstOrNull()
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = AuxioIcons.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    Box(
        modifier =
            Modifier.size(size).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            covers.chunked(2).forEach { row ->
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    row.forEach { image ->
                        if (image != null) {
                            Image(
                                bitmap = image,
                                contentDescription = null,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                    // An odd cover count leaves a gap rather than stretching the last one.
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

private fun collageAlbums(artist: Artist, seed: String): List<Album> {
    val albums = artist.albums
    if (albums.size <= 1) return albums
    val random = Random(seed.hashCode().toLong() * 31L + artist.name.lowercase().hashCode())
    return albums.shuffled(random)
}

/** Decoded covers, keyed by the file the picture came from, so scrolling does not re-read them. */
private object ArtworkCache {
    private const val MAX_ENTRIES = 256

    private val images =
        object : LinkedHashMap<String, ImageBitmap?>(64, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, ImageBitmap?>?
            ): Boolean = size > MAX_ENTRIES
        }

    fun contains(key: String): Boolean = synchronized(images) { images.containsKey(key) }

    fun get(key: String): ImageBitmap? = synchronized(images) { images[key] }

    fun put(key: String, image: ImageBitmap?) {
        synchronized(images) { images[key] = image }
    }
}

/** First cover found among [tracks], decoded once and then cached. */
@Composable
private fun rememberArtwork(tracks: List<Track>): ImageBitmap? {
    val key = tracks.firstOrNull()?.path?.toAbsolutePath()?.toString() ?: return null
    var image by
        remember(key) {
            mutableStateOf(if (ArtworkCache.contains(key)) ArtworkCache.get(key) else null)
        }

    LaunchedEffect(key) {
        if (ArtworkCache.contains(key)) return@LaunchedEffect

        val found =
            withContext(Dispatchers.IO) {
                tracks.firstNotNullOfOrNull { track ->
                    Artwork.bytes(track)?.let { decodeArtwork(it) }
                }
            }

        ArtworkCache.put(key, found)
        if (found == null) {
            DebugLog.write("no artwork for ${tracks.first().path}")
        }
        image = found
    }

    return image
}

private fun decodeArtwork(bytes: ByteArray): ImageBitmap? =
    runCatching { ImageIO.read(ByteArrayInputStream(bytes))?.toComposeImageBitmap() }.getOrNull()

@Composable
private fun EntryRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    artwork: ImageBitmap? = null,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (artwork != null) {
            Cover(bitmap = artwork, size = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
        }
        RowContent(title = title, subtitle = subtitle)
    }
}

@Composable
private fun TrackRow(
    track: Track,
    subtitle: String,
    playing: Boolean,
    onClick: () -> Unit,
    artwork: ImageBitmap? = null,
) {
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
        if (artwork != null) {
            Cover(bitmap = artwork, size = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
        }
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
    shuffle: Boolean,
    loop: LoopMode,
    onToggleShuffle: () -> Unit,
    onCycleLoop: () -> Unit,
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
                modifier = Modifier.width(150.dp),
            )

            IconButton(onClick = onCycleLoop) {
                Icon(
                    imageVector =
                        if (loop == LoopMode.ONE) AuxioIcons.RepeatOne else AuxioIcons.Repeat,
                    contentDescription =
                        when (loop) {
                            LoopMode.OFF -> "Repeat off"
                            LoopMode.ALL -> "Repeat all"
                            LoopMode.ONE -> "Repeat one"
                        },
                    tint =
                        if (loop != LoopMode.OFF) MaterialTheme.colorScheme.primary
                        else LocalContentColor.current,
                )
            }
            IconButton(onClick = onPrevious, enabled = current != null) {
                Icon(imageVector = AuxioIcons.SkipPrevious, contentDescription = "Previous")
            }
            IconButton(onClick = onTogglePause, enabled = current != null) {
                Icon(
                    imageVector = if (paused) AuxioIcons.PlayArrow else AuxioIcons.Pause,
                    contentDescription = if (paused) "Play" else "Pause",
                )
            }
            IconButton(onClick = onNext, enabled = current != null) {
                Icon(imageVector = AuxioIcons.SkipNext, contentDescription = "Next")
            }
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    imageVector = AuxioIcons.Shuffle,
                    contentDescription = if (shuffle) "Shuffle on" else "Shuffle off",
                    tint =
                        if (shuffle) MaterialTheme.colorScheme.primary
                        else LocalContentColor.current,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Text(text = formatTime(position), style = MaterialTheme.typography.labelSmall)
            WaveProgress(
                progress = if (duration > 0.0) (position / duration).toFloat() else 0f,
                enabled = duration > 0.0,
                playing = current != null && !paused,
                onSeek = { fraction -> onSeek(fraction.toDouble()) },
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
                    modifier = Modifier.width(100.dp).padding(start = 12.dp, end = 4.dp),
                )
            }
        }
    }
}

private fun count(value: Int, singular: String): String =
    "$value ${if (value == 1) singular else "${singular}s"}"

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
