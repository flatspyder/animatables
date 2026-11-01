package com.singleoriginapps.animatables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.core.snap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * Hands off a small in-place state change with a fade and a short vertical-only entrance.
 * Content stays centred as surrounding layout reflows to its new size.
 *
 * [content] must render the state passed to it, rather than capturing [targetState]. Prefer stable,
 * immutable state values. Outgoing content is removed from accessibility semantics immediately.
 * This is not a navigation container; use Navigation's lifecycle and back handling for screens.
 *
 * Uses the host theme's fast effects and spatial tokens. [AnimatablesMotion] can remove the
 * transition through snap specifications, preserving the slot composition. Compose's clock also
 * snaps to the resting state at system duration scale zero.
 */
@Composable
public fun <T> MotionContent(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val enabled = rememberMotionEnabled()
    val travel = with(LocalDensity.current) { 12.dp.roundToPx() }
    val effects = if (enabled) MaterialTheme.motionScheme.fastEffectsSpec<Float>() else snap<Float>()
    val spatial = if (enabled) MaterialTheme.motionScheme.fastSpatialSpec<IntSize>() else snap<IntSize>()
    val movement = if (enabled) MaterialTheme.motionScheme.fastSpatialSpec<IntOffset>() else snap<IntOffset>()
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true,
    ) {
        AnimatedContent(
            targetState = targetState,
            contentAlignment = Alignment.Center,
            transitionSpec = {
                ((fadeIn(effects) + slideInVertically(movement) { if (enabled) travel else 0 })
                    togetherWith fadeOut(effects)).using(
                    SizeTransform(clip = false) { _, _ -> spatial },
                )
            },
            label = "Animatables content",
        ) { state ->
            Box(
                modifier = if (state == targetState) Modifier else Modifier.clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) { content(state) }
        }
    }
}
