package com.singleoriginapps.animatables.catalog.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.singleoriginapps.animatables.AnimatablesMotion
import com.singleoriginapps.animatables.AnimatedCountBadge
import com.singleoriginapps.animatables.AnimatedStatusText
import com.singleoriginapps.animatables.AsyncActionButton
import com.singleoriginapps.animatables.AsyncActionState
import com.singleoriginapps.animatables.IconSwap
import com.singleoriginapps.animatables.SuccessCheck
import com.singleoriginapps.animatables.catalog.CatalogTheme
import com.singleoriginapps.animatables.catalog.components.CustomSuccessIcon
import com.singleoriginapps.animatables.catalog.components.DemoActions
import com.singleoriginapps.animatables.catalog.components.DemoColumn
import com.singleoriginapps.animatables.samples.MaterialSymbols
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun AsyncActionDemo() {
    var state by rememberSaveable { mutableStateOf(AsyncActionState.Idle) }
    var fail by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (state == AsyncActionState.Working) {
            delay(1400.milliseconds)
            state = if (fail) AsyncActionState.Error else AsyncActionState.Success
        }
    }
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncActionButton(
            state,
            onClick = {
                state =
                    if (state == AsyncActionState.Working) AsyncActionState.Idle else AsyncActionState.Working
            },
            label = "Save changes",
            modifier = Modifier.width(240.dp),
            workingLabel = "Saving · cancel",
            successLabel = "Changes saved",
            errorLabel = "Retry save"
        )
        if (state == AsyncActionState.Error) {
            Text(
                "Couldn’t save. Your changes are still here.",
                color = MaterialTheme.colorScheme.error
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(fail, { fail = it })
            Text("Simulate failure", style = MaterialTheme.typography.labelMedium)
        }
        TextButton(onClick = { state = AsyncActionState.Idle }) { Text("Reset") }
    }
}

@Composable
internal fun IconDemo() {
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
    Icon(
        if (close) MaterialSymbols.Close else MaterialSymbols.Menu,
        contentDescription = if (close) "Close" else "Menu",
        modifier = Modifier.size(40.dp),
        tint = MaterialTheme.colorScheme.primary
    )
}

@Composable
internal fun SuccessDemo(customSuccessIcon: Boolean) {
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
            {
                complete = true
                replay++
            }
        )
    }
}

@Composable
internal fun StatusDemo() {
    var state by rememberSaveable { mutableIntStateOf(0) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedStatusText(
            listOf(
                "Ready when you are",
                "Finding your rhythm",
                "Everything in its place"
            )[state]
        )
        FilledTonalButton(onClick = { state = (state + 1) % 3 }) { Text("Change status") }
    }
}

@Composable
internal fun BadgeDemo() {
    var count by rememberSaveable { mutableIntStateOf(9) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Your inbox", style = MaterialTheme.typography.titleLarge)
            AnimatedCountBadge(count)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { count = (count - 1).coerceAtLeast(0) }) {
                Icon(
                    MaterialSymbols.Remove,
                    contentDescription = "Remove notification"
                )
            }
            FilledTonalButton(onClick = { count++ }) { Text("Add one") }
        }
        TextButton(onClick = { count = if (count < 10) 12 else 9 }) {
            Text("Try 9 ↔ 12")
        }
    }
}

@Preview(name = "Actions preview", widthDp = 412, heightDp = 660, showBackground = true)
@Composable
private fun ActionRecoveryPreview() {
    CatalogTheme(false) {
        AnimatablesMotion(false) {
            Column(
                Modifier
                    .widthIn(max = 380.dp)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                AsyncActionDemo()
            }
        }
    }
}

@Preview(
    name = "Actions large text RTL",
    widthDp = 412,
    heightDp = 660,
    fontScale = 1.5f,
    locale = "ar",
    showBackground = true
)
@Composable
private fun ActionRecoveryRtlPreview() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ActionRecoveryPreview()
    }
}

@Preview(name = "Feedback Demos", widthDp = 412, heightDp = 800, showBackground = true)
@Composable
private fun FeedbackDemosPreview() {
    CatalogTheme {
        AnimatablesMotion(false) {
            Surface {
                Column(
                    Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    IconDemo()
                    SuccessDemo(customSuccessIcon = false)
                    StatusDemo()
                    BadgeDemo()
                }
            }
        }
    }
}
