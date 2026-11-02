package com.singleoriginapps.animatables.catalog.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.singleoriginapps.animatables.AnimatablesMotion
import com.singleoriginapps.animatables.ReasoningStream
import com.singleoriginapps.animatables.StreamingText
import com.singleoriginapps.animatables.ThinkingStates
import com.singleoriginapps.animatables.catalog.CatalogTheme
import com.singleoriginapps.animatables.catalog.components.DemoActions
import com.singleoriginapps.animatables.catalog.components.DemoColumn
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun ThinkingDemo() {
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
            delay(2000.milliseconds)
            index++
        }
        if (index == states.lastIndex) running = false
    }
    DemoColumn {
        ThinkingStates(states[index], active = running, replayKey = replay)
        DemoActions(
            if (running) "Pause" else "Continue",
            {
                if (index == states.lastIndex) index = 0
                running = !running
            },
            {
                index = 0
                running = true
                replay++
            }
        )
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
internal fun ReasoningDemo() {
    var count by rememberSaveable { mutableIntStateOf(1) }
    var running by rememberSaveable { mutableStateOf(true) }
    var replay by rememberSaveable { mutableIntStateOf(0) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(running, replay) {
        while (running && count < activityEntries.size) {
            delay(1500.milliseconds)
            count++
        }
        if (count == activityEntries.size) {
            delay(500.milliseconds)
            running = false
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
            {
                if (count == activityEntries.size) count = 1
                running = !running
            },
            {
                count = 1
                running = true
                replay++
            }
        )
        TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "Recent activity" else "Show transcript")
        }
        Text("Demo activity log", style = MaterialTheme.typography.bodySmall)
    }
}

private const val DEMO_RESPONSE =
    "Motion gives a changing interface continuity. New words arrive softly, while the text you have already read stays in place.\n\nThe caller supplies the real response as it arrives. You can pause this demo, resume it, or start a fresh stream."

@Composable
internal fun StreamDemo() {
    val chunks = remember { Regex("\\S+\\s*").findAll(DEMO_RESPONSE).map { it.value }.toList() }
    var count by rememberSaveable { mutableIntStateOf(1) }
    var running by rememberSaveable { mutableStateOf(true) }
    var streamKey by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(running, streamKey) {
        while (running && count < chunks.size) {
            delay(130.milliseconds)
            count++
        }
        if (count == chunks.size) {
            delay(600.milliseconds)
            running = false
        }
    }
    DemoColumn {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
            // Only the catalog knows the complete fixture. Reserve its native paragraph height
            // so centring the demo cannot move settled lines upward whenever a new line wraps.
            Text(
                DEMO_RESPONSE,
                Modifier
                    .fillMaxWidth()
                    .alpha(0f)
                    .clearAndSetSemantics {}
            )
            StreamingText(
                chunks.take(count).joinToString(""),
                Modifier.fillMaxWidth(),
                streamKey = streamKey,
                animate = running
            )
        }
        DemoActions(
            if (running) "Pause stream" else "Continue",
            {
                if (count == chunks.size) count = 1
                running = !running
            },
            {
                count = 1
                running = true
                streamKey++
            }
        )
        Text(
            "Demo response · appended text from the caller",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Preview(name = "Activity Demos", widthDp = 412, heightDp = 800, showBackground = true)
@Composable
private fun ActivityDemosPreview() {
    CatalogTheme {
        AnimatablesMotion(false) {
            Surface {
                Column(
                    Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    ThinkingDemo()
                    ReasoningDemo()
                    StreamDemo()
                }
            }
        }
    }
}
