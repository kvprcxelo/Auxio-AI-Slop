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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * A seek bar drawn as a travelling wave.
 *
 * The played part is a sine wave that scrolls while music plays, and the rest is a plain line.
 * Tapping or dragging still seeks, so it behaves like the slider it replaces.
 */
@Composable
internal fun WaveProgress(
    progress: Float,
    enabled: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 26.dp,
) {
    val accent = MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.outlineVariant

    val phase =
        if (enabled) {
            val transition = rememberInfiniteTransition(label = "wave")
            val animated by
                transition.animateFloat(
                    initialValue = 0f,
                    targetValue = (2 * PI).toFloat(),
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(durationMillis = 2400, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart,
                        ),
                    label = "phase",
                )
            animated
        } else {
            0f
        }

    Canvas(
        modifier =
            modifier
                .height(height)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        onSeek((offset.x / size.width).coerceIn(0f, 1f))
                    }
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures { change, _ ->
                        onSeek((change.position.x / size.width).coerceIn(0f, 1f))
                    }
                }
    ) {
        val centerY = size.height / 2f
        val amplitude = size.height / 3.5f
        val wavelength = 56.dp.toPx()
        val played = size.width * progress.coerceIn(0f, 1f)

        drawLine(
            color = idle,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )

        if (played <= 0f) return@Canvas

        fun waveY(x: Float): Float =
            centerY + amplitude * sin(x / wavelength * 2f * PI.toFloat() + phase)

        val line = Path().apply { moveTo(0f, waveY(0f)) }
        var x = 0f
        val step = 2f
        while (x < played) {
            x = minOf(x + step, played)
            line.lineTo(x, waveY(x))
        }

        val filled =
            Path().apply {
                addPath(line)
                lineTo(played, centerY + amplitude)
                lineTo(0f, centerY + amplitude)
                close()
            }

        drawPath(filled, accent.copy(alpha = 0.28f))
        drawPath(
            path = line,
            color = accent,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}
