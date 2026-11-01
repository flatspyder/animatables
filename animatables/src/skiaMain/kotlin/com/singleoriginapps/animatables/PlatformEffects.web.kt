package com.singleoriginapps.animatables

import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.LayerOutsets
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode

internal actual fun motionBlurEffect(radiusPx: Float): RenderEffect? =
    if (radiusPx > .1f) BlurEffect(radiusPx, radiusPx, TileMode.Decal) else null

// Compose/Skia 1.12 records the paint for an outset saveLayer into its parent's picture.
// Updating opacity/focus then leaves that recorded paint stale (including initial alpha=0).
// Keep the normal dynamic render-node path on Skia. DECAL still softens content, while
// Android independently reserves expanded raster bounds for the blur fringe.
internal actual fun motionBlurOutsets(blurOutsets: LayerOutsets): LayerOutsets = LayerOutsets.Zero
