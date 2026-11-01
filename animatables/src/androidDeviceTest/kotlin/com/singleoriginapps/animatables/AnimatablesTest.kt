package com.singleoriginapps.animatables

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

/** Behavioral contracts: semantics, interruption, opt-out, and cancelled loading work. */
class AnimatablesTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun disabledMotionRendersOnlyLatestContent() {
        val state = mutableStateOf("Saving")
        compose.setContent {
            MaterialTheme {
                AnimatablesMotion(enabled = false) {
                    MotionContent(state.value) { Text(it) }
                }
            }
        }
        compose.runOnIdle { state.value = "Saved" }
        compose.onNodeWithText("Saved").assertExists()
        compose.onNodeWithText("Saving").assertDoesNotExist()
    }

    @Test
    fun changingMotionPreferencePreservesRememberedSlotState() {
        val motionEnabled = mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                AnimatablesMotion(enabled = motionEnabled.value) {
                    Column {
                        MotionContent(Unit) {
                            val clicks = remember { mutableStateOf(0) }
                            Button(onClick = { clicks.value++ }) { Text("Content ${clicks.value}") }
                        }
                        ExpandableCard(true, {}, "Details") {
                            val clicks = remember { mutableStateOf(0) }
                            Button(onClick = { clicks.value++ }) { Text("Body ${clicks.value}") }
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Content 0").performClick()
        compose.onNodeWithText("Body 0").performClick()
        compose.runOnIdle { motionEnabled.value = false }
        compose.onNodeWithText("Content 1").assertExists()
        compose.onNodeWithText("Body 1").assertExists()
        compose.runOnIdle { motionEnabled.value = true }
        compose.onNodeWithText("Content 1").assertExists()
        compose.onNodeWithText("Body 1").assertExists()
    }

    @Test
    fun nestedProviderCannotOverrideDisabledAncestor() {
        val state = mutableStateOf("Before")
        compose.setContent {
            MaterialTheme {
                AnimatablesMotion(enabled = false) {
                    AnimatablesMotion(enabled = true) {
                        MotionContent(state.value) { Text(it) }
                    }
                }
            }
        }
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { state.value = "After" }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("Before").assertDoesNotExist()
        compose.onNodeWithText("After").assertExists()
    }

    @Test
    fun disclosureHeaderExposesStateAndRemainsInteractive() {
        val expanded = mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                AnimatablesMotion(enabled = false) {
                    ExpandableCard(expanded.value, { expanded.value = it }, "Details") {
                        Text("Body content")
                    }
                }
            }
        }
        compose.onNodeWithText("Details")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed"))
            .performClick()
        compose.onNodeWithText("Body content").assertExists()
        compose.onNodeWithText("Details")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"))
            .performClick()
        compose.onNodeWithText("Body content").assertDoesNotExist()
    }

    @Test
    fun badgeAnnouncesFullCountAboveVisualCapAndZero() {
        val count = mutableStateOf(142)
        compose.setContent {
            MaterialTheme {
                AnimatedCountBadge(count.value, countDescription = { "$it unread messages" })
            }
        }
        compose.onNodeWithContentDescription("142 unread messages").assertExists()
        compose.runOnIdle { count.value = 0 }
        compose.onNodeWithContentDescription("0 unread messages").assertExists()
    }

    @Test
    fun interruptedStateSwapHasOneAccessibleTarget() {
        val state = mutableStateOf("Queued")
        compose.setContent { MaterialTheme { AnimatedStatusText(state.value) } }
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { state.value = "Saving" }
        compose.mainClock.advanceTimeBy(48)
        compose.runOnIdle { state.value = "Saved" }
        compose.mainClock.advanceTimeByFrame()
        compose.onAllNodesWithText("Saved").assertCountEquals(1)
        compose.onNodeWithText("Queued").assertDoesNotExist()
        compose.onNodeWithText("Saving").assertDoesNotExist()
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun cancelledInitialLoadCannotShowStalePlaceholder() {
        compose.mainClock.autoAdvance = false
        val loading = mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                AnimatablesMotion(enabled = false) {
                    LoadingContent(
                        loading = loading.value,
                        placeholder = { Text("Skeleton") },
                    ) { Text("Loaded") }
                }
            }
        }
        compose.runOnIdle { loading.value = false }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("Loaded").assertExists()
        // Outlast the pending show delay. The old effect must not bring the placeholder back.
        compose.mainClock.advanceTimeBy(250)
        compose.onNodeWithText("Skeleton").assertDoesNotExist()
        compose.onNodeWithText("Loaded").assertExists()
    }

    @Test
    fun visiblePlaceholderHoldsBeforeRevealingContent() {
        compose.mainClock.autoAdvance = false
        val loading = mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                AnimatablesMotion(enabled = false) {
                    LoadingContent(
                        loading = loading.value,
                        placeholder = { Text("Skeleton") },
                    ) { Text("Loaded") }
                }
            }
        }
        compose.mainClock.advanceTimeBy(192)
        compose.runOnIdle { loading.value = false }
        compose.mainClock.advanceTimeBy(96)
        compose.onNodeWithText("Loaded").assertDoesNotExist()
        compose.onNodeWithContentDescription("Loading").assertExists()
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithText("Loaded").assertExists()
        compose.onNodeWithContentDescription("Loading").assertDoesNotExist()
    }

    @Test
    fun completionMeaningSurvivesDisabledMotion() {
        val complete = mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                AnimatablesMotion(enabled = false) {
                    CompletionIndicator(
                        complete.value,
                        label = "Upload complete",
                        pendingLabel = "Not uploaded"
                    )
                }
            }
        }
        compose.onNodeWithContentDescription("Not uploaded").assertExists()
        compose.runOnIdle { complete.value = true }
        compose.onNodeWithContentDescription("Upload complete").assertExists()
        compose.onNodeWithContentDescription("Not uploaded").assertDoesNotExist()
    }
}
