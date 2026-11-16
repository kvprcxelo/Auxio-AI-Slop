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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.awt.FileDialog
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.io.ByteArrayInputStream
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.oxycblt.auxio.linux.Settings
import org.oxycblt.auxio.linux.Storage
import org.oxycblt.auxio.linux.music.Album
import org.oxycblt.auxio.linux.music.Artist
import org.oxycblt.auxio.linux.music.ArtworkStore
import org.oxycblt.auxio.linux.music.Dates
import org.oxycblt.auxio.linux.music.Genre
import org.oxycblt.auxio.linux.music.Library
import org.oxycblt.auxio.linux.music.LibraryScanner
import org.oxycblt.auxio.linux.music.MusicLibrary
import org.oxycblt.auxio.linux.music.Playlist
import org.oxycblt.auxio.linux.music.Playlists
import org.oxycblt.auxio.linux.music.Track
import org.oxycblt.auxio.linux.playback.DebugLog
import org.oxycblt.auxio.linux.playback.LoopMode
import org.oxycblt.auxio.linux.playback.MpvPlayer
import org.oxycblt.auxio.linux.playback.PlayQueue
import org.oxycblt.auxio.linux.theme.Brightness
import org.oxycblt.auxio.linux.theme.Desktop
import org.oxycblt.auxio.linux.theme.Matugen
import org.oxycblt.auxio.linux.theme.SchemeType
import org.oxycblt.auxio.linux.theme.ThemeMode
import org.oxycblt.auxio.linux.theme.ThemeSettings
import org.oxycblt.auxio.linux.theme.Themes
import org.oxycblt.auxio.linux.theme.parseColor
import org.oxycblt.auxio.linux.theme.toHexString

private enum class Section(val label: String) {
    Songs("Songs"),
    Albums("Albums"),
    Artists("Artists"),
    Genres("Genres"),
    Playlists("Playlists"),
}

/** A short-lived line of feedback: scan results, playback failures. */
private data class Message(val text: String, val error: Boolean = false)

@Composable
fun AuxioApp(
    player: MpvPlayer,
    library: MusicLibrary,
    roots: List<Path>,
    initialSeparators: String = Settings.DEFAULT_SEPARATORS,
    initialTheme: ThemeSettings = Settings.theme(),
) {
    var songs by remember { mutableStateOf(library.songs) }
    var playlists by remember { mutableStateOf(library.playlists) }
    var separators by remember { mutableStateOf(initialSeparators) }
    var dateFormat by remember { mutableStateOf(Settings.dateFormat()) }
    var ignorePunctuation by remember { mutableStateOf(Settings.ignoresLeadingPunctuation()) }
    var collageSeed by remember { mutableStateOf(Settings.collageSeed()) }
    var cacheLimitMb by remember { mutableStateOf(Settings.cacheLimitMb()) }
    var theme by remember { mutableStateOf(initialTheme) }
    var section by remember { mutableStateOf(Section.Songs) }
    var openAlbum by remember { mutableStateOf<Album?>(null) }
    var openArtist by remember { mutableStateOf<Artist?>(null) }
    var openGenre by remember { mutableStateOf<Genre?>(null) }
    var openPlaylist by remember { mutableStateOf<Playlist?>(null) }
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
    var creatingPlaylist by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var playlistMenu by remember { mutableStateOf(false) }

    val desktopAppearance = remember { Desktop.appearance() }
    val desktopWallpaper = remember { Desktop.wallpaper() }
    val matugenAvailable = remember {
        Matugen.isAvailable().also { available ->
            if (available) Matugen.ensureConfig()
        }
    }

    // The first frame is already the real scheme: desktop and custom seeds are built here, and a
    // wallpaper reuses its cached result instead of showing the old palette until matugen finishes.
    var scheme by remember {
        mutableStateOf(Themes.startingScheme(theme, desktopAppearance, desktopWallpaper))
    }

    LaunchedEffect(theme) {
        // A slider reports every pixel; only the value it settles on is worth a new scheme.
        delay(120)
        scheme =
            withContext(Dispatchers.IO) {
                runCatching { Themes.scheme(theme, desktopAppearance, desktopWallpaper) }
                    .getOrDefault(darkColorScheme())
            }
    }

    val separatorChars = remember(separators) { separators.toSet() }
    val currentLibrary =
        remember(songs, separatorChars, ignorePunctuation, playlists) {
            Library.build(songs, separatorChars, ignorePunctuation, playlists)
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

    fun shufflePlay(tracks: List<Track>) {
        val unique = tracks.distinctBy { it.id }
        if (unique.isEmpty()) return

        shuffle = true
        queue.setShuffle(true)
        // Shuffle order is rebuilt around the start track, so start somewhere random.
        play(unique, unique.indices.random())
    }

    fun toggleShuffle() {
        shuffle = !shuffle
        queue.setShuffle(shuffle)
    }

    fun cycleLoop() {
        queue.cycleLoop()
        loop = queue.loop
    }

    fun reloadPlaylists() {
        scope.launch {
            val found = withContext(Dispatchers.IO) { Playlists.load(roots, songs) }
            playlists = found
        }
    }

    fun createPlaylist() {
        val file = Playlists.create(newPlaylistName)
        creatingPlaylist = false
        newPlaylistName = ""
        if (file == null) {
            notify("Could not create that playlist", error = true)
        } else {
            notify("Created ${'$'}{file.fileName}")
            reloadPlaylists()
        }
    }

    fun importPlaylist() {
        playlistMenu = false
        val source = choosePlaylistFile() ?: return
        val imported = Playlists.importFrom(source)
        if (imported == null) {
            notify("Could not import ${'$'}{source.fileName}", error = true)
        } else {
            notify("Imported ${'$'}{imported.fileName}")
            reloadPlaylists()
        }
    }

    if (creatingPlaylist) {
        NewPlaylistDialog(
            name = newPlaylistName,
            scheme = scheme,
            onName = { newPlaylistName = it },
            onCreate = ::createPlaylist,
            onDismiss = { creatingPlaylist = false },
        )
    }

    /** Rebuilds the library; [full] reads every file instead of reusing cached tags. */
    fun reload(full: Boolean) {
        if (scanning) return

        scanning = true
        notify(if (full) "Scanning every file…" else "Reloading from cached tags…")
        scope.launch {
            val result =
                withContext(Dispatchers.IO) {
                    runCatching {
                        LibraryScanner.scan(
                            roots,
                            useCache = !full,
                            cacheFile = Storage.libraryCache,
                        )
                    }
                }
            result
                .onSuccess { found ->
                    songs = found.songs
                    playlists = found.playlists
                    notify("Found ${found.songs.size} songs")
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

    val visibleGenres =
        remember(currentLibrary, query) {
            if (query.isBlank()) currentLibrary.genres
            else currentLibrary.genres.filter { it.name.contains(query, ignoreCase = true) }
        }

    val visiblePlaylists =
        remember(currentLibrary, query) {
            if (query.isBlank()) currentLibrary.playlists
            else currentLibrary.playlists.filter { it.name.contains(query, ignoreCase = true) }
        }

    MaterialTheme(colorScheme = scheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Surface is the near-neutral Material role, which reads as grey whatever seed
                // is chosen. The toolbar uses the seed's container colour instead, so changing
                // the scheme is visible there too.
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
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
                            colors =
                                OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    unfocusedTextColor =
                                        MaterialTheme.colorScheme.onPrimaryContainer,
                                    focusedBorderColor =
                                        MaterialTheme.colorScheme.onPrimaryContainer,
                                    unfocusedBorderColor =
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                            alpha = 0.6f
                                        ),
                                    focusedLabelColor =
                                        MaterialTheme.colorScheme.onPrimaryContainer,
                                    unfocusedLabelColor =
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                            alpha = 0.8f
                                        ),
                                    cursorColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
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
                            color =
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                        )
                        IconButton(onClick = { showSettings = !showSettings }) {
                            Icon(
                                imageVector = AuxioIcons.Settings,
                                contentDescription = "Settings",
                            )
                        }
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
                            cacheLimitMb = cacheLimitMb,
                            onCacheLimitMb = {
                                cacheLimitMb = it
                                Settings.cacheLimitMb(it)
                                // Dropping the limit should free the space straight away.
                                scope.launch(Dispatchers.IO) { ArtworkStore.enforceLimit() }
                            },
                            onReload = { reload(full = false) },
                            onFullReload = { reload(full = true) },
                            theme = theme,
                            desktopAccent = desktopAppearance.accent,
                            desktopWallpaper = desktopWallpaper,
                            matugenAvailable = matugenAvailable,
                            onTheme = {
                                theme = it
                                Settings.theme(it)
                            },
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
                        val genre = openGenre
                        val playlist = openPlaylist
                        when {
                            album != null ->
                                AlbumDetail(
                                    album = album,
                                    current = current,
                                    dateFormat = dateFormat,
                                    onBack = { openAlbum = null },
                                    onPlay = { index -> play(album.tracks, index) },
                                    onShuffle = { shufflePlay(album.tracks) },
                                    modifier = contentModifier,
                                )
                            artist != null ->
                                ArtistDetail(
                                    artist = artist,
                                    current = current,
                                    collageSeed = collageSeed,
                                    dateFormat = dateFormat,
                                    onBack = { openArtist = null },
                                    onOpenAlbum = { openAlbum = it },
                                    onPlay = { index -> play(artist.tracks, index) },
                                    onShuffle = { shufflePlay(artist.tracks) },
                                    modifier = contentModifier,
                                )
                            genre != null ->
                                TrackCollectionDetail(
                                    title = genre.name,
                                    subtitle = count(genre.tracks.size, "song"),
                                    tracks = genre.tracks,
                                    current = current,
                                    onBack = { openGenre = null },
                                    onPlay = { index -> play(genre.tracks, index) },
                                    onShuffle = { shufflePlay(genre.tracks) },
                                    modifier = contentModifier,
                                )
                            playlist != null ->
                                TrackCollectionDetail(
                                    title = playlist.name,
                                    subtitle = count(playlist.tracks.size, "song"),
                                    tracks = playlist.tracks,
                                    current = current,
                                    onBack = { openPlaylist = null },
                                    onPlay = { index -> play(playlist.tracks, index) },
                                    onShuffle = { shufflePlay(playlist.tracks) },
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

                                    Box(modifier = Modifier.fillMaxSize()) {
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
                                            Section.Genres ->
                                                GenreList(
                                                    genres = visibleGenres,
                                                    onOpen = { openGenre = it },
                                                    modifier = Modifier.fillMaxSize(),
                                                )
                                            Section.Playlists ->
                                                if (visiblePlaylists.isEmpty()) {
                                                    EmptyPlaylists(
                                                        menuOpen = playlistMenu,
                                                        onToggleMenu = {
                                                            playlistMenu = !playlistMenu
                                                        },
                                                        onCreate = {
                                                            playlistMenu = false
                                                            newPlaylistName = ""
                                                            creatingPlaylist = true
                                                        },
                                                        onImport = ::importPlaylist,
                                                        modifier = Modifier.fillMaxSize(),
                                                    )
                                                } else {
                                                    PlaylistList(
                                                        playlists = visiblePlaylists,
                                                        onOpen = { openPlaylist = it },
                                                        modifier = Modifier.fillMaxSize(),
                                                    )
                                                }
                                        }

                                        when {
                                            section == Section.Playlists &&
                                                visiblePlaylists.isNotEmpty() ->
                                                Column(
                                                    modifier =
                                                        Modifier.align(Alignment.BottomEnd)
                                                            .padding(20.dp),
                                                    horizontalAlignment = Alignment.End,
                                                ) {
                                                    if (playlistMenu) {
                                                        PlaylistActionsMenu(
                                                            onCreate = {
                                                                playlistMenu = false
                                                                newPlaylistName = ""
                                                                creatingPlaylist = true
                                                            },
                                                            onImport = ::importPlaylist,
                                                        )
                                                        Spacer(modifier = Modifier.height(12.dp))
                                                    }
                                                    FloatingActionButton(
                                                        onClick = { playlistMenu = !playlistMenu },
                                                        shape = RoundedCornerShape(20.dp),
                                                        containerColor =
                                                            MaterialTheme.colorScheme
                                                                .primaryContainer,
                                                    ) {
                                                        Icon(
                                                            imageVector = AuxioIcons.Plus,
                                                            contentDescription = "Playlist actions",
                                                        )
                                                    }
                                                }
                                            section != Section.Playlists &&
                                                sectionTracks(
                                                        section,
                                                        visibleSongs,
                                                        visibleAlbums,
                                                        visibleArtists,
                                                        visibleGenres,
                                                        visiblePlaylists,
                                                    )
                                                    .isNotEmpty() ->
                                                FloatingActionButton(
                                                    onClick = {
                                                        shufflePlay(
                                                            sectionTracks(
                                                                section,
                                                                visibleSongs,
                                                                visibleAlbums,
                                                                visibleArtists,
                                                                visibleGenres,
                                                                visiblePlaylists,
                                                            )
                                                        )
                                                    },
                                                    modifier =
                                                        Modifier.align(Alignment.BottomEnd)
                                                            .padding(20.dp),
                                                    shape = RoundedCornerShape(20.dp),
                                                    containerColor =
                                                        MaterialTheme.colorScheme.primaryContainer,
                                                ) {
                                                    Icon(
                                                        imageVector = AuxioIcons.Shuffle,
                                                        contentDescription =
                                                            "Shuffle ${'$'}{section.label.lowercase()}",
                                                    )
                                                }
                                        }
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

/** One of the three colour sources, with the line that explains what it does. */
@Composable
private fun ThemeModeRow(mode: ThemeMode, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onSelect)
                .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = mode.label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = mode.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ColorSwatch(color: Color) {
    Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(color))
}

/** Songs a tab root can shuffle: everything the current list stands for. */
private fun sectionTracks(
    section: Section,
    songs: List<Track>,
    albums: List<Album>,
    artists: List<Artist>,
    genres: List<Genre>,
    playlists: List<Playlist>,
): List<Track> =
    when (section) {
        Section.Songs -> songs
        Section.Albums -> albums.flatMap { it.tracks }
        Section.Artists -> artists.flatMap { it.tracks }
        Section.Genres -> genres.flatMap { it.tracks }
        Section.Playlists -> playlists.flatMap { it.tracks }
    }

/** The empty playlists tab: the placeholder from the design, with a way out of it. */
@Composable
private fun EmptyPlaylists(
    menuOpen: Boolean,
    onToggleMenu: () -> Unit,
    onCreate: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = AuxioIcons.Playlist,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Your playlists will show up here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (menuOpen) {
            PlaylistActionsMenu(onCreate = onCreate, onImport = onImport)
            Spacer(modifier = Modifier.height(8.dp))
        }
        Button(onClick = onToggleMenu) {
            Text("New playlist")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = AuxioIcons.ArrowDropDown, contentDescription = null)
        }
    }
}

@Composable
private fun NewPlaylistDialog(
    name: String,
    scheme: ColorScheme,
    onName: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    // The dialog is a window of its own: without the theme it falls back to the light default.
    MaterialTheme(colorScheme = scheme) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("New playlist") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = onName,
                    singleLine = true,
                    label = { Text("Name") },
                )
            },
            confirmButton = {
                TextButton(onClick = onCreate, enabled = name.isNotBlank()) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        )
    }
}

/** The two ways to get a playlist into the library, opened from the corner button. */
@Composable
private fun PlaylistActionsMenu(onCreate: () -> Unit, onImport: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
    ) {
        Column(modifier = Modifier.width(220.dp).padding(vertical = 8.dp)) {
            MenuEntry(text = "Create new playlist", onClick = onCreate)
            MenuEntry(text = "Import playlist", onClick = onImport)
        }
    }
}

@Composable
private fun MenuEntry(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

/** Native file picker for an `.m3u`/`.m3u8` file; null when cancelled or off a desktop. */
private fun choosePlaylistFile(): Path? {
    if (GraphicsEnvironment.isHeadless()) return null

    val dialog = FileDialog(null as Frame?, "Import playlist", FileDialog.LOAD)
    dialog.setFilenameFilter { _, fileName ->
        fileName.endsWith(".m3u", ignoreCase = true) ||
            fileName.endsWith(".m3u8", ignoreCase = true)
    }
    dialog.isVisible = true

    val directory = dialog.directory ?: return null
    val file = dialog.file ?: return null
    return Path.of(directory, file)
}

@Composable
private fun SettingsPage(
    separators: String,
    dateFormat: String,
    ignorePunctuation: Boolean,
    collageSeed: String,
    cacheLimitMb: Long,
    scanning: Boolean,
    songCount: Int,
    onSeparators: (String) -> Unit,
    onDateFormat: (String) -> Unit,
    onIgnorePunctuation: (Boolean) -> Unit,
    onCollageSeed: (String) -> Unit,
    onCacheLimitMb: (Long) -> Unit,
    theme: ThemeSettings,
    desktopAccent: Color?,
    desktopWallpaper: Path?,
    matugenAvailable: Boolean,
    onTheme: (ThemeSettings) -> Unit,
    onReload: () -> Unit,
    onFullReload: () -> Unit,
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
            SettingsHeading("Appearance", first = true)

            SettingsLabel("Brightness")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Brightness.entries.forEach { brightness ->
                    FilterChip(
                        selected = theme.brightness == brightness,
                        onClick = { onTheme(theme.copy(brightness = brightness)) },
                        label = { Text(brightness.label) },
                    )
                }
            }
            SettingsNote("System follows your desktop's own light or dark preference.")

            Spacer(modifier = Modifier.size(16.dp))
            SettingsLabel("Colour source")
            ThemeMode.entries.forEach { mode ->
                ThemeModeRow(
                    mode = mode,
                    selected = theme.mode == mode,
                    onSelect = { onTheme(theme.copy(mode = mode)) },
                )
            }

            Spacer(modifier = Modifier.size(8.dp))
            when (theme.mode) {
                ThemeMode.DESKTOP ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ColorSwatch(desktopAccent ?: Desktop.DEFAULT_ACCENT)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text =
                                desktopAccent?.let { "Desktop accent ${it.toHexString()}" }
                                    ?: "No desktop accent found, so Auxio's own is used.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                ThemeMode.MATUGEN -> {
                    OutlinedTextField(
                        value = theme.wallpaper,
                        onValueChange = { onTheme(theme.copy(wallpaper = it)) },
                        singleLine = true,
                        label = { Text("Wallpaper") },
                        placeholder = {
                            Text(desktopWallpaper?.toString() ?: "No wallpaper detected")
                        },
                        modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text =
                            if (matugenAvailable) {
                                "matugen reads the wallpaper's colours. Leave the field empty to " +
                                    "use the one the desktop reports. Its configuration lives in " +
                                    "~/.config/matugen/config.toml, which Auxio creates if missing."
                            } else {
                                "matugen is not installed, so the desktop accent is used instead. " +
                                    "Install it to follow a wallpaper."
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            if (matugenAvailable) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.error,
                        modifier = Modifier.widthIn(max = 720.dp),
                    )
                }
                ThemeMode.CUSTOM -> {
                    val seed = theme.seedColor
                    val red = (seed.red * 255f).roundToInt()
                    val green = (seed.green * 255f).roundToInt()
                    val blue = (seed.blue * 255f).roundToInt()

                    SettingsLabel("Seed colour")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ColorSwatch(seed)
                        Spacer(modifier = Modifier.width(12.dp))
                        var hex by remember(theme.seed) { mutableStateOf(theme.seed) }
                        OutlinedTextField(
                            value = hex,
                            onValueChange = { typed ->
                                hex = typed
                                parseColor(typed)?.let {
                                    onTheme(theme.copy(seed = it.toHexString()))
                                }
                            },
                            singleLine = true,
                            label = { Text("Hex") },
                            isError = parseColor(hex) == null,
                            modifier = Modifier.width(200.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Accepted as #rrggbb.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.size(8.dp))
                    ChannelSlider("Red", red) { value ->
                        onTheme(
                            theme.copy(
                                seed = Color(value / 255f, green / 255f, blue / 255f).toHexString()
                            )
                        )
                    }
                    ChannelSlider("Green", green) { value ->
                        onTheme(
                            theme.copy(
                                seed = Color(red / 255f, value / 255f, blue / 255f).toHexString()
                            )
                        )
                    }
                    ChannelSlider("Blue", blue) { value ->
                        onTheme(
                            theme.copy(
                                seed = Color(red / 255f, green / 255f, value / 255f).toHexString()
                            )
                        )
                    }

                    Spacer(modifier = Modifier.size(8.dp))
                    SettingsLabel("Palette")
                    SchemeType.entries.toList().chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { type ->
                                FilterChip(
                                    selected = theme.type == type,
                                    onClick = { onTheme(theme.copy(type = type)) },
                                    label = { Text(type.label) },
                                )
                            }
                        }
                    }
                }
            }

            SettingsDivider()
            SettingsHeading("Library")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onReload, enabled = !scanning) {
                    Icon(
                        imageVector = AuxioIcons.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reload")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = onFullReload, enabled = !scanning) {
                    Text("Full reload")
                }
            }
            Spacer(modifier = Modifier.size(8.dp))
            SettingsNote(
                if (scanning) {
                    "Scanning your music folders…"
                } else {
                    "Reload reuses the tags cached by the last scan and only reads new or " +
                        "changed files. Full reload re-reads every file, even unchanged ones."
                }
            )

            SettingsDivider()
            SettingsHeading("Metadata")

            SettingsLabel("Artist separators")
            OutlinedTextField(
                value = separators,
                onValueChange = { onSeparators(it.filterNot { char -> char.isWhitespace() }) },
                singleLine = true,
                label = { Text("Characters, for example ; / , &") },
                modifier = Modifier.width(360.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            SettingsNote(
                "An artist tag containing any of these characters is split into separate " +
                    "artists, so \"A; B\" lists under both A and B. Repeated names are dropped " +
                    "regardless of case. Changes apply immediately and are saved."
            )

            Spacer(modifier = Modifier.size(16.dp))
            SettingsLabel("Album date format")
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
            SettingsNote(
                "Tokens: YYYY (2024), YY (24), MM (05), DD (03). Anything else is kept " +
                    "literally, so YYYYMMDD and DD.MM.YY both work. Dates come from the " +
                    "album's date tag; a tag with only a year always shows the year."
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
                        modifier = Modifier.widthIn(max = 700.dp),
                    )
                }
            }

            SettingsDivider()
            SettingsHeading("Storage")

            SettingsLabel("Cover cache limit")
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = cacheLimitMb.toString(),
                    onValueChange = { typed ->
                        typed.filter(Char::isDigit).take(6).toLongOrNull()?.let(onCacheLimitMb)
                    },
                    singleLine = true,
                    label = { Text("Megabytes") },
                    suffix = { Text("MB") },
                    modifier = Modifier.width(200.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Default ${Settings.DEFAULT_CACHE_LIMIT_MB} MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
            SettingsNote(
                "Covers pulled out of tags and folders are kept in ~/.cache/auxio so a scan " +
                    "does not read every file again. Once the cache passes this size, the least " +
                    "recently stored covers are deleted. Nothing is lost: a deleted cover is read " +
                    "from its file the next time it is shown."
            )

            SettingsDivider()
            SettingsHeading("Advanced")

            SettingsLabel("Artist artwork seed")
            OutlinedTextField(
                value = collageSeed,
                onValueChange = onCollageSeed,
                singleLine = true,
                label = { Text("Seed") },
                modifier = Modifier.width(360.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            SettingsNote(
                "Artist rows and headers show a collage of the artist's album covers, shuffled " +
                    "with this seed and the artist's name. The same seed always gives the same " +
                    "arrangement; change it to reshuffle every artist."
            )

            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

/** A settings section title; the first one skips the gap the dividers would have added. */
@Composable
private fun SettingsHeading(text: String, first: Boolean = false) {
    if (!first) Spacer(modifier = Modifier.size(8.dp))
    Text(text = text, style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.size(8.dp))
}

@Composable
private fun SettingsDivider() {
    Spacer(modifier = Modifier.size(16.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.size(16.dp))
}

@Composable
private fun SettingsLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.size(8.dp))
}

/** Wrapped notes so a long line does not run the whole width of a wide window. */
@Composable
private fun SettingsNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.widthIn(max = 720.dp),
    )
}

/** One sRGB channel, named, with its value beside the slider. */
@Composable
private fun ChannelSlider(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(56.dp),
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 0f..255f,
            modifier = Modifier.width(360.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = value.toString(), style = MaterialTheme.typography.bodySmall)
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
    onShuffle: () -> Unit,
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

        Spacer(modifier = Modifier.size(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
        Spacer(modifier = Modifier.size(16.dp))

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
    current: Track?,
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

            item {
                Text(
                    text = "Songs",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            itemsIndexed(artist.tracks) { index, track ->
                TrackRow(
                    track = track,
                    subtitle = track.album.orEmpty(),
                    playing = current?.id == track.id,
                    artwork = rememberArtwork(listOf(track)),
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
                    ArtworkStore.bytes(track)?.let { decodeArtwork(it) }
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
    val progress = if (duration > 0.0) (position / duration).toFloat() else 0f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Equal weights either side keep the transport buttons centred in the bar.
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cover(bitmap = cover, size = 56.dp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = current?.title ?: "Nothing playing",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val subtitle = current?.artist ?: current?.album.orEmpty()
                        if (subtitle.isNotEmpty()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleShuffle) {
                        Icon(
                            imageVector = AuxioIcons.Shuffle,
                            contentDescription = if (shuffle) "Shuffle on" else "Shuffle off",
                            tint =
                                if (shuffle) MaterialTheme.colorScheme.primary
                                else LocalContentColor.current,
                        )
                    }
                    IconButton(onClick = onPrevious, enabled = current != null) {
                        Icon(
                            imageVector = AuxioIcons.SkipPrevious,
                            contentDescription = "Previous",
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onTogglePause,
                        enabled = current != null,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.padding(horizontal = 4.dp).size(52.dp),
                    ) {
                        Icon(
                            imageVector = if (paused) AuxioIcons.PlayArrow else AuxioIcons.Pause,
                            contentDescription = if (paused) "Play" else "Pause",
                        )
                    }
                    IconButton(onClick = onNext, enabled = current != null) {
                        Icon(imageVector = AuxioIcons.SkipNext, contentDescription = "Next")
                    }
                    IconButton(onClick = onCycleLoop) {
                        Icon(
                            imageVector =
                                if (loop == LoopMode.ONE) AuxioIcons.RepeatOne
                                else AuxioIcons.Repeat,
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
                }

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Vol",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = (volume / 100.0).toFloat(),
                        onValueChange = { onVolume(it.toDouble() * 100.0) },
                        modifier = Modifier.width(168.dp).padding(start = 8.dp, end = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            SeekBar(
                progress = progress,
                enabled = duration > 0.0,
                onSeek = { fraction -> onSeek(fraction.toDouble()) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun GenreList(
    genres: List<Genre>,
    onOpen: (Genre) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(genres) { genre ->
            EntryRow(
                title = genre.name,
                subtitle = count(genre.tracks.size, "song"),
                artwork = rememberArtwork(genre.tracks),
                onClick = { onOpen(genre) },
            )
        }
    }
}

@Composable
private fun PlaylistList(
    playlists: List<Playlist>,
    onOpen: (Playlist) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(playlists) { playlist ->
            EntryRow(
                title = playlist.name,
                subtitle = count(playlist.tracks.size, "song"),
                artwork = rememberArtwork(playlist.tracks),
                onClick = { onOpen(playlist) },
            )
        }
    }
}

/** Genre and playlist pages: a cover from the collection, then its songs. */
@Composable
private fun TrackCollectionDetail(
    title: String,
    subtitle: String,
    tracks: List<Track>,
    current: Track?,
    onBack: () -> Unit,
    onPlay: (Int) -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        DetailHeader(
            title = title,
            subtitle = subtitle,
            cover = rememberArtwork(tracks),
            reserveCover = true,
            onBack = onBack,
        )

        Spacer(modifier = Modifier.size(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
        Spacer(modifier = Modifier.size(16.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(tracks) { index, track ->
                TrackRow(
                    track = track,
                    subtitle = listOfNotNull(track.artist, track.album).joinToString(" · "),
                    playing = current?.id == track.id,
                    artwork = rememberArtwork(listOf(track)),
                    onClick = { onPlay(index) },
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
