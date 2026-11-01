package com.singleoriginapps.animatables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

/**
 * A compact five-bar audio icon, sized to 24 dp by default, driven by normalized [level].
 *
 * [active] false shows a baseline; active silence remains a small readable profile. Supply real
 * samples (0..1, clamped; NaN is zero) and a localized [label] such as "Recording" or "Microphone
 * muted". This is a visual indicator, not a button or microphone controller. When placed inside
 * an already labelled control, pass null for label to make it decorative and keep the host's
 * touch target and semantics. No per-sample announcements are generated. Fast theme effects
 * smooth input; disabled motion shows a fixed active profile or inactive baseline.
 */
@Composable
public fun AudioLevelIcon(
    level: Float,
    active: Boolean,
    modifier: Modifier = Modifier,
    label: String? = if (active) "Audio active" else "Audio inactive",
    color: Color = LocalContentColor.current,
) {
    val enabled = rememberMotionEnabled()
    val amplitude = rememberAudioLevel(if (!active) 0f else if (!enabled) .5f else level, enabled)
    Canvas(
        modifier.size(24.dp)
            .clearAndSetSemantics { if (label != null) contentDescription = label }) {
        val slot = size.width / 5
        val stroke = minOf(2.5.dp.toPx(), slot * .6f, size.height)
        val profile = floatArrayOf(.4f, .75f, 1f, .75f, .4f)
        profile.forEachIndexed { index, weight ->
            val fraction =
                if (active) (.15f + .85f * normalizedFraction(amplitude.value)) * weight else 0f
            val half = (size.height - stroke).coerceAtLeast(0f) * fraction / 2
            val x = slot * (index + .5f)
            drawLine(
                color,
                Offset(x, center.y - half),
                Offset(x, center.y + half),
                stroke,
                StrokeCap.Round
            )
        }
    }
}
