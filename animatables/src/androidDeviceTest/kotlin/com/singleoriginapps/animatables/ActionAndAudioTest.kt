package com.singleoriginapps.animatables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ActionAndAudioTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun actionKeepsOneStableInteractiveButtonDuringInterruptedChanges() {
        val state = mutableStateOf(AsyncActionState.Idle)
        val enabled = mutableStateOf(true)
        val focus = FocusRequester()
        lateinit var inputMode: InputModeManager
        var clicks = 0
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                inputMode = LocalInputModeManager.current
                AsyncActionButton(
                    state.value,
                    { clicks++ },
                    "Save",
                    Modifier.width(260.dp).testTag("action").focusRequester(focus),
                    enabled = enabled.value,
                    workingLabel = "Saving your changes",
                    successLabel = "Saved",
                    errorLabel = "Retry"
                )
            }
        }
        compose.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard); focus.requestFocus() }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("action").assertIsFocused()
        val bounds = compose.onNodeWithTag("action").fetchSemanticsNode().boundsInRoot
        listOf(
            AsyncActionState.Working,
            AsyncActionState.Error,
            AsyncActionState.Working,
            AsyncActionState.Success
        ).forEach { target ->
            compose.runOnIdle { state.value = target }
            compose.mainClock.advanceTimeBy(48)
            compose.onNodeWithTag("action").assertIsFocused()
            compose.onNodeWithTag("action").assertIsEnabled()
                .performSemanticsAction(SemanticsActions.OnClick) { it() }
            assertEquals(bounds, compose.onNodeWithTag("action").fetchSemanticsNode().boundsInRoot)
            val label = when (target) {
                AsyncActionState.Working -> "Saving your changes"
                AsyncActionState.Success -> "Saved"
                AsyncActionState.Error -> "Retry"
                else -> "Save"
            }
            assertEquals(
                bounds.center.x,
                compose.onNodeWithText(label).fetchSemanticsNode().boundsInRoot.center.x, 1f
            )
        }
        compose.onAllNodesWithText("Saved").assertCountEquals(1)
        compose.onNodeWithText("Saving your changes").assertDoesNotExist()
        compose.onNodeWithText("Retry").assertDoesNotExist()
        compose.runOnIdle { assertEquals(4, clicks); enabled.value = false }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("action").assertIsNotEnabled()
    }

    @Test
    fun actionLabelVisiblyTransitionsAndMatchesItsDisabledRestingState() {
        val state = mutableStateOf(AsyncActionState.Idle)
        val motion = mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                AnimatablesMotion(motion.value) {
                    AsyncActionButton(
                        state.value, {}, "Save", Modifier.width(260.dp).testTag("action"),
                        successLabel = "Changes saved"
                    )
                }
            }
        }
        compose.runOnIdle { state.value = AsyncActionState.Success }
        compose.mainClock.advanceTimeBy(32)
        val early = compose.onNodeWithTag("action").captureToImage()
        compose.mainClock.advanceTimeBy(48)
        val middle = compose.onNodeWithTag("action").captureToImage()
        compose.mainClock.advanceTimeBy(2000)
        val settled = compose.onNodeWithTag("action").captureToImage()
        fun changedLabel(a: ImageBitmap, b: ImageBitmap): Int {
            val first = a.toPixelMap()
            val second = b.toPixelMap()
            var changed = 0
            // Restrict comparison to the label, excluding the independently fading icon.
            for (y in 0 until a.height) for (x in a.width / 4 until a.width * 3 / 4) {
                if (first[x, y] != second[x, y]) changed++
            }
            return changed
        }
        assertTrue(changedLabel(early, middle) > 20)
        assertTrue(changedLabel(middle, settled) > 20)
        compose.runOnIdle { motion.value = false }
        compose.mainClock.advanceTimeBy(64)
        assertEquals(
            0,
            audioChangedPixels(settled, compose.onNodeWithTag("action").captureToImage())
        )
    }

    @Test
    fun actionProgressIsAccessibleButCompletionRemainsCallerOwned() {
        val progress = mutableFloatStateOf(Float.NaN)
        val state = mutableStateOf(AsyncActionState.Working)
        compose.setContent {
            MaterialTheme {
                AnimatablesMotion(false) {
                    AsyncActionButton(
                        state.value, {}, "Send", Modifier.testTag("action"),
                        workingLabel = "Sending", progress = { progress.floatValue })
                }
            }
        }
        fun check(value: Float) = compose.onNodeWithTag("action").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo(value, 0f..1f)
            )
        )
        check(0f)
        compose.runOnIdle { progress.floatValue = .6f }
        check(.6f)
        compose.runOnIdle { progress.floatValue = Float.POSITIVE_INFINITY }
        check(1f)
        compose.onNodeWithText("Sending").assertExists()
        compose.runOnIdle { state.value = AsyncActionState.Success }
        compose.onNodeWithText("Done").assertExists()
        compose.onNodeWithTag("action")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ProgressBarRangeInfo))
    }

    @Test
    fun refreshFailureAndRetryKeepEditableContentAndFocus() {
        val loading = mutableStateOf(false)
        val error = mutableStateOf<String?>(null)
        var retries = 0
        compose.setContent {
            MaterialTheme {
                AnimatablesMotion(false) {
                    RefreshableContent(
                        true,
                        loading.value,
                        error = error.value,
                        onRetry = { retries++; loading.value = true },
                        placeholder = { Text("Skeleton") },
                        empty = { Text("Empty") }) {
                        var note by remember { mutableStateOf("") }
                        OutlinedTextField(note, { note = it }, Modifier.testTag("note"))
                    }
                }
            }
        }
        compose.onNodeWithTag("note").performClick().performTextInput("Still editing")
        compose.runOnIdle { loading.value = true }
        compose.onNodeWithText("Refreshing").assertExists()
        compose.onNodeWithText("Skeleton").assertDoesNotExist()
        compose.onNodeWithTag("note").assertIsFocused().assertTextContains("Still editing")
        compose.runOnIdle { loading.value = false; error.value = "Connection lost" }
        compose.onNodeWithTag("note").assertIsFocused().assertTextContains("Still editing")
        compose.onNodeWithText("Connection lost").assertExists()
        compose.onNodeWithText("Retry").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
        compose.onNodeWithText("Refreshing").assertExists()
        compose.onNodeWithText("Connection lost").assertDoesNotExist()
        compose.runOnIdle { loading.value = false; error.value = null }
        compose.onNodeWithTag("note").assertTextContains("Still editing")
    }

    @Test
    fun initialLoadFailureEmptyAndSuccessfulRetryHaveDistinctStates() {
        val loading = mutableStateOf(false)
        val hasContent = mutableStateOf(false)
        val error = mutableStateOf<String?>(null)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                AnimatablesMotion(false) {
                    RefreshableContent(
                        hasContent.value,
                        loading.value,
                        error = error.value,
                        onRetry = { loading.value = true },
                        placeholder = { Text("Skeleton") },
                        empty = { Text("Empty collection") }) { Text("Loaded collection") }
                }
            }
        }
        compose.onNodeWithText("Empty collection").assertExists()
        compose.runOnIdle { error.value = "Cannot load collection" }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("Empty collection").assertDoesNotExist()
        compose.onNodeWithText("Retry").performClick()
        compose.mainClock.advanceTimeBy(192)
        compose.onNodeWithContentDescription("Loading").assertExists()
        compose.onNodeWithText("Cannot load collection").assertDoesNotExist()
        compose.runOnIdle { hasContent.value = true; loading.value = false; error.value = null }
        compose.mainClock.advanceTimeBy(96)
        compose.onNodeWithText("Loaded collection").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithText("Loaded collection").assertExists()
        compose.onNodeWithContentDescription("Loading").assertDoesNotExist()
    }

    @Test
    fun fastInitialRetryCannotResurrectAnOldPlaceholder() {
        val loading = mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                AnimatablesMotion(false) {
                    RefreshableContent(
                        !loading.value,
                        loading.value,
                        placeholder = { Text("Skeleton") },
                        empty = { Text("Empty") }) { Text("Fresh data") }
                }
            }
        }
        compose.runOnIdle { loading.value = false }
        compose.mainClock.advanceTimeBy(600)
        compose.onNodeWithText("Fresh data").assertExists()
        compose.onNodeWithContentDescription("Loading").assertDoesNotExist()
    }

    @Test
    fun fullHeightCollectionKeepsScrollAndRecoveryIsVisible() {
        val loading = mutableStateOf(false)
        val error = mutableStateOf<String?>(null)
        lateinit var scroll: LazyListState
        compose.setContent {
            MaterialTheme {
                AnimatablesMotion(false) {
                    RefreshableContent(
                        true,
                        loading.value,
                        Modifier.size(300.dp, 400.dp),
                        error = error.value,
                        onRetry = {},
                        placeholder = { Text("Skeleton") },
                        empty = {}) {
                        scroll = rememberLazyListState()
                        LazyColumn(
                            Modifier.fillMaxWidth().height(400.dp).testTag("list"),
                            state = scroll
                        ) {
                            items(100, key = { it }) { Text("Item $it", Modifier.height(48.dp)) }
                        }
                    }
                }
            }
        }
        compose.onNodeWithTag("list").performScrollToIndex(40)
        compose.runOnIdle { assertEquals(40, scroll.firstVisibleItemIndex); loading.value = true }
        compose.onNodeWithText("Refreshing").assertIsDisplayed()
        compose.runOnIdle { loading.value = false; error.value = "Offline" }
        compose.onNodeWithText("Retry").assertIsDisplayed()
        compose.onNodeWithText("Offline").assertIsDisplayed()
        compose.runOnIdle { assertEquals(40, scroll.firstVisibleItemIndex) }
    }

    @Test
    fun invalidAudioSamplesClampAndChangingBarCountStillRenders() {
        val levels = mutableStateOf(listOf(-1f, Float.NaN, Float.POSITIVE_INFINITY, 2f))
        val count = mutableStateOf(32)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                AudioWaveform(
                    levels.value, true, Modifier.size(240.dp, 72.dp)
                        .background(Color.White).testTag("wave"), barCount = count.value
                )
            }
        }
        val invalid = compose.onNodeWithTag("wave").captureToImage()
        compose.runOnIdle { levels.value = listOf(0f, 0f, 1f, 1f) }
        compose.mainClock.advanceTimeBy(1000)
        assertEquals(0, audioChangedPixels(invalid, compose.onNodeWithTag("wave").captureToImage()))
        compose.runOnIdle { count.value = 96 }
        compose.mainClock.advanceTimeBy(1000)
        assertTrue(audioChangedPixels(invalid, compose.onNodeWithTag("wave").captureToImage()) > 50)
        compose.runOnIdle { levels.value = emptyList(); count.value = 3 }
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithTag("wave").assertIsDisplayed()
    }

    @Test
    fun audioRespondsToRealLevelsAndSettlesAfterRapidRetargeting() {
        val level = mutableFloatStateOf(.1f)
        val enabled = mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                AnimatablesMotion(enabled.value) {
                    Column(Modifier.background(Color.White).testTag("audio")) {
                        AudioWaveform(
                            listOf(level.floatValue, .2f, level.floatValue),
                            true,
                            Modifier.size(240.dp, 72.dp),
                            label = "Recording"
                        )
                        AudioLevelIcon(level.floatValue, true, label = "Microphone level")
                    }
                }
            }
        }
        val first = compose.onNodeWithTag("audio").captureToImage()
        compose.runOnIdle { level.floatValue = .95f }
        compose.mainClock.advanceTimeBy(80)
        val middle = compose.onNodeWithTag("audio").captureToImage()
        assertTrue(audioChangedPixels(first, middle) > 50)
        compose.runOnIdle { level.floatValue = .2f }
        compose.mainClock.advanceTimeBy(32)
        compose.runOnIdle { level.floatValue = .8f }
        compose.mainClock.advanceTimeBy(2000)
        val final = compose.onNodeWithTag("audio").captureToImage()
        assertTrue(audioChangedPixels(first, final) > 50)
        compose.onNodeWithContentDescription("Recording").assertExists()
        compose.onNodeWithContentDescription("Microphone level").assertExists()
        // Disabled profiles are static even while the caller continues providing samples.
        compose.runOnIdle { enabled.value = false }
        compose.mainClock.advanceTimeBy(64)
        val disabled = compose.onNodeWithTag("audio").captureToImage()
        compose.runOnIdle { level.floatValue = Float.NaN }
        compose.mainClock.advanceTimeBy(1000)
        assertEquals(
            0,
            audioChangedPixels(disabled, compose.onNodeWithTag("audio").captureToImage())
        )
    }

    @Test
    fun ringLeavesNativeButtonInteractiveAndReservesConstantBounds() {
        val level = mutableFloatStateOf(0f)
        val active = mutableStateOf(true)
        var clicks = 0
        compose.setContent {
            MaterialTheme {
                AnimatablesMotion(false) {
                    AudioActivityRing(level.floatValue, active.value, Modifier.testTag("ring")) {
                        FilledIconButton(
                            onClick = { clicks++; active.value = !active.value },
                            modifier = Modifier.size(56.dp).testTag("record")
                        ) {
                            AudioLevelIcon(level.floatValue, active.value, label = null)
                        }
                    }
                }
            }
        }
        val bounds = compose.onNodeWithTag("ring").fetchSemanticsNode().boundsInRoot
        val button = compose.onNodeWithTag("record").fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.width > button.width)
        compose.onNodeWithTag("record").performClick()
        compose.runOnIdle { level.floatValue = Float.POSITIVE_INFINITY }
        compose.onNodeWithTag("record").performClick()
        compose.runOnIdle { assertEquals(2, clicks) }
        assertEquals(bounds, compose.onNodeWithTag("ring").fetchSemanticsNode().boundsInRoot)
    }
}

internal fun audioChangedPixels(first: ImageBitmap, second: ImageBitmap): Int {
    assertEquals(first.width, second.width)
    assertEquals(first.height, second.height)
    val a = first.toPixelMap()
    val b = second.toPixelMap()
    var changed = 0
    for (y in 0 until first.height) for (x in 0 until first.width) {
        if (a[x, y] != b[x, y]) changed++
    }
    return changed
}
