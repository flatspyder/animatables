package com.singleoriginapps.animatables

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A success mark arriving upward through fade, rotation and soft focus, then a drawn check.
 *
 * The reference-derived 500ms entrance travels 40dp, resolves a 10dp blur on Android 12+, and
 * draws the check after an 80ms lead-in. Other supported targets use Compose/Skia blur. It stays full size; scaling no longer drives the event.
 * [replayKey] deliberately repeats completion. Clearing settles quietly into a pending outline.
 * Keep a persistent outcome label nearby, and localize [label] and [pendingLabel]. System/app
 * disabled motion shows the complete resting state immediately, without the stroke delay.
 */
@Composable
public fun SuccessCheck(
    complete: Boolean,
    modifier: Modifier = Modifier,
    replayKey: Any = Unit,
    label: String = "Complete",
    pendingLabel: String = "Not complete",
) {
    val enabled = rememberMotionEnabled()
    val initial = if (complete && !enabled) 1f else 0f
    val appearance = remember { Animatable(initial) }
    val position = remember { Animatable(initial) }
    val pathProgress = remember { Animatable(initial) }
    var lastReplayKey by remember { mutableStateOf(replayKey) }
    val clearing = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    LaunchedEffect(complete, replayKey, enabled) {
        val target = if (complete) 1f else 0f
        val replay = replayKey != lastReplayKey
        lastReplayKey = replayKey
        if (!enabled) {
            appearance.snapTo(target)
            position.snapTo(target)
            pathProgress.snapTo(target)
        } else if (complete) {
            if (replay || appearance.value <= .001f) {
                // Restart from below only when the mark is hidden, or for a deliberate replay.
                // A rapid reversal while it is visible keeps its existing spatial position.
                appearance.snapTo(0f)
                position.snapTo(0f)
                pathProgress.snapTo(0f)
            }
            coroutineScope {
                launch { appearance.animateTo(1f, ReferenceRevealTokens.RevealSpec) }
                launch { position.animateTo(1f, ReferenceRevealTokens.SuccessBobSpec) }
                launch {
                    val scale = coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
                    if (scale > 0f) delay((ReferenceRevealTokens.SuccessPathDelayMillis * scale).toLong())
                    pathProgress.animateTo(1f, ReferenceRevealTokens.SuccessPathSpec)
                }
            }
        } else {
            coroutineScope {
                launch { appearance.animateTo(0f, clearing) }
                launch { pathProgress.animateTo(0f, clearing) }
                // Finish an interrupted entrance quietly as the mark fades. Resetting its
                // position here would make a quick re-completion jump down while still visible.
                launch { position.animateTo(1f, clearing) }
            }
        }
    }
    val fill = MaterialTheme.colorScheme.primaryContainer
    val stroke = MaterialTheme.colorScheme.onPrimaryContainer
    val pending = MaterialTheme.colorScheme.outline
    Box(
        modifier.size(48.dp).clearAndSetSemantics {
            contentDescription = if (complete) label else pendingLabel
            liveRegion = LiveRegionMode.Polite
        },
    ) {
        // The pending outline stays in place while the independently transformed success mark
        // fades. Changing completion never abruptly switches the visible mark's coordinate space.
        Canvas(Modifier.matchParentSize()) {
            val fraction = if (!enabled) {
                if (complete) 1f else 0f
            } else appearance.value.coerceIn(0f, 1f)
            val width = size.minDimension * .075f
            drawCircle(
                pending.copy(alpha = 1f - fraction),
                (size.minDimension - width) / 2f,
                style = Stroke(width)
            )
        }
        Canvas(
            Modifier.matchParentSize().graphicsLayer {
                val alphaProgress = if (!enabled) {
                    if (complete) 1f else 0f
                } else appearance.value.coerceIn(0f, 1f)
                val spatialProgress = if (!enabled) 1f else position.value
                val focusProgress = spatialProgress.coerceIn(0f, 1f)
                alpha = alphaProgress
                translationY = 40.dp.toPx() * (1f - spatialProgress)
                rotationZ = 80f * (1f - focusProgress)
                renderEffect = motionBlurEffect(10.dp.toPx() * (1f - focusProgress))
            },
        ) {
            val drawn = if (!enabled) {
                if (complete) 1f else 0f
            } else pathProgress.value.coerceIn(0f, 1f)
            val width = size.minDimension * .075f
            drawCircle(fill)
            val check = Path().apply {
                moveTo(size.width * .27f, size.height * .51f)
                lineTo(size.width * .44f, size.height * .68f)
                lineTo(size.width * .75f, size.height * .34f)
            }
            val measure = PathMeasure().apply { setPath(check, false) }
            val visible = Path()
            measure.getSegment(0f, measure.length * drawn, visible, true)
            drawPath(visible, stroke, style = Stroke(width, cap = StrokeCap.Round))
        }
    }
}
