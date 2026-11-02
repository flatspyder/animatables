package com.singleoriginapps.animatables.catalog.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.singleoriginapps.animatables.catalog.CatalogTheme

@Composable
internal fun DemoColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        content = content
    )
}

@Composable
internal fun DemoActions(label: String, onChange: () -> Unit, onReplay: () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilledTonalButton(onClick = onChange) { Text(label) }
        OutlinedButton(onClick = onReplay) { Text("Replay") }
    }
}

@Preview(name = "Demo Actions", widthDp = 300, showBackground = true)
@Composable
private fun DemoActionsPreview() {
    CatalogTheme {
        Box(Modifier.padding(16.dp)) {
            DemoActions(label = "Change state", onChange = {}, onReplay = {})
        }
    }
}
