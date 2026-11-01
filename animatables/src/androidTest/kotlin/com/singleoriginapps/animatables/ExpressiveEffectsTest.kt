package com.singleoriginapps.animatables

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Pixel comparisons establish that the effects really move, rather than only changing semantics. */
class ExpressiveEffectsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun formattedNumberHasDistinctRollingAndRestingFrames() {
        compose.mainClock.autoAdvance = false
        val value = mutableStateOf("−€1\u202f234,50%")
        compose.setContent {
            MaterialTheme {
                Box(
                    Modifier.size(320.dp, 96.dp).background(MaterialTheme.colorScheme.surface).testTag("effect"),
                    contentAlignment = Alignment.Center,
                ) { NumberPopIn(value.value, decimalSeparator = ',') }
            }
        }
        compose.mainClock.advanceTimeBy(800)
        compose.runOnIdle { value.value = "−€1\u202f245,60%" }
        compose.mainClock.advanceTimeByFrame()
        compose.onAllNodesWithContentDescription(value.value).assertCountEquals(1)
        captureMovingFrames("number-pop-in")
    }

    @Test fun formattedNumericSequenceKeepsItsOrderInAnRtlHost() {
        compose.mainClock.autoAdvance = false
        val direction = mutableStateOf(LayoutDirection.Ltr)
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalLayoutDirection provides direction.value) {
                    AnimatablesMotion(false) {
                        Box(
                            Modifier.size(320.dp, 96.dp).background(MaterialTheme.colorScheme.surface).testTag("effect"),
                            contentAlignment = Alignment.Center,
                        ) { NumberPopIn("−1,234.50%") }
                    }
                }
            }
        }
        val leftToRight = compose.onNodeWithTag("effect").captureToImage()
        compose.runOnIdle { direction.value = LayoutDirection.Rtl }
        compose.mainClock.advanceTimeByFrame()
        val rightToLeftHost = compose.onNodeWithTag("effect").captureToImage()
        assertEquals("RTL layout must not reverse a supplied formatted numeric run", 0, changedPixels(leftToRight, rightToLeftHost))
    }

    @Test fun iconReplayMovesWithoutRecreatingItsSlot() {
        compose.mainClock.autoAdvance = false
        val replay = mutableStateOf(0)
        val creations = AtomicInteger(0)
        compose.setContent {
            MaterialTheme {
                Box(
                    Modifier.size(120.dp).background(MaterialTheme.colorScheme.surface).testTag("effect"),
                    contentAlignment = Alignment.Center,
                ) {
                    IconSwap(targetState = "Saved", replayKey = replay.value) { label ->
                        remember { creations.incrementAndGet() }
                        Text(label, style = MaterialTheme.typography.headlineLarge)
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(1200)
        compose.runOnIdle { replay.value++ }
        compose.mainClock.advanceTimeByFrame()
        captureMovingFrames("icon-swap")
        assertEquals("A replay must preserve state inside the content slot", 1, creations.get())
    }

    @Test fun initiallyCompleteCheckAnimatesOnMount() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                Box(
                    Modifier.size(120.dp).background(MaterialTheme.colorScheme.surface).testTag("effect"),
                    contentAlignment = Alignment.Center,
                ) { SuccessCheck(complete = true, modifier = Modifier.size(72.dp), label = "Saved") }
            }
        }
        compose.onNodeWithContentDescription("Saved").assertExists()
        captureMovingFrames("success-check")
    }

    private fun captureMovingFrames(name: String) {
        val initial = compose.onNodeWithTag("effect").captureToImage()
        compose.mainClock.advanceTimeBy(128)
        val middle = compose.onNodeWithTag("effect").captureToImage()
        compose.mainClock.advanceTimeBy(1200)
        val resting = compose.onNodeWithTag("effect").captureToImage()
        saveEvidence(name, "initial", initial)
        saveEvidence(name, "middle", middle)
        saveEvidence(name, "resting", resting)
        assertTrue("$name must visibly differ between entrance and rest", changedPixels(initial, resting) > 40)
        assertTrue("$name must have a visible in-between frame", changedPixels(middle, resting) > 20)
    }
}

class ExpressiveEffectsZeroScaleTest {
    @get:Rule val compose = createComposeRule(effectContext = object : MotionDurationScale {
        override val scaleFactor: Float = 0f
    })

    @Test fun scaleZeroRendersTheRestingStateWithoutStaggerOrMountMotion() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                Column(Modifier.size(320.dp, 200.dp).background(MaterialTheme.colorScheme.surface).testTag("effect")) {
                    NumberPopIn("12,345")
                    IconSwap("Saved") { Text(it) }
                    SuccessCheck(true, label = "Complete")
                }
            }
        }
        val initial = compose.onNodeWithTag("effect").captureToImage()
        compose.mainClock.advanceTimeBy(128)
        val middle = compose.onNodeWithTag("effect").captureToImage()
        compose.mainClock.advanceTimeBy(1200)
        val resting = compose.onNodeWithTag("effect").captureToImage()
        assertEquals("Zero scale must not leave a delayed entrance", 0, changedPixels(initial, middle))
        assertEquals("Zero scale must render the final state on mount", 0, changedPixels(initial, resting))
        compose.onNodeWithContentDescription("12,345").assertExists()
        compose.onNodeWithContentDescription("Complete").assertExists()
        saveEvidence("zero-scale", "resting", resting)
    }
}

private fun changedPixels(first: ImageBitmap, second: ImageBitmap): Int {
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

private fun saveEvidence(name: String, frame: String, bitmap: ImageBitmap) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val directory = File(context.getExternalFilesDir(null), "motion-evidence").apply { mkdirs() }
    File(directory, "$name-$frame.png").outputStream().use {
        bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
    }
}
