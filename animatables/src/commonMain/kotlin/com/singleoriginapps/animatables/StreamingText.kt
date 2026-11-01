package com.singleoriginapps.animatables

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Displays real caller-supplied text with ordered word fades, without travel or soft focus.
 *
 * Appended words join one paced queue; settled words remain unchanged. Extending a partially
 * received word keeps its opacity and identity. A non-prefix replacement or a new [streamKey]
 * starts a new sequence. [animate] false and app/system motion opt-outs show current text at rest.
 *
 * A single native paragraph supplies shaping, RTL, whitespace, wrapping, and line breaks. It is
 * drawn through the same word-opacity path throughout, avoiding a base/overlay handoff at layout
 * or animation completion. Accessibility exposes one complete current text without a live region
 * announcement per token. The host owns arrival, pause/stop, and pagination of large transcripts.
 */
@Composable
public fun StreamingText(
    text: String,
    modifier: Modifier = Modifier,
    streamKey: Any = Unit,
    animate: Boolean = true,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val enabled = rememberMotionEnabled() && animate
    // Match Material Text: an explicit style is authoritative, rather than inheriting
    // unspecified letter spacing/font attributes from the surrounding LocalTextStyle.
    val resolvedStyle = style.copy(color = style.color.takeOrElse { LocalContentColor.current })
    val measurer = rememberTextMeasurer()
    val layoutDirection = LocalLayoutDirection.current
    val sequence = remember(streamKey) { WordSequence() }
    val run = remember(text, streamKey) { sequence.accept(text, enabled) }
    val paragraph = remember { StreamingParagraph() }
    LaunchedEffect(sequence, run.generation, enabled) {
        if (!enabled) {
            run.words.forEach { it.opacity.snapTo(1f) }
            return@LaunchedEffect
        }
        // Receiving an appended batch never restarts the fades already in progress. The queue
        // suspends without timers when no new words arrive; it produces no text or progress.
        coroutineScope {
            for (word in run.queue) {
                if (word.opacity.value >= 1f) continue
                launch { word.opacity.animateTo(1f, StreamingMotionTokens.WordFadeSpec) }
                val scale = coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
                if (scale > 0f) delay((StreamingMotionTokens.WordSpacingMillis * scale).toLong())
            }
        }
    }
    Layout(
        content = {},
        modifier = modifier
            .clearAndSetSemantics { this.text = AnnotatedString(text) }
            .drawWithContent {
                val current = paragraph.layout
                if (current != null && current.layoutInput.text.text == text) {
                    paragraph.words.forEach { word ->
                        val opacity =
                            if (enabled) word.motion.opacity.value.coerceIn(0f, 1f) else 1f
                        if (opacity > 0f) {
                            clipPath(word.clip) { drawText(current, alpha = opacity) }
                        }
                    }
                }
            },
    ) { _, constraints ->
        val measured = measurer.measure(
            text = text,
            style = resolvedStyle,
            constraints = constraints,
            layoutDirection = layoutDirection,
        )
        paragraph.layout = measured
        val overhang = 1.5.dp.toPx()
        paragraph.words = run.words.map { word ->
            val selected = measured.getPathForRange(word.range.first, word.range.last + 1)
            val clip = Path().apply {
                addPath(selected, Offset(-overhang, 0f))
                addPath(selected)
                addPath(selected, Offset(overhang, 0f))
            }
            PaintedWord(word, clip)
        }
        layout(measured.size.width, measured.size.height) {}
    }
}

/** Explicit word sequencing tokens for the reference-inspired streaming treatment. */
private object StreamingMotionTokens {
    const val WordSpacingMillis = 60L
    val WordFadeSpec =
        tween<Float>(durationMillis = 350, easing = CubicBezierEasing(.22f, 1f, .36f, 1f))
}

private val WordPattern = Regex("\\S+")

private class StreamWord(val start: Int, initialOpacity: Float) {
    var range: IntRange = start..start
    val opacity = Animatable(initialOpacity)
}

private data class WordRun(
    val generation: Int,
    val words: List<StreamWord>,
    val queue: Channel<StreamWord>,
)

private class WordSequence {
    private var previous = ""
    private var generation = 0
    private var words = linkedMapOf<Int, StreamWord>()
    private var queue = Channel<StreamWord>(Channel.UNLIMITED)

    fun accept(text: String, enabled: Boolean): WordRun {
        if (!text.startsWith(previous)) {
            generation++
            queue.close()
            queue = Channel(Channel.UNLIMITED)
            words = linkedMapOf()
        }
        val current = WordPattern.findAll(text).map { match ->
            val word = words.getOrPut(match.range.first) {
                StreamWord(match.range.first, if (enabled) 0f else 1f).also { queue.trySend(it) }
            }
            word.range = match.range
            word
        }.toList()
        previous = text
        return WordRun(generation, current, queue)
    }
}

private data class PaintedWord(val motion: StreamWord, val clip: Path)
private class StreamingParagraph {
    var layout: TextLayoutResult? = null
    var words: List<PaintedWord> = emptyList()
}
