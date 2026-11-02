package com.singleoriginapps.animatables.catalog.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.singleoriginapps.animatables.AnimatablesMotion
import com.singleoriginapps.animatables.AudioActivityRing
import com.singleoriginapps.animatables.AudioLevelIcon
import com.singleoriginapps.animatables.AudioWaveform
import com.singleoriginapps.animatables.catalog.CatalogTheme
import com.singleoriginapps.animatables.samples.MaterialSymbols
import com.singleoriginapps.animatables.samples.SpeechSamples
import com.singleoriginapps.animatables.samples.SpeechWaveformSamples
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun AudioDemo(kind: String) {
    var active by rememberSaveable { mutableStateOf(true) }
    var playing by rememberSaveable { mutableStateOf(true) }
    var sample by rememberSaveable {
        mutableIntStateOf(
            if (kind == "waveform") SpeechWaveformSamples.InitialFrame else SpeechSamples.InitialFrame
        )
    }
    val inspecting = LocalInspectionMode.current
    LaunchedEffect(playing, active, inspecting) {
        while (playing && active && !inspecting) {
            delay(
                (if (kind == "waveform") SpeechWaveformSamples.FrameMillis else SpeechSamples.FrameMillis).milliseconds
            )
            sample++
        }
    }
    val level = SpeechSamples.level(sample)
    val levels = SpeechWaveformSamples.levels(sample)
    val activityLabel = if (!active) "Sample muted" else if (playing) {
        if (kind == "waveform") "Speech sample" else "Simulated voice"
    } else "Sample paused"

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (kind) {
            "waveform" -> AudioWaveform(
                levels,
                active,
                Modifier
                    .fillMaxWidth()
                    .height(88.dp),
                label = activityLabel
            )

            "icon" -> AudioLevelIcon(
                level,
                active,
                color = MaterialTheme.colorScheme.primary,
                label = activityLabel
            )

            else -> AudioActivityRing(level, active) {
                FilledIconButton(
                    onClick = { active = !active },
                    modifier = Modifier
                        .size(56.dp)
                        .semantics {
                            contentDescription = if (active) "Mute sample" else "Unmute sample"
                        }
                ) {
                    RecordingSymbol(active)
                }
            }
        }
        Text(activityLabel, style = MaterialTheme.typography.titleMedium)
        Text(
            if (kind == "waveform") "Measured speech · visual only" else "Simulated audio",
            style = MaterialTheme.typography.bodySmall
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = {
                playing = !playing
                if (playing) active = true
            }) {
                Text(if (playing) "Pause sample" else "Play sample")
            }
            TextButton(onClick = { active = !active }) {
                Text(if (active) "Mute" else "Unmute")
            }
        }
    }
}

@Composable
private fun RecordingSymbol(recording: Boolean) {
    Icon(
        if (recording) MaterialSymbols.Mic else MaterialSymbols.MicOff,
        contentDescription = null,
        modifier = Modifier.size(24.dp)
    )
}

@Preview(name = "Audio presentations", widthDp = 412, heightDp = 750, showBackground = true)
@Composable
private fun AudioPresentationsPreview() {
    CatalogTheme(false) {
        AnimatablesMotion(false) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                AudioDemo("waveform")
                AudioDemo("icon")
                AudioDemo("ring")
            }
        }
    }
}

@Preview(
    name = "Audio large text RTL",
    widthDp = 412,
    heightDp = 1000,
    fontScale = 1.5f,
    locale = "ar",
    showBackground = true
)
@Composable
private fun AudioPresentationsRtlPreview() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AudioPresentationsPreview()
    }
}
