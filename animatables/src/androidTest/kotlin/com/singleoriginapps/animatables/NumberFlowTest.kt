package com.singleoriginapps.animatables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** MP-01: place retention, carries/borrows, centre anchoring and exact static formatted states. */
class NumberFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun carryAndBorrowRollWhileTheCompleteRunKeepsItsCentre() {
        compose.mainClock.autoAdvance = false
        val value = mutableStateOf("9")
        val motion = mutableStateOf(true)
        compose.setContent {
            FlowStage {
                AnimatablesMotion(motion.value) {
                    NumberPopIn(value.value, Modifier.testTag("number"), style = FlowStyle)
                }
            }
        }
        compose.mainClock.advanceTimeBy(1600)
        val originalBounds = compose.onNodeWithTag("number").getUnclippedBoundsInRoot()
        val originalCenter = (originalBounds.left.value + originalBounds.right.value) / 2f
        for (next in listOf("10", "99", "100", "99", "9")) {
            compose.runOnIdle { value.value = next }
            compose.mainClock.advanceTimeBy(96)
            val moving = compose.onNodeWithTag("stage").captureToImage()
            val movingBounds = compose.onNodeWithTag("number").getUnclippedBoundsInRoot()
            assertEquals("Run centre must stay fixed during $next", originalCenter,
                (movingBounds.left.value + movingBounds.right.value) / 2f, .35f)
            compose.mainClock.advanceTimeBy(1600)
            val resting = compose.onNodeWithTag("stage").captureToImage()
            assertTrue("Carry/borrow to $next must visibly roll", pixelDifference(moving, resting) > 20)
            compose.onAllNodesWithContentDescription(next).assertCountEquals(1)
            compose.runOnIdle { motion.value = false }
            compose.mainClock.advanceTimeBy(32)
            assertEquals("Animated $next must settle to its exact static rendering", 0,
                pixelDifference(resting, compose.onNodeWithTag("stage").captureToImage()))
            compose.runOnIdle { motion.value = true }
            compose.mainClock.advanceTimeBy(32)
        }
    }

    @Test fun decimalAnchorKeepsUnchangedPlacesAtExactlyTheSameXDuringCarry() {
        compose.mainClock.autoAdvance = false
        val value = mutableStateOf("19")
        var cropWidth = 0
        compose.setContent {
            cropWidth = with(LocalDensity.current) { 26.dp.roundToPx() }
            FlowStage {
                NumberPopIn(value.value, Modifier.testTag("number"), style = FlowStyle,
                    anchor = NumberFlowAnchor.Decimal)
            }
        }
        compose.mainClock.advanceTimeBy(1600)
        val before = compose.onNodeWithTag("stage").captureToImage()
        compose.runOnIdle { value.value = "119" }
        compose.mainClock.advanceTimeBy(96)
        val during = compose.onNodeWithTag("stage").captureToImage()
        val center = before.width / 2
        assertTrue("The retained-place crop must actually contain digit ink", inkPixels(before, center - cropWidth, center) > 20)
        assertEquals("Retained units/tens pixels must not slide or replay", 0,
            pixelDifference(before, during, center - cropWidth, center))
        compose.mainClock.advanceTimeBy(1600)
        assertEquals("Retained places must keep their final x too", 0,
            pixelDifference(before, compose.onNodeWithTag("stage").captureToImage(), center - cropWidth, center))
    }

    @Test fun unchangedEqualLengthColumnsDoNotReplayWithProportionalFonts() {
        compose.mainClock.autoAdvance = false
        val value = mutableStateOf("181")
        var edgeWidth = 0
        compose.setContent {
            edgeWidth = with(LocalDensity.current) { 10.dp.roundToPx() }
            FlowStage { NumberPopIn(value.value, style = FlowStyle, anchor = NumberFlowAnchor.Decimal) }
        }
        compose.mainClock.advanceTimeBy(1600)
        val before = compose.onNodeWithTag("stage").captureToImage()
        compose.runOnIdle { value.value = "191" }
        compose.mainClock.advanceTimeBy(96)
        val during = compose.onNodeWithTag("stage").captureToImage()
        val center = before.width / 2
        assertTrue("The retained units crop must contain visible ink", inkPixels(before, center - edgeWidth, center) > 20)
        assertEquals("The unchanged units1 must remain completely still", 0,
            pixelDifference(before, during, center - edgeWidth, center))
        assertTrue("The changed tens column must move", pixelDifference(before, during) > 20)
    }

    @Test fun localeFormattingAndLongValuesSettleExactlyWithoutNumericConversion() {
        compose.mainClock.autoAdvance = false
        val value = mutableStateOf("−€1\u202f234,50%")
        val separator = mutableStateOf(',')
        val enabled = mutableStateOf(true)
        compose.setContent {
            FlowStage {
                AnimatablesMotion(enabled.value) {
                    NumberPopIn(value.value, Modifier.testTag("number"), style = FlowStyle,
                        decimalSeparator = separator.value)
                }
            }
        }
        val formats = listOf(
            "−€1\u202f234,50%" to ',',
            "١٬٢٣٤٫٥٠\u00a0ر.س" to '\u066B',
            "₹१२,३४,५६७.८९" to '.',
            ".50%" to '.',
            "−,75\u00a0€" to ',',
            "123456789012345678901234567890.12" to '.',
        )
        for ((formatted, decimal) in formats) {
            compose.runOnIdle { value.value = formatted; separator.value = decimal; enabled.value = true }
            compose.mainClock.advanceTimeBy(2000)
            val settled = compose.onNodeWithTag("stage").captureToImage()
            compose.onAllNodesWithContentDescription(formatted).assertCountEquals(1)
            compose.runOnIdle { enabled.value = false }
            compose.mainClock.advanceTimeBy(32)
            assertEquals("Full localized value must survive animation unchanged: $formatted", 0,
                pixelDifference(settled, compose.onNodeWithTag("stage").captureToImage()))
            val bounds = compose.onNodeWithTag("number").getUnclippedBoundsInRoot()
            assertTrue("Long values must obey available width", (bounds.right - bounds.left) <= 300.dp)
        }
    }

    @Test fun rapidReversalsFinishAtTheNewestValueWithoutOldColumns() {
        compose.mainClock.autoAdvance = false
        val value = mutableStateOf("99")
        val enabled = mutableStateOf(true)
        compose.setContent {
            FlowStage {
                AnimatablesMotion(enabled.value) {
                    NumberPopIn(value.value, style = FlowStyle)
                }
            }
        }
        compose.mainClock.advanceTimeBy(1600)
        for (next in listOf("100", "98", "102.50", "−0.01", "0.00")) {
            compose.runOnIdle { value.value = next }
            compose.mainClock.advanceTimeBy(64)
        }
        compose.mainClock.advanceTimeBy(2000)
        val settled = compose.onNodeWithTag("stage").captureToImage()
        compose.onAllNodesWithContentDescription("0.00").assertCountEquals(1)
        compose.runOnIdle { enabled.value = false }
        compose.mainClock.advanceTimeBy(32)
        assertEquals("Rapid reversals must leave precisely the latest digits", 0,
            pixelDifference(settled, compose.onNodeWithTag("stage").captureToImage()))
    }

    @Test fun initialValueIsReadableAndOnlyExplicitReplayRestartsIt() {
        compose.mainClock.autoAdvance = false
        val replay = mutableStateOf(0)
        compose.setContent { FlowStage { NumberPopIn("65.78", replayKey = replay.value, style = FlowStyle) } }
        compose.mainClock.advanceTimeBy(32)
        val initial = compose.onNodeWithTag("stage").captureToImage()
        compose.mainClock.advanceTimeBy(1600)
        val settled = compose.onNodeWithTag("stage").captureToImage()
        assertEquals("Initial values should not pop into an empty stage", 0, pixelDifference(initial, settled))
        compose.runOnIdle { replay.value++ }
        compose.mainClock.advanceTimeBy(96)
        val replaying = compose.onNodeWithTag("stage").captureToImage()
        assertTrue("Explicit replay should visibly roll", pixelDifference(replaying, settled) > 20)
        compose.mainClock.advanceTimeBy(1600)
        assertEquals(0, pixelDifference(settled, compose.onNodeWithTag("stage").captureToImage()))
    }

    @Test fun constrainedViewportClipsInsteadOfPaintingOutsideItsBounds() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            FlowStage {
                AnimatablesMotion(false) {
                    NumberPopIn("−12345678901234567890.00%", Modifier.width(100.dp).testTag("number"), style = FlowStyle)
                }
            }
        }
        val bitmap = compose.onNodeWithTag("stage").captureToImage()
        val bounds = compose.onNodeWithTag("number").getUnclippedBoundsInRoot()
        assertEquals(100.dp, bounds.right - bounds.left)
        // The viewport is centred, with at least80dp of blank stage on either side.
        val map = bitmap.toPixelMap()
        for (y in 0 until bitmap.height) {
            assertEquals(Color.White, map[0, y])
            assertEquals(Color.White, map[bitmap.width - 1, y])
        }
        compose.onNodeWithContentDescription("−12345678901234567890.00%").assertExists()
    }
}

class NumberFlowZeroScaleTest {
    @get:Rule val compose = createComposeRule(effectContext = object : MotionDurationScale {
        override val scaleFactor = 0f
    })

    @Test fun zeroScaleSkipsReelsAndRetiringColumnsOnFirstTargetFrame() {
        compose.mainClock.autoAdvance = false
        val value = mutableStateOf("99")
        compose.setContent { FlowStage { NumberPopIn(value.value, style = FlowStyle) } }
        compose.runOnIdle { value.value = "100" }
        compose.mainClock.advanceTimeBy(32)
        val firstTargetFrame = compose.onNodeWithTag("stage").captureToImage()
        compose.mainClock.advanceTimeBy(1600)
        assertEquals(0, pixelDifference(firstTargetFrame, compose.onNodeWithTag("stage").captureToImage()))
        compose.onAllNodesWithContentDescription("100").assertCountEquals(1)
    }
}

private val FlowStyle = TextStyle(fontFamily = FontFamily.Serif, fontSize = 32.sp, color = Color.Black)

@Composable
private fun FlowStage(content: @Composable () -> Unit) {
    MaterialTheme {
        Box(Modifier.size(300.dp, 96.dp).background(Color.White).testTag("stage"),
            contentAlignment = Alignment.Center) { content() }
    }
}

private fun pixelDifference(first: ImageBitmap, second: ImageBitmap, left: Int = 0, right: Int = first.width): Int {
    assertEquals(first.width, second.width)
    assertEquals(first.height, second.height)
    val a = first.toPixelMap()
    val b = second.toPixelMap()
    var changed = 0
    for (y in 0 until first.height) for (x in left.coerceAtLeast(0) until right.coerceAtMost(first.width)) {
        val one = a[x, y]
        val two = b[x, y]
        if (abs(one.red - two.red) + abs(one.green - two.green) + abs(one.blue - two.blue) > .025f) changed++
    }
    return changed
}

private fun inkPixels(bitmap: ImageBitmap, left: Int, right: Int): Int {
    val pixels = bitmap.toPixelMap()
    var ink = 0
    for (y in 0 until bitmap.height) for (x in left.coerceAtLeast(0) until right.coerceAtMost(bitmap.width)) {
        if (pixels[x, y].red < .9f) ink++
    }
    return ink
}
