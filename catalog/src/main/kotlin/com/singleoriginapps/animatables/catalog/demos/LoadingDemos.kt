package com.singleoriginapps.animatables.catalog.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.singleoriginapps.animatables.AnimatablesMotion
import com.singleoriginapps.animatables.CompletionSpinner
import com.singleoriginapps.animatables.CompletionSpinnerStyle
import com.singleoriginapps.animatables.LoadingContent
import com.singleoriginapps.animatables.catalog.CatalogTheme
import com.singleoriginapps.animatables.catalog.components.CustomSuccessIcon
import com.singleoriginapps.animatables.catalog.components.ProfileSkeleton
import com.singleoriginapps.animatables.samples.RefreshSample
import kotlinx.coroutines.delay

@Composable
internal fun SpinnerDemo(
    step: Int,
    wavy: Boolean,
    determinate: Boolean,
    customSuccessIcon: Boolean
) {
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

@Composable
internal fun RefreshRecoveryDemo() = RefreshSample()

@Composable
internal fun LoadingDemo() {
    var loading by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(loading) {
        if (loading) {
            delay(1800)
            loading = false
        }
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LoadingContent(loading = loading, placeholder = { ProfileSkeleton() }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SO", style = MaterialTheme.typography.labelLarge)
                }
                Column {
                    Text("Something good", style = MaterialTheme.typography.titleMedium)
                    Text("Made at your own pace", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        FilledTonalButton(onClick = { loading = !loading }) {
            Text(if (loading) "Show content" else "Replay loading")
        }
    }
}

@Preview(
    name = "Completion spinner resting states",
    widthDp = 412,
    heightDp = 360,
    showBackground = true
)
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
                Text(
                    "Indeterminate · animations disabled",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    CompletionSpinner()
                    CompletionSpinner(style = CompletionSpinnerStyle.Wavy)
                    CompletionSpinner(complete = true)
                }
            }
        }
    }
}

@Preview(name = "Loading & Refresh Demos", widthDp = 412, heightDp = 600, showBackground = true)
@Composable
private fun LoadingDemosPreview() {
    CatalogTheme {
        AnimatablesMotion(false) {
            Surface {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                    LoadingDemo()
                    RefreshRecoveryDemo()
                }
            }
        }
    }
}
