package com.singleoriginapps.animatables

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * A large, centered bar waveform driven by caller-supplied normalized [levels].
 *
 * Supply an immutable snapshot of audio amplitudes or frequency bands, ordered left to right.
 * Samples are linearly resampled to [barCount] (3..96); empty input is silence, NaN is zero, and
 * values are clamped to 0..1. The component never records, requests permissions, fabricates
 * samples or advances a clock. The caller owns audio capture, sampling and pause/stop controls.
 * [active] false shows a quiet baseline. [label] should describe the actual state, localized by
 * the caller; amplitude changes do not generate accessibility announcements.
 *
 * Fast theme effects smooth incoming data without a bouncy spatial spring. Animation values are
 * read only in draw. Disabled motion shows a fixed active profile or inactive baseline, never a
 * continuing waveform. Set [modifier] to size the presentation; the default is 240 by 72 dp.
 */
@Composable
public fun AudioWaveform(
    levels: List<Float>,
    active: Boolean,
    modifier: Modifier = Modifier,
    label: String = if (active) "Audio active" else "Audio inactive",
    color: Color = MaterialTheme.colorScheme.primary,
    barCount: Int = 32,
) {
    require(barCount in 3..96) { "barCount must be between 3 and 96" }
    val enabled = rememberMotionEnabled()
    val samples = audioSamples(levels, barCount)
    val bars = List(barCount) { index ->
        val target = when {
            !active -> 0f
            !enabled -> .2f + .5f * sin(PI * index / (barCount - 1)).toFloat()
            else -> samples[index]
        }
        rememberAudioLevel(target, enabled)
    }
    Canvas(modifier.size(240.dp, 72.dp).clearAndSetSemantics { contentDescription = label }) {
        val slot = size.width / barCount
        val stroke = minOf(4.dp.toPx(), slot * .6f, size.height)
        bars.forEachIndexed { index, bar ->
            val height = stroke + (size.height - stroke).coerceAtLeast(0f) * normalizedFraction(bar.value)
            val x = slot * (index + .5f)
            drawLine(color, Offset(x, center.y - (height - stroke) / 2),
                Offset(x, center.y + (height - stroke) / 2), stroke, StrokeCap.Round)
        }
    }
}

@Composable
internal fun rememberAudioLevel(target: Float, enabled: Boolean): State<Float> = animateFloatAsState(
    targetValue = normalizedFraction(target),
    animationSpec = if (enabled) MaterialTheme.motionScheme.fastEffectsSpec() else snap(),
    label = "Audio level",
)

internal fun audioSamples(levels: List<Float>, count: Int): List<Float> = List(count) { index ->
    if (levels.isEmpty()) 0f
    else {
        val position = index.toFloat() * (levels.size - 1) / (count - 1).coerceAtLeast(1)
        val start = position.toInt().coerceIn(levels.indices)
        val end = (start + 1).coerceAtMost(levels.lastIndex)
        val fraction = position - start
        normalizedFraction(levels[start]) * (1 - fraction) + normalizedFraction(levels[end]) * fraction
    }
}
