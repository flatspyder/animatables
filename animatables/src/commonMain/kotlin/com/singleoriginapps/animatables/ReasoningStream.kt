package com.singleoriginapps.animatables

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
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
import kotlin.math.roundToInt

/**
 * Scrolls a compact window of actual user-visible activity entries upward as new entries arrive.
 *
 * One continuous measured column moves by the actual height of displaced entries, so wrapped
 * entries keep their position relative to surviving entries. [maxVisibleEntries] limits entries,
 * not visual lines. Each entry wraps naturally. A fixed 28dp edge fade appears only while content
 * is hidden beyond that edge; the newest entry has no bottom fade at rest, and a full transcript
 * has no edge mask. The semantic value is the entire transcript,
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
    LaunchedEffect(geometry, enabled) {
        val current = geometry ?: return@LaunchedEffect
        if (!enabled || positionedFor != current.identity) {
            scroll.snapTo(current.targetOffset.toFloat())
            positionedFor = current.identity
        } else {
            scroll.animateTo(current.targetOffset.toFloat(), spec)
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
                drawContent()
                val current = geometry
                if (current != null && current.hasOverflow && size.height > 0f) {
                    val fade = ReasoningPolishTokens.EdgeFade.toPx().coerceAtMost(size.height / 2f)
                    val offset =
                        if (!enabled || positionedFor != current.identity) current.targetOffset.toFloat()
                        else scroll.value.coerceIn(0f, current.targetOffset.toFloat())
                    val hasContentAbove = offset > .5f
                    val hasContentBelow = current.targetOffset - offset > .5f
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to if (hasContentAbove) Color.Transparent else Color.Black,
                            (fade / size.height) to Color.Black,
                            (1f - fade / size.height) to Color.Black,
                            1f to if (hasContentBelow) Color.Transparent else Color.Black,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                }
            },
    ) { measurables, constraints ->
        val padding = 12.dp.roundToPx()
        val spacing = 8.dp.roundToPx()
        val childConstraints = Constraints(maxWidth = constraints.maxWidth)
        val children = measurables.map { it.measure(childConstraints) }
        val visibleCount = maxVisibleEntries.coerceAtMost(children.size)
        val totalContent =
            children.sumOf { it.height } + spacing * (children.size - 1).coerceAtLeast(0)
        val visibleContent = children.takeLast(visibleCount).sumOf { it.height } +
                spacing * (visibleCount - 1).coerceAtLeast(0)
        val width = constraints.constrainWidth(children.maxOfOrNull { it.width } ?: 0)
        val height = constraints.constrainHeight(visibleContent + padding * 2)
        val totalHeight = totalContent + padding * 2
        val target = (totalHeight - height).coerceAtLeast(0)
        val identity = ActivityPosition(generation, maxVisibleEntries, width)
        val current = ActivityGeometry(identity, target, totalHeight, target > 0)
        if (geometry != current) geometry = current
        layout(width, height) {
            val offset = if (!enabled || positionedFor != identity) target.toFloat()
            else scroll.value.coerceIn(0f, target.toFloat())
            var top = padding - offset.roundToInt()
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
    val totalHeight: Int,
    val hasOverflow: Boolean,
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
    val ScrollSpec = tween<Float>(
        durationMillis = 500,
        easing = CubicBezierEasing(.22f, 1f, .36f, 1f),
    )
}
