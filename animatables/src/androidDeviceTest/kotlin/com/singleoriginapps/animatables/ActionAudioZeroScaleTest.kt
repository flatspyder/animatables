package com.singleoriginapps.animatables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ActionAudioZeroScaleTest {
    @get:Rule val compose = createComposeRule(effectContext = object : MotionDurationScale {
        override val scaleFactor = 0f
    })

    @Test
    fun audioHasStaticActiveAndDistinctInactivePresentationsAtSystemScaleZero() {
        val level = mutableFloatStateOf(.1f)
        val active = mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                Column(Modifier.background(Color.White).testTag("audio")) {
                    AudioWaveform(listOf(level.floatValue), active.value)
                    AudioLevelIcon(level.floatValue, active.value)
                    AudioActivityRing(level.floatValue, active.value) {
                        Button({}, Modifier.size(56.dp)) { Text("Mic") }
                    }
                }
            }
        }
        val first = compose.onNodeWithTag("audio").captureToImage()
        compose.runOnIdle { level.floatValue = 1f }
        compose.mainClock.advanceTimeBy(500)
        assertEquals(0, audioChangedPixels(first, compose.onNodeWithTag("audio").captureToImage()))
        compose.runOnIdle { active.value = false }
        compose.mainClock.advanceTimeBy(64)
        compose.onAllNodesWithContentDescription("Audio inactive").assertCountEquals(2)
        org.junit.Assert.assertTrue(audioChangedPixels(first, compose.onNodeWithTag("audio").captureToImage()) > 20)
    }

    @Test
    fun actionAndRefreshExposeLatestOutcomeWithoutAppOptOut() {
        val state = mutableStateOf(AsyncActionState.Working)
        val loading = mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                Column {
                    AsyncActionButton(state.value, {}, "Save", workingLabel = "Saving", successLabel = "Saved")
                    RefreshableContent(true, loading.value, error = if (loading.value) null else "Offline",
                        onRetry = {}, placeholder = { Text("Skeleton") }, empty = { Text("Empty") }) { Text("Previous data") }
                }
            }
        }
        compose.runOnIdle { state.value = AsyncActionState.Success; loading.value = false }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("Saved").assertExists()
        compose.onNodeWithText("Saving").assertDoesNotExist()
        compose.onNodeWithText("Previous data").assertExists()
        compose.onNodeWithText("Offline").assertExists()
        compose.onNodeWithText("Retry").assertExists()
        compose.onNodeWithText("Refreshing").assertDoesNotExist()
    }
}
