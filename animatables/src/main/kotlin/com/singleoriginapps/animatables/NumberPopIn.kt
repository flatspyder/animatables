package com.singleoriginapps.animatables

import android.icu.text.BreakIterator
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/** Chooses which part of a formatted numeric run remains anchored as its width changes. */
public enum class NumberFlowAnchor {
    /** The complete run's horizontal centre stays fixed; width changes grow on both sides. */
    Center,
    /**
     * The decimal boundary (the right edge of the units column for integers) stays at the centre
     * of the available viewport. Place columns stay at the same x even during carry/borrow.
     * Prefer an explicit width for a column of values; intrinsic sizing reserves space either side.
     */
    Decimal,
}

/**
 * Rolls changed digits in stable place-value columns, with a predictable horizontal anchor.
 *
 * The supplied string is never converted to Float, rounded or reformatted. Integer columns are
 * identified from the units place; fraction columns from [decimalSeparator]. Currency, signs,
 * grouping, Unicode decimal digits and suffixes retain the supplied glyphs. Prefix/suffix prose
 * is shaped as a native text run. Numeric runs retain their supplied order in an RTL host.
 * Pass the actual locale's separator (for example ',' or '\u066B'); it cannot safely be inferred
 * from a formatted string such as "1,234". Scientific notation is displayed, not interpreted.
 *
 * [anchor] defaults to a centred run. Use [NumberFlowAnchor.Decimal] when unchanged places must
 * retain their exact x while the number gains or loses digits. Tabular font features plus equal
 * measured digit slots prevent proportional-font jitter. The host controls available width and
 * text size; overflowing values clip rather than shrink or paint outside their bounds.
 *
 * Changed digits roll in the numeric trend's direction; unchanged digits stay still. Repeated
 * updates retarget the current reel position. Initial values are readable at rest; [replayKey]
 * explicitly requests a one-step roll/fade entrance. Standard
 * Material spatial tokens avoid bounce in numeric data; effects use the host's matching default
 * tier. Gentle reel-edge fading appears only during movement. App/system-disabled motion draws
 * the exact final state immediately. One complete accessible value replaces the visual columns.
 */
@Composable
public fun NumberPopIn(
    value: String,
    modifier: Modifier = Modifier,
    replayKey: Any = Unit,
    style: TextStyle = MaterialTheme.typography.displayMedium,
    decimalSeparator: Char = '.',
    anchor: NumberFlowAnchor = NumberFlowAnchor.Center,
) {
    val enabled = rememberMotionEnabled()
    val parsed = remember(value, decimalSeparator) { parseNumber(value, decimalSeparator) }
    val state = remember { NumberFlowState() }
    val measurer = rememberTextMeasurer(cacheSize = 128)
    val resolvedColor = style.color.takeOrElse { LocalContentColor.current }
    val numericStyle = style.copy(
        color = resolvedColor,
        fontFeatureSettings = listOfNotNull(style.fontFeatureSettings?.takeIf { it.isNotBlank() }, "tnum")
            .joinToString(", "),
    )
    val digitBases = (parsed.tokens.mapNotNull { it.digitBase } +
        state.slots.values.mapNotNull { it.token.digitBase } + '0'.code).toSet()
    val measuredStrings = buildSet {
        digitBases.forEach { base -> repeat(10) { add(codePointString(base + it)) } }
        parsed.tokens.filter { it.digit == null }.forEach { add(it.text) }
        state.slots.values.forEach { slot ->
            if (slot.token.digit == null) add(slot.token.text)
            slot.previousText?.let { add(it) }
        }
    }
    val layouts = measuredStrings.associateWith {
        measurer.measure(it, numericStyle, layoutDirection = LayoutDirection.Ltr, maxLines = 1, softWrap = false)
    }
    val digits = digitBases.flatMap { base -> (0..9).map { layouts.getValue(codePointString(base + it)) } }
    val digitWidth = digits.maxOfOrNull { it.size.width }?.toFloat()?.coerceAtLeast(1f) ?: 1f
    val fontMetrics = layouts.getValue("0")
    val lineStep = digits.maxOfOrNull { it.size.height }?.toFloat()?.coerceAtLeast(1f) ?: 1f
    val verticalPadding = lineStep * .22f
    val baseline = fontMetrics.firstBaseline + verticalPadding
    val desiredHeight = ceil(lineStep + verticalPadding * 2f).toInt()
    val geometry = numberGeometry(parsed, digitWidth, layouts, anchor)
    val spatialSpec = MotionScheme.standard().defaultSpatialSpec<Float>()
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    LaunchedEffect(parsed, geometry, replayKey, enabled) {
        val old = state.parsed
        val trend = if (old == null) 1 else compareNumbers(old, parsed)
        val replay = old != null && state.replayKey != replayKey
        state.parsed = parsed
        state.replayKey = replayKey
        val first = !state.initialized
        if (first || !enabled) {
            state.width.snapTo(geometry.width)
            state.centerShift.snapTo(geometry.centerShift)
        }
        state.initialized = true
        val targetKeys = geometry.positions.keys
        state.slots.values.forEach { it.present = it.token.key in targetKeys }
        geometry.positions.forEach { (key, position) ->
            if (state.slots[key] == null) {
                state.slots[key] = FlowSlot(position.token, position.x, enabled && old != null, trend)
            }
        }
        coroutineScope {
            launch { if (enabled) state.width.animateTo(geometry.width, spatialSpec) }
            launch { if (enabled) state.centerShift.animateTo(geometry.centerShift, spatialSpec) }
            state.slots.values.toList().forEach { slot ->
                val destination = geometry.positions[slot.token.key]
                if (destination == null) {
                    launch {
                        if (enabled) slot.opacity.animateTo(0f, effectsSpec) else slot.opacity.snapTo(0f)
                        if (!slot.present) state.slots.remove(slot.token.key)
                    }
                } else {
                    slot.present = true
                    val previousToken = slot.token
                    val changed = previousToken.text != destination.token.text
                    slot.token = destination.token
                    launch {
                        if (enabled) slot.x.animateTo(destination.x, spatialSpec) else slot.x.snapTo(destination.x)
                    }
                    launch {
                        if (replay && enabled) slot.opacity.snapTo(0f)
                        if (enabled) slot.opacity.animateTo(1f, effectsSpec) else slot.opacity.snapTo(1f)
                    }
                    val digit = destination.token.digit
                    if (digit != null) {
                        launch {
                            if (!enabled) {
                                slot.reel.snapTo(digit.toFloat())
                            } else if (changed || first || replay || !slot.settled) {
                                if (replay) slot.reel.snapTo(digit - 1f)
                                val direction = if (trend != 0) trend else {
                                    digit.compareTo(previousToken.digit ?: digit)
                                }
                                val target = reelTarget(slot.reel.value, digit, direction)
                                slot.settled = false
                                slot.reel.animateTo(target, spatialSpec)
                                // Modulo-equivalent normalization avoids growing Float magnitudes.
                                slot.reel.snapTo(digit.toFloat())
                                slot.settled = true
                            }
                        }
                    } else if (changed || replay) {
                        launch {
                            slot.previousText = if (changed) previousToken.text else null
                            slot.symbolMix.snapTo(if (enabled) 0f else 1f)
                            if (enabled) slot.symbolMix.animateTo(1f, effectsSpec)
                            slot.previousText = null
                        }
                    }
                }
            }
        }
    }
    val slots = state.slots.values.toList()
    Layout(
        modifier = modifier.clearAndSetSemantics { contentDescription = value }.clipToBounds(),
        content = {
            Canvas(Modifier) {
                val shift = if (enabled && state.initialized) state.centerShift.value else geometry.centerShift
                val origin = size.width / 2f + shift
                if (!enabled || !state.initialized) {
                    geometry.positions.values.forEach { position ->
                        val token = position.token
                        val glyph = layouts.getValue(token.text)
                        val x = origin + position.x + when {
                            token.digit != null -> (digitWidth - glyph.size.width) / 2f
                            token.prefix -> -glyph.size.width.toFloat()
                            else -> 0f
                        }
                        drawText(glyph, topLeft = Offset(x, baseline - glyph.firstBaseline))
                    }
                } else {
                    slots.forEach { slot ->
                        val alpha = slot.opacity.value.coerceIn(0f, 1f)
                        if (alpha > .001f) {
                            val x = origin + slot.x.value
                            val token = slot.token
                            if (token.digit != null) {
                                drawDigitReel(slot, x, alpha, digitWidth, lineStep, baseline, layouts)
                            } else {
                                val mix = slot.symbolMix.value.coerceIn(0f, 1f)
                                slot.previousText?.let { previous ->
                                    layouts[previous]?.let { glyph ->
                                        drawText(glyph, alpha = alpha * (1f - mix), topLeft = Offset(
                                            x - if (token.prefix) glyph.size.width else 0,
                                            baseline - glyph.firstBaseline,
                                        ))
                                    }
                                }
                                layouts[token.text]?.let { glyph ->
                                    drawText(glyph, alpha = alpha * mix, topLeft = Offset(
                                        x - if (token.prefix) glyph.size.width else 0,
                                        baseline - glyph.firstBaseline,
                                    ))
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { measurables, constraints ->
        val animatedWidth = if (enabled && state.initialized) state.width.value else geometry.width
        // Even intrinsic widths keep the centre on the same pixel as the parent remeasures.
        val evenWidth = (ceil(animatedWidth / 2f).toInt() * 2).coerceAtLeast(2)
        val width = constraints.constrainWidth(evenWidth)
        val height = constraints.constrainHeight(desiredHeight)
        val canvas = measurables.single().measure(Constraints.fixed(width, height))
        layout(width, height) { canvas.place(0, 0) }
    }
}

private fun DrawScope.drawDigitReel(
    slot: FlowSlot,
    x: Float,
    alpha: Float,
    digitWidth: Float,
    step: Float,
    baseline: Float,
    layouts: Map<String, TextLayoutResult>,
) {
    val position = slot.reel.value
    val low = floor(position).toInt()
    val high = ceil(position).toInt()
    val moving = !slot.settled || alpha < .999f
    fun DrawScope.drawGlyphs() {
        for (index in low..high) {
            val digit = ((index % 10) + 10) % 10
            val glyph = layouts[codePointString(slot.token.digitBase!! + digit)] ?: continue
            drawText(
                glyph,
                alpha = alpha,
                topLeft = Offset(
                    x + (digitWidth - glyph.size.width) / 2f,
                    baseline - glyph.firstBaseline + (index - position) * step,
                ),
            )
        }
    }
    if (!moving) {
        drawGlyphs()
    } else {
        clipRect(left = x - 1f, right = x + digitWidth + 1f, top = 0f, bottom = size.height) {
            drawIntoCanvas { canvas ->
                canvas.saveLayer(Rect(x - 1f, 0f, x + digitWidth + 1f, size.height), Paint())
                drawGlyphs()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        .15f to Color.Black,
                        .85f to Color.Black,
                        1f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
                canvas.restore()
            }
        }
    }
}

private class NumberFlowState {
    val slots = mutableStateMapOf<String, FlowSlot>()
    val width = Animatable(0f)
    val centerShift = Animatable(0f)
    var initialized by mutableStateOf(false)
    var parsed: ParsedNumber? = null
    var replayKey: Any = Unit
}

private class FlowSlot(initial: NumberToken, initialX: Float, enabled: Boolean, direction: Int) {
    var token by mutableStateOf(initial)
    var previousText by mutableStateOf<String?>(null)
    var present = true
    var settled = !enabled
    val x = Animatable(initialX)
    val reel = Animatable((initial.digit ?: 0).toFloat() -
        if (enabled && initial.digit != null) { if (direction < 0) -1f else 1f } else 0f)
    val opacity = Animatable(if (enabled) 0f else 1f)
    val symbolMix = Animatable(1f)
}

private data class NumberToken(
    val key: String,
    val text: String,
    val digit: Int? = null,
    val digitBase: Int? = null,
    val prefix: Boolean = false,
)
private data class ParsedNumber(
    val tokens: List<NumberToken>,
    val boundary: Int,
    val integer: List<Int>,
    val fraction: List<Int>,
    val negative: Boolean,
)
private data class NumberPosition(val token: NumberToken, val x: Float)
private data class NumberGeometry(val positions: Map<String, NumberPosition>, val width: Float, val centerShift: Float)

private fun numberGeometry(
    parsed: ParsedNumber,
    digitWidth: Float,
    layouts: Map<String, TextLayoutResult>,
    anchor: NumberFlowAnchor,
): NumberGeometry {
    val widths = parsed.tokens.map { if (it.digit != null) digitWidth else layouts.getValue(it.text).size.width.toFloat() }
    val left = widths.take(parsed.boundary).sum()
    val right = widths.drop(parsed.boundary).sum()
    var cursor = -left
    val positions = parsed.tokens.mapIndexed { index, token ->
        val x = cursor + if (token.prefix) widths[index] else 0f
        cursor += widths[index]
        token.key to NumberPosition(token, x)
    }.toMap()
    return NumberGeometry(
        positions,
        if (anchor == NumberFlowAnchor.Decimal) 2f * max(left, right) else left + right,
        if (anchor == NumberFlowAnchor.Decimal) 0f else (left - right) / 2f,
    )
}

private fun parseNumber(value: String, separator: Char): ParsedNumber {
    val glyphs = graphemes(value)
    val digitValues = glyphs.map { glyph ->
        if (glyph.codePointCount(0, glyph.length) == 1) Character.digit(glyph.codePointAt(0), 10).takeIf { it >= 0 } else null
    }
    val first = digitValues.indexOfFirst { it != null }
    val last = digitValues.indexOfLast { it != null }
    if (first < 0) {
        return ParsedNumber(if (value.isEmpty()) emptyList() else listOf(NumberToken("literal", value)), 0, emptyList(), emptyList(), false)
    }
    val decimal = glyphs.indices.lastOrNull { index ->
        glyphs[index] == separator.toString() && index >= first - 1 && (index <= last || index == last + 1)
    }
    val numericEnd = max(last, decimal ?: last)
    val numericStart = minOf(first, decimal ?: first)
    val prefix = glyphs.take(numericStart).joinToString("")
    val suffix = glyphs.drop(numericEnd + 1).joinToString("")
    val integerIndices = (first..last).filter { digitValues[it] != null && (decimal == null || it < decimal) }
    val fractionIndices = (first..last).filter { digitValues[it] != null && decimal != null && it > decimal }
    val tokens = mutableListOf<NumberToken>()
    if (prefix.isNotEmpty()) tokens += NumberToken("prefix", prefix, prefix = true)
    var index = numericStart
    var boundary = -1
    while (index <= numericEnd) {
        if (index == decimal) {
            boundary = tokens.size
            tokens += NumberToken("decimal", glyphs[index])
            index++
        } else if (digitValues[index] != null) {
            val fraction = decimal != null && index > decimal
            val place = if (fraction) -(fractionIndices.indexOf(index) + 1) else integerIndices.size - integerIndices.indexOf(index) - 1
            val digit = digitValues[index]!!
            tokens += NumberToken("digit:$place", glyphs[index], digit, glyphs[index].codePointAt(0) - digit)
            index++
        } else {
            val start = index
            while (index <= numericEnd && digitValues[index] == null && index != decimal) index++
            val fraction = decimal != null && start > decimal
            val rightDigits = if (fraction) fractionIndices.count { it >= index } else integerIndices.count { it >= index }
            tokens += NumberToken("mark:${if (fraction) "fraction" else "integer"}:$rightDigits", glyphs.subList(start, index).joinToString(""))
        }
    }
    if (boundary < 0) boundary = tokens.size
    if (suffix.isNotEmpty()) tokens += NumberToken("suffix", suffix)
    val negative = prefix.any { it == '-' || it == '−' || it == '﹣' || it == '－' } || (prefix.contains('(') && suffix.contains(')'))
    return ParsedNumber(tokens, boundary, integerIndices.map { digitValues[it]!! }, fractionIndices.map { digitValues[it]!! }, negative)
}

/** Magnitude comparison uses digit strings, so long counters never lose precision or overflow. */
private fun compareNumbers(old: ParsedNumber, new: ParsedNumber): Int {
    val oldInteger = old.integer.dropWhile { it == 0 }
    val newInteger = new.integer.dropWhile { it == 0 }
    val oldZero = oldInteger.isEmpty() && old.fraction.all { it == 0 }
    val newZero = newInteger.isEmpty() && new.fraction.all { it == 0 }
    val oldNegative = old.negative && !oldZero
    val newNegative = new.negative && !newZero
    if (oldNegative != newNegative) return if (newNegative) -1 else 1
    var comparison = newInteger.size.compareTo(oldInteger.size)
    if (comparison == 0) {
        for (index in newInteger.indices) {
            comparison = newInteger[index].compareTo(oldInteger[index])
            if (comparison != 0) break
        }
    }
    if (comparison == 0) {
        for (index in 0 until max(old.fraction.size, new.fraction.size)) {
            comparison = new.fraction.getOrElse(index) { 0 }.compareTo(old.fraction.getOrElse(index) { 0 })
            if (comparison != 0) break
        }
    }
    return if (newNegative) -comparison else comparison
}

private fun reelTarget(current: Float, digit: Int, direction: Int): Float {
    var target = floor(current / 10f) * 10f + digit
    if (direction > 0 && target < current - .001f) target += 10f
    if (direction < 0 && target > current + .001f) target -= 10f
    if (direction == 0) {
        if (target - current > 5f) target -= 10f
        if (current - target > 5f) target += 10f
    }
    return target
}

private fun codePointString(codePoint: Int): String = String(Character.toChars(codePoint))

private fun graphemes(value: String): List<String> {
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
