package com.singleoriginapps.animatables.catalog.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.singleoriginapps.animatables.AnimatablesMotion
import com.singleoriginapps.animatables.NumberFlowAnchor
import com.singleoriginapps.animatables.NumberPopIn
import com.singleoriginapps.animatables.ShimmerText
import com.singleoriginapps.animatables.TextReveal
import com.singleoriginapps.animatables.TextStateSwap
import com.singleoriginapps.animatables.catalog.CatalogTheme
import com.singleoriginapps.animatables.catalog.components.DemoActions
import com.singleoriginapps.animatables.catalog.components.DemoColumn

@Composable
internal fun NumberDemo() {
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
internal fun TextSwapDemo() {
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
internal fun RevealDemo() {
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
internal fun ShimmerDemo() {
    var active by rememberSaveable { mutableStateOf(true) }
    var replay by rememberSaveable { mutableIntStateOf(0) }
    var continuous by rememberSaveable { mutableStateOf(true) }
    DemoColumn {
        ShimmerText(
            "Planning the next move",
            active = active,
            continuous = continuous,
            replayKey = replay,
            style = MaterialTheme.typography.titleLarge
        )
        DemoActions(
            if (active) "Pause shimmer" else "Resume shimmer",
            { active = !active },
            {
                active = true
                replay++
            }
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(continuous, { continuous = it })
            Text("Continuous shimmer", style = MaterialTheme.typography.labelMedium)
        }
        Text(
            if (continuous) "Repeats until paused." else "Two sweeps, then a quiet resting state.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Preview(name = "Text Demos", widthDp = 412, heightDp = 800, showBackground = true)
@Composable
private fun TextDemosPreview() {
    CatalogTheme {
        AnimatablesMotion(false) {
            Surface {
                Column(
                    Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    NumberDemo()
                    TextSwapDemo()
                    RevealDemo()
                    ShimmerDemo()
                }
            }
        }
    }
}
