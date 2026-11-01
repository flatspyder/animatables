package com.singleoriginapps.animatables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

class CompletionSpinnerTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun successEntranceMatchesAtEverySampledFrame() {
        val complete = mutableStateOf(false)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme(outline = Color.Transparent)) {
                Row(Modifier.testTag("pair")) {
                    Box(
                        Modifier.size(128.dp).background(Color.White).testTag("spinner"),
                        contentAlignment = Alignment.Center
                    ) {
                        CompletionSpinner(
                            complete = complete.value, color = Color.Transparent,
                            trackColor = Color.Transparent
                        )
                    }
                    Box(
                        Modifier.size(128.dp).background(Color.White).testTag("success"),
                        contentAlignment = Alignment.Center
                    ) { SuccessCheck(complete = complete.value) }
                }
            }
        }
        compose.runOnIdle { complete.value = true }
        var previousSample = 0L
        listOf(32L, 80L, 112L, 160L, 300L, 600L).forEach { elapsed ->
            compose.mainClock.advanceTimeBy(elapsed - previousSample)
            previousSample = elapsed
            // Capture both siblings in one rendered frame. Separate capture requests can span
            // a draw and compare different animation instants despite the paused test clock.
            val pair = compose.onNodeWithTag("pair").captureToImage()
            val pixels = pair.toPixelMap()
            val halfWidth = pair.width / 2
            var different = 0
            // Filtered moving edges can differ by one or two 8-bit levels between sibling
            // layers. Require perceptual equality in motion and exact equality once settled.
            val tolerance = if (elapsed == 600L) 0f else 2f / 255f
            var largestDifference = 0f
            for (y in 0 until pair.height) for (x in 0 until halfWidth) {
                val left = pixels[x, y]
                val right = pixels[x + halfWidth, y]
                val delta = maxOf(
                    abs(left.red - right.red), abs(left.green - right.green),
                    abs(left.blue - right.blue), abs(left.alpha - right.alpha)
                )
                largestDifference = maxOf(largestDifference, delta)
                if (delta > tolerance) different++
            }
            assertEquals(
                "Success entrance differs after $elapsed ms (max delta $largestDifference)",
                0,
                different
            )
        }
    }

    @Test
    fun customSuccessIconIsCenteredAndInheritsContentColor() {
        compose.setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primaryContainer = Color.Blue,
                    onPrimaryContainer = Color.Red
                )
            ) {
                AnimatablesMotion(false) {
                    Row {
                        val icon: @androidx.compose.runtime.Composable () -> Unit = {
                            Box(Modifier.fillMaxSize().background(LocalContentColor.current))
                        }
                        Box(Modifier.size(48.dp).testTag("spinner")) {
                            CompletionSpinner(complete = true, successIcon = icon)
                        }
                        Box(Modifier.size(48.dp).testTag("success")) {
                            SuccessCheck(complete = true, successIcon = icon)
                        }
                    }
                }
            }
        }
        val spinner = compose.onNodeWithTag("spinner").captureToImage()
        assertEquals(
            0,
            differentSpinnerPixels(spinner, compose.onNodeWithTag("success").captureToImage())
        )
        val pixels = spinner.toPixelMap()
        val center = pixels[spinner.width / 2, spinner.height / 2]
        val edge = pixels[spinner.width / 10, spinner.height / 2]
        assertTrue("Icon inherits the content color", center.red > .9f && center.blue < .1f)
        assertTrue(
            "Icon stays inside its centered half-size slot",
            edge.blue > .9f && edge.red < .1f
        )
    }

    @Test
    fun determinateProgressClampsAndCompletesWithoutLatching() {
        val progress = mutableFloatStateOf(-.5f)
        compose.setContent {
            MaterialTheme {
                AnimatablesMotion(false) { CompletionSpinner(progress = { progress.floatValue }) }
            }
        }
        fun assertProgress(expected: Float) {
            compose.onNodeWithContentDescription("Loading").assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo(expected, 0f..1f),
                ),
            )
        }
        assertProgress(0f)
        compose.runOnIdle { progress.floatValue = .65f }
        assertProgress(.65f)
        compose.runOnIdle { progress.floatValue = Float.NaN }
        assertProgress(0f)
        compose.runOnIdle { progress.floatValue = 1f }
        compose.onNodeWithContentDescription("Complete").assertExists()
        compose.onNodeWithContentDescription("Loading").assertDoesNotExist()
        compose.runOnIdle { progress.floatValue = .2f }
        assertProgress(.2f)
        compose.runOnIdle { progress.floatValue = Float.POSITIVE_INFINITY }
        compose.onNodeWithContentDescription("Complete").assertExists()
    }

    @Test
    fun explicitCompletionOverridesPartialProgressAndResetReturnsToLoading() {
        val complete = mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                CompletionSpinner(
                    progress = { .3f }, complete = complete.value,
                    label = "Sending", completeLabel = "Sent"
                )
            }
        }
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { complete.value = true }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithContentDescription("Sent").assertExists()
        compose.onNodeWithContentDescription("Sending").assertDoesNotExist()
        compose.runOnIdle { complete.value = false }
        compose.mainClock.advanceTimeBy(32)
        compose.onNodeWithContentDescription("Sending").assertExists()
        compose.onNodeWithContentDescription("Sent").assertDoesNotExist()
    }

    @Test
    fun interruptedCompletionSettlesToSameCheckAsDisabledMotion() {
        val complete = mutableStateOf(false)
        val enabled = mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                AnimatablesMotion(enabled.value) {
                    Box(
                        Modifier.size(96.dp).background(MaterialTheme.colorScheme.surface)
                            .testTag("capture"), contentAlignment = Alignment.Center
                    ) {
                        CompletionSpinner(progress = { .4f }, complete = complete.value)
                    }
                }
            }
        }
        val loading = compose.onNodeWithTag("capture").captureToImage()
        compose.runOnIdle { complete.value = true }
        compose.mainClock.advanceTimeBy(112)
        val middle = compose.onNodeWithTag("capture").captureToImage()
        assertTrue("Completion needs a visible handoff", changedSpinnerPixels(loading, middle) > 20)
        compose.runOnIdle { complete.value = false }
        compose.mainClock.advanceTimeBy(48)
        compose.runOnIdle { complete.value = true }
        compose.mainClock.advanceTimeBy(3000)
        val settled = compose.onNodeWithTag("capture").captureToImage()
        compose.onNodeWithContentDescription("Complete").assertExists()
        compose.runOnIdle { enabled.value = false }
        compose.mainClock.advanceTimeByFrame()
        val disabled = compose.onNodeWithTag("capture").captureToImage()
        assertEquals(
            "Interrupted motion must reach the complete resting check", 0,
            changedSpinnerPixels(settled, disabled)
        )
    }

    @Test
    fun initiallyCompleteDoesNotReplayCelebrationOnMount() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                Box(
                    Modifier.size(96.dp).background(MaterialTheme.colorScheme.surface)
                        .testTag("capture"), contentAlignment = Alignment.Center
                ) {
                    CompletionSpinner(complete = true)
                }
            }
        }
        val initial = compose.onNodeWithTag("capture").captureToImage()
        compose.mainClock.advanceTimeBy(112)
        val middle = compose.onNodeWithTag("capture").captureToImage()
        compose.mainClock.advanceTimeBy(2000)
        assertEquals(0, changedSpinnerPixels(initial, middle))
        assertEquals(
            0, changedSpinnerPixels(
                initial,
                compose.onNodeWithTag("capture").captureToImage()
            )
        )
    }

    @Test
    fun appDisabledMotionKeepsBothIndeterminateStylesStaticAndTruthful() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                AnimatablesMotion(false) {
                    Row(Modifier.background(MaterialTheme.colorScheme.surface).testTag("capture")) {
                        CompletionSpinner(label = "Standard loading")
                        CompletionSpinner(
                            style = CompletionSpinnerStyle.Wavy,
                            label = "Wavy loading"
                        )
                    }
                }
            }
        }
        val initial = compose.onNodeWithTag("capture").captureToImage()
        compose.mainClock.advanceTimeBy(1200)
        assertEquals(
            0, changedSpinnerPixels(
                initial,
                compose.onNodeWithTag("capture").captureToImage()
            )
        )
        listOf("Standard loading", "Wavy loading").forEach {
            compose.onNodeWithContentDescription(it).assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate
                )
            )
        }
    }
}

class CompletionSpinnerZeroScaleTest {
    @get:Rule
    val compose = createComposeRule(effectContext = object : MotionDurationScale {
        override val scaleFactor = 0f
    })

    @Test
    fun systemDisabledMotionStopsSpinnersAndCompletesImmediately() {
        val complete = mutableStateOf(false)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                Row(Modifier.background(MaterialTheme.colorScheme.surface).testTag("capture")) {
                    CompletionSpinner(
                        complete = complete.value,
                        label = "Standard loading",
                        completeLabel = "Standard done"
                    )
                    CompletionSpinner(
                        complete = complete.value, style = CompletionSpinnerStyle.Wavy,
                        label = "Wavy loading", completeLabel = "Wavy done"
                    )
                }
            }
        }
        val initial = compose.onNodeWithTag("capture").captureToImage()
        compose.mainClock.advanceTimeBy(1200)
        assertEquals(
            0, changedSpinnerPixels(
                initial,
                compose.onNodeWithTag("capture").captureToImage()
            )
        )
        compose.runOnIdle { complete.value = true }
        compose.mainClock.advanceTimeByFrame()
        val completed = compose.onNodeWithTag("capture").captureToImage()
        compose.mainClock.advanceTimeBy(1200)
        assertEquals(
            0, changedSpinnerPixels(
                completed,
                compose.onNodeWithTag("capture").captureToImage()
            )
        )
        compose.onNodeWithContentDescription("Standard done").assertExists()
        compose.onNodeWithContentDescription("Wavy done").assertExists()
    }
}

private fun changedSpinnerPixels(first: ImageBitmap, second: ImageBitmap): Int {
    assertEquals(first.width, second.width)
    assertEquals(first.height, second.height)
    val a = first.toPixelMap()
    val b = second.toPixelMap()
    var changed = 0
    for (y in 0 until first.height) for (x in 0 until first.width) {
        val left = a[x, y]
        val right = b[x, y]
        if (abs(left.red - right.red) + abs(left.green - right.green) + abs(left.blue - right.blue) > .03f) changed++
    }
    return changed
}

private fun differentSpinnerPixels(first: ImageBitmap, second: ImageBitmap): Int {
    assertEquals(first.width, second.width)
    assertEquals(first.height, second.height)
    val a = first.toPixelMap()
    val b = second.toPixelMap()
    var different = 0
    for (y in 0 until first.height) for (x in 0 until first.width) {
        if (a[x, y] != b[x, y]) different++
    }
    return different
}
