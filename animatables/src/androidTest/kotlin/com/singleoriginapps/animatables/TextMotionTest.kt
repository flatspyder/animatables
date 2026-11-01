package com.singleoriginapps.animatables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Content identity, native paragraph layout, actual visible motion, and instant resting states. */
class TextMotionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun revealResolvesVisiblyAndReplayRestartsIt() {
        compose.mainClock.autoAdvance = false
        val replay = mutableStateOf(0)
        compose.setContent {
            MaterialTheme {
                Surface {
                    TextReveal(
                        listOf("A little room", "for your next idea."),
                        modifier = Modifier.testTag("reveal"),
                        replayKey = replay.value,
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(32)
        val entering = compose.onNodeWithTag("reveal").captureToImage()
        compose.mainClock.advanceTimeBy(1800)
        val settled = compose.onNodeWithTag("reveal").captureToImage()
        assertTrue("Reveal must change rendered pixels", changedPixels(entering, settled) > 20)
        compose.onAllNodesWithText("A little room\nfor your next idea.").assertCountEquals(1)
        compose.runOnIdle { replay.value++ }
        compose.mainClock.advanceTimeBy(32)
        val replaying = compose.onNodeWithTag("reveal").captureToImage()
        assertTrue("Replay must visibly restart the reveal", changedPixels(replaying, settled) > 20)
    }

    @Test fun streamingPreservesParagraphLayoutAndOnlyExposesLatestText() {
        compose.mainClock.autoAdvance = false
        val text = mutableStateOf("A paragraph with  two spaces.\nAnd a second paragraph.")
        compose.setContent {
            MaterialTheme {
                Surface {
                    StreamingText(text.value, modifier = Modifier.width(220.dp).testTag("stream"))
                }
            }
        }
        compose.mainClock.advanceTimeBy(32)
        val firstBounds = compose.onNodeWithTag("stream").getUnclippedBoundsInRoot()
        val entering = compose.onNodeWithTag("stream").captureToImage()
        compose.mainClock.advanceTimeBy(1800)
        val settledBounds = compose.onNodeWithTag("stream").getUnclippedBoundsInRoot()
        val settled = compose.onNodeWithTag("stream").captureToImage()
        assertEquals("Transient words must not change paragraph bounds", firstBounds, settledBounds)
        assertTrue("New words must resolve visibly", changedPixels(entering, settled) > 20)
        compose.onAllNodesWithText(text.value).assertCountEquals(1)
        compose.runOnIdle { text.value += " More actual received text." }
        compose.mainClock.advanceTimeBy(32)
        compose.onAllNodesWithText(text.value).assertCountEquals(1)
        compose.runOnIdle { text.value = "A replacement response." }
        compose.mainClock.advanceTimeBy(32)
        compose.onAllNodesWithText("A replacement response.").assertCountEquals(1)
        compose.onNodeWithText("A paragraph", substring = true).assertDoesNotExist()
    }

    @Test fun rtlStreamingWordsAreVisibleBeforeTheySettle() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(color = Color.White) {
                        StreamingText(
                            "مرحبا بكم في مساحة الأفكار الجديدة",
                            modifier = Modifier.width(220.dp).testTag("rtlStream"),
                            style = TextStyle(color = Color.Black, fontSize = 24.sp),
                        )
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(96)
        val moving = compose.onNodeWithTag("rtlStream").captureToImage()
        assertTrue("RTL word layers must be inside the paragraph while moving", darkPixels(moving) > 30)
        compose.mainClock.advanceTimeBy(1800)
        val settled = compose.onNodeWithTag("rtlStream").captureToImage()
        assertTrue("The moving frame must still differ from the complete paragraph", changedPixels(moving, settled) > 20)
    }

    @Test fun variableHeightActivityHasOneContinuousSurvivingEntry() {
        compose.mainClock.autoAdvance = false
        val entries = mutableStateOf(listOf("Older", "Tall entry\nuses three\nvisual lines", "████████████████"))
        compose.setContent {
            MaterialTheme {
                Surface(color = Color.White) {
                    ReasoningStream(
                        entries.value,
                        modifier = Modifier.width(240.dp).testTag("activity"),
                        style = TextStyle(
                            color = Color.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 20.sp,
                            lineHeight = 28.sp,
                        ),
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(1800)
        val before = compose.onNodeWithTag("activity").captureToImage()
        val beforeBands = solidRowBands(before)
        assertEquals("One retained block entry before append", 1, beforeBands.size)
        compose.runOnIdle { entries.value += "New work\ncontinues in\nthree lines" }
        compose.mainClock.advanceTimeBy(32)
        val first = compose.onNodeWithTag("activity").captureToImage()
        compose.mainClock.advanceTimeBy(64)
        val second = compose.onNodeWithTag("activity").captureToImage()
        val firstBands = solidRowBands(first)
        val secondBands = solidRowBands(second)
        assertEquals("The retained entry must not ghost into a second window", 1, firstBands.size)
        assertEquals("One retained entry throughout the scroll", 1, secondBands.size)
        assertTrue("The retained entry must travel upward continuously", firstBands.single().first > secondBands.single().first)
        assertEquals("Viewport uses actual equal old/new wrapped heights", before.height, second.height)
    }

    @Test fun appOptOutMatchesFullySettledRenderedText() {
        compose.mainClock.autoAdvance = false
        val enabled = mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                Surface {
                    AnimatablesMotion(enabled.value) {
                        Column(Modifier.width(240.dp).testTag("resting")) {
                            TextReveal(listOf("Everything is here", "even without motion."))
                            StreamingText("A complete paragraph.\nWith natural wrapping and spacing.")
                        }
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(2000)
        val settled = compose.onNodeWithTag("resting").captureToImage()
        compose.runOnIdle { enabled.value = false }
        compose.mainClock.advanceTimeBy(32)
        val disabled = compose.onNodeWithTag("resting").captureToImage()
        assertEquals("App opt-out must render the complete settled text", 0, changedPixels(settled, disabled))
    }

    @Test fun appOptOutDisplaysFullRevealAndStreamWithoutDelay() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                AnimatablesMotion(false) {
                    TextReveal(listOf("First line", "Second line"))
                    StreamingText("All words are available immediately.")
                }
            }
        }
        compose.onNodeWithText("First line\nSecond line").assertExists()
        compose.onNodeWithText("All words are available immediately.").assertExists()
    }

    @Test fun activityWindowExposesFullTranscriptAndAcceptsReplacement() {
        val entries = mutableStateOf(listOf("Search started", "Sources found", "Draft ready"))
        compose.setContent {
            MaterialTheme {
                AnimatablesMotion(false) {
                    ReasoningStream(entries.value, maxVisibleEntries = 2, active = false)
                }
            }
        }
        compose.onAllNodesWithText("Search started\nSources found\nDraft ready").assertCountEquals(1)
        compose.runOnIdle { entries.value = listOf("A new task started") }
        compose.onAllNodesWithText("A new task started").assertCountEquals(1)
        compose.onNodeWithText("Draft ready", substring = true).assertDoesNotExist()
    }
}

class TextMotionZeroDurationTest {
    @get:Rule val compose = createComposeRule(effectContext = object : MotionDurationScale {
        override val scaleFactor: Float = 0f
    })

    @Test fun zeroSystemScaleHasCompleteTextWithoutStagger() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                TextReveal(listOf("One", "Two", "Three"))
                StreamingText("Complete current streaming text.")
                ReasoningStream(listOf("Opened document", "Found matching passage"))
            }
        }
        compose.mainClock.advanceTimeBy(32)
        compose.onNodeWithText("One\nTwo\nThree").assertExists()
        compose.onNodeWithText("Complete current streaming text.").assertExists()
        compose.onNodeWithText("Opened document\nFound matching passage").assertExists()
    }
}

private fun changedPixels(first: ImageBitmap, second: ImageBitmap): Int {
    assertEquals(first.width, second.width)
    assertEquals(first.height, second.height)
    val a = first.toPixelMap()
    val b = second.toPixelMap()
    var count = 0
    for (y in 0 until first.height) for (x in 0 until first.width) {
        if (a[x, y] != b[x, y]) count++
    }
    return count
}

private fun darkPixels(image: ImageBitmap): Int {
    val pixels = image.toPixelMap()
    var count = 0
    for (y in 0 until image.height) for (x in 0 until image.width) {
        val color = pixels[x, y]
        if (color.red < .6f && color.green < .6f && color.blue < .6f && color.alpha > .5f) count++
    }
    return count
}

/** Finds a block-glyph row, distinct from the sparse glyphs in surrounding ordinary text. */
private fun solidRowBands(image: ImageBitmap): List<IntRange> {
    val pixels = image.toPixelMap()
    val bands = mutableListOf<IntRange>()
    var start = -1
    for (y in 0 until image.height) {
        var dark = 0
        for (x in 0 until image.width) {
            val color = pixels[x, y]
            if (color.red < .35f && color.green < .35f && color.blue < .35f && color.alpha > .5f) dark++
        }
        val solid = dark > image.width * .60f
        if (solid && start < 0) start = y
        if (!solid && start >= 0) {
            if (y - start > 2) bands += start until y
            start = -1
        }
    }
    if (start >= 0 && image.height - start > 2) bands += start until image.height
    return bands
}
