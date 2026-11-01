package com.singleoriginapps.animatables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalAnimatablesMotionEnabled = staticCompositionLocalOf { true }

/**
 * Controls optional Animatables motion within [content], without replacing the host Material theme.
 *
 * Pass `enabled = false` for an app preference or to inspect resting states. A disabled ancestor
 * cannot be re-enabled by a descendant. System animation duration scale is additionally respected
 * by Compose's animation clock. Web and iOS components also observe the platform's reduced-motion
 * preference. Desktop hosts bridge their preference with [enabled]. This opt-out never enables
 * motion that a platform accessibility preference has disabled.
 */
@Composable
public fun AnimatablesMotion(enabled: Boolean = true, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAnimatablesMotionEnabled provides (enabled && LocalAnimatablesMotionEnabled.current),
        content = content,
    )
}
