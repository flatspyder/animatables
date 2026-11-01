package com.singleoriginapps.animatables

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Scrolls a compact window of actual user-visible activity entries upward as new entries arrive.
 *
 * One continuous measured column moves by the actual height of displaced entries, so wrapped
 * entries keep their position relative to surviving entries. [maxVisibleEntries] limits the resting window to that many entries,
 * not visual lines; each entry wraps naturally. Top and bottom edge fades soften text only while
 * it crosses the clipped viewport. Equal-depth top/bottom fades remain visible through the scroll
 * and clear before the column settles, leaving exactly the latest entries fully opaque without a history peek.
 * The semantic value is the entire transcript,
 * without announcing each update automatically. Pass truthful user-visible activity, not invented
 * internal reasoning. Intended for compact activity logs; paginate very large logs in the host.
 *
 * The host owns data arrival and pause/expanded transcript controls. [active] false disables motion
 * while still rendering the newest entries; freeze the caller's displayed snapshot to pause data.
 * App/system-disabled motion shows the latest window immediately. No timer generates entries.
 */
@Composable
public fun ReasoningStream(
    entries: List<String>,
    modifier: Modifier = Modifier,
    maxVisibleEntries: Int = 2,
    active: Boolean = true,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    require(maxVisibleEntries > 0) { "maxVisibleEntries must be positive" }
    val enabled = rememberMotionEnabled() && active
    val spec = ReasoningPolishTokens.ScrollSpec
    val scroll = remember { Animatable(0f) }
    val tracker = remember { ActivityIdentity() }
    val generation = remember(entries) { tracker.accept(entries) }
    var geometry by remember { mutableStateOf<ActivityGeometry?>(null) }
    var positionedFor by remember { mutableStateOf<ActivityPosition?>(null) }
    var animatingFor by remember { mutableStateOf<ActivityGeometry?>(null) }
    val edgeVisibility = remember { Animatable(0f) }
    LaunchedEffect(geometry, enabled) {
        val current = geometry ?: return@LaunchedEffect
        animatingFor = null
        if (!enabled || positionedFor != current.identity || scroll.value == current.targetOffset.toFloat()) {
            scroll.snapTo(current.targetOffset.toFloat())
            edgeVisibility.snapTo(0f)
            positionedFor = current.identity
        } else {
            animatingFor = current
            coroutineScope {
                launch { scroll.animateTo(current.targetOffset.toFloat(), spec) }
                // A shared 500ms envelope keeps both edges visible during the same scroll,
                // then restores full ink before the column stops. Retarget from current alpha.
                launch { edgeVisibility.animateTo(0f, ReasoningPolishTokens.EdgeFadeSpec) }
            }
            animatingFor = null
        }
    }
    Layout(
        content = {
            entries.forEachIndexed { index, entry ->
                key(index) { Text(entry, style = style) }
            }
        },
        modifier = modifier
            .clearAndSetSemantics { text = AnnotatedString(entries.joinToString("\n")) }
            .clipToBounds()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                val current = geometry
                val viewportHeight = current?.viewportHeight?.toFloat() ?: size.height
                clipRect(bottom = viewportHeight) {
                    this@drawWithContent.drawContent()
                    if (enabled && current != null && animatingFor == current && viewportHeight > 0f) {
                        val fade = minOf(ReasoningPolishTokens.EdgeFade.toPx(), viewportHeight / 2f)
                        val edgeAlpha = 1f - edgeVisibility.value.coerceIn(0f, 1f)
                        if (fade > 0f) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0f to Color.Black.copy(alpha = edgeAlpha),
                                    (fade / viewportHeight) to Color.Black,
                                    (1f - fade / viewportHeight) to Color.Black,
                                    1f to Color.Black.copy(alpha = edgeAlpha),
                                    endY = viewportHeight,
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                        }
                    }
                }
            },
    ) { measurables, constraints ->
        val spacing = 8.dp.roundToPx()
        val childConstraints = Constraints(maxWidth = constraints.maxWidth)
        val children = measurables.map { it.measure(childConstraints) }
        val visibleCount = maxVisibleEntries.coerceAtMost(children.size)
        val totalContent =
            children.sumOf { it.height } + spacing * (children.size - 1).coerceAtLeast(0)
        val visibleContent = children.takeLast(visibleCount).sumOf { it.height } +
                spacing * (visibleCount - 1).coerceAtLeast(0)
        val width = constraints.constrainWidth(children.maxOfOrNull { it.width } ?: 0)
        val height = constraints.constrainHeight(visibleContent)
        val viewportHeight = minOf(height, visibleContent)
        val target = (totalContent - viewportHeight).coerceAtLeast(0)
        val identity = ActivityPosition(generation, maxVisibleEntries, width)
        val current = ActivityGeometry(identity, target, viewportHeight)
        if (geometry != current) geometry = current
        layout(width, height) {
            val offset = if (!enabled || positionedFor != identity) target.toFloat()
            else scroll.value.coerceIn(0f, target.toFloat())
            var top = -offset.roundToInt()
            children.forEach { child ->
                // Relative placement preserves native RTL text alignment. Vertical movement is
                // shared by every entry rather than crossfading duplicated windows.
                child.placeRelative(x = 0, y = top)
                top += child.height + spacing
            }
        }
    }
}

private data class ActivityPosition(val generation: Int, val maximum: Int, val width: Int)
private data class ActivityGeometry(
    val identity: ActivityPosition,
    val targetOffset: Int,
    val viewportHeight: Int,
)

private class ActivityIdentity {
    private var previous: List<String> = emptyList()
    private var generation = 0
    fun accept(entries: List<String>): Int {
        if (entries.size < previous.size || entries.take(previous.size) != previous) generation++
        previous = entries.toList()
        return generation
    }
}

/** Reference-inspired motion/viewport tokens; real data arrival remains controlled by the host. */
private object ReasoningPolishTokens {
    val EdgeFade = 28.dp
    const val ScrollMillis = 500
    val EdgeFadeSpec = keyframes<Float> {
        durationMillis = ScrollMillis
        1f at 24
        1f at 160
        0f at 280
    }
    val ScrollSpec = tween<Float>(
        durationMillis = ScrollMillis,
        easing = CubicBezierEasing(.22f, 1f, .36f, 1f),
    )
}
