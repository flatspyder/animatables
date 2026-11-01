package com.singleoriginapps.animatables

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/**
 * Sweeps a narrow highlight across the glyphs, without moving the label or painting its bounds.
 *
 * Each activation or [replayKey] change plays two finite two-second sweeps (four seconds at normal
 * system scale), then leaves ordinary static text. A neutral band resolves shaded glyphs to their
 * original color; shading preserves at least 4.5:1 contrast against the theme surface when the
 * supplied color already meets that contrast. Caller alpha is retained. Set [active] false to stop immediately. The
 * app-level motion preference and system Remove animations also leave a static, readable label.
 * [text] is a real status supplied by the caller; this component does not simulate work/progress.
 */
@Composable
public fun ShimmerText(
    text: String,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    replayKey: Any = Unit,
    style: TextStyle = LocalTextStyle.current,
) {
    val enabled = rememberMotionEnabled()
    val progress = remember { Animatable(0f) }
    var running by remember { mutableStateOf(false) }
    val base = style.color.takeOrElse { LocalContentColor.current }
    val surface = MaterialTheme.colorScheme.surface
    val shaded = remember(base, surface) { readableShimmerShade(base, surface) }
    // SrcIn retains the native glyph's alpha; the gradient colors must themselves be opaque.
    val highlight = base.copy(alpha = 1f)
    LaunchedEffect(text, active, replayKey, enabled) {
        running = false
        if (!active || !enabled) {
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        try {
            repeat(MotionEffectTokens.ShimmerSweeps) {
                progress.snapTo(0f)
                running = true
                progress.animateTo(1f, animationSpec = MotionEffectTokens.ShimmerSweepSpec)
            }
        } finally {
            running = false
        }
    }
    BasicText(
        text = text,
        modifier = modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                if (active && enabled && running && size.width > 0f) {
                    val center = (-1f + progress.value * 3f) * size.width
                    val halfBand = (size.width * .4f).coerceAtLeast(12.dp.toPx())
                    // SrcIn recolors only the alpha laid down by native text in this isolated
                    // layer. Nothing is painted into the label's rectangular background.
                    drawRect(
                        brush = Brush.linearGradient(
                            colorStops = arrayOf(
                                0f to shaded,
                                .5f to highlight,
                                1f to shaded,
                            ),
                            start = Offset(center - halfBand, 0f),
                            end = Offset(center + halfBand, 0f),
                        ),
                        blendMode = BlendMode.SrcIn,
                    )
                }
            },
        style = style.copy(color = base),
    )
}

/** Strong luminance separation without the reference's low-contrast blanket alpha reduction. */
private fun readableShimmerShade(base: Color, surface: Color): Color {
    val original = base.copy(alpha = 1f)
    fun contrast(color: Color): Float {
        val ink = color.copy(alpha = base.alpha).compositeOver(surface).luminance()
        val backdrop = surface.luminance()
        return (maxOf(ink, backdrop) + .05f) / (minOf(ink, backdrop) + .05f)
    }
    // A low-contrast caller color is preserved rather than being made still fainter.
    if (contrast(original) <= ShimmerContrastTokens.Minimum) return original
    var readable = 0f
    var faded = 1f
    repeat(12) {
        val mix = (readable + faded) / 2f
        if (contrast(lerp(original, surface, mix)) >= ShimmerContrastTokens.Minimum) readable = mix
        else faded = mix
    }
    return lerp(original, surface, readable).copy(alpha = 1f)
}

private object ShimmerContrastTokens {
    const val Minimum = 4.5f
}
