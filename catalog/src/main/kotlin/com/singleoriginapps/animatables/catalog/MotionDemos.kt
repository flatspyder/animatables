package com.singleoriginapps.animatables.catalog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.singleoriginapps.animatables.AnimatablesMotion
import com.singleoriginapps.animatables.IconSwap
import com.singleoriginapps.animatables.NumberFlowAnchor
import com.singleoriginapps.animatables.NumberPopIn
import com.singleoriginapps.animatables.ReasoningStream
import com.singleoriginapps.animatables.ShimmerText
import com.singleoriginapps.animatables.StreamingText
import com.singleoriginapps.animatables.CompletionSpinner
import com.singleoriginapps.animatables.CompletionSpinnerStyle
import com.singleoriginapps.animatables.SuccessCheck
import com.singleoriginapps.animatables.TextReveal
import com.singleoriginapps.animatables.TextStateSwap
import com.singleoriginapps.animatables.ThinkingStates
import kotlinx.coroutines.delay

@Composable
internal fun SystemMotionLabel() {
    val scale = rememberSystemAnimationScale()
    Text(
        if (scale == 0f) "Android animations are disabled" else "Android animation scale: ${scale}×",
        style = MaterialTheme.typography.bodySmall,
        color = if (scale == 0f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
    )
}

@Composable
internal fun MotionEffectDemo(
    id: String,
    spinnerStep: Int = 0,
    spinnerWavy: Boolean = false,
    spinnerDeterminate: Boolean = true,
    customSuccessIcon: Boolean = false,
) {
    when (id) {
        "number" -> NumberDemo()
        "text-swap" -> TextSwapDemo()
        "icon" -> IconDemo()
        "success" -> SuccessDemo(customSuccessIcon)
        "spinner" -> SpinnerDemo(spinnerStep, spinnerWavy, spinnerDeterminate, customSuccessIcon)
        "reveal" -> RevealDemo()
        "shimmer" -> ShimmerDemo()
        "thinking" -> ThinkingDemo()
        "reasoning" -> ReasoningDemo()
        "streaming" -> StreamDemo()
    }
}

@Composable
private fun NumberDemo() {
    var index by rememberSaveable { mutableIntStateOf(0) }
    var replay by rememberSaveable { mutableIntStateOf(0) }
    var decimalAnchor by rememberSaveable { mutableStateOf(false) }
    val values = listOf("99.90", "100.00", "99.99", "9.00", "10.00", "3,204.16", "−12.50")
    DemoColumn {
        NumberPopIn(
            values[index],
            Modifier.fillMaxWidth(),
            replayKey = replay,
            style = MaterialTheme.typography.displaySmall,
            anchor = if (decimalAnchor) NumberFlowAnchor.Decimal else NumberFlowAnchor.Center
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(decimalAnchor, { decimalAnchor = it })
            Text("Anchor decimal point", style = MaterialTheme.typography.labelMedium)
        }
        DemoActions("Change number", { index = (index + 1) % values.size }, { replay++ })
    }
}

@Composable
private fun TextSwapDemo() {
    var index by rememberSaveable { mutableIntStateOf(0) }
    var replay by rememberSaveable { mutableIntStateOf(0) }
    val states = listOf("Transaction processing…", "Transaction complete", "Ready for the next one")
    DemoColumn {
        TextStateSwap(
            states[index],
            replayKey = replay,
            style = MaterialTheme.typography.titleMedium
        )
        DemoActions("Next state", { index = (index + 1) % states.size }, { replay++ })
    }
}

@Composable
private fun IconDemo() {
    var selected by rememberSaveable { mutableStateOf(false) }
    var replay by rememberSaveable { mutableIntStateOf(0) }
    DemoColumn {
        IconSwap(selected, Modifier.size(56.dp), replayKey = replay) { close -> DemoSymbol(close) }
        Text(if (selected) "Close" else "Menu", style = MaterialTheme.typography.labelMedium)
        DemoActions("Swap icon", { selected = !selected }, { replay++ })
    }
}

@Composable
private fun DemoSymbol(close: Boolean) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier
        .size(40.dp)
        .semantics { contentDescription = if (close) "Close" else "Menu" }) {
        val stroke = 3.dp.toPx()
        if (close) {
            drawLine(
                color,
                Offset(size.width * .23f, size.height * .23f),
                Offset(size.width * .77f, size.height * .77f),
                stroke,
                StrokeCap.Round
            )
            drawLine(
                color,
                Offset(size.width * .77f, size.height * .23f),
                Offset(size.width * .23f, size.height * .77f),
                stroke,
                StrokeCap.Round
            )
        } else {
            listOf(.25f, .5f, .75f).forEach { y ->
                drawLine(
                    color,
                    Offset(size.width * .15f, size.height * y),
                    Offset(size.width * .85f, size.height * y),
                    stroke,
                    StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun SuccessDemo(customSuccessIcon: Boolean) {
    var complete by rememberSaveable { mutableStateOf(true) }
    var replay by rememberSaveable { mutableIntStateOf(0) }
    DemoColumn {
        SuccessCheck(
            complete,
            Modifier.size(64.dp),
            replayKey = replay,
            label = "Changes saved",
            pendingLabel = "Ready to save",
            successIcon = if (customSuccessIcon) ({ CustomSuccessIcon() }) else null,
        )
        Text(
            if (complete) "Changes saved" else "Ready to save",
            style = MaterialTheme.typography.titleMedium
        )
        DemoActions(
            if (complete) "Reset" else "Complete",
            { complete = !complete },
            { complete = true; replay++ })
    }
}

@Composable
private fun RevealDemo() {
    var replay by rememberSaveable { mutableIntStateOf(0) }
    DemoColumn {
        TextReveal(
            listOf("A little room for possibility.", "Make your next idea happen."),
            replayKey = replay,
            style = MaterialTheme.typography.titleMedium
        )
        FilledTonalButton(onClick = { replay++ }) { Text("Replay reveal") }
    }
}

@Composable
private fun ShimmerDemo() {
    var active by rememberSaveable { mutableStateOf(true) }
    var replay by rememberSaveable { mutableIntStateOf(0) }
    DemoColumn {
        ShimmerText(
            "Planning the next move",
            active = active,
            replayKey = replay,
            style = MaterialTheme.typography.titleLarge
        )
        DemoActions(
            if (active) "Pause shimmer" else "Resume shimmer",
            { active = !active },
            { active = true; replay++ })
        Text(
            "Two slower sweeps, then a quiet resting state.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ThinkingDemo() {
    val states = listOf(
        "Reading your request",
        "Exploring possibilities",
        "Bringing it together",
        "Ready to review"
    )
    var index by rememberSaveable { mutableIntStateOf(0) }
    var running by rememberSaveable { mutableStateOf(true) }
    var replay by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(running, replay) {
        while (running && index < states.lastIndex) {
            delay(2000); index++
        }
        if (index == states.lastIndex) running = false
    }
    DemoColumn {
        ThinkingStates(states[index], active = running, replayKey = replay)
        DemoActions(
            if (running) "Pause" else "Continue",
            { if (index == states.lastIndex) index = 0; running = !running },
            { index = 0; running = true; replay++ })
        Text(
            "Demo sequence · real apps supply task state",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private val activityEntries = listOf(
    "Reading the component documentation.",
    "Comparing Material motion tokens.",
    "Checking the layout with larger text.",
    "Verifying the animation's resting state.",
    "The changes are ready to review.",
)

@Composable
private fun ReasoningDemo() {
    var count by rememberSaveable { mutableIntStateOf(1) }
    var running by rememberSaveable { mutableStateOf(true) }
    var replay by rememberSaveable { mutableIntStateOf(0) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(running, replay) {
        while (running && count < activityEntries.size) {
            delay(1500); count++
        }
        if (count == activityEntries.size) {
            delay(500); running = false
        }
    }
    DemoColumn {
        ReasoningStream(
            activityEntries.take(count),
            Modifier.fillMaxWidth(),
            maxVisibleEntries = if (expanded) activityEntries.size else 2,
            active = running
        )
        DemoActions(
            if (running) "Pause" else "Continue",
            { if (count == activityEntries.size) count = 1; running = !running },
            { count = 1; running = true; replay++ })
        TextButton(onClick = {
            expanded = !expanded
        }) { Text(if (expanded) "Recent activity" else "Show transcript") }
        Text("Demo activity log", style = MaterialTheme.typography.bodySmall)
    }
}

private const val demoResponse =
    "Motion gives a changing interface continuity. New words arrive softly, while the text you have already read stays in place.\n\nThe caller supplies the real response as it arrives. You can pause this demo, resume it, or start a fresh stream."

@Composable
private fun StreamDemo() {
    val chunks = remember { Regex("\\S+\\s*").findAll(demoResponse).map { it.value }.toList() }
    var count by rememberSaveable { mutableIntStateOf(1) }
    var running by rememberSaveable { mutableStateOf(true) }
    var streamKey by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(running, streamKey) {
        while (running && count < chunks.size) {
            delay(130); count++
        }
        if (count == chunks.size) {
            delay(450); running = false
        }
    }
    DemoColumn {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
            // Only the catalog knows the complete fixture. Reserve its native paragraph height
            // so centring the demo cannot move settled lines upward whenever a new line wraps.
            Text(demoResponse, Modifier
                .fillMaxWidth()
                .alpha(0f)
                .clearAndSetSemantics {})
            StreamingText(
                chunks.take(count).joinToString(""),
                Modifier.fillMaxWidth(),
                streamKey = streamKey,
                animate = running
            )
        }
        DemoActions(
            if (running) "Pause stream" else "Continue",
            { if (count == chunks.size) count = 1; running = !running },
            { count = 1; running = true; streamKey++ })
        Text(
            "Demo response · appended text from the caller",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun DemoColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        content = content
    )
}

@Composable
private fun DemoActions(label: String, onChange: () -> Unit, onReplay: () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilledTonalButton(onClick = onChange) { Text(label) }
        OutlinedButton(onClick = onReplay) { Text("Replay") }
    }
}

internal fun effectApiExample(id: String): String? = when (id) {
    "number" -> "NumberPopIn(value = formattedValue,\n  replayKey = replay)"
    "text-swap" -> "TextStateSwap(text = status)"
    "icon" -> "IconSwap(targetState = expanded) {\n  state -> IconFor(state)\n}"
    "success" -> "SuccessCheck(complete = saved,\n  replayKey = replay)"
    "spinner" -> "CompletionSpinner(\n  progress = { progress },\n  complete = complete,\n  style = CompletionSpinnerStyle.Wavy,\n)"
    "reveal" -> "TextReveal(lines = lines,\n  replayKey = replay)"
    "shimmer" -> "ShimmerText(text = status,\n  active = running, replayKey = replay)"
    "thinking" -> "ThinkingStates(text = taskStatus,\n  active = running)"
    "reasoning" -> "ReasoningStream(entries = activityLog,\n  maxVisibleEntries = 2, active = running)"
    "streaming" -> "StreamingText(text = responseSoFar,\n  streamKey = responseId)"
    else -> null
}

@Composable
private fun SpinnerDemo(step: Int, wavy: Boolean, determinate: Boolean, customSuccessIcon: Boolean) {
    val completed = step % 2 != 0
    val progress = if (completed) 1f else .35f
    val complete = completed && !determinate
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CompletionSpinner(
            progress = if (determinate) ({ progress }) else null,
            complete = complete,
            style = if (wavy) CompletionSpinnerStyle.Wavy else CompletionSpinnerStyle.Standard,
            successIcon = if (customSuccessIcon) ({ CustomSuccessIcon() }) else null,
        )
        Text(
            if (complete || determinate && progress >= 1f) "Task complete"
            else if (determinate) "${(progress * 100).toInt()}% complete" else "Working…",
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Preview(name = "Completion spinner resting states", widthDp = 412, heightDp = 360, showBackground = true)
@Composable
private fun CompletionSpinnerRestingPreview() {
    CatalogTheme {
        AnimatablesMotion(false) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text("Standard · wavy · complete", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    CompletionSpinner(progress = { .4f })
                    CompletionSpinner(progress = { .4f }, style = CompletionSpinnerStyle.Wavy)
                    CompletionSpinner(progress = { 1f })
                }
                Text("Indeterminate · animations disabled", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    CompletionSpinner()
                    CompletionSpinner(style = CompletionSpinnerStyle.Wavy)
                    CompletionSpinner(complete = true)
                }
            }
        }
    }
}

@Composable
private fun CustomSuccessIcon() {
    val color = LocalContentColor.current
    Canvas(Modifier.fillMaxSize()) {
        val icon = Path().apply {
            moveTo(size.width * .5f, 0f)
            lineTo(size.width, size.height * .5f)
            lineTo(size.width * .5f, size.height)
            lineTo(0f, size.height * .5f)
            close()
        }
        drawPath(icon, color)
    }
}
