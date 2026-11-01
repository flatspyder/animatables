package com.singleoriginapps.animatables

import android.graphics.Shader
import android.icu.text.BreakIterator
import android.os.Build
import androidx.compose.ui.graphics.LayerOutsets
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect

// Keep Android ICU and RenderEffect behavior behind the shared API.
internal actual fun numberDigitValue(glyph: String): Int? =
    if (glyph.codePointCount(0, glyph.length) == 1) Character.digit(glyph.codePointAt(0), 10)
        .takeIf { it >= 0 } else null

internal actual fun numberCodePoint(glyph: String): Int = glyph.codePointAt(0)

/** Older Android versions keep the slide/fade and render sharp, legible text. */
internal actual fun motionBlurEffect(radiusPx: Float): RenderEffect? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && radiusPx > .1f) {
        android.graphics.RenderEffect.createBlurEffect(
            radiusPx,
            radiusPx,
            Shader.TileMode.DECAL,
        ).asComposeRenderEffect()
    } else null

internal actual fun codePointString(codePoint: Int): String = String(Character.toChars(codePoint))

internal actual fun graphemes(value: String): List<String> {
    val iterator = BreakIterator.getCharacterInstance().apply { setText(value) }
    val result = mutableListOf<String>()
    var start = iterator.first()
    var end = iterator.next()
    while (end != BreakIterator.DONE) {
        result += value.substring(start, end)
        start = end
        end = iterator.next()
    }
    return result
}

// Compose's Android MotionDurationScale supplies the platform setting in the shared observer.
@androidx.compose.runtime.Composable
internal actual fun rememberPlatformMotionEnabled(): Boolean = true

internal actual fun motionBlurOutsets(blurOutsets: LayerOutsets): LayerOutsets = blurOutsets
