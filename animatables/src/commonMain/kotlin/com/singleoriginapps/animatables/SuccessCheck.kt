package com.singleoriginapps.animatables

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.LayerOutsets
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SuccessCheckBlurOutsets = LayerOutsets(20.dp)

/**
 * A success mark arriving upward through fade, rotation and soft focus, then a drawn check.
 *
 * The reference-derived 500ms entrance travels 40dp, resolves a 10dp blur on Android 12+, and
 * draws the check after an 80ms lead-in. Other supported targets use Compose/Skia blur. It stays full size; scaling no longer drives the event.
 * [replayKey] deliberately repeats completion. Clearing settles quietly into a pending outline.
 * Keep a persistent outcome label nearby, and localize [label] and [pendingLabel]. System/app
 * disabled motion shows the complete resting state immediately, without the stroke delay.
 * [successIcon] replaces the check in a centered half-size slot and inherits the theme content color.
 */
@Composable
public fun SuccessCheck(
    complete: Boolean,
    modifier: Modifier = Modifier,
    replayKey: Any = Unit,
    label: String = "Complete",
    pendingLabel: String = "Not complete",
    successIcon: (@Composable () -> Unit)? = null,
) {
    val pending = MaterialTheme.colorScheme.outline
    SuccessFeedback(
        complete = complete, modifier = modifier, replayKey = replayKey,
        label = label, pendingLabel = pendingLabel, successIcon = successIcon,
        fill = MaterialTheme.colorScheme.primaryContainer,
        content = MaterialTheme.colorScheme.onPrimaryContainer,
    ) { fraction, _ ->
        Canvas(Modifier.matchParentSize()) {
            val width = size.minDimension * CompletionMark.StrokeFraction
            drawCircle(
                pending,
                (size.minDimension - width) / 2f,
                alpha = 1f - fraction(),
                style = Stroke(width),
            )
        }
    }
}

/** Shared entrance and semantics for completion with different pending content. */
@Composable
internal fun SuccessFeedback(
    complete: Boolean,
    modifier: Modifier,
    replayKey: Any = Unit,
    label: String,
    pendingLabel: String,
    fill: Color,
    content: Color,
    successIcon: (@Composable () -> Unit)?,
    initialCompleteStatic: Boolean = false,
    enabled: Boolean = rememberMotionEnabled(),
    pendingSemantics: SemanticsPropertyReceiver.() -> Unit = {},
    pending: @Composable BoxScope.(fraction: () -> Float, show: Boolean) -> Unit,
) {
    val initial = if (complete && (!enabled || initialCompleteStatic)) 1f else 0f
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
    val showPending by remember { derivedStateOf { appearance.value < .999f || appearance.isRunning } }
    Box(
        modifier.size(48.dp).clearAndSetSemantics {
            contentDescription = if (complete) label else pendingLabel
            liveRegion = LiveRegionMode.Polite
            if (!complete) pendingSemantics()
        },
    ) {
        pending({ if (!enabled) { if (complete) 1f else 0f } else appearance.value.coerceIn(0f, 1f) }, showPending)
        Box(
            Modifier.matchParentSize().graphicsLayer {
                val alphaProgress = if (!enabled) {
                    if (complete) 1f else 0f
                } else appearance.value.coerceIn(0f, 1f)
                val spatialProgress = if (!enabled) 1f else position.value
                // Focus/rotation share the reference effects curve; only translation has the
                // expressive bob. A spatial overshoot must not prematurely remove the blur.
                val focusProgress = alphaProgress
                alpha = alphaProgress
                translationY = 40.dp.toPx() * (1f - spatialProgress)
                rotationZ = 80f * (1f - focusProgress)
                motionBlur(10.dp.toPx() * (1f - focusProgress), SuccessCheckBlurOutsets)
            }.drawWithCache {
                val mark = CompletionMark(size)
                onDrawBehind {
                    val drawn = if (!enabled) {
                        if (complete) 1f else 0f
                    } else pathProgress.value.coerceIn(0f, 1f)
                    if (successIcon == null) with(mark) { draw(fill, content, drawn) }
                    else drawCircle(fill)
                }
            },
        ) {
            if (successIcon != null) {
                Box(Modifier.fillMaxSize(.5f).align(Alignment.Center).graphicsLayer {
                    alpha = if (!enabled) { if (complete) 1f else 0f }
                    else pathProgress.value.coerceIn(0f, 1f)
                }, contentAlignment = Alignment.Center) {
                    CompositionLocalProvider(LocalContentColor provides content) { successIcon() }
                }
            }
        }
    }
}
