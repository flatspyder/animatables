package com.singleoriginapps.animatables.catalog.demos

import androidx.compose.runtime.Composable

@Composable
internal fun DemoContent(
    id: String,
    spinnerStep: Int = 0,
    spinnerWavy: Boolean = false,
    spinnerDeterminate: Boolean = true,
    customSuccessIcon: Boolean = false,
) {
    when (id) {
        // Feedback
        "async-action" -> AsyncActionDemo()
        "icon" -> IconDemo()
        "success" -> SuccessDemo(customSuccessIcon)
        "02" -> StatusDemo()
        "03" -> BadgeDemo()

        // Text
        "number" -> NumberDemo()
        "text-swap" -> TextSwapDemo()
        "reveal" -> RevealDemo()
        "shimmer" -> ShimmerDemo()

        // Activity
        "thinking" -> ThinkingDemo()
        "reasoning" -> ReasoningDemo()
        "streaming" -> StreamDemo()

        // Loading
        "spinner" -> SpinnerDemo(spinnerStep, spinnerWavy, spinnerDeterminate, customSuccessIcon)
        "refresh" -> RefreshRecoveryDemo()
        "04" -> LoadingDemo()

        // Layout
        "01" -> ExpansionDemo()
        "06" -> ContentDemo()

        // Audio
        "waveform" -> AudioDemo("waveform")
        "audio-icon" -> AudioDemo("icon")
        "audio-ring" -> AudioDemo("ring")

        else -> Unit
    }
}
