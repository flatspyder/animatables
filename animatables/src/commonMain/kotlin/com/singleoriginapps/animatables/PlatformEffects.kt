package com.singleoriginapps.animatables

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.LayerOutsets
import androidx.compose.ui.graphics.RenderEffect

// Platform-specific rendering and Unicode stay internal; public components live in commonMain.
internal expect fun motionBlurEffect(radiusPx: Float): RenderEffect?
internal expect fun motionBlurOutsets(blurOutsets: LayerOutsets): LayerOutsets

@androidx.compose.runtime.Composable
internal expect fun rememberPlatformMotionEnabled(): Boolean

/**
 * Reserves soft-edge raster bounds where the platform supports animated outsets. Merely using
 * DECAL with clip=false does not expand an alpha-composited layer's raster bounds. The caller
 * supplies a fixed maximum blur outset; platform adapters choose a safe rendering path without
 * changing the measured layout.
 * Settled content has neither an effect nor an enlarged offscreen buffer.
 */
internal fun GraphicsLayerScope.motionBlur(radiusPx: Float, blurOutsets: LayerOutsets) {
    val effect = motionBlurEffect(radiusPx)
    renderEffect = effect
    outsets = if (effect == null) LayerOutsets.Zero else motionBlurOutsets(blurOutsets)
}
