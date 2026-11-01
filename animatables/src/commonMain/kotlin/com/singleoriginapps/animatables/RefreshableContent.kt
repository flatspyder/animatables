package com.singleoriginapps.animatables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Initial loading, empty/error recovery and refresh feedback that retains existing content.
 *
 * [hasContent] describes usable caller-owned data, independently of [loading] and [error]. When
 * true, [content] stays in the same composition while refresh starts, fails or retries, preserving
 * its local state, scroll and focus. Keep the previous data in the caller until a replacement is
 * available. A loading notice takes precedence over an old error during retry. Set error to null
 * after recovery; this component neither caches data nor clears errors automatically.
 *
 * Without content, [LoadingContent]'s delayed static [placeholder] and minimum visible hold apply.
 * After loading, display [empty] or the localized [error] with an optional native retry button.
 * Existing content receives a separate inline notice; it is never replaced by a refresh skeleton.
 * [onRetry] owns retry work. Localize the labels and error. Fast theme motion hands off the notice;
 * disabled motion retains all text/actions with a static partial refresh indicator.
 */
@Composable
public fun RefreshableContent(
    hasContent: Boolean,
    loading: Boolean,
    modifier: Modifier = Modifier,
    error: String? = null,
    onRetry: (() -> Unit)? = null,
    loadingLabel: String = "Loading",
    refreshingLabel: String = "Refreshing",
    retryLabel: String = "Retry",
    placeholder: @Composable () -> Unit,
    empty: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    LoadingContent(
        loading = loading && !hasContent,
        modifier = modifier,
        loadingDescription = loadingLabel,
        placeholder = placeholder,
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Place notices before content so a full-height scrolling body cannot hide recovery.
            if (hasContent) {
                MotionContent(
                    targetState = if (loading) RefreshNotice.Loading else error?.let(RefreshNotice::Error),
                    modifier = Modifier.fillMaxWidth(),
                ) { notice ->
                    when (notice) {
                        RefreshNotice.Loading -> Column(
                            Modifier.fillMaxWidth().semantics {
                                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                            },
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(refreshingLabel, style = MaterialTheme.typography.bodyMedium)
                            val indicator = Modifier.fillMaxWidth().clearAndSetSemantics {}
                            if (rememberMotionEnabled()) LinearProgressIndicator(modifier = indicator)
                            else LinearProgressIndicator(progress = { .25f }, modifier = indicator)
                        }

                        is RefreshNotice.Error -> RecoveryNotice(
                            notice.message, retryLabel,
                            if (!loading && error != null) onRetry else null
                        )

                        null -> Unit
                    }
                }
            }
            // Keep this branch independent of refresh/error so nested state and focus survive.
            if (hasContent) content()
            else if (loading) Text(loadingLabel, style = MaterialTheme.typography.bodyMedium)
            else if (error != null) RecoveryNotice(error, retryLabel, onRetry)
            else empty()
        }
    }
}

private sealed interface RefreshNotice {
    data object Loading : RefreshNotice
    data class Error(val message: String) : RefreshNotice
}

@Composable
private fun RecoveryNotice(message: String, retryLabel: String, onRetry: (() -> Unit)?) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
        if (onRetry != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onRetry) { Text(retryLabel) }
            }
        }
    }
}
