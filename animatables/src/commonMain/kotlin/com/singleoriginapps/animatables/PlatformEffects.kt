package com.singleoriginapps.animatables

import androidx.compose.ui.graphics.RenderEffect

// Platform-specific rendering and Unicode stay internal; public components live in commonMain.
internal expect fun motionBlurEffect(radiusPx: Float): RenderEffect?

@androidx.compose.runtime.Composable
internal expect fun rememberPlatformMotionEnabled(): Boolean
