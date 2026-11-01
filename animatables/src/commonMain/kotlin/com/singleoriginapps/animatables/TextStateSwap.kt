package com.singleoriginapps.animatables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.LayerOutsets
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/** Choreography verified against the reference: the exit finishes before the next label enters. */
private object TextSwapMotionTokens {
    const val PhaseDurationMillis = 150
    const val IncomingDelayMillis = 150
    val Travel = 4.dp
    val BlurRadius = 2.dp
    val BlurOutsets = LayerOutsets(BlurRadius * 2)
    private val Easing = CubicBezierEasing(.42f, 0f, .58f, 1f)
    fun <T> phase(delayMillis: Int = 0): TweenSpec<T> = tween(
        durationMillis = PhaseDurationMillis,
        delayMillis = delayMillis,
        easing = Easing,
    )
}

private data class TextSwapTarget(val text: String, val replayKey: Any)

/**
 * Replaces a short label with an upward handoff: outgoing text rises and softens while the next
 * label rises into focus from below after the outgoing label clears. [replayKey] can replay the
 * handoff without changing [text].
 *
 * Text remains native, wrapping and scaling with [style]. Android 12+ adds a brief text blur;
 * earlier Android versions keep the slide and fade. Other supported targets use Compose/Skia
 * soft focus. A single target label is exposed to
 * accessibility, with a polite live region for meaningful status changes. Both system Remove
 * animations and [AnimatablesMotion] opt-out present the target immediately without blur/travel.
 * Reserve this emphasis for occasional state changes, rather than rapidly updating telemetry.
 */
@Composable
public fun TextStateSwap(
    text: String,
    modifier: Modifier = Modifier,
    replayKey: Any = Unit,
    style: TextStyle = LocalTextStyle.current,
) {
    TextSwapContent(text, modifier, replayKey) { label ->
        Text(
            label,
            style = if (style.textAlign == TextAlign.Unspecified) style.copy(textAlign = TextAlign.Center) else style,
        )
    }
}

/** Shared choreography lets ThinkingStates mask its actual glyphs without replacing text layout. */
@Composable
internal fun TextSwapContent(
    text: String,
    modifier: Modifier,
    replayKey: Any,
    travelDp: Dp = TextSwapMotionTokens.Travel,
    incomingDelayMillis: Int = TextSwapMotionTokens.IncomingDelayMillis,
    content: @Composable (String) -> Unit,
) {
    require(incomingDelayMillis >= 0) { "incomingDelayMillis must be nonnegative" }
    val enabled = rememberMotionEnabled()
    val density = LocalDensity.current
    val travel = with(density) { travelDp.roundToPx() }
    val blur = with(density) { TextSwapMotionTokens.BlurRadius.toPx() }
    val size = if (enabled) TextSwapMotionTokens.phase<IntSize>(incomingDelayMillis) else snap()
    val incomingMovement =
        if (enabled) TextSwapMotionTokens.phase<IntOffset>(incomingDelayMillis) else snap()
    val outgoingMovement = if (enabled) TextSwapMotionTokens.phase<IntOffset>() else snap()
    val incomingEffects =
        if (enabled) TextSwapMotionTokens.phase<Float>(incomingDelayMillis) else snap()
    val outgoingEffects = if (enabled) TextSwapMotionTokens.phase<Float>() else snap()
    val target = TextSwapTarget(text, replayKey)
    Box(
        modifier = modifier.clearAndSetSemantics {
            this.text = AnnotatedString(text)
            liveRegion = LiveRegionMode.Polite
        },
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true,
    ) {
        AnimatedContent(
            targetState = target,
            contentAlignment = Alignment.Center,
            transitionSpec = {
                (slideInVertically(incomingMovement) { if (enabled) travel else 0 }
                        togetherWith slideOutVertically(outgoingMovement) { if (enabled) -travel else 0 })
                    .using(SizeTransform(clip = false) { _, _ -> size })
            },
            label = "Text state handoff",
        ) { state ->
            val softness by transition.animateFloat(
                transitionSpec = {
                    if (targetState == EnterExitState.Visible) incomingEffects else outgoingEffects
                },
                label = "Text state focus",
            ) { phase -> if (phase == EnterExitState.Visible) 0f else 1f }
            Box(Modifier.graphicsLayer {
                val focus = if (enabled) softness.coerceIn(0f, 1f) else 0f
                // Keep opacity and focus on one expanded layer: an outer fade layer would crop
                // the softened edges back to the unblurred text bounds.
                alpha = 1f - focus
                motionBlur(focus * blur, TextSwapMotionTokens.BlurOutsets)
            }, contentAlignment = Alignment.Center) { content(state.text) }
        }
    }
}
