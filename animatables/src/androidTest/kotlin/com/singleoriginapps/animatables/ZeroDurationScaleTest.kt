package com.singleoriginapps.animatables

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

/** Supplies the same zero duration scale contract used by Android's Remove animations setting. */
class ZeroDurationScaleTest {
    @get:Rule val compose = createComposeRule(effectContext = object : MotionDurationScale {
        override val scaleFactor: Float = 0f
    })

    @Test fun systemScaleZeroLeavesMeaningfulFinalStatesWithoutAppOptOut() {
        val state = mutableStateOf(false)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                MotionContent(state.value) { complete ->
                    if (complete) {
                        CompletionIndicator(true, label = "Upload complete")
                        Text("Saved")
                    } else Text("Saving")
                }
            }
        }
        compose.runOnIdle { state.value = true }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("Saving").assertDoesNotExist()
        compose.onNodeWithText("Saved").assertExists()
        compose.onNodeWithContentDescription("Upload complete").assertExists()
    }
}
