package com.singleoriginapps.animatables

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Compact compatibility form of [SuccessCheck], using the same finite pop, rotation and check draw.
 *
 * Use [SuccessCheck] directly to request an explicit replay or its larger default presentation.
 * Labels are localized by the host; state remains meaningful with motion disabled.
 */
@Composable
public fun CompletionIndicator(
    complete: Boolean,
    modifier: Modifier = Modifier,
    label: String = "Complete",
    pendingLabel: String = "Not complete",
) {
    SuccessCheck(
        complete = complete,
        modifier = modifier.size(32.dp),
        label = label,
        pendingLabel = pendingLabel,
    )
}
