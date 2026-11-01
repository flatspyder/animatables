package com.singleoriginapps.animatables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.LayerOutsets
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private object IconSwapMotionTokens {
    const val DurationMillis = 250
    const val InitialScale = .25f
    val BlurRadius = 2.dp
    val BlurOutsets = LayerOutsets(BlurRadius * 2)
    private val Easing = CubicBezierEasing(.42f, 0f, .58f, 1f)
    fun <T> transition(): TweenSpec<T> = tween(durationMillis = DurationMillis, easing = Easing)
}

/**
 * Replaces an icon through overlapping centred scale and soft-focus transitions.
 *
 * [content] must use its state argument and supply a meaningful content description or let the
 * surrounding control label it. Only the incoming icon remains exposed to accessibility. This
 * decorates an icon; keep the button's hit area, ripple, role and click behavior on the host control.
 *
 * [replayKey] can repeat the entrance without destroying remembered state in the icon slot.
 * A named 250ms choreography token supplies the reference handoff. System and app animation
 * opt-outs replace all transformations with their resting values. Blur is an Android 12+ accent; other supported targets use Compose/Skia blur.
 */
@Composable
public fun <T> IconSwap(
    targetState: T,
    modifier: Modifier = Modifier,
    replayKey: Any = Unit,
    content: @Composable (T) -> Unit,
) {
    val enabled = rememberMotionEnabled()
    val spatial = if (enabled) IconSwapMotionTokens.transition<Float>() else snap()
    val effects = if (enabled) IconSwapMotionTokens.transition<Float>() else snap()
    val size = if (enabled) IconSwapMotionTokens.transition<IntSize>() else snap()
    val replaySpatial = remember { Animatable(0f) }
    val replayEffects = remember { Animatable(0f) }
    val run = remember(replayKey) { Any() }
    var activeRun by remember { mutableStateOf<Any?>(null) }
    LaunchedEffect(run, enabled) {
        replaySpatial.snapTo(if (enabled) 0f else 1f)
        replayEffects.snapTo(if (enabled) 0f else 1f)
        activeRun = run
        if (enabled) coroutineScope {
            launch { replaySpatial.animateTo(1f, spatial) }
            launch { replayEffects.animateTo(1f, effects) }
        }
    }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true,
    ) {
        AnimatedContent(
            targetState = targetState,
            contentAlignment = Alignment.Center,
            transitionSpec = {
                (scaleIn(
                    spatial,
                    initialScale = IconSwapMotionTokens.InitialScale,
                    transformOrigin = TransformOrigin.Center,
                ) togetherWith scaleOut(
                    spatial,
                    targetScale = IconSwapMotionTokens.InitialScale,
                    transformOrigin = TransformOrigin.Center,
                )).using(SizeTransform(clip = false) { _, _ -> size })
            },
            label = "Icon swap",
        ) { state ->
            val blur by transition.animateFloat(
                transitionSpec = { effects },
                label = "Icon focus"
            ) {
                if (it == EnterExitState.Visible) 0f else 1f
            }
            Box(
                modifier = (if (state == targetState) Modifier else Modifier.clearAndSetSemantics {})
                    .graphicsLayer {
                        val entrance =
                            if (!enabled) 1f else if (activeRun == run) replaySpatial.value else 0f
                        val opacity =
                            if (!enabled) 1f else if (activeRun == run) replayEffects.value.coerceIn(
                                0f,
                                1f
                            ) else 0f
                        transformOrigin = TransformOrigin.Center
                        alpha = opacity * if (enabled) 1f - blur.coerceIn(0f, 1f) else 1f
                        scaleX = IconSwapMotionTokens.InitialScale +
                                (1f - IconSwapMotionTokens.InitialScale) * entrance
                        scaleY = scaleX
                        motionBlur(
                            IconSwapMotionTokens.BlurRadius.toPx() *
                                    if (!enabled) 0f else maxOf(
                                        blur.coerceIn(0f, 1f),
                                        1f - opacity
                                    ),
                            IconSwapMotionTokens.BlurOutsets,
                        )
                    },
                contentAlignment = Alignment.Center,
            ) { content(state) }
        }
    }
}
