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
 * by Compose's animation clock; this opt-out never overrides Android's Remove animations setting.
 */
@Composable
public fun AnimatablesMotion(enabled: Boolean = true, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAnimatablesMotionEnabled provides (enabled && LocalAnimatablesMotionEnabled.current),
        content = content,
    )
}
