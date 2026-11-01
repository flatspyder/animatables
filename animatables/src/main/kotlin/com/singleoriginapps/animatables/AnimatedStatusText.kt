package com.singleoriginapps.animatables

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * An occasional status-label handoff, delegating to [TextStateSwap] with Material body typography.
 *
 * Supply complete meaningful labels such as “Saving” and “Saved”. Never put a per-frame progress
 * value here: rapidly changing values should be ordinary Text. The latest complete label is
 * exposed as one polite accessibility live region, including when motion is disabled.
 */
@Composable
public fun AnimatedStatusText(text: String, modifier: Modifier = Modifier) {
    TextStateSwap(text = text, modifier = modifier, style = MaterialTheme.typography.bodyLarge)
}
