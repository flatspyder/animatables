package com.singleoriginapps.animatables

import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode

internal actual fun motionBlurEffect(radiusPx: Float): RenderEffect? =
    if (radiusPx > .1f) BlurEffect(radiusPx, radiusPx, TileMode.Decal) else null
