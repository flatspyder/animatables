package com.singleoriginapps.animatables

import android.os.Build
import android.graphics.Shader
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.flow.collect
import kotlin.coroutines.coroutineContext

/** Bounded decorative timing, separate from the theme's spatial and effects springs. */
internal object MotionEffectTokens {
    const val ShimmerSweeps: Int = 2
    private const val ShimmerSweepMillis: Int = 2000
    val ShimmerSweepSpec: FiniteAnimationSpec<Float> = tween(
        durationMillis = ShimmerSweepMillis,
        easing = LinearEasing,
    )
}

/** Reference-derived choreography, named separately from the host's general Material scheme. */
internal object ReferenceRevealTokens {
    val Decelerate = CubicBezierEasing(.22f, 1f, .36f, 1f)
    val SuccessBob = CubicBezierEasing(.34f, 1.35f, .64f, 1f)
    val RevealSpec: FiniteAnimationSpec<Float> = tween(500, easing = Decelerate)
    val SuccessBobSpec: FiniteAnimationSpec<Float> = tween(500, easing = SuccessBob)
    val SuccessPathSpec: FiniteAnimationSpec<Float> = tween(500, easing = Decelerate)
    const val LineStaggerMillis = 40L
    const val MaximumLineStaggerMillis = 300L
    const val SuccessPathDelayMillis = 80L
}

/**
 * Includes a synchronous first-frame read and a live effect-context observer. This also lets
 * finite effects cancel their pending work as soon as the system's animation scale becomes zero.
 */
@Composable
internal fun rememberMotionEnabled(): Boolean {
    val appEnabled = LocalAnimatablesMotionEnabled.current
    val scope = rememberCoroutineScope()
    val synchronousScale = scope.coroutineContext[MotionDurationScale]
    var effectScaleEnabled by remember {
        mutableStateOf((synchronousScale?.scaleFactor ?: 1f) > 0f)
    }
    LaunchedEffect(Unit) {
        val durationScale = coroutineContext[MotionDurationScale]
        snapshotFlow { durationScale?.scaleFactor ?: 1f }.collect {
            effectScaleEnabled = it > 0f
        }
    }
    return appEnabled && (synchronousScale?.scaleFactor?.let { it > 0f } ?: effectScaleEnabled)
}

/** Older Android versions keep the slide/fade and render sharp, legible text. */
internal fun motionBlurEffect(radiusPx: Float): RenderEffect? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && radiusPx > .1f) {
        android.graphics.RenderEffect.createBlurEffect(
            radiusPx,
            radiusPx,
            Shader.TileMode.DECAL,
        ).asComposeRenderEffect()
    } else null

/** Reads the radius in the draw phase. Prefer [motionBlurEffect] inside an existing layer. */
internal fun Modifier.motionBlur(radiusPx: () -> Float): Modifier = graphicsLayer {
    renderEffect = motionBlurEffect(radiusPx().coerceAtLeast(0f))
}
