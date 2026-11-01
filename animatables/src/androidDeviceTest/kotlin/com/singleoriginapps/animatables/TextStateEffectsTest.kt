package com.singleoriginapps.animatables

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.abs

/** Verifies rendered motion separately from the accessible target's immediate state update. */
class TextStateEffectsTest {
    // Keep visual evidence deterministic even if an emulator was left with animations disabled.
    @get:Rule
    val compose = createComposeRule(effectContext = object : MotionDurationScale {
        override val scaleFactor: Float = 1f
    })

    @Test
    fun textSwapHasAVisibleHandoffBeforeTheSameTargetSettles() {
        compose.mainClock.autoAdvance = false
        val label = mutableStateOf("Ready to save")
        compose.setContent {
            MaterialTheme {
                EvidenceSurface {
                    TextStateSwap(label.value, style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
        compose.mainClock.advanceTimeBy(32)
        saveTextEvidence("text-state-swap", "before", capture())
        compose.runOnIdle { label.value = "Saved and ready" }
        compose.mainClock.advanceTimeBy(96)
        compose.onAllNodesWithText("Saved and ready").assertCountEquals(1)
        compose.onNodeWithText("Ready to save").assertDoesNotExist()
        val middle = capture()
        compose.mainClock.advanceTimeBy(1600)
        val resting = capture()
        saveTextEvidence("text-state-swap", "middle", middle)
        saveTextEvidence("text-state-swap", "resting", resting)
        assertTrue(
            "The rendered target must visibly resolve after its semantics already changed",
            changedTextPixels(middle, resting) > 30,
        )
    }

    @Test
    fun interruptedTextSwapExposesExactlyTheLatestLabel() {
        compose.mainClock.autoAdvance = false
        val label = mutableStateOf("Queued")
        compose.setContent {
            MaterialTheme { EvidenceSurface { TextStateSwap(label.value) } }
        }
        compose.mainClock.advanceTimeBy(32)
        compose.runOnIdle { label.value = "Uploading" }
        compose.mainClock.advanceTimeBy(48)
        compose.runOnIdle { label.value = "Retrying upload" }
        compose.mainClock.advanceTimeBy(32)
        compose.runOnIdle { label.value = "Upload complete" }
        compose.mainClock.advanceTimeByFrame()
        compose.onAllNodesWithText("Upload complete").assertCountEquals(1)
        compose.onNodeWithText("Queued").assertDoesNotExist()
        compose.onNodeWithText("Uploading").assertDoesNotExist()
        compose.onNodeWithText("Retrying upload").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(1600)
        compose.onAllNodesWithText("Upload complete").assertCountEquals(1)
        saveTextEvidence("text-state-swap", "interrupted-resting", capture())
    }

    @Test
    fun shimmerChangesGlyphPixelsStopsImmediatelyAndEventuallyBecomesIdle() {
        compose.mainClock.autoAdvance = false
        val active = mutableStateOf(false)
        val replay = mutableStateOf(0)
        compose.setContent {
            MaterialTheme {
                EvidenceSurface {
                    ShimmerText(
                        "Finding your rhythm",
                        continuous = false,
                        active = active.value,
                        replayKey = replay.value,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(32)
        val static = capture()
        compose.runOnIdle { active.value = true }
        compose.mainClock.advanceTimeBy(1000)
        val sweeping = capture()
        saveTextEvidence("shimmer-text", "sweep", sweeping)
        assertTrue(
            "The highlight must change rendered glyphs",
            changedTextPixels(static, sweeping) > 30
        )
        assertEquals(
            "Shimmer must not paint a background above the text",
            0,
            changedTopRowPixels(static, sweeping)
        )

        compose.runOnIdle { active.value = false }
        compose.mainClock.advanceTimeByFrame()
        val stopped = capture()
        assertEquals(
            "active=false must immediately restore the ordinary label",
            0,
            changedTextPixels(static, stopped)
        )
        saveTextEvidence("shimmer-text", "stopped", stopped)

        compose.runOnIdle { active.value = true; replay.value++ }
        // This also catches accidental infinite shimmer: Compose must be able to reach idle
        // while active remains true, with no caller intervention and no wall-clock sleep.
        compose.mainClock.advanceTimeBy(4000)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        val resting = capture()
        assertEquals(
            "The bounded run must finish on an ordinary static label",
            0,
            changedTextPixels(static, resting)
        )
        saveTextEvidence("shimmer-text", "resting", resting)
        compose.onAllNodesWithText("Finding your rhythm").assertCountEquals(1)
    }

    @Test
    fun thinkingStatesOnlyDisplaysTheStatusSuppliedByTheCaller() {
        compose.mainClock.autoAdvance = false
        val status = mutableStateOf("Reading the selected document")
        compose.setContent {
            MaterialTheme { EvidenceSurface { ThinkingStates(status.value, active = false) } }
        }
        compose.mainClock.advanceTimeBy(10_000)
        compose.onAllNodesWithText("Reading the selected document").assertCountEquals(1)
        compose.runOnIdle { status.value = "Document summary is ready" }
        compose.mainClock.advanceTimeByFrame()
        compose.onAllNodesWithText("Document summary is ready").assertCountEquals(1)
        compose.onNodeWithText("Reading the selected document").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(1600)
        compose.onAllNodesWithText("Document summary is ready").assertCountEquals(1)
    }

    private fun capture(): ImageBitmap =
        compose.onNodeWithTag("text-effect-evidence").captureToImage()
}

@Composable
private fun EvidenceSurface(content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(360.dp, 112.dp)
            .background(MaterialTheme.colorScheme.surface)
            .testTag("text-effect-evidence"),
        contentAlignment = Alignment.Center,
    ) { content() }
}

private fun changedTextPixels(first: ImageBitmap, second: ImageBitmap): Int {
    assertEquals(first.width, second.width)
    assertEquals(first.height, second.height)
    val a = first.toPixelMap()
    val b = second.toPixelMap()
    var count = 0
    for (y in 0 until first.height) for (x in 0 until first.width) {
        val left = a[x, y]
        val right = b[x, y]
        if (abs(left.red - right.red) + abs(left.green - right.green) + abs(left.blue - right.blue) > .03f) count++
    }
    return count
}

private fun changedTopRowPixels(first: ImageBitmap, second: ImageBitmap): Int {
    val a = first.toPixelMap()
    val b = second.toPixelMap()
    return (0 until first.width).count { x -> a[x, 0] != b[x, 0] }
}

private fun saveTextEvidence(effect: String, phase: String, image: ImageBitmap) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val directory = File(context.getExternalFilesDir(null), "motion-evidence").apply { mkdirs() }
    File(directory, "$effect-$phase.png").outputStream().use {
        assertTrue(
            "PNG evidence must be written",
            image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        )
    }
}
