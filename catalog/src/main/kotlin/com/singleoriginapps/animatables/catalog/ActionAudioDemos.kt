package com.singleoriginapps.animatables.catalog

import androidx.compose.material3.Icon
import com.singleoriginapps.animatables.samples.MaterialSymbols
import com.singleoriginapps.animatables.samples.RefreshSample
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalInspectionMode
import com.singleoriginapps.animatables.AnimatablesMotion
import com.singleoriginapps.animatables.AsyncActionButton
import com.singleoriginapps.animatables.AsyncActionState
import com.singleoriginapps.animatables.AudioActivityRing
import com.singleoriginapps.animatables.AudioLevelIcon
import com.singleoriginapps.animatables.AudioWaveform
import kotlinx.coroutines.delay
import com.singleoriginapps.animatables.samples.SpeechWaveformSamples
import com.singleoriginapps.animatables.samples.SpeechSamples

@Composable
internal fun AsyncActionDemo() {
    var state by rememberSaveable { mutableStateOf(AsyncActionState.Idle) }
    var fail by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (state == AsyncActionState.Working) {
            delay(1400)
            state = if (fail) AsyncActionState.Error else AsyncActionState.Success
        }
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AsyncActionButton(state, onClick = {
            state = if (state == AsyncActionState.Working) AsyncActionState.Idle else AsyncActionState.Working
        }, label = "Save changes", modifier = Modifier.width(240.dp), workingLabel = "Saving · cancel", successLabel = "Changes saved",
            errorLabel = "Retry save")
        if (state == AsyncActionState.Error) Text("Couldn’t save. Your changes are still here.",
            color = MaterialTheme.colorScheme.error)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(fail, { fail = it })
            Text("Simulate failure", style = MaterialTheme.typography.labelMedium)
        }
        TextButton(onClick = { state = AsyncActionState.Idle }) { Text("Reset") }
    }
}

@Composable
internal fun RefreshRecoveryDemo() = RefreshSample()

@Composable
internal fun AudioDemo(kind: String) {
    var active by rememberSaveable { mutableStateOf(true) }
    var playing by rememberSaveable { mutableStateOf(true) }
    var sample by rememberSaveable { mutableIntStateOf(if (kind == "waveform") SpeechWaveformSamples.InitialFrame else SpeechSamples.InitialFrame) }
    val inspecting = LocalInspectionMode.current
    LaunchedEffect(playing, active, inspecting) {
        while (playing && active && !inspecting) { delay(if (kind == "waveform") SpeechWaveformSamples.FrameMillis else SpeechSamples.FrameMillis); sample++ }
    }
    val level = SpeechSamples.level(sample)
    val levels = SpeechWaveformSamples.levels(sample)
    val activityLabel = if (!active) "Sample muted" else if (playing) { if (kind == "waveform") "Speech sample" else "Simulated voice" } else "Sample paused"
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when (kind) {
            "waveform" -> AudioWaveform(levels, active, Modifier.fillMaxWidth().height(88.dp),
                label = activityLabel)
            "icon" -> AudioLevelIcon(level, active, color = MaterialTheme.colorScheme.primary,
                label = activityLabel)
            else -> AudioActivityRing(level, active) {
                FilledIconButton(onClick = { active = !active }, modifier = Modifier.size(56.dp)
                    .semantics { contentDescription = if (active) "Mute sample" else "Unmute sample" }) {
                    RecordingSymbol(active)
                }
            }
        }
        Text(activityLabel, style = MaterialTheme.typography.titleMedium)
        Text(if (kind == "waveform") "Measured speech · visual only" else "Simulated audio", style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { playing = !playing; if (playing) active = true }) {
                Text(if (playing) "Pause sample" else "Play sample")
            }
            TextButton(onClick = { active = !active }) { Text(if (active) "Mute" else "Unmute") }
        }
    }
}

@Composable
private fun RecordingSymbol(recording: Boolean) {
    Icon(if (recording) MaterialSymbols.Mic else MaterialSymbols.MicOff,
        contentDescription = null, modifier = Modifier.size(24.dp))
}

@Preview(name = "Actions and recovery", widthDp = 412, heightDp = 660, showBackground = true)
@Composable
private fun ActionRecoveryPreview() {
    CatalogTheme(false) { AnimatablesMotion(false) {
        Column(Modifier.widthIn(max = 380.dp).padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            AsyncActionDemo()
            RefreshRecoveryDemo()
        }
    } }
}

@Preview(name = "Audio presentations", widthDp = 412, heightDp = 750, showBackground = true)
@Composable
private fun AudioPresentationsPreview() {
    CatalogTheme(false) { AnimatablesMotion(false) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            AudioDemo("waveform")
            AudioDemo("icon")
            AudioDemo("ring")
        }
    } }
}

@Preview(name = "Actions large text RTL", widthDp = 412, heightDp = 850, fontScale = 1.5f, locale = "ar", showBackground = true)
@Composable
private fun ActionRecoveryRtlPreview() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { ActionRecoveryPreview() }
}

@Preview(name = "Audio large text RTL", widthDp = 412, heightDp = 1000, fontScale = 1.5f, locale = "ar", showBackground = true)
@Composable
private fun AudioPresentationsRtlPreview() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { AudioPresentationsPreview() }
}
