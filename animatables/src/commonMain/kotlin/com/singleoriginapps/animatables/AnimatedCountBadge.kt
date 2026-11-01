package com.singleoriginapps.animatables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.IntSize

/**
 * Material badge with a restrained count crossfade, including a visible zero state.
 *
 * Counts above [maxCount] display “maxCount+”; accessibility still receives the full count. Use
 * [countDescription] to provide a localized noun, for example “142 unread messages”. The size
 * transition deliberately uses Material's Standard fast spatial token to avoid bouncy data.
 * Hide the entire badge at the call site if zero should mean “no badge” in the product.
 *
 * @throws IllegalArgumentException if [count] is negative or [maxCount] is below one.
 */
@Composable
public fun AnimatedCountBadge(
    count: Int,
    modifier: Modifier = Modifier,
    maxCount: Int = 99,
    countDescription: (Int) -> String = { it.toString() },
) {
    require(count >= 0) { "count must be nonnegative" }
    require(maxCount > 0) { "maxCount must be positive" }
    val label = if (count > maxCount) "$maxCount+" else count.toString()
    val semantics = modifier.clearAndSetSemantics { contentDescription = countDescription(count) }
    val enabled = rememberMotionEnabled()
    val effects =
        if (enabled) MaterialTheme.motionScheme.fastEffectsSpec<Float>() else snap<Float>()
    val size = if (enabled) MotionScheme.standard().fastSpatialSpec<IntSize>() else snap<IntSize>()
    Badge(semantics) {
        AnimatedContent(
            targetState = label,
            transitionSpec = {
                (fadeIn(effects) togetherWith fadeOut(effects)).using(
                    SizeTransform(clip = false) { _, _ -> size },
                )
            },
            label = "Badge count",
        ) { value -> Box { Text(value) } }
    }
}
