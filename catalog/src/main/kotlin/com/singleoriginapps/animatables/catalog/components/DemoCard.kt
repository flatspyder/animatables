package com.singleoriginapps.animatables.catalog.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.singleoriginapps.animatables.AnimatablesMotion
import com.singleoriginapps.animatables.catalog.CatalogTheme
import com.singleoriginapps.animatables.catalog.demos.DemoContent
import com.singleoriginapps.animatables.catalog.model.Collection
import com.singleoriginapps.animatables.catalog.model.Demo
import com.singleoriginapps.animatables.catalog.model.apiSnippetFor

@Composable
internal fun DemoCard(demo: Demo, onInspect: (String) -> Unit) {
    var showApi by rememberSaveable { mutableStateOf(false) }
    var spinnerStep by rememberSaveable { mutableIntStateOf(0) }
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    demo.id,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    demo.group.title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 156.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                DemoContent(demo.id, spinnerStep = spinnerStep)
            }
            Text(demo.title, style = MaterialTheme.typography.titleLarge)
            Text(
                demo.note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (demo.id == "spinner") {
                FilledTonalButton(onClick = { spinnerStep++ }) { Text("Toggle completion") }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onInspect(demo.id) }, modifier = Modifier.weight(1f)) {
                    Text("Inspect motion")
                }
                TextButton(onClick = { showApi = !showApi }) {
                    Text(if (showApi) "Hide API" else "View API")
                }
            }
            if (showApi) {
                Text(
                    demo.api,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    apiSnippetFor(demo.id),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Preview(name = "Demo Card · Light", widthDp = 360, showBackground = true)
@Composable
private fun DemoCardLightPreview() {
    CatalogTheme {
        AnimatablesMotion(false) {
            Box(Modifier.padding(16.dp)) {
                DemoCard(
                    demo = Demo(
                        "async-action",
                        "Action feedback",
                        "Save, working, success and retry in one stable control.",
                        Collection.Feedback,
                        "AsyncActionButton"
                    ),
                    onInspect = {}
                )
            }
        }
    }
}

@Preview(name = "Demo Card · Dark", widthDp = 360, showBackground = true)
@Composable
private fun DemoCardDarkPreview() {
    CatalogTheme(dark = true) {
        AnimatablesMotion(false) {
            Box(Modifier.padding(16.dp)) {
                DemoCard(
                    demo = Demo(
                        "waveform",
                        "Audio waveform",
                        "Anchored energy bands respond to speech attacks and pauses.",
                        Collection.Audio,
                        "AudioWaveform"
                    ),
                    onInspect = {}
                )
            }
        }
    }
}
