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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import java.awt.FileDialog
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.io.ByteArrayInputStream
import java.nio.file.Files
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
import org.oxycblt.auxio.linux.music.DeletedPlaylist
import org.oxycblt.auxio.linux.music.Genre
import org.oxycblt.auxio.linux.music.Library
import org.oxycblt.auxio.linux.music.LibraryScanner
import org.oxycblt.auxio.linux.music.MusicLibrary
import org.oxycblt.auxio.linux.music.Playlist
import org.oxycblt.auxio.linux.music.Playlists
import org.oxycblt.auxio.linux.music.RemovedSong
import org.oxycblt.auxio.linux.music.SortMode
import org.oxycblt.auxio.linux.music.Sorting
import org.oxycblt.auxio.linux.music.Track
import org.oxycblt.auxio.linux.playback.DebugLog
import org.oxycblt.auxio.linux.playback.LoopMode
import org.oxycblt.auxio.linux.playback.PlayQueue
import org.oxycblt.auxio.linux.playback.Player
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

/** A removed song that the notice can still put back. */
private data class Undo(
    val removal: RemovedSong,
    val playlistPath: Path,
    val title: String,
)

@Composable
fun AuxioApp(
    player: Player,
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
    var addingSongsTo by remember { mutableStateOf<Playlist?>(null) }
    var selectedSongs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingUndo by remember { mutableStateOf<Undo?>(null) }
    var pendingDeleteUndo by remember { mutableStateOf<DeletedPlaylist?>(null) }
    var deletingPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var sortMode by remember { mutableStateOf(SortMode.NAME_ASC) }
    // Playlist sorting only changes the page until a drag; null means the file's order.
    var playlistSort by remember { mutableStateOf<SortMode?>(null) }
    var sortMenu by remember { mutableStateOf(false) }
    var transportHeightPx by remember { mutableStateOf(0) }

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
    fun notify(
        text: String,
        error: Boolean = false,
        durationMillis: Long = if (error) 8000 else 4000,
    ) {
        val next = Message(text, error)
        pendingUndo = null
        pendingDeleteUndo = null
        message = next
        scope.launch {
            delay(durationMillis)
            if (message == next) {
                message = null
                pendingUndo = null
                pendingDeleteUndo = null
            }
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
            notify("Created ${file.fileName}")
            reloadPlaylists()
        }
    }

    fun importPlaylist() {
        playlistMenu = false
        val source = choosePlaylistFile() ?: return
        if (Playlists.isAlreadyLoaded(source, playlists)) {
            reloadPlaylists()
            notify("${source.fileName} is already in your library")
            return
        }
        val imported = Playlists.importFrom(source)
        if (imported == null) {
            notify("Could not import ${source.fileName}", error = true)
        } else {
            notify("Imported ${imported.fileName}")
            reloadPlaylists()
        }
    }

    /** Appends the picked songs to the playlist being edited and reloads the library's lists. */
    fun addSongs() {
        val target = addingSongsTo ?: return
        val chosen = songs.filter { it.id in selectedSongs }
        addingSongsTo = null
        selectedSongs = emptySet()
        if (chosen.isEmpty()) return

        scope.launch {
            val added = withContext(Dispatchers.IO) { Playlists.addTracks(target, chosen) }
            val found = withContext(Dispatchers.IO) { Playlists.load(roots, songs) }
            playlists = found
            if (openPlaylist?.path == target.path) {
                openPlaylist = found.firstOrNull { it.path == target.path }
            }
            when {
                added < 0 -> notify("Could not add songs to ${target.name}", error = true)
                added == 0 -> notify("Those songs are already in ${target.name}")
                else -> notify("Added ${count(added, "song")} to ${target.name}")
            }
        }
    }

    /** Rewrites the playlist without [track], then refreshes both the list and the open page. */
    fun removeTrack(playlist: Playlist, track: Track) {
        pendingDeleteUndo = null
        scope.launch {
            val removal = withContext(Dispatchers.IO) { Playlists.removeTrack(playlist, track) }
            val found = withContext(Dispatchers.IO) { Playlists.load(roots, songs) }
            playlists = found
            if (openPlaylist?.path == playlist.path) {
                // A playlist outside the managed folder stops being listed once its last song is
                // gone; keep its page open anyway, so the empty state and Undo stay in view.
                openPlaylist =
                    found.firstOrNull { it.path == playlist.path }
                        ?: if (removal != null) {
                            playlist.copy(
                                tracks = playlist.tracks.filterNot { it.path == track.path }
                            )
                        } else {
                            playlist
                        }
            }
            if (removal == null) {
                notify("Could not remove ${track.title} from ${playlist.name}", error = true)
            } else {
                // Longer than a normal notice, so a misclick has time to be undone.
                notify("Removed ${track.title} from ${playlist.name}", durationMillis = 10_000)
                pendingUndo = Undo(removal, playlist.path, track.title)
            }
        }
    }

    /** Puts the last removed song back, at the line it came from. */
    fun undoRemoval() {
        val pending = pendingUndo ?: return
        pendingUndo = null
        scope.launch {
            val restored = withContext(Dispatchers.IO) { Playlists.undoRemove(pending.removal) }
            val found = withContext(Dispatchers.IO) { Playlists.load(roots, songs) }
            playlists = found
            if (openPlaylist?.path == pending.playlistPath) {
                openPlaylist = found.firstOrNull { it.path == pending.playlistPath }
            }
            if (restored) notify("Restored ${pending.title}")
            else notify("Could not restore ${pending.title}", error = true)
        }
    }

    /** Deletes the playlist file and leaves its page, keeping enough to undo the deletion. */
    fun deletePlaylist(playlist: Playlist) {
        deletingPlaylist = null
        scope.launch {
            val deleted = withContext(Dispatchers.IO) { Playlists.delete(playlist) }
            val found = withContext(Dispatchers.IO) { Playlists.load(roots, songs) }
            playlists = found
            if (deleted != null) {
                if (openPlaylist?.path == playlist.path) {
                    openPlaylist = null
                    playlistSort = null
                }
                notify("Deleted ${playlist.name}", durationMillis = 10_000)
                pendingDeleteUndo = deleted
            } else {
                notify("Could not delete ${playlist.name}", error = true)
            }
        }
    }

    /** Puts a deleted playlist's file back. */
    fun undoDelete() {
        val deleted = pendingDeleteUndo ?: return
        pendingDeleteUndo = null
        scope.launch {
            val restored = withContext(Dispatchers.IO) { Playlists.restore(deleted) }
            val found = withContext(Dispatchers.IO) { Playlists.load(roots, songs) }
            playlists = found
            if (restored) notify("Restored ${deleted.name}")
            else notify("Could not restore ${deleted.name}", error = true)
        }
    }

    /**
     * Reorders the open playlist to exactly the order the page showed after a drop and persists it.
     *
     * The caller passes the whole visible list instead of indices: a sort can change between the
     * row being composed and the drop, and index arithmetic against a stale list would save the
     * wrong entries. Dropping while sorted still writes the visible order and returns the page to
     * Playlist order.
     */
    fun moveTrack(playlist: Playlist, ordered: List<Track>) {
        if (ordered == playlist.tracks) return

        val updated = playlist.copy(tracks = ordered)
        playlistSort = null
        if (openPlaylist?.path == playlist.path) openPlaylist = updated
        playlists = playlists.map { if (it.path == playlist.path) updated else it }

        scope.launch {
            val saved = withContext(Dispatchers.IO) { Playlists.reorder(updated) }
            val found = withContext(Dispatchers.IO) { Playlists.load(roots, songs) }
            playlists = found
            if (openPlaylist?.path == updated.path) {
                openPlaylist = found.firstOrNull { it.path == updated.path } ?: updated
            }
            if (!saved) notify("Could not save the new order", error = true)
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

    deletingPlaylist?.let { target ->
        DeletePlaylistDialog(
            name = target.name,
            scheme = scheme,
            onDelete = { deletePlaylist(target) },
            onDismiss = { deletingPlaylist = null },
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

    val orderedSongs =
        remember(visibleSongs, sortMode, ignorePunctuation) {
            Sorting.tracks(visibleSongs, sortMode, ignorePunctuation)
        }
    val orderedAlbums =
        remember(visibleAlbums, sortMode, ignorePunctuation) {
            Sorting.albums(visibleAlbums, sortMode, ignorePunctuation)
        }
    val orderedArtists =
        remember(visibleArtists, sortMode, ignorePunctuation) {
            Sorting.artists(visibleArtists, sortMode, ignorePunctuation)
        }
    val orderedGenres =
        remember(visibleGenres, sortMode, ignorePunctuation) {
            Sorting.genres(visibleGenres, sortMode, ignorePunctuation)
        }
    val orderedPlaylists =
        remember(visiblePlaylists, sortMode, ignorePunctuation) {
            Sorting.playlists(visiblePlaylists, sortMode, ignorePunctuation)
        }

    val datesAvailable =
        !showSettings &&
            when {
                openPlaylist != null -> true
                openAlbum != null || openArtist != null || openGenre != null -> true
                else -> section == Section.Songs || section == Section.Albums
            }

    val rootFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { rootFocus.requestFocus() } }

    MaterialTheme(colorScheme = scheme) {
        Box(
            modifier =
                Modifier.fillMaxSize().focusRequester(rootFocus).focusable().onPreviewKeyEvent {
                    event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                        when {
                            sortMenu -> {
                                sortMenu = false
                                true
                            }
                            playlistMenu -> {
                                playlistMenu = false
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                }
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
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
                            if (!showSettings) {
                                IconButton(
                                    onClick = {
                                        playlistMenu = false
                                        sortMenu = !sortMenu
                                    }
                                ) {
                                    Icon(
                                        imageVector = AuxioIcons.Sort,
                                        contentDescription = "Sort",
                                    )
                                }
                            }
                            IconButton(onClick = { showSettings = !showSettings }) {
                                Icon(
                                    imageVector = AuxioIcons.Settings,
                                    contentDescription = "Settings",
                                )
                            }
                        }
                        // Clicks anywhere in the top bar put an open playlist menu away.
                        if (playlistMenu) {
                            MenuScrim(
                                modifier = Modifier.matchParentSize(),
                                onDismiss = { playlistMenu = false },
                            )
                        }
                    }

                    // The content takes the space left over by the transport bar. Without the
                    // weight
                    // its fillMaxSize() would consume the whole column and push the bar out of
                    // view.
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
                                album != null -> {
                                    val tracks =
                                        remember(album.tracks, sortMode, ignorePunctuation) {
                                            Sorting.tracks(
                                                album.tracks,
                                                sortMode,
                                                ignorePunctuation,
                                            )
                                        }
                                    AlbumDetail(
                                        album = album.copy(tracks = tracks),
                                        current = current,
                                        dateFormat = dateFormat,
                                        onBack = { openAlbum = null },
                                        onPlay = { index -> play(tracks, index) },
                                        onShuffle = { shufflePlay(tracks) },
                                        modifier = contentModifier,
                                    )
                                }
                                artist != null -> {
                                    val albums =
                                        remember(artist.albums, sortMode, ignorePunctuation) {
                                            Sorting.albums(
                                                artist.albums,
                                                sortMode,
                                                ignorePunctuation,
                                            )
                                        }
                                    val tracks =
                                        remember(artist.tracks, sortMode, ignorePunctuation) {
                                            Sorting.tracks(
                                                artist.tracks,
                                                sortMode,
                                                ignorePunctuation,
                                            )
                                        }
                                    ArtistDetail(
                                        artist = artist.copy(albums = albums, tracks = tracks),
                                        current = current,
                                        collageSeed = collageSeed,
                                        dateFormat = dateFormat,
                                        onBack = { openArtist = null },
                                        onOpenAlbum = { openAlbum = it },
                                        onPlay = { index -> play(tracks, index) },
                                        onShuffle = { shufflePlay(tracks) },
                                        modifier = contentModifier,
                                    )
                                }
                                genre != null -> {
                                    val tracks =
                                        remember(genre.tracks, sortMode, ignorePunctuation) {
                                            Sorting.tracks(
                                                genre.tracks,
                                                sortMode,
                                                ignorePunctuation,
                                            )
                                        }
                                    TrackCollectionDetail(
                                        title = genre.name,
                                        subtitle = count(tracks.size, "song"),
                                        tracks = tracks,
                                        current = current,
                                        onBack = { openGenre = null },
                                        onPlay = { index -> play(tracks, index) },
                                        onShuffle = { shufflePlay(tracks) },
                                        modifier = contentModifier,
                                    )
                                }
                                playlist != null -> {
                                    // Sorting is a temporary view, but a drag adopts the visible
                                    // order as the playlist's real order.
                                    val tracks =
                                        remember(
                                            playlist.tracks,
                                            playlistSort,
                                            ignorePunctuation,
                                        ) {
                                            playlistSort?.let {
                                                Sorting.tracks(
                                                    playlist.tracks,
                                                    it,
                                                    ignorePunctuation,
                                                )
                                            } ?: playlist.tracks
                                        }
                                    TrackCollectionDetail(
                                        title = playlist.name,
                                        subtitle = count(tracks.size, "song"),
                                        tracks = tracks,
                                        onAddSongs = {
                                            addingSongsTo = playlist
                                            selectedSongs = emptySet()
                                        },
                                        onRemove = { track -> removeTrack(playlist, track) },
                                        onMove = { visibleOrder ->
                                            moveTrack(playlist, visibleOrder)
                                        },
                                        onDelete = { deletingPlaylist = playlist },
                                        coverTracks = playlist.tracks,
                                        current = current,
                                        onBack = {
                                            openPlaylist = null
                                            playlistSort = null
                                        },
                                        onPlay = { index -> play(tracks, index) },
                                        onShuffle = { shufflePlay(tracks) },
                                        modifier = contentModifier,
                                    )
                                }
                                else -> {
                                    Column(modifier = contentModifier) {
                                        PrimaryTabRow(selectedTabIndex = section.ordinal) {
                                            Section.entries.forEach { entry ->
                                                Tab(
                                                    selected = section == entry,
                                                    onClick = {
                                                        section = entry
                                                        playlistMenu = false
                                                        sortMenu = false
                                                        if (
                                                            entry != Section.Songs &&
                                                                entry != Section.Albums &&
                                                                sortMode.isDate
                                                        ) {
                                                            sortMode = SortMode.NAME_ASC
                                                        }
                                                    },
                                                    text = { Text(entry.label) },
                                                )
                                            }
                                        }

                                        Box(modifier = Modifier.fillMaxSize()) {
                                            when (section) {
                                                Section.Songs ->
                                                    SongList(
                                                        tracks = orderedSongs,
                                                        current = current,
                                                        onPlay = { index ->
                                                            play(orderedSongs, index)
                                                        },
                                                        modifier = Modifier.fillMaxSize(),
                                                    )
                                                Section.Albums ->
                                                    AlbumList(
                                                        albums = orderedAlbums,
                                                        dateFormat = dateFormat,
                                                        onOpen = { openAlbum = it },
                                                        modifier = Modifier.fillMaxSize(),
                                                    )
                                                Section.Artists ->
                                                    ArtistList(
                                                        artists = orderedArtists,
                                                        collageSeed = collageSeed,
                                                        onOpen = { openArtist = it },
                                                        modifier = Modifier.fillMaxSize(),
                                                    )
                                                Section.Genres ->
                                                    GenreList(
                                                        genres = orderedGenres,
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
                                                            onDismissMenu = {
                                                                playlistMenu = false
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
                                                            playlists = orderedPlaylists,
                                                            onOpen = {
                                                                openPlaylist = it
                                                                playlistSort = null
                                                                sortMenu = false
                                                            },
                                                            modifier = Modifier.fillMaxSize(),
                                                        )
                                                    }
                                            }

                                            // Clicking the page behind the menu closes it; the menu
                                            // and its button draw above this layer.
                                            if (
                                                playlistMenu &&
                                                    section == Section.Playlists &&
                                                    visiblePlaylists.isNotEmpty()
                                            ) {
                                                MenuScrim(onDismiss = { playlistMenu = false })
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
                                                            Spacer(
                                                                modifier = Modifier.height(12.dp)
                                                            )
                                                        }
                                                        FloatingActionButton(
                                                            onClick = {
                                                                playlistMenu = !playlistMenu
                                                            },
                                                            shape = RoundedCornerShape(20.dp),
                                                            containerColor =
                                                                MaterialTheme.colorScheme
                                                                    .primaryContainer,
                                                        ) {
                                                            Icon(
                                                                imageVector = AuxioIcons.Plus,
                                                                contentDescription =
                                                                    "Playlist actions",
                                                            )
                                                        }
                                                    }
                                                section != Section.Playlists &&
                                                    sectionTracks(
                                                            section,
                                                            orderedSongs,
                                                            orderedAlbums,
                                                            orderedArtists,
                                                            orderedGenres,
                                                            orderedPlaylists,
                                                        )
                                                        .isNotEmpty() ->
                                                    FloatingActionButton(
                                                        onClick = {
                                                            shufflePlay(
                                                                sectionTracks(
                                                                    section,
                                                                    orderedSongs,
                                                                    orderedAlbums,
                                                                    orderedArtists,
                                                                    orderedGenres,
                                                                    orderedPlaylists,
                                                                )
                                                            )
                                                        },
                                                        modifier =
                                                            Modifier.align(Alignment.BottomEnd)
                                                                .padding(20.dp),
                                                        shape = RoundedCornerShape(20.dp),
                                                        containerColor =
                                                            MaterialTheme.colorScheme
                                                                .primaryContainer,
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

                    Box(
                        modifier =
                            Modifier.fillMaxWidth().onSizeChanged {
                                transportHeightPx = it.height
                            }
                    ) {
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
                        // As with the top bar, clicking the player bar dismisses the menu.
                        if (playlistMenu) {
                            MenuScrim(
                                modifier = Modifier.matchParentSize(),
                                onDismiss = { playlistMenu = false },
                            )
                        }
                    }
                }
            }

            if (sortMenu) {
                MenuScrim(onDismiss = { sortMenu = false })
                val onPlaylistPage = openPlaylist != null
                SortMenu(
                    selected = if (onPlaylistPage) playlistSort else sortMode,
                    datesAvailable = datesAvailable,
                    includeManual = onPlaylistPage,
                    onSelect = { mode ->
                        if (onPlaylistPage) {
                            playlistSort = mode
                        } else if (mode != null) {
                            sortMode = mode
                        }
                        sortMenu = false
                    },
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 88.dp, end = 16.dp),
                )
            }

            // Feedback floats just above the player bar instead of taking a line of its own.
            message?.let { shown ->
                val bottom = with(LocalDensity.current) { transportHeightPx.toDp() } + 12.dp
                ToastMessage(
                    text = shown.text,
                    error = shown.error,
                    onUndo =
                        when {
                            pendingUndo != null -> ::undoRemoval
                            pendingDeleteUndo != null -> ::undoDelete
                            else -> null
                        },
                    modifier =
                        Modifier.align(Alignment.BottomCenter)
                            .padding(start = 16.dp, end = 16.dp, bottom = bottom),
                )
            }

            addingSongsTo?.let { target ->
                AddSongsDialog(
                    songs = currentLibrary.songs,
                    selected = selectedSongs,
                    scheme = scheme,
                    onToggle = { track, checked ->
                        selectedSongs =
                            if (checked) selectedSongs + track.id else selectedSongs - track.id
                    },
                    onAdd = ::addSongs,
                    onDismiss = {
                        addingSongsTo = null
                        selectedSongs = emptySet()
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
    onDismissMenu: () -> Unit,
    onCreate: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (menuOpen) {
            MenuScrim(onDismiss = onDismissMenu)
        }
        Column(
            modifier = Modifier.fillMaxSize(),
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
            Button(onClick = onToggleMenu) {
                Text("New playlist")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = AuxioIcons.ArrowDropDown, contentDescription = null)
            }
            // The menu hangs under its button rather than covering it.
            if (menuOpen) {
                Spacer(modifier = Modifier.height(8.dp))
                PlaylistActionsMenu(onCreate = onCreate, onImport = onImport)
            }
        }
    }
}

/** The sort choices, shown under the top bar's sort button. */
@Composable
private fun SortMenu(
    selected: SortMode?,
    datesAvailable: Boolean,
    includeManual: Boolean,
    onSelect: (SortMode?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.width(260.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            if (includeManual) {
                SortOption(
                    label = "Playlist order",
                    selected = selected == null,
                    onClick = { onSelect(null) },
                )
            }
            SortMode.entries.forEach { mode ->
                SortOption(
                    label = mode.label,
                    selected = selected == mode,
                    enabled = !mode.isDate || datesAvailable,
                    onClick = { onSelect(mode) },
                )
            }
        }
    }
}

@Composable
private fun SortOption(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Text(
        text = (if (selected) "✓ " else "") + label,
        style = MaterialTheme.typography.bodyMedium,
        color =
            if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        modifier =
            Modifier.fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

/** A small floating notice above the player bar, with Undo when the action can be reversed. */
@Composable
private fun ToastMessage(
    text: String,
    error: Boolean,
    onUndo: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier =
                Modifier.padding(
                    start = 16.dp,
                    end = if (onUndo == null) 16.dp else 8.dp,
                    top = 8.dp,
                    bottom = 8.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (error) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.widthIn(max = 520.dp),
            )
            if (onUndo != null) {
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onUndo) { Text("Undo") }
            }
        }
    }
}

/** Invisible catcher that closes an open playlist menu when the page behind it is clicked. */
@Composable
private fun MenuScrim(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    Box(
        modifier =
            modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
    )
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

/** Songs the picker shows for a search: title, artist or album matches. */
internal fun filterPickerSongs(songs: List<Track>, query: String): List<Track> =
    if (query.isBlank()) {
        songs
    } else {
        songs.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.artist?.contains(query, ignoreCase = true) == true ||
                it.album?.contains(query, ignoreCase = true) == true
        }
    }

/** Picks songs from the library and appends them to the playlist being edited. */
@Composable
internal fun AddSongsDialog(
    songs: List<Track>,
    selected: Set<String>,
    scheme: ColorScheme,
    onToggle: (Track, Boolean) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    // An in-window modal rather than a platform dialog: the song list is long enough to need the
    // room, and it keeps the picker inside the window it belongs to.
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    MaterialTheme(colorScheme = scheme) {
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                            onDismiss()
                            true
                        } else {
                            false
                        }
                    }
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.width(560.dp).clickable {},
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Add songs", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (songs.isEmpty()) {
                        Text("Your library has no songs to add.")
                    } else {
                        var pickerQuery by remember { mutableStateOf("") }
                        val matching =
                            remember(songs, pickerQuery) {
                                filterPickerSongs(songs, pickerQuery)
                            }
                        OutlinedTextField(
                            value = pickerQuery,
                            onValueChange = { pickerQuery = it },
                            singleLine = true,
                            label = { Text("Search songs") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (matching.isEmpty()) {
                            Text(
                                text = "No songs match that search.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                            items(matching) { track ->
                                Row(
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .clickable {
                                                onToggle(track, track.id !in selected)
                                            }
                                            .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = track.id in selected,
                                        onCheckedChange = { onToggle(track, it) },
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = track.title,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text =
                                                listOfNotNull(track.artist, track.album)
                                                    .joinToString(" · "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = onAdd, enabled = selected.isNotEmpty()) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeletePlaylistDialog(
    name: String,
    scheme: ColorScheme,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    MaterialTheme(colorScheme = scheme) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Delete playlist?") },
            text = { Text("Delete $name? Its .m3u file will be removed.") },
            confirmButton = {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
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

/**
 * Native file picker for an `.m3u`/`.m3u8` file; null when cancelled or off a desktop.
 *
 * The GTK/Qt portals are tried first so the dialog is the one the desktop actually uses; AWT's
 * FileDialog is the fallback when neither tool is installed.
 */
private fun choosePlaylistFile(): Path? {
    val zenity = findCommand("zenity")
    val kdialog = if (zenity == null) findCommand("kdialog") else null
    val native =
        when {
            zenity != null ->
                listOf(
                    zenity,
                    "--file-selection",
                    "--title=Import playlist",
                    "--file-filter=Playlist files | *.m3u *.m3u8",
                )
            kdialog != null ->
                listOf(
                    kdialog,
                    "--title",
                    "Import playlist",
                    "--getopenfilename",
                    System.getProperty("user.home") ?: ".",
                    "*.m3u *.m3u8",
                )
            else -> null
        }
    if (native != null) return runNativePicker(native)

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

/** Full path to [name] on PATH, if the desktop has it. */
private fun findCommand(name: String): String? =
    System.getenv("PATH")
        .orEmpty()
        .split(java.io.File.pathSeparator)
        .asSequence()
        .mapNotNull { directory -> runCatching { Path.of(directory, name) }.getOrNull() }
        .firstOrNull { Files.isExecutable(it) }
        ?.toString()

/** Runs a portal picker and returns the chosen file; null on cancel or failure. */
private fun runNativePicker(command: List<String>): Path? =
    runCatching {
            val process = ProcessBuilder(command).start()
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            if (process.waitFor() == 0 && output.isNotEmpty()) {
                Path.of(output.lineSequence().first().trim())
            } else {
                null
            }
        }
        .getOrNull()

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
                        // Compose Desktop draws a dark elevation highlight on hover; the
                        // chip needs no elevation at all.
                        elevation = null,
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
                                    elevation = null,
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
    coverTracks: List<Track>? = null,
    reserveCover: Boolean = cover != null || coverTracks != null,
    onDelete: (() -> Unit)? = null,
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
            if (coverTracks != null) {
                TrackCollage(tracks = coverTracks, size = 72.dp)
            } else {
                Cover(bitmap = cover, size = 72.dp)
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
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
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = AuxioIcons.Delete,
                    contentDescription = "Delete playlist",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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

/** Playlist artwork: the covers of the first four songs, arranged as a compact collage. */
@Composable
private fun TrackCollage(tracks: List<Track>, size: Dp) {
    val covers = tracks.take(4).map { rememberArtwork(listOf(it)) }

    Box(
        modifier =
            Modifier.size(size)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (covers.isEmpty() || covers.all { it == null }) {
            Icon(
                imageVector = AuxioIcons.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            when (covers.size) {
                1 -> CollageTile(covers[0], Modifier.fillMaxSize())
                2 ->
                    Row(modifier = Modifier.fillMaxSize()) {
                        CollageTile(covers[0], Modifier.weight(1f).fillMaxHeight())
                        CollageTile(covers[1], Modifier.weight(1f).fillMaxHeight())
                    }
                3 ->
                    Row(modifier = Modifier.fillMaxSize()) {
                        CollageTile(covers[0], Modifier.weight(1f).fillMaxHeight())
                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            CollageTile(covers[1], Modifier.weight(1f).fillMaxWidth())
                            CollageTile(covers[2], Modifier.weight(1f).fillMaxWidth())
                        }
                    }
                else ->
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            CollageTile(covers[0], Modifier.weight(1f).fillMaxHeight())
                            CollageTile(covers[1], Modifier.weight(1f).fillMaxHeight())
                        }
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            CollageTile(covers[2], Modifier.weight(1f).fillMaxHeight())
                            CollageTile(covers[3], Modifier.weight(1f).fillMaxHeight())
                        }
                    }
            }
        }
    }
}

@Composable
private fun CollageTile(image: ImageBitmap?, modifier: Modifier) {
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Spacer(modifier = modifier)
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
    onRemove: (() -> Unit)? = null,
    dragging: Boolean = false,
    dragOffset: Float = 0f,
    shift: Float = 0f,
    onDragStart: ((Float) -> Unit)? = null,
    onDrag: ((Float) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    onDragCancel: (() -> Unit)? = null,
) {
    var rowHeight by remember { mutableStateOf(1) }
    // The row can stay in place while the sort around it changes, so its pointer handler must read
    // the latest callbacks instead of the ones captured when it was first composed.
    val latestDragStart = rememberUpdatedState(onDragStart)
    val latestDrag = rememberUpdatedState(onDrag)
    val latestDragEnd = rememberUpdatedState(onDragEnd)
    val latestDragCancel = rememberUpdatedState(onDragCancel)
    val animatedShift by
        animateFloatAsState(
            targetValue = if (dragging) 0f else shift,
            animationSpec = tween(durationMillis = 120),
            label = "row shift",
        )
    val verticalOffset = if (dragging) dragOffset else animatedShift
    val cardShape = RoundedCornerShape(12.dp)

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .onSizeChanged { rowHeight = it.height }
                .zIndex(if (dragging) 1f else 0f)
                .offset { IntOffset(0, verticalOffset.roundToInt()) }
                // The whole row lifts out of the list while it is being dragged.
                .then(if (dragging) Modifier.shadow(12.dp, cardShape).clip(cardShape) else Modifier)
                .background(
                    when {
                        dragging -> MaterialTheme.colorScheme.surfaceContainerHigh
                        playing -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    }
                )
                .clickable(onClick = onClick)
                .then(
                    if (onDragStart != null) {
                        Modifier.pointerInput(track.id) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    latestDragStart.value?.invoke(rowHeight.toFloat())
                                },
                                onVerticalDrag = { change, amount ->
                                    change.consume()
                                    latestDrag.value?.invoke(amount)
                                },
                                onDragEnd = { latestDragEnd.value?.invoke() },
                                onDragCancel = { latestDragCancel.value?.invoke() },
                            )
                        }
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (artwork != null) {
            Cover(bitmap = artwork, size = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
        }
        RowContent(title = track.title, subtitle = subtitle)
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = AuxioIcons.Close,
                    contentDescription = "Remove from playlist",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (onDragStart != null) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = AuxioIcons.DragHandle,
                    contentDescription = "Reorder song",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .clickable { onOpen(playlist) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TrackCollage(tracks = playlist.tracks, size = 40.dp)
                Spacer(modifier = Modifier.width(12.dp))
                RowContent(
                    title = playlist.name,
                    subtitle = count(playlist.tracks.size, "song"),
                )
            }
        }
    }
}

/** Genre and playlist pages: a cover from the collection, then its songs. */
@Composable
private fun TrackCollectionDetail(
    title: String,
    subtitle: String,
    tracks: List<Track>,
    onAddSongs: (() -> Unit)? = null,
    onRemove: ((Track) -> Unit)? = null,
    onMove: ((List<Track>) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    coverTracks: List<Track>? = null,
    current: Track?,
    onBack: () -> Unit,
    onPlay: (Int) -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragFrom by remember(tracks) { mutableStateOf<Int?>(null) }
    var dragDistance by remember(tracks) { mutableStateOf(0f) }
    var dragRowHeight by remember(tracks) { mutableStateOf(1f) }

    val dragTo =
        if (tracks.isEmpty()) {
            null
        } else {
            dragFrom?.let { from ->
                (from + dragDistance / dragRowHeight.coerceAtLeast(1f))
                    .roundToInt()
                    .coerceIn(0, tracks.lastIndex)
            }
        }

    fun finishDrag(commit: Boolean) {
        val from = dragFrom ?: return
        val target =
            (from + dragDistance / dragRowHeight.coerceAtLeast(1f))
                .roundToInt()
                .coerceIn(0, tracks.lastIndex)
        dragFrom = null
        dragDistance = 0f
        if (commit && from != target) {
            val visibleOrder = tracks.toMutableList()
            visibleOrder.add(target, visibleOrder.removeAt(from))
            onMove?.invoke(visibleOrder)
        }
    }

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            DetailHeader(
                title = title,
                subtitle = subtitle,
                cover = rememberArtwork(tracks),
                coverTracks = coverTracks,
                reserveCover = true,
                onBack = onBack,
                onDelete = onDelete,
            )

            Spacer(modifier = Modifier.size(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { onPlay(0) },
                    enabled = tracks.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    Icon(imageVector = AuxioIcons.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play")
                }
                OutlinedButton(
                    onClick = onShuffle,
                    enabled = tracks.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    Icon(imageVector = AuxioIcons.Shuffle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Shuffle")
                }
            }
            Spacer(modifier = Modifier.size(16.dp))

            if (tracks.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
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
                        text =
                            if (onAddSongs != null) "This playlist is empty."
                            else "No songs here yet.",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (onAddSongs != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add songs to start listening.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onAddSongs) {
                            Icon(imageVector = AuxioIcons.Plus, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add songs")
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(tracks) { index, track ->
                        val from = dragFrom
                        val to = dragTo
                        val beingDragged = index == from
                        val shift =
                            when {
                                from == null || to == null || beingDragged -> 0f
                                from < to && index > from && index <= to -> -dragRowHeight
                                from > to && index >= to && index < from -> dragRowHeight
                                else -> 0f
                            }
                        TrackRow(
                            track = track,
                            subtitle = listOfNotNull(track.artist, track.album).joinToString(" · "),
                            playing = current?.id == track.id,
                            artwork = rememberArtwork(listOf(track)),
                            onClick = { onPlay(index) },
                            onRemove = onRemove?.let { remove -> { remove(track) } },
                            dragging = beingDragged,
                            dragOffset = if (beingDragged) dragDistance else 0f,
                            shift = shift,
                            onDragStart =
                                onMove?.let {
                                    { height: Float ->
                                        dragFrom = index
                                        dragRowHeight = height
                                        dragDistance = 0f
                                    }
                                },
                            onDrag = onMove?.let { { amount: Float -> dragDistance += amount } },
                            onDragEnd = onMove?.let { { finishDrag(true) } },
                            onDragCancel = onMove?.let { { finishDrag(false) } },
                        )
                    }
                }
            }
        }

        if (onAddSongs != null && tracks.isNotEmpty()) {
            FloatingActionButton(
                onClick = onAddSongs,
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(imageVector = AuxioIcons.Plus, contentDescription = "Add songs")
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
