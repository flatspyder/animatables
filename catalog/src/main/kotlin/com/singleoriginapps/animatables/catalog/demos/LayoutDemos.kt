package com.singleoriginapps.animatables.catalog.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.singleoriginapps.animatables.AnimatablesMotion
import com.singleoriginapps.animatables.ExpandableCard
import com.singleoriginapps.animatables.MotionContent
import com.singleoriginapps.animatables.catalog.CatalogTheme

@Composable
internal fun ExpansionDemo() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExpandableCard(expanded, { expanded = it }, "A weekend away", Modifier.fillMaxWidth()) {
        Text(
            "Slow mornings. New places.\nA little room for possibility.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
internal fun ContentDemo() {
    var selected by rememberSaveable { mutableStateOf(false) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MotionContent(selected) { detail ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    if (detail) "A quieter kind of focus" else "Find your focus",
                    style = MaterialTheme.typography.titleMedium
                )
                if (detail) {
                    Text(
                        "Make a little space for your next idea.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        FilledTonalButton(onClick = { selected = !selected }) {
            Text(if (selected) "Less detail" else "More detail")
        }
    }
}

@Preview(name = "Layout Demos", widthDp = 412, heightDp = 400, showBackground = true)
@Composable
private fun LayoutDemosPreview() {
    CatalogTheme {
        AnimatablesMotion(false) {
            Surface {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    ExpansionDemo()
                    ContentDemo()
                }
            }
        }
    }
}
