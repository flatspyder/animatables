package com.singleoriginapps.animatables

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
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
 * By default [continuous] repeats a two-second sweep while [active]. The host must provide a
 * pause/stop control for ongoing motion. Set continuous to false for two sweeps (four seconds at
 * normal system scale), then ordinary static text. Light surfaces use dark ink with a lighter
 * travelling spotlight; dark surfaces use a brighter spotlight over shaded ink. A broad bright
 * centre and short soft shoulders make the sweep distinct. The band preserves at least 4.5:1
 * contrast against the theme surface when the supplied color already meets that contrast. Caller alpha is retained. Set [active] false to stop immediately. The
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
    continuous: Boolean = true,
) {
    val enabled = rememberMotionEnabled()
    val progress = remember { Animatable(0f) }
    // Use Compose's infinite-animation policy for ongoing playback (including test idleness).
    // Removing this branch stops requesting frames when paused or motion is disabled.
    val repeating = if (active && enabled && continuous) {
        key(text, replayKey) {
            rememberInfiniteTransition(label = "Continuous shimmer").animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(MotionEffectTokens.ShimmerSweepSpec),
                label = "Shimmer sweep",
            )
        }
    } else null
    var running by remember { mutableStateOf(false) }
    val base = style.color.takeOrElse { LocalContentColor.current }
    val surface = MaterialTheme.colorScheme.surface
    val ink = MaterialTheme.colorScheme.onSurface
    val lightBackground = surface.luminance() > ink.luminance()
    val strongInk = if (inkContrast(ink, surface) > inkContrast(base, surface)) ink else base
    // SrcIn retains caller glyph alpha. Only the spotlight becomes lighter in light mode;
    // the rest of the text keeps the stronger dark ink instead of a washed-out blanket shade.
    val shaded = if (lightBackground) strongInk.copy(alpha = 1f)
        else remember(base, surface) { readableShimmerShade(base, surface) }
    val highlight = if (lightBackground) readableShimmerShade(strongInk.copy(alpha = base.alpha), surface)
        else strongInk.copy(alpha = 1f)
    LaunchedEffect(text, active, replayKey, enabled, continuous) {
        running = false
        if (!active || !enabled || continuous) {
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
                if (active && enabled && (repeating != null || running) && size.width > 0f) {
                    val center = (-1f + (repeating?.value ?: progress.value) * 3f) * size.width
                    val halfBand = (size.width * ShimmerContrastTokens.HalfBandFraction).coerceAtLeast(12.dp.toPx())
                    // SrcIn recolors only the alpha laid down by native text in this isolated
                    // layer. Nothing is painted into the label's rectangular background.
                    drawRect(
                        brush = Brush.linearGradient(
                            colorStops = arrayOf(
                                0f to shaded,
                                ShimmerContrastTokens.ShoulderStart to shaded,
                                ShimmerContrastTokens.PlateauStart to highlight,
                                ShimmerContrastTokens.PlateauEnd to highlight,
                                ShimmerContrastTokens.ShoulderEnd to shaded,
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
    const val HalfBandFraction = .5f
    const val ShoulderStart = .12f
    const val PlateauStart = .3f
    const val PlateauEnd = .7f
    const val ShoulderEnd = .88f
}

private fun inkContrast(color: Color, surface: Color): Float {
    val ink = color.compositeOver(surface).luminance()
    val backdrop = surface.luminance()
    return (maxOf(ink, backdrop) + .05f) / (minOf(ink, backdrop) + .05f)
}
