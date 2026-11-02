package com.singleoriginapps.animatables.catalog.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.singleoriginapps.animatables.AnimatablesMotion
import com.singleoriginapps.animatables.catalog.CatalogTheme
import com.singleoriginapps.animatables.catalog.components.SystemMotionLabel
import com.singleoriginapps.animatables.catalog.demos.DemoContent
import com.singleoriginapps.animatables.catalog.model.Collection
import com.singleoriginapps.animatables.catalog.model.Demo
import com.singleoriginapps.animatables.catalog.model.apiSnippetFor
import com.singleoriginapps.animatables.catalog.rememberSystemAnimationScale
import com.singleoriginapps.animatables.samples.MaterialSymbols

@Composable
internal fun EffectInspectorScreen(
    demo: Demo,
    motion: Boolean,
    onMotionChange: (Boolean) -> Unit,
    dark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    var centreGuide by rememberSaveable(demo.id) { mutableStateOf(false) }
    var spinnerStep by rememberSaveable(demo.id) { mutableIntStateOf(0) }
    var spinnerWavy by rememberSaveable(demo.id) { mutableStateOf(false) }
    var spinnerDeterminate by rememberSaveable(demo.id) { mutableStateOf(true) }
    var customSuccessIcon by rememberSaveable(demo.id) { mutableStateOf(false) }
    val guideColor = MaterialTheme.colorScheme.outline.copy(alpha = .45f)

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TextButton(onClick = onClose) {
                Icon(
                    MaterialSymbols.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Back to collection")
            }
            Text(
                demo.title,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                demo.note,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Switch(
                    motion,
                    onMotionChange,
                    modifier = Modifier.semantics { contentDescription = "Library motion" }
                )
                Column {
                    Text(
                        if (motion) "Library motion on" else "Library motion off",
                        style = MaterialTheme.typography.labelLarge
                    )
                    SystemMotionLabel()
                }
            }
            if (rememberSystemAnimationScale() == 0f) {
                Text(
                    "Android's animation duration scale is zero. Enable animations in Android settings to see movement; replay still updates the final state.",
                    color = MaterialTheme.colorScheme.error
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = centreGuide,
                    onClick = { centreGuide = !centreGuide },
                    label = { Text("Centre guide") }
                )
                TextButton(onClick = { onDarkChange(!dark) }) {
                    Text(if (dark) "Light theme" else "Dark theme")
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 260.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        RoundedCornerShape(28.dp)
                    )
                    .drawBehind {
                        if (centreGuide) {
                            drawLine(
                                guideColor,
                                Offset(size.width / 2f, 0f),
                                Offset(size.width / 2f, size.height),
                                1.dp.toPx()
                            )
                        }
                    }
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                DemoContent(
                    demo.id,
                    spinnerStep,
                    spinnerWavy,
                    spinnerDeterminate,
                    customSuccessIcon
                )
            }
            if (demo.id == "spinner") {
                Text("Spinner options", style = MaterialTheme.typography.titleMedium)
                Text("Appearance", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(!spinnerWavy, { spinnerWavy = false }, label = { Text("Standard") })
                    FilterChip(spinnerWavy, { spinnerWavy = true }, label = { Text("Wavy") })
                }
                Text("Progress mode", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        spinnerDeterminate,
                        { spinnerDeterminate = true },
                        label = { Text("Determinate") }
                    )
                    FilterChip(
                        !spinnerDeterminate,
                        { spinnerDeterminate = false },
                        label = { Text("Indeterminate") }
                    )
                }
                FilledTonalButton(onClick = { spinnerStep++ }) { Text("Toggle completion") }
            }
            if (demo.id == "success" || demo.id == "spinner") {
                FilterChip(
                    selected = customSuccessIcon,
                    onClick = { customSuccessIcon = !customSuccessIcon },
                    label = { Text("Use a custom success icon") },
                )
            }
            Text("Use in Compose", style = MaterialTheme.typography.titleMedium)
            Text(
                apiSnippetFor(demo.id),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace
            )
            Text(
                "Replay a settled effect or interrupt it with another tap. Android's animation setting and the app motion preference remain in control.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(name = "Effect Inspector · Light", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun EffectInspectorLightPreview() {
    CatalogTheme {
        AnimatablesMotion(false) {
            EffectInspectorScreen(
                demo = Demo(
                    "async-action",
                    "Action feedback",
                    "Save, working, success and retry in one stable control.",
                    Collection.Feedback,
                    "AsyncActionButton"
                ),
                motion = true,
                onMotionChange = {},
                dark = false,
                onDarkChange = {},
                onClose = {}
            )
        }
    }
}

@Preview(name = "Effect Inspector · Dark", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun EffectInspectorDarkPreview() {
    CatalogTheme(dark = true) {
        AnimatablesMotion(false) {
            EffectInspectorScreen(
                demo = Demo(
                    "waveform",
                    "Audio waveform",
                    "Anchored energy bands respond to speech attacks and pauses.",
                    Collection.Audio,
                    "AudioWaveform"
                ),
                motion = true,
                onMotionChange = {},
                dark = true,
                onDarkChange = {},
                onClose = {}
            )
        }
    }
}
