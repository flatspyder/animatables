package com.singleoriginapps.animatables

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.unit.dp

/** The native Material indicator displayed while [CompletionSpinner] is pending. */
public enum class CompletionSpinnerStyle { Standard, Wavy }

/**
 * Native Material progress followed by the same success entrance as [SuccessCheck].
 *
 * Supply [progress] for determinate loading, or null for indeterminate loading. Either
 * [complete] or progress at least 1 completes it. Values are clamped and NaN is zero; reset
 * both inputs to return to loading. Progress is read in draw and semantics, while crossing the
 * completion boundary updates the target state. An initially completed spinner is static.
 *
 * Use for occasional foreground completion. The host owns work, cancellation and a pause or
 * hide control for long-running loading. Localize [label] and [completeLabel]. The optional
 * [successIcon] replaces the check in a centered half-size slot, inheriting [completeContentColor].
 * Motion disabled shows the target success immediately or a static partial loading indicator.
 */
@Composable
public fun CompletionSpinner(
    progress: (() -> Float)? = null,
    complete: Boolean = false,
    modifier: Modifier = Modifier,
    style: CompletionSpinnerStyle = CompletionSpinnerStyle.Standard,
    color: Color = MaterialTheme.colorScheme.primary,
    completeColor: Color = MaterialTheme.colorScheme.primaryContainer,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    label: String = "Loading",
    completeLabel: String = "Complete",
    completeContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    successIcon: (@Composable () -> Unit)? = null,
) {
    val latestProgress = rememberUpdatedState(progress)
    val progressComplete by remember {
        derivedStateOf { (latestProgress.value?.invoke() ?: 0f) >= 1f }
    }
    val completed = complete || progressComplete
    val enabled = rememberMotionEnabled()
    val indicatorProgress = remember {
        {
            latestProgress.value?.invoke()?.let { if (it.isNaN()) 0f else it.coerceIn(0f, 1f) } ?: .25f
        }
    }
    SuccessFeedback(
        complete = completed, modifier = modifier, label = completeLabel, pendingLabel = label,
        fill = completeColor, content = completeContentColor, successIcon = successIcon,
        initialCompleteStatic = true, enabled = enabled,
        pendingSemantics = {
            progressBarRangeInfo = if (latestProgress.value == null) ProgressBarRangeInfo.Indeterminate
            else ProgressBarRangeInfo(indicatorProgress(), 0f..1f)
        },
    ) { fraction, show ->
        if (show || !completed) {
            val spinnerModifier = Modifier.matchParentSize().graphicsLayer {
                alpha = 1f - fraction()
            }
            when (style) {
                CompletionSpinnerStyle.Standard -> if (progress == null && enabled) {
                    CircularProgressIndicator(modifier = spinnerModifier, color = color,
                        trackColor = trackColor, strokeWidth = 4.dp)
                } else {
                    CircularProgressIndicator(progress = indicatorProgress, modifier = spinnerModifier,
                        color = color, trackColor = trackColor, strokeWidth = 4.dp)
                }
                CompletionSpinnerStyle.Wavy -> if (progress == null && enabled) {
                    CircularWavyProgressIndicator(modifier = spinnerModifier, color = color, trackColor = trackColor)
                } else {
                    CircularWavyProgressIndicator(progress = indicatorProgress, modifier = spinnerModifier,
                        color = color, trackColor = trackColor,
                        waveSpeed = if (enabled) WavyProgressIndicatorDefaults.CircularWavelength else 0.dp)
                }
            }
        }
    }
}
