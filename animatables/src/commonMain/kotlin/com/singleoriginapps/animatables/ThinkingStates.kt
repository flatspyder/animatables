package com.singleoriginapps.animatables

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * A truthful activity label combining a finite glyph highlight with the text-state handoff.
 *
 * The host owns [text] and must change it only when the actual operation changes state. No
 * synthetic messages, progress or hidden reasoning are generated. [active] controls the finite
 * shimmer; [replayKey] explicitly replays both effects. Once sweeps finish, the current label
 * remains still and legible. System/app motion opt-out leaves an ordinary status label.
 */
@Composable
public fun ThinkingStates(
    text: String,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    replayKey: Any = Unit,
) {
    TextSwapContent(text, modifier, replayKey, travelDp = 8.dp, incomingDelayMillis = 50) { label ->
        ShimmerText(
            text = label,
            active = active && label == text,
            replayKey = replayKey,
            style = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
        )
    }
}
