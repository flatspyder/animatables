package com.singleoriginapps.animatables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Measures visible artwork inside the host stage, rather than only measuring wrapper bounds. */
class CenteringPolishTest {
    @get:Rule val compose = createComposeRule(effectContext = object : MotionDurationScale {
        override val scaleFactor: Float = 1f
    })

    @Test fun unequalTextLengthsStayCentredDuringRapidRtlLargeTextReversal() {
        compose.mainClock.autoAdvance = false
        val label = mutableStateOf("MMMM")
        compose.setContent {
            MaterialTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                    LocalDensity provides Density(density.density, fontScale = 1.6f),
                ) {
                    CenteringStage {
                        TextStateSwap(
                            label.value,
                            modifier = Modifier.fillMaxSize(),
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 22.sp, color = Color.Red),
                        )
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(1200)
        assertArtworkCentred(capture(), Artwork.Red)
        compose.runOnIdle { label.value = "MMMM MMMM MMMM MMMM" }
        // The 150ms exit is followed by the 150ms entrance. Sample each visible phase,
        // rather than treating the intentional near-transparent handoff as missing artwork.
        for (delta in listOf(32L, 32L, 160L, 48L)) {
            compose.mainClock.advanceTimeBy(delta)
            assertArtworkCentred(capture(), Artwork.Red)
        }
        compose.runOnIdle { label.value = "MMMM" }
        for (delta in listOf(32L, 32L, 160L, 48L)) {
            compose.mainClock.advanceTimeBy(delta)
            assertArtworkCentred(capture(), Artwork.Red)
        }
        compose.mainClock.advanceTimeBy(1200)
        assertArtworkCentred(capture(), Artwork.Red)
        compose.onAllNodesWithText("MMMM").assertCountEquals(1)
    }

    @Test fun wrapContentTextAndThinkingRemainCentredAsNaturalWidthChanges() {
        compose.mainClock.autoAdvance = false
        val label = mutableStateOf("MMMM")
        val thinking = mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                    LocalDensity provides Density(density.density, fontScale = 1.4f),
                ) {
                    ProvideTextStyle(TextStyle(fontFamily = FontFamily.Monospace, fontSize = 22.sp, color = Color.Red)) {
                        CenteringStage {
                            // Deliberately no fill/size modifier: AnimatedContent must preserve
                            // the actual visible centre while its own natural width changes.
                            if (thinking.value) ThinkingStates(label.value, active = false)
                            else TextStateSwap(label.value)
                        }
                    }
                }
            }
        }
        for (useThinking in listOf(false, true)) {
            compose.runOnIdle { thinking.value = useThinking; label.value = "MMMM" }
            compose.mainClock.advanceTimeBy(1200)
            assertArtworkCentred(capture(), Artwork.Red)
            compose.runOnIdle { label.value = "MMMM MMMM MMMM MMMM" }
            for (delta in listOf(32L, 32L, 160L, 48L)) {
                compose.mainClock.advanceTimeBy(delta)
                assertArtworkCentred(capture(), Artwork.Red)
            }
            compose.runOnIdle { label.value = "MMMM" }
            for (delta in listOf(32L, 32L, 160L, 48L)) {
                compose.mainClock.advanceTimeBy(delta)
                assertArtworkCentred(capture(), Artwork.Red)
            }
            compose.mainClock.advanceTimeBy(1200)
            assertArtworkCentred(capture(), Artwork.Red)
            compose.onAllNodesWithText("MMMM").assertCountEquals(1)
        }
    }

    @Test fun wrapContentHandoffKeepsItsCentreWhileWidthChangesAndReverses() {
        compose.mainClock.autoAdvance = false
        val large = mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                CenteringStage {
                    MotionContent(large.value) { expanded ->
                        Box(
                            Modifier.size(if (expanded) 144.dp else 24.dp, 36.dp)
                                .background(if (expanded) Color.Blue else Color.Red),
                        )
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(1200)
        assertArtworkCentred(capture(), Artwork.Red, checkVertical = true)
        compose.runOnIdle { large.value = true }
        compose.mainClock.advanceTimeBy(64)
        assertArtworkCentred(capture(), Artwork.Blue)
        compose.mainClock.advanceTimeBy(32)
        assertArtworkCentred(capture(), Artwork.Blue)
        // Reverse before size and entry motion settle, while the host still centres a changing
        // wrap-content width. Fixed full-size modifiers would conceal this source of drift.
        compose.runOnIdle { large.value = false }
        compose.mainClock.advanceTimeBy(96)
        assertArtworkCentred(capture(), Artwork.Red)
        compose.mainClock.advanceTimeBy(1200)
        assertArtworkCentred(capture(), Artwork.Red, checkVertical = true)
        compose.runOnIdle { large.value = true }
        compose.mainClock.advanceTimeBy(1200)
        assertArtworkCentred(capture(), Artwork.Blue, checkVertical = true)
    }

    @Test fun differentlySizedIconsScaleAboutTheCentreOfALargerHostSlot() {
        compose.mainClock.autoAdvance = false
        val state = mutableStateOf(false)
        val replay = mutableStateOf(0)
        compose.setContent {
            MaterialTheme {
                CenteringStage {
                    IconSwap(state.value, modifier = Modifier.size(180.dp, 140.dp), replayKey = replay.value) { large ->
                        Box(Modifier.size(if (large) 76.dp else 24.dp).background(Color.Red))
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(1200)
        assertArtworkCentred(capture(), Artwork.Red, checkVertical = true)
        compose.runOnIdle { state.value = true }
        repeat(4) {
            compose.mainClock.advanceTimeBy(32)
            assertArtworkCentred(capture(), Artwork.Red, checkVertical = true)
        }
        compose.runOnIdle { state.value = false }
        compose.mainClock.advanceTimeBy(1200)
        assertArtworkCentred(capture(), Artwork.Red, checkVertical = true)
        compose.runOnIdle { replay.value++ }
        compose.mainClock.advanceTimeBy(64)
        repeat(4) {
            compose.mainClock.advanceTimeBy(32)
            assertArtworkCentred(capture(), Artwork.Red, checkVertical = true)
        }
    }

    @Test fun contentEntryTravelsVerticallyWithoutHorizontalDrift() {
        compose.mainClock.autoAdvance = false
        val state = mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                CenteringStage {
                    MotionContent(state.value, Modifier.fillMaxSize()) { large ->
                        Box(
                            Modifier.size(if (large) 144.dp else 24.dp, if (large) 64.dp else 24.dp)
                                .background(if (large) Color.Blue else Color.Red),
                        )
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(1200)
        assertArtworkCentred(capture(), Artwork.Red, checkVertical = true)
        compose.runOnIdle { state.value = true }
        compose.mainClock.advanceTimeBy(64)
        val middle = capture()
        val middleBounds = artworkBounds(middle, Artwork.Blue)
        assertArtworkCentred(middle, Artwork.Blue)
        compose.mainClock.advanceTimeBy(1200)
        val resting = capture()
        val restingBounds = artworkBounds(resting, Artwork.Blue)
        assertArtworkCentred(resting, Artwork.Blue, checkVertical = true)
        assertTrue("Entrance should resolve upward into place", middleBounds.centerY > restingBounds.centerY + .5f)
        compose.runOnIdle { state.value = false }
        compose.mainClock.advanceTimeBy(64)
        assertArtworkCentred(capture(), Artwork.Red)
        compose.mainClock.advanceTimeBy(1200)
        assertArtworkCentred(capture(), Artwork.Red, checkVertical = true)
    }

    @Test fun appMotionChangesAndIconReplayPreserveRememberedSlots() {
        compose.mainClock.autoAdvance = false
        val motion = mutableStateOf(true)
        val replay = mutableStateOf(0)
        val iconCreations = AtomicInteger(0)
        val contentCreations = AtomicInteger(0)
        compose.setContent {
            MaterialTheme {
                AnimatablesMotion(motion.value) {
                    Column {
                        IconSwap(Unit, replayKey = replay.value) {
                            remember { iconCreations.incrementAndGet() }
                            Text("Remembered icon")
                        }
                        MotionContent(Unit) {
                            remember { contentCreations.incrementAndGet() }
                            Text("Remembered content")
                        }
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(1200)
        compose.runOnIdle { motion.value = false }
        compose.mainClock.advanceTimeBy(64)
        compose.runOnIdle { motion.value = true; replay.value++ }
        compose.mainClock.advanceTimeBy(1200)
        assertEquals("Motion preference must not recreate icon state", 1, iconCreations.get())
        assertEquals("Motion preference must not recreate content state", 1, contentCreations.get())
    }

    private fun capture(): ImageBitmap = compose.onNodeWithTag("centering-stage").captureToImage()
}

@Composable
private fun CenteringStage(content: @Composable () -> Unit) {
    Box(
        Modifier.size(320.dp, 220.dp).background(Color.White).testTag("centering-stage"),
        contentAlignment = Alignment.Center,
    ) { content() }
}

private enum class Artwork { Red, Blue }
private data class ArtworkBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

private fun artworkBounds(image: ImageBitmap, artwork: Artwork): ArtworkBounds {
    val pixels = image.toPixelMap()
    var left = image.width
    var top = image.height
    var right = -1
    var bottom = -1
    for (y in 0 until image.height) for (x in 0 until image.width) {
        val color = pixels[x, y]
        val matches = when (artwork) {
            Artwork.Red -> color.red - color.green > .08f && color.red - color.blue > .08f
            Artwork.Blue -> color.blue - color.green > .08f && color.blue - color.red > .08f
        }
        if (matches) {
            left = minOf(left, x)
            right = maxOf(right, x)
            top = minOf(top, y)
            bottom = maxOf(bottom, y)
        }
    }
    assertTrue("The $artwork artwork must be visibly rendered", right >= left && bottom >= top)
    return ArtworkBounds(left, top, right, bottom)
}

private fun assertArtworkCentred(image: ImageBitmap, artwork: Artwork, checkVertical: Boolean = false) {
    val bounds = artworkBounds(image, artwork)
    assertTrue(
        "Visible $artwork artwork must stay horizontally centred: ${bounds.centerX} in ${image.width}px stage",
        abs(bounds.centerX - (image.width - 1) / 2f) <= 3f,
    )
    if (checkVertical) {
        assertTrue(
            "Visible $artwork artwork must stay vertically centred: ${bounds.centerY} in ${image.height}px stage",
            abs(bounds.centerY - (image.height - 1) / 2f) <= 3f,
        )
    }
}
