package com.singleoriginapps.animatables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/**
 * A level-reactive halo around an existing circular recording button supplied as [content].
 *
 * Reserves a constant 12 dp on every side so changes never resize the button or move surrounding
 * controls. [level] is real caller-supplied amplitude, normalized to 0..1 (clamped; NaN is zero).
 * [active] false leaves a quiet outline. Fast theme effects smooth samples; disabled motion uses
 * a fixed active ring or inactive outline. There is no decorative loop or audio capture here.
 *
 * The halo neither handles input nor adds semantics: the caller's button keeps its native focus,
 * click handling, touch target and localized recording/muted label. Provide a stop/pause control
 * for recording sessions. Size the button inside content; this wrapper adds only the halo space.
 */
@Composable
public fun AudioActivityRing(
    level: Float,
    active: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    content: @Composable BoxScope.() -> Unit,
) {
    val enabled = rememberMotionEnabled()
    val amplitude = rememberAudioLevel(if (!active) 0f else if (!enabled) .5f else level, enabled)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize().clearAndSetSemantics {}) {
            val fraction = normalizedFraction(amplitude.value)
            val stroke = 2.dp.toPx()
            val room = (size.minDimension / 2 - stroke).coerceAtLeast(0f)
            val radius = (room - 6.dp.toPx() + 4.dp.toPx() * fraction).coerceIn(0f, room)
            drawCircle(
                color.copy(alpha = color.alpha * (if (active) .12f + .12f * fraction else .08f)),
                radius = room
            )
            drawCircle(
                color.copy(alpha = color.alpha * (if (active) .6f + .4f * fraction else .3f)),
                radius = radius, style = Stroke(stroke + 2.dp.toPx() * fraction)
            )
        }
        Box(Modifier.padding(12.dp), contentAlignment = Alignment.Center, content = content)
    }
}
