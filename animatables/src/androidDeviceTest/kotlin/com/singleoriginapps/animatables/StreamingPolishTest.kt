package com.singleoriginapps.animatables

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.ceil
import kotlin.math.floor

/** Rendered contracts for queue ordering, append stability, native layout, and visible edge fades. */
class StreamingPolishTest {
    @get:Rule
    val compose = createComposeRule()

    private val style = TextStyle(
        color = Color.Black,
        fontFamily = FontFamily.Monospace,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    )

    @Test
    fun burstAppendJoinsTheOrderedFadeQueueWithoutDimmingEarlierWords() {
        compose.mainClock.autoAdvance = false
        val value = mutableStateOf("ALPHA BRAVO DELTA")
        var reference: TextLayoutResult? = null
        compose.setContent {
            MaterialTheme {
                Surface(color = Color.White) {
                    Column {
                        StreamingText(
                            value.value,
                            Modifier.width(240.dp).testTag("stream"),
                            style = style
                        )
                        Text(
                            value.value,
                            Modifier.width(240.dp),
                            style = style,
                            onTextLayout = { reference = it })
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(32)
        val early = compose.onNodeWithTag("stream").captureToImage()
        val alpha = requireNotNull(reference).getPathForRange(0, 5).getBounds()
        val bravo = requireNotNull(reference).getPathForRange(6, 11).getBounds()
        val delta = requireNotNull(reference).getPathForRange(12, 17).getBounds()
        assertTrue(
            "The first word begins before the second",
            ink(early, alpha) > ink(early, bravo) + 1f
        )
        assertTrue(
            "Later words must not all appear as a native-text fallback",
            ink(early, delta) < 1f
        )
        compose.mainClock.advanceTimeBy(64)
        val beforeAppend = compose.onNodeWithTag("stream").captureToImage()
        compose.runOnIdle { value.value += " ECHO FOXTROT" }
        compose.mainClock.advanceTimeBy(48)
        val afterAppend = compose.onNodeWithTag("stream").captureToImage()
        val echo = requireNotNull(reference).getPathForRange(18, 22).getBounds()
        assertTrue(
            "Appending a batch must not dim words already fading",
            ink(afterAppend, alpha) >= ink(beforeAppend, alpha)
        )
        assertTrue(
            "The older queued word must precede the new batch",
            ink(afterAppend, delta) > ink(afterAppend, echo)
        )
        compose.mainClock.advanceTimeBy(64)
        val later = compose.onNodeWithTag("stream").captureToImage()
        assertTrue("Earlier fades remain monotonic", ink(later, alpha) >= ink(afterAppend, alpha))
    }

    @Test
    fun adjacentWordsKeepSettlingTogether() {
        compose.mainClock.autoAdvance = false
        var reference: TextLayoutResult? = null
        compose.setContent {
            MaterialTheme {
                Surface(color = Color.White) {
                    Column {
                        StreamingText(
                            "ALPHA BRAVO",
                            Modifier.width(240.dp).testTag("stream"),
                            style = style
                        )
                        Text(
                            "ALPHA BRAVO", Modifier.width(240.dp), style = style,
                            onTextLayout = { reference = it })
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(208)
        val first = compose.onNodeWithTag("stream").captureToImage()
        compose.mainClock.advanceTimeBy(96)
        val second = compose.onNodeWithTag("stream").captureToImage()
        val alpha = requireNotNull(reference).getPathForRange(0, 5).getBounds()
        val bravo = requireNotNull(reference).getPathForRange(6, 11).getBounds()
        assertTrue(
            "The earlier word is still settling",
            ink(second, alpha) > ink(first, alpha) + 1f
        )
        assertTrue(
            "The following word settles at the same time",
            ink(second, bravo) > ink(first, bravo) + 1f
        )
    }

    @Test
    fun completingAPartialWordKeepsItsOpacityAndSettledPrefixSteady() {
        compose.mainClock.autoAdvance = false
        val value = mutableStateOf("ALPHA")
        var reference: TextLayoutResult? = null
        compose.setContent {
            MaterialTheme {
                Surface(color = Color.White) {
                    Column {
                        StreamingText(
                            value.value,
                            Modifier.width(240.dp).testTag("stream"),
                            style = style
                        )
                        Text(
                            value.value,
                            Modifier.width(240.dp),
                            style = style,
                            onTextLayout = { reference = it })
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(700)
        val settled = compose.onNodeWithTag("stream").captureToImage()
        val prefix = requireNotNull(reference).getPathForRange(0, 5).getBounds()
        compose.runOnIdle { value.value = "ALPHA BR" }
        compose.mainClock.advanceTimeBy(96)
        val partial = compose.onNodeWithTag("stream").captureToImage()
        val firstLetters = requireNotNull(reference).getPathForRange(6, 8).getBounds()
        val partialInk = ink(partial, firstLetters)
        assertTrue("A partial new word should be visibly fading", partialInk > 1f)
        assertEquals(
            "A settled prefix must retain identical ink",
            ink(settled, prefix),
            ink(partial, prefix),
            .01f
        )
        compose.runOnIdle { value.value = "ALPHA BRAVO" }
        compose.mainClock.advanceTimeBy(32)
        val completed = compose.onNodeWithTag("stream").captureToImage()
        assertTrue(
            "Completing the same word must not restart its opacity",
            ink(completed, firstLetters) >= partialInk
        )
        assertEquals(
            "Append leaves settled text steady",
            ink(settled, prefix),
            ink(completed, prefix),
            .01f
        )
    }

    @Test
    fun settledRtlItalicParagraphMatchesNativeTextPixels() {
        compose.mainClock.autoAdvance = false
        val text = "مرحبا بكم  في مساحة الأفكار\nA native italic paragraph."
        val italic = style.copy(fontStyle = FontStyle.Italic)
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(color = Color.White) {
                        Column {
                            StreamingText(
                                text,
                                Modifier.width(240.dp).testTag("stream"),
                                style = italic
                            )
                            Text(text, Modifier.width(240.dp).testTag("native"), style = italic)
                        }
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(1800)
        val streamed = compose.onNodeWithTag("stream").captureToImage()
        val native = compose.onNodeWithTag("native").captureToImage()
        assertEquals(native.width, streamed.width)
        assertEquals(native.height, streamed.height)
        val difference = pixelDifferenceFraction(streamed, native)
        exportFidelityFrames(streamed, native)
        assertTrue(
            "Settled shaping and wrapping should match native text (difference=$difference)",
            difference < .01f
        )
    }

    @Test
    fun activityCountLimitStillHoldsInsideAnExplicitlyLargerContainer() {
        compose.mainClock.autoAdvance = false
        val all = List(5) { "Entry $it" }
        val maximum = mutableStateOf(1)
        val history = mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                Surface(color = Color.White) {
                    ReasoningStream(
                        if (history.value) all else all.takeLast(maximum.value),
                        Modifier.width(240.dp).height(160.dp).testTag("activity"),
                        maxVisibleEntries = maximum.value, active = false, style = style
                    )
                }
            }
        }
        for (count in 1..3) {
            compose.runOnIdle { maximum.value = count; history.value = true }
            compose.mainClock.advanceTimeByFrame()
            val actual = compose.onNodeWithTag("activity").captureToImage()
            compose.runOnIdle { history.value = false }
            compose.mainClock.advanceTimeByFrame()
            val reference = compose.onNodeWithTag("activity").captureToImage()
            assertEquals(
                "Exactly $count entries render, even with excess container height", 0f,
                pixelDifferenceFraction(actual, reference), .001f
            )
        }
    }

    @Test
    fun activityDoesNotFadeWhenTheWindowGrowsWithoutScrolling() {
        compose.mainClock.autoAdvance = false
        val entries = mutableStateOf(List(1) { "████████████████" })
        val active = mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                Surface(color = Color.White) {
                    ReasoningStream(
                        entries.value, Modifier.width(240.dp).testTag("activity"),
                        active = active.value, style = style
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(700)
        compose.runOnIdle { entries.value += "████████████████" }
        compose.mainClock.advanceTimeBy(80)
        val growing = compose.onNodeWithTag("activity").captureToImage()
        compose.runOnIdle { active.value = false }
        compose.mainClock.advanceTimeByFrame()
        val static = compose.onNodeWithTag("activity").captureToImage()
        assertEquals(
            "Edges stay opaque when no text crosses the viewport", 0f,
            pixelDifferenceFraction(growing, static), .001f
        )
    }

    @Test
    fun activityFadesOnlyDuringTravelAndRendersExactlyTheRestingWindow() {
        compose.mainClock.autoAdvance = false
        val entries = mutableStateOf(List(2) { "████████████████" })
        val active = mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                Surface(color = Color.White) {
                    ReasoningStream(
                        entries.value, Modifier.width(240.dp).testTag("activity"),
                        active = active.value, style = style
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(700)
        compose.runOnIdle { entries.value += "████████████████" }
        compose.mainClock.advanceTimeBy(80)
        val moving = compose.onNodeWithTag("activity").captureToImage()
        compose.mainClock.advanceTimeBy(512)
        val settled = compose.onNodeWithTag("activity").captureToImage()
        // An exact pixel comparison includes the container edges: no third line, fading
        // history fragment, extra resting inset or residual mask may remain anywhere.
        compose.runOnIdle { entries.value = entries.value.takeLast(2); active.value = false }
        compose.mainClock.advanceTimeByFrame()
        val unmasked = compose.onNodeWithTag("activity").captureToImage()
        val middle = moving.height / 2
        assertTrue(
            "Outgoing text has a visible top-edge gradient",
            grayRows(moving, 0 until middle) > grayRows(unmasked, 0 until middle) + 3
        )
        assertTrue(
            "Incoming text has a visible bottom-edge gradient",
            grayRows(moving, middle until moving.height) > grayRows(
                unmasked,
                middle until unmasked.height
            ) + 3
        )
        assertEquals(
            "Only the two latest entries remain, fully opaque", 0f,
            pixelDifferenceFraction(settled, unmasked), .001f
        )
    }
}

private fun ink(image: ImageBitmap, bounds: Rect): Float {
    val pixels = image.toPixelMap()
    var total = 0f
    for (y in floor(bounds.top).toInt().coerceAtLeast(0) until ceil(bounds.bottom).toInt()
        .coerceAtMost(image.height)) {
        for (x in floor(bounds.left).toInt().coerceAtLeast(0) until ceil(bounds.right).toInt()
            .coerceAtMost(image.width)) {
            total += 1f - pixels[x, y].red
        }
    }
    return total
}

private fun pixelDifferenceFraction(first: ImageBitmap, second: ImageBitmap): Float {
    val a = first.toPixelMap()
    val b = second.toPixelMap()
    var different = 0
    for (y in 0 until first.height) for (x in 0 until first.width) {
        if (kotlin.math.abs(a[x, y].red - b[x, y].red) > .04f) different++
    }
    return different.toFloat() / (first.width * first.height)
}

private fun grayRows(image: ImageBitmap, rows: IntRange = 0 until image.height): Int {
    val pixels = image.toPixelMap()
    val x = image.width / 3
    return rows.count { pixels[x, it].red in .02f.. .98f }
}

private fun exportFidelityFrames(streamed: ImageBitmap, native: ImageBitmap) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val outputRoot = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        ?: requireNotNull(instrumentation.targetContext.getExternalFilesDir(null)).absolutePath
    val directory = File(outputRoot, "streaming-fidelity").apply { mkdirs() }
    File(directory, "streamed.png").outputStream().use {
        streamed.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
    }
    File(directory, "native.png").outputStream().use {
        native.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
    }
    val a = streamed.toPixelMap()
    val b = native.toPixelMap()
    val diff = Bitmap.createBitmap(streamed.width, streamed.height, Bitmap.Config.ARGB_8888)
    for (y in 0 until streamed.height) for (x in 0 until streamed.width) {
        val difference =
            (kotlin.math.abs(a[x, y].red - b[x, y].red) * 255f * 4f).toInt().coerceIn(0, 255)
        diff.setPixel(x, y, android.graphics.Color.rgb(difference, 0, difference))
    }
    File(directory, "difference.png").outputStream().use {
        diff.compress(Bitmap.CompressFormat.PNG, 100, it)
    }
}
