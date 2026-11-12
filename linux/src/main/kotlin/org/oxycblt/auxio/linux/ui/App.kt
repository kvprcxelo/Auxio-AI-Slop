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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import org.oxycblt.auxio.linux.music.Track
import org.oxycblt.auxio.linux.playback.MpvPlayer

@Composable
fun AuxioApp(player: MpvPlayer, tracks: List<Track>, roots: List<Path>) {
    var current by remember { mutableStateOf<Track?>(null) }
    var position by remember { mutableStateOf(0.0) }
    var duration by remember { mutableStateOf(0.0) }
    var paused by remember { mutableStateOf(true) }
    var volume by remember { mutableStateOf(100.0) }
    var query by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    val visible =
        remember(tracks, query) {
            if (query.isBlank()) tracks
            else
                tracks.filter {
                    it.title.contains(query, ignoreCase = true) ||
                        it.artist?.contains(query, ignoreCase = true) == true
                }
        }

    LaunchedEffect(Unit) {
        while (true) {
            position = player.timePosition()
            duration = player.duration()
            paused = player.isPaused()
            delay(250)
        }
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
                        text = "${visible.size} of ${tracks.size} songs",
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

                if (tracks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No music found in ${roots.joinToString()}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        items(visible) { track ->
                            TrackRow(
                                track = track,
                                playing = current?.path == track.path,
                                onClick = {
                                    current = track
                                    status =
                                        runCatching { player.load(track.path) }
                                            .exceptionOrNull()
                                            ?.let { "Could not play ${track.title}: ${it.message}" }
                                },
                            )
                        }
                    }
                }

                TransportBar(
                    current = current,
                    paused = paused,
                    position = position,
                    duration = duration,
                    volume = volume,
                    onTogglePause = {
                        current?.let {
                            player.togglePause()
                            paused = player.isPaused()
                        }
                    },
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
private fun TrackRow(track: Track, playing: Boolean, onClick: () -> Unit) {
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist ?: track.path.parent?.fileName?.toString().orEmpty(),
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
    onTogglePause: () -> Unit,
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
                modifier = Modifier.width(220.dp),
            )

            IconButton(onClick = onTogglePause, enabled = current != null) {
                Text(
                    text = if (paused) "\u25B6" else "\u23F8",
                    style = MaterialTheme.typography.titleLarge,
                )
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
