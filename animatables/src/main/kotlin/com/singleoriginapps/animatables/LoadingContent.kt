package com.singleoriginapps.animatables

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import kotlinx.coroutines.delay

private enum class LoadingPhase { Waiting, Placeholder, Content }

/** Timing policy is separate from visual motion; these delays are never animation durations. */
private object LoadingTiming {
    const val ShowDelayMillis = 150L
    const val MinimumVisibleMillis = 350L
}

/**
 * Delays a structural placeholder, holds it long enough to read, then crossfades to content.
 *
 * [placeholder] should match the geometry of [content] and should be static. Initial loading
 * reserves the placeholder's space invisibly for 150ms. A fast reload retains existing content;
 * a slower one shows the placeholder for at least 350ms. Completing a fast load cancels a
 * pending show. The minimum hold follows the visible placeholder, so an old load cannot hide a newer one. The latest
 * [content] lambda is used when loading finishes.
 *
 * Give the loading state a localized [loadingDescription]. Timing policy still applies with
 * animations disabled; visibility changes then snap. This component performs no data fetching.
 */
@Composable
public fun LoadingContent(
    loading: Boolean,
    modifier: Modifier = Modifier,
    loadingDescription: String = "Loading",
    placeholder: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    var phase by remember { mutableStateOf(if (loading) LoadingPhase.Waiting else LoadingPhase.Content) }
    var minimumHoldActive by remember { mutableStateOf(false) }
    val latestLoading by rememberUpdatedState(loading)
    LaunchedEffect(loading) {
        if (loading && phase != LoadingPhase.Placeholder) {
            delay(LoadingTiming.ShowDelayMillis)
            // Establish the hold before publishing the placeholder phase. A completion arriving
            // between these effects must not bypass the minimum visible interval.
            minimumHoldActive = true
            phase = LoadingPhase.Placeholder
        } else if (!loading && !minimumHoldActive) {
            phase = LoadingPhase.Content
        }
    }
    LaunchedEffect(phase) {
        if (phase == LoadingPhase.Placeholder) {
            // This timer survives loading changes. Both timings use the coroutine clock, making
            // production behavior and virtual-clock tests consistent without a wall-clock API.
            delay(LoadingTiming.MinimumVisibleMillis)
            minimumHoldActive = false
            if (!latestLoading) phase = LoadingPhase.Content
        }
    }
    MotionContent(targetState = phase, modifier = modifier) { displayedPhase ->
        when (displayedPhase) {
            LoadingPhase.Waiting -> Box(
                Modifier.graphicsLayer { alpha = 0f }.clearAndSetSemantics {
                    contentDescription = loadingDescription
                    progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                },
            ) { placeholder() }
            LoadingPhase.Placeholder -> Box(
                Modifier.clearAndSetSemantics {
                    contentDescription = loadingDescription
                    progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                },
            ) { placeholder() }
            LoadingPhase.Content -> content()
        }
    }
}
