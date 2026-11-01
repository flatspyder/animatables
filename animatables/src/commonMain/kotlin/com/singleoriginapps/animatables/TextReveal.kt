package com.singleoriginapps.animatables

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Reveals a short group of related lines with a staggered rise, fade, and soft focus resolve.
 *
 * Use for an occasional introduction or result, not every row of a frequently visited screen.
 * Changing [replayKey] deliberately replays the reveal; changing [lines] starts a new group.
 * The full text is accessible immediately through one semantic node and is never announced per
 * frame. Each line wraps normally. Android below API 31 keeps the rise/fade without blur.
 * Other supported targets use Compose/Skia soft focus.
 * System or app-disabled motion displays every line immediately, without stagger delays.
 */
@Composable
public fun TextReveal(
    lines: List<String>,
    modifier: Modifier = Modifier,
    replayKey: Any = Unit,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val enabled = rememberMotionEnabled()
    val lift = with(LocalDensity.current) { 12.dp.toPx() }
    val blur = with(LocalDensity.current) { 3.dp.toPx() }
    Column(
        modifier = modifier.clearAndSetSemantics {
            text = AnnotatedString(lines.joinToString("\n"))
        },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        key(replayKey, lines) {
            lines.forEachIndexed { index, line ->
                key(index) {
                    val progress = remember { Animatable(if (enabled) 0f else 1f) }
                    LaunchedEffect(enabled) {
                        if (enabled) {
                            val scale =
                                coroutineContext[androidx.compose.ui.MotionDurationScale]?.scaleFactor
                                    ?: 1f
                            val stagger = (index * ReferenceRevealTokens.LineStaggerMillis)
                                .coerceAtMost(ReferenceRevealTokens.MaximumLineStaggerMillis)
                            if (scale > 0f) delay((stagger * scale).toLong())
                            progress.animateTo(1f, ReferenceRevealTokens.RevealSpec)
                        } else {
                            progress.snapTo(1f)
                        }
                    }
                    Text(
                        text = line,
                        style = style,
                        modifier = Modifier.graphicsLayer {
                            val fraction = if (enabled) progress.value.coerceIn(0f, 1f) else 1f
                            alpha = fraction
                            translationY = lift * (1f - fraction)
                            renderEffect = motionBlurEffect(blur * (1f - fraction))
                        },
                    )
                }
            }
        }
    }
}
