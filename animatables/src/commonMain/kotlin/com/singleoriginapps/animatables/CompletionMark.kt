package com.singleoriginapps.animatables

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser

/** Material Symbols check, progressively revealed along its two arms. Cached in drawWithCache. */
internal class CompletionMark(private val size: Size) {
    private val check = PathParser().parsePathString(MaterialSymbols.CheckPath).toPath()

    fun DrawScope.draw(fill: Color, content: Color, progress: Float = 1f) {
        drawCircle(fill)
        withTransform({
            translate(size.width * .1f, size.height * .1f)
            scale(size.width * .8f / 960f, size.height * .8f / 960f, pivot = Offset.Zero)
            translate(0f, 960f)
        }) {
            // The reveal traverses the arms from left to right, retaining Google's exact
            // rounded outline at rest instead of approximating it with hand-drawn strokes.
            val fraction = progress.coerceIn(0f, 1f)
            clipRect(0f, -960f, 160f + 640f * fraction, 0f) {
                if (fraction > 0f) drawPath(check, content)
            }
        }
    }

    companion object {
        const val StrokeFraction = .075f
    }
}
