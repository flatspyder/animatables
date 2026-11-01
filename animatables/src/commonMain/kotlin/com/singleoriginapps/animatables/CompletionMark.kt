package com.singleoriginapps.animatables

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/** Cached geometry shared by the completion family. Construct in drawWithCache. */
internal class CompletionMark(size: Size) {
    val check = Path().apply {
        moveTo(size.width * StartX, size.height * StartY)
        lineTo(size.width * CornerX, size.height * CornerY)
        lineTo(size.width * EndX, size.height * EndY)
    }
    val stroke = Stroke(size.minDimension * StrokeFraction, cap = StrokeCap.Round)
    private val measure = PathMeasure().apply { setPath(check, false) }
    private val visible = Path()

    fun DrawScope.draw(fill: Color, content: Color, progress: Float = 1f) {
        drawCircle(fill)
        val path = if (progress >= 1f) check else {
            visible.rewind()
            measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), visible, true)
            visible
        }
        drawPath(path, content, style = stroke)
    }

    companion object {
        const val StartX = .27f
        const val StartY = .51f
        const val CornerX = .44f
        const val CornerY = .68f
        const val EndX = .75f
        const val EndY = .34f
        const val StrokeFraction = .075f
    }
}
