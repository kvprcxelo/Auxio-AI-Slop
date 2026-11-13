/*
 * Copyright (c) 2026 Auxio Project
 * WaveProgress.kt is part of Auxio.
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
import androidx.compose.animation.core.snap
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * The transport's seek bar, drawn with Material 3's expressive wavy progress indicator.
 *
 * Position is polled a few times a second, so the target is animated with Material's own
 * [WavyProgressIndicatorDefaults.ProgressAnimationSpec] to turn those steps into continuous motion.
 * A drag snaps instead: the wave should sit under the pointer, not chase it.
 *
 * Progress indicators do not handle input, so the wave sits inside a taller touch target that turns
 * taps and horizontal drags into a seek fraction.
 */
@Composable
internal fun WaveProgress(
    progress: Float,
    enabled: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clamped = progress.coerceIn(0f, 1f)
    var dragging by remember { mutableStateOf(false) }

    val animated by
        animateFloatAsState(
            targetValue = clamped,
            animationSpec =
                if (dragging) snap() else WavyProgressIndicatorDefaults.ProgressAnimationSpec,
            label = "waveProgress",
        )

    Box(
        modifier =
            modifier
                .height(44.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        onSeek((offset.x / size.width).coerceIn(0f, 1f))
                    }
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                    ) { change, _ ->
                        onSeek((change.position.x / size.width).coerceIn(0f, 1f))
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        LinearWavyProgressIndicator(progress = { animated }, modifier = Modifier.fillMaxWidth())
    }
}
