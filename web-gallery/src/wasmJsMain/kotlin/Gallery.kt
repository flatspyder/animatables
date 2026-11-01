@file:OptIn(
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    kotlin.js.ExperimentalWasmJsInterop::class
)

package com.singleoriginapps.animatables.gallery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import com.singleoriginapps.animatables.AnimatablesMotion
import com.singleoriginapps.animatables.AnimatedCountBadge
import com.singleoriginapps.animatables.AnimatedStatusText
import com.singleoriginapps.animatables.ExpandableCard
import com.singleoriginapps.animatables.IconSwap
import com.singleoriginapps.animatables.LoadingContent
import com.singleoriginapps.animatables.MotionContent
import com.singleoriginapps.animatables.NumberPopIn
import com.singleoriginapps.animatables.ReasoningStream
import com.singleoriginapps.animatables.ShimmerText
import com.singleoriginapps.animatables.StreamingText
import com.singleoriginapps.animatables.CompletionSpinner
import com.singleoriginapps.animatables.CompletionSpinnerStyle
import com.singleoriginapps.animatables.SuccessCheck
import com.singleoriginapps.animatables.TextReveal
import com.singleoriginapps.animatables.TextStateSwap
import kotlinx.coroutines.delay
import com.singleoriginapps.animatables.ThinkingStates
import com.singleoriginapps.animatables.AsyncActionButton
import com.singleoriginapps.animatables.AsyncActionState
import com.singleoriginapps.animatables.RefreshableContent
import com.singleoriginapps.animatables.AudioWaveform
import com.singleoriginapps.animatables.AudioLevelIcon
import com.singleoriginapps.animatables.AudioActivityRing
import com.singleoriginapps.animatables.samples.SpeechSamples

private object GalleryState {
    var component by mutableStateOf("NumberPopIn")
    var dark by mutableStateOf(false)
    var motion by mutableStateOf(true)
    var replay by mutableStateOf(0)
    var step by mutableStateOf(0)
    var prepare by mutableStateOf(false)
    var request by mutableIntStateOf(0)
    var tone by mutableStateOf("")
    var spinnerWavy by mutableStateOf(false)
    var spinnerDeterminate by mutableStateOf(true)
    var customSuccessIcon by mutableStateOf(false)
}

private fun connect(update: (String, Boolean, Boolean, Int, Int, Boolean, Int, String, Boolean, Boolean, Boolean) -> Unit): Unit =
    js(
        """{
    window.addEventListener('message', event => {
        if (event.source !== window.parent || event.origin !== window.location.origin) return;
        const data = event.data;
        if (data?.type !== 'animatables-config') return;
        update(String(data.component), Boolean(data.dark), Boolean(data.motion), Number(data.replay), Number(data.step),
            Boolean(data.prepare), Number(data.request) || 0, String(data.tone || ''),
            Boolean(data.spinnerWavy), data.spinnerDeterminate !== false, Boolean(data.customSuccessIcon));
    });
    window.parent.postMessage({type: 'animatables-ready'}, window.location.origin);
}"""
    )

private fun reportRendered(component: String, request: Int, prepared: Boolean): Unit = js(
    """{
    document.documentElement.setAttribute('data-rendered-component', component);
    document.documentElement.setAttribute('data-rendered-request', String(request));
    if (prepared) window.parent.postMessage({type: 'animatables-prepared', component, request}, window.location.origin);
}"""
)

fun main() {
    connect { component, dark, motion, replay, step, prepare, request, tone, spinnerWavy, spinnerDeterminate, customSuccessIcon ->
        GalleryState.component = component
        GalleryState.dark = dark
        GalleryState.motion = motion
        GalleryState.replay = replay
        GalleryState.step = step
        GalleryState.prepare = prepare
        GalleryState.request = request
        GalleryState.tone = tone
        GalleryState.spinnerWavy = spinnerWavy
        GalleryState.spinnerDeterminate = spinnerDeterminate
        GalleryState.customSuccessIcon = customSuccessIcon
    }
    ComposeViewport(viewportContainerId = "playground") {
        val dark = GalleryState.dark
        val previewSurface = if (dark) Color(0xFF201B19) else when (GalleryState.tone) {
            "peach" -> Color(0xFFF7DACE)
            "lilac" -> Color(0xFFE9E0F4)
            "green" -> Color(0xFFE2EBD6)
            "yellow" -> Color(0xFFF6E9BA)
            else -> Color(0xFFFFF8F4)
        }
        val light = lightColorScheme(
            primary = Color(0xFF924731), primaryContainer = Color(0xFFFFDBCC),
            onPrimaryContainer = Color(0xFF47281B), surface = previewSurface,
            onSurface = Color(0xFF2F2421), surfaceContainer = Color(0xFFF1E3D9),
        )
        val night = darkColorScheme(
            primary = Color(0xFFFFB59A), primaryContainer = Color(0xFF5B3527),
            onPrimaryContainer = Color(0xFFFFDBCC), surface = previewSurface,
            onSurface = Color(0xFFF0E2DB), surfaceContainer = Color(0xFF302723),
        )
        MaterialExpressiveTheme(colorScheme = if (dark) night else light) {
            val prepared = remember { mutableSetOf<String>() }
            var warming by remember { mutableStateOf(false) }
            var variant by remember { mutableIntStateOf(0) }
            val preparationKey =
                "${GalleryState.component}:${GalleryState.dark}:${GalleryState.tone}:" +
                    "${GalleryState.spinnerWavy}:${GalleryState.spinnerDeterminate}:${GalleryState.customSuccessIcon}"
            AnimatablesMotion(enabled = GalleryState.motion && !warming) {
                val component = GalleryState.component
                val request = GalleryState.request
                LaunchedEffect(preparationKey, request) {
                    warming = false
                    if (GalleryState.prepare && preparationKey !in prepared) {
                        // Draw both incoming/outgoing content before starting the visible clock.
                        // Keep the canvas mounted and covered by its HTML resting-state image.
                        warming = true
                        repeat(4) { state ->
                            variant = state
                            withFrameNanos { }
                            withFrameNanos { }
                        }
                        warming = false
                        prepared.add(preparationKey)
                    }
                    withFrameNanos { }
                    withFrameNanos { }
                    reportRendered(component, request, GalleryState.prepare)
                }
                Surface(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Exercise Skia's filtered offscreen layer during preparation as well as
                        // text shaping. Alpha-zero/display:none content would skip useful work.
                        val warmLayer = if (warming && variant < 2) Modifier.graphicsLayer {
                            val radius = if (variant == 0) 2.dp.toPx() else 10.dp.toPx()
                            renderEffect = BlurEffect(radius, radius, TileMode.Decal)
                        } else Modifier
                        Box(warmLayer) {
                            key(component, if (warming) variant else -1) {
                                Demo(
                                    component,
                                    if (warming) variant else GalleryState.step,
                                    GalleryState.replay,
                                    preparing = warming,
                                    spinnerWavy = GalleryState.spinnerWavy,
                                    spinnerDeterminate = GalleryState.spinnerDeterminate,
                                    customSuccessIcon = GalleryState.customSuccessIcon,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Demo(
    component: String,
    step: Int,
    replay: Int,
    preparing: Boolean = false,
    spinnerWavy: Boolean = false,
    spinnerDeterminate: Boolean = true,
    customSuccessIcon: Boolean = false,
) {
    val alternate = step % 2 != 0
    val labels = listOf("Ready when you are", "A little more expressive", "Made to feel considered")
    val label = labels[step % labels.size]
    val body =
        "Small details make an interface feel considered. Keep settled words still, preserve the reader’s place, and let new ideas arrive gently."
    Column(
        Modifier.widthIn(max = 460.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        when (component) {
            "AsyncActionButton" -> {
                var localStep by remember { mutableIntStateOf(step) }
                LaunchedEffect(step) { localStep = step }
                val state = AsyncActionState.entries[localStep % 4]
                AsyncActionButton(state, onClick = { localStep++ }, label = "Save changes", modifier = Modifier.width(240.dp),
                    workingLabel = "Saving", successLabel = "Changes saved", errorLabel = "Retry save",
                    progress = { .45f })
                Text(if (state == AsyncActionState.Error) "Couldn’t save. Your changes are still here."
                    else "Save · working · success · retry", style = MaterialTheme.typography.bodyMedium)
            }
            "RefreshableContent" -> RefreshRecoveryDemo(step)
            "AudioWaveform" -> AudioSampleDemo("waveform", step, preparing)
            "AudioLevelIcon" -> AudioSampleDemo("icon", step, preparing)
            "AudioActivityRing" -> AudioSampleDemo("ring", step, preparing)
            "NumberPopIn" -> {
                Text("TOTAL BALANCE", style = MaterialTheme.typography.labelMedium)
                NumberPopIn(
                    listOf("$1,249.00", "$1,250.50", "$999.99", "$10,000.00")[step % 4],
                    replayKey = replay,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.displaySmall
                )
                Text(
                    "Place-value reels · tabular digits",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            "TextStateSwap" -> TextStateSwap(
                label,
                replayKey = replay,
                style = MaterialTheme.typography.headlineSmall
            )

            "IconSwap" -> IconSwap(alternate, replayKey = replay) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    SampleIcon(saved = it, modifier = Modifier.padding(24.dp).size(42.dp))
                }
            }

            "SuccessCheck" -> {
                SuccessCheck(complete = !alternate, replayKey = replay,
                    successIcon = if (customSuccessIcon) ({ CustomSuccessIcon() }) else null)
                Text(
                    if (alternate) "Ready for your next idea" else "All changes saved",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            "CompletionSpinner" -> SpinnerDemo(step, spinnerWavy, spinnerDeterminate, customSuccessIcon)

            "TextReveal" -> TextReveal(
                listOf(
                    "A thoughtful entrance.",
                    "A readable resting state.",
                    "Motion with a purpose."
                ),
                replayKey = replay, style = MaterialTheme.typography.headlineSmall
            )

            "ShimmerText" -> ShimmerText(
                "A moment of clarity",
                replayKey = replay,
                style = MaterialTheme.typography.headlineMedium
            )

            "ThinkingStates" -> ThinkingStates(
                listOf(
                    "Gathering your ideas",
                    "Finding the connections",
                    "Ready to share"
                )[step % 3], replayKey = replay
            )

            "StreamingText" -> key(replay) {
                StreamingText(
                    body.split(" ").take(listOf(7, 15, 23, 29)[step % 4]).joinToString(" "),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            "ReasoningStream" -> ReasoningStream(
                listOf(
                    "Opened the sample document",
                    "Found 12 matching sections",
                    "Grouped related topics",
                    "Prepared the summary"
                )
                    .take((step % 4) + 1),
                modifier = Modifier.fillMaxWidth(),
                maxVisibleEntries = 2,
                style = MaterialTheme.typography.bodyLarge
            )

            "ExpandableCard" -> {
                var expanded by remember { mutableStateOf(preparing && alternate) }
                var appliedStep by remember { mutableIntStateOf(step) }
                LaunchedEffect(step) {
                    if (appliedStep != step) {
                        expanded = !expanded
                        appliedStep = step
                    }
                }
                ExpandableCard(expanded, { expanded = it }, "Delivery details") {
                    Text("Arriving tomorrow", fontWeight = FontWeight.Medium)
                    Text("Your order is on its way. Tracking updates appear here.")
                }
            }

            "MotionContent" -> MotionContent(targetState = alternate) {
                Text(
                    if (it) "Your next chapter starts here" else "Make room for a new idea",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            "AnimatedStatusText" -> AnimatedStatusText(if (alternate) "Changes saved" else "Ready to edit")
            "AnimatedCountBadge" -> {
                Text("YOUR INBOX", style = MaterialTheme.typography.labelLarge)
                AnimatedCountBadge(count = listOf(3, 12, 99, 120)[step % 4])
            }

            "LoadingContent" -> LoadingContent(
                loading = alternate,
                placeholder = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            Modifier.fillMaxWidth().height(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {}
                        Text("Loading your collection…")
                    }
                }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your collection is ready", style = MaterialTheme.typography.titleLarge)
                    Text("Everything in its right place.")
                }
            }
        }
    }
}

/** Code-native vectors avoid a fallback-font lookup when the icon first changes. */
@Composable
private fun SampleIcon(saved: Boolean, modifier: Modifier = Modifier) {
    val color = LocalContentColor.current
    Canvas(modifier.semantics { contentDescription = if (saved) "Saved" else "Add" }) {
        val width = 2.5.dp.toPx()
        if (saved) {
            drawLine(
                color, Offset(size.width * .22f, size.height * .5f),
                Offset(size.width * .43f, size.height * .7f), width, StrokeCap.Round
            )
            drawLine(
                color, Offset(size.width * .43f, size.height * .7f),
                Offset(size.width * .8f, size.height * .3f), width, StrokeCap.Round
            )
        } else {
            drawLine(
                color, Offset(size.width * .5f, size.height * .2f),
                Offset(size.width * .5f, size.height * .8f), width, StrokeCap.Round
            )
            drawLine(
                color, Offset(size.width * .2f, size.height * .5f),
                Offset(size.width * .8f, size.height * .5f), width, StrokeCap.Round
            )
        }
    }
}

/** The HTML detail page owns options and playback; the canvas only renders task state. */
@Composable
private fun SpinnerDemo(step: Int, wavy: Boolean, determinate: Boolean, customSuccessIcon: Boolean) {
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
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/** A caller-supplied icon example; the component provides its size and content color. */
@Composable
private fun CustomSuccessIcon() {
    val color = LocalContentColor.current
    Canvas(Modifier.fillMaxSize()) {
        val icon = Path().apply {
            moveTo(size.width * .5f, 0f)
            lineTo(size.width, size.height * .5f)
            lineTo(size.width * .5f, size.height)
            lineTo(0f, size.height * .5f)
            close()
        }
        drawPath(icon, color)
    }
}


/** One retained collection. Refresh and offline/retry never switch to another dataset. */
@Composable
private fun RefreshRecoveryDemo(step: Int) {
    var request by remember { mutableIntStateOf(0) }
    var appliedStep by remember { mutableIntStateOf(step) }
    var loading by remember { mutableStateOf(false) }
    var offline by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var updated by remember { mutableStateOf(false) }
    LaunchedEffect(step) {
        if (appliedStep != step) { appliedStep = step; offline = false; request++ }
    }
    LaunchedEffect(request) {
        if (request > 0) {
            loading = true
            delay(1400)
            if (offline) error = "You’re offline. Your saved places are still available."
            else { error = null; updated = true }
            loading = false
        }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Pin Kyoto, then try refreshing while offline.", style = MaterialTheme.typography.bodySmall)
        RefreshableContent(hasContent = true, loading = loading, error = error,
            onRetry = { offline = false; request++ }, refreshingLabel = "Checking for updates…",
            placeholder = {}, empty = {}) {
            var pinned by remember { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Saved places", style = MaterialTheme.typography.titleLarge)
                Text("Kyoto · Lisbon · Copenhagen")
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { pinned = !pinned }) { Text(if (pinned) "Kyoto pinned" else "Pin Kyoto") }
                    TextButton(onClick = { offline = true; request++ }, enabled = !loading) { Text("Try offline") }
                }
                Text(if (updated) "Updated just now" else "Saved earlier", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Sample generation belongs to the demo, not the audio components. */
@Composable
private fun AudioSampleDemo(kind: String, step: Int, preparing: Boolean) {
    var playing by remember { mutableStateOf(true) }
    var active by remember { mutableStateOf(true) }
    var sample by remember { mutableIntStateOf(SpeechSamples.InitialFrame) }
    var appliedStep by remember { mutableIntStateOf(step) }
    LaunchedEffect(step) {
        if (appliedStep != step) { appliedStep = step; playing = true; active = true }
    }
    LaunchedEffect(playing, active, preparing, GalleryState.motion) {
        while (playing && active && !preparing && GalleryState.motion) { delay(SpeechSamples.FrameMillis); sample++ }
    }
    val level = SpeechSamples.level(sample)
    val levels = SpeechSamples.waveform(sample)
    val label = if (!active) "Sample muted" else if (playing) "Simulated voice" else "Sample paused"
    when (kind) {
        "waveform" -> AudioWaveform(levels, active, Modifier.fillMaxWidth().height(88.dp), label = label)
        "icon" -> AudioLevelIcon(level, active, label = label, color = MaterialTheme.colorScheme.primary)
        else -> AudioActivityRing(level, active) {
            FilledIconButton(onClick = { active = !active }, modifier = Modifier.size(56.dp)
                .semantics { contentDescription = if (active) "Mute sample" else "Unmute sample" }) {
                val color = LocalContentColor.current
                Canvas(Modifier.size(18.dp)) {
                    if (active) drawRoundRect(color, cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
                    else drawCircle(color)
                }
            }
        }
    }
    Text(label, style = MaterialTheme.typography.titleMedium)
    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { playing = !playing }) { Text(if (playing) "Pause sample" else "Play sample") }
        TextButton(onClick = { active = !active }) { Text(if (active) "Mute" else "Unmute") }
    }
}
