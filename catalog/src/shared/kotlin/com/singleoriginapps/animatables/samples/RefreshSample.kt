package com.singleoriginapps.animatables.samples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.singleoriginapps.animatables.RefreshableContent
import kotlinx.coroutines.delay

/** Shared catalog fixture: the selection remains interactive during refresh and recovery. */
@Composable
internal fun RefreshSample(refreshKey: Int = 0) {
    var loading by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var offline by rememberSaveable { mutableStateOf(false) }
    var updates by rememberSaveable { mutableIntStateOf(0) }
    var appliedKey by rememberSaveable { mutableIntStateOf(refreshKey) }
    LaunchedEffect(refreshKey) {
        if (appliedKey != refreshKey) {
            appliedKey = refreshKey
            offline = false
            loading = true
        }
    }
    LaunchedEffect(loading) {
        if (loading) {
            delay(1400)
            if (offline) error = "You’re offline. Saved places are still available."
            else { error = null; updates++ }
            loading = false
        }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Choose a place, then refresh or try offline. Your selection stays available.",
            style = MaterialTheme.typography.bodySmall)
        RefreshableContent(hasContent = true, loading = loading, error = error,
            onRetry = { offline = false; loading = true },
            refreshingLabel = "Checking for updates…", placeholder = {}, empty = {}) {
            var selected by rememberSaveable { mutableStateOf("Kyoto") }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Saved places", style = MaterialTheme.typography.titleLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Kyoto", "Lisbon", "Copenhagen").forEach { place ->
                        FilterChip(selected == place, onClick = { selected = place }, label = { Text(place) })
                    }
                }
                Text("Exploring $selected", style = MaterialTheme.typography.titleMedium)
                Text(if (updates > 0) "Updated just now" else "Showing saved information",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = {
                if (loading) loading = false else { offline = false; loading = true }
            }) { Text(if (loading) "Cancel refresh" else "Refresh") }
            TextButton(onClick = { offline = true; loading = true }, enabled = !loading) { Text("Try offline") }
        }
    }
}
