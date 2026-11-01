package com.singleoriginapps.animatables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/** Caller-owned outcome of an action; changing this value never starts work or resets a timer. */
public enum class AsyncActionState { Idle, Working, Success, Error }

/**
 * A native Material button with coordinated working, success and retry feedback.
 *
 * A fixed 20 dp icon slot stays at the logical start, including when idle. An equal trailing
 * slot balances the label so it is centered on the whole button. The label fades and rises into
 * place; glyphs crossfade without moving with a changing label's width. The
 * caller sizes the button with [modifier], for example `Modifier.width(240.dp)` to fit the
 * longest localized label. Without an explicit width it wraps the current content; labels wrap
 * at the available width. [onClick] always belongs to the same native button, so
 * animation never removes its focus or blocks input. The caller controls [enabled], including
 * whether a pending action can be invoked again, cancelled or retried. No work or timer is owned
 * here; success remains until [state] changes. [errorLabel] is the actionable retry label, not
 * an error explanation: show that explanation alongside the button.
 *
 * Optional [progress] is a normalized fraction read in draw/semantics; it does not complete the
 * action. NaN is zero and values are clamped. Without progress, working is indeterminate.
 * Fast theme motion clarifies occasional state changes. Disabled motion shows the target label
 * and glyph immediately, with a static partial indicator for indeterminate work.
 */
@Composable
public fun AsyncActionButton(
    state: AsyncActionState,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    workingLabel: String = "Working",
    successLabel: String = "Done",
    errorLabel: String = "Retry",
    progress: (() -> Float)? = null,
) {
    val labels = listOf(label, workingLabel, successLabel, errorLabel)
    val motion = rememberMotionEnabled()
    val effects = if (motion) MaterialTheme.motionScheme.fastEffectsSpec<Float>() else snap()
    val spatial = if (motion) MaterialTheme.motionScheme.fastSpatialSpec<IntSize>() else snap()
    val movement = if (motion) MaterialTheme.motionScheme.fastSpatialSpec<IntOffset>() else snap()
    val travel = with(LocalDensity.current) { 8.dp.roundToPx() }
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.width(IntrinsicSize.Max).semantics {
            if (state == AsyncActionState.Working) {
                progressBarRangeInfo =
                    progress?.let { ProgressBarRangeInfo(normalizedFraction(it()), 0f..1f) }
                        ?: ProgressBarRangeInfo.Indeterminate
            }
        },
    ) {
        Row(
            Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.size(20.dp).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
                Crossfade(state, animationSpec = effects, label = "Action icon") { displayed ->
                    ActionGlyph(displayed, progress)
                }
            }
            AnimatedContent(
                state, modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
                transitionSpec = {
                    ((fadeIn(effects) + slideInVertically(movement) { if (motion) travel else 0 })
                            togetherWith fadeOut(effects))
                        .using(SizeTransform(clip = false) { _, _ -> spatial })
                }, label = "Action label"
            ) { displayed ->
                Text(
                    labels[displayed.ordinal], textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().then(
                        if (displayed == state) Modifier else Modifier.clearAndSetSemantics {})
                )
            }
            // Match the icon slot and the Row's two equal gaps, including when idle.
            Box(Modifier.size(20.dp).clearAndSetSemantics {})
        }
    }
}

@Composable
private fun ActionGlyph(state: AsyncActionState, progress: (() -> Float)?) {
    val color = LocalContentColor.current
    val modifier = Modifier.size(20.dp).clearAndSetSemantics {}
    if (state == AsyncActionState.Working) {
        if (progress == null && rememberMotionEnabled()) {
            CircularProgressIndicator(modifier = modifier, color = color, strokeWidth = 2.dp)
        } else {
            CircularProgressIndicator(
                progress = { progress?.invoke()?.let(::normalizedFraction) ?: .25f },
                modifier = modifier, color = color, strokeWidth = 2.dp
            )
        }
    } else if (state != AsyncActionState.Idle) {
        Icon(
            imageVector = if (state == AsyncActionState.Success) MaterialSymbols.Check else MaterialSymbols.Error,
            contentDescription = null,
            modifier = modifier,
            tint = color,
        )
    }
}
