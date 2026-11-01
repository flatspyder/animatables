package com.singleoriginapps.animatables.catalog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.singleoriginapps.animatables.AnimatablesMotion
import com.singleoriginapps.animatables.AnimatedCountBadge
import com.singleoriginapps.animatables.AnimatedStatusText
import com.singleoriginapps.animatables.ExpandableCard
import com.singleoriginapps.animatables.LoadingContent
import com.singleoriginapps.animatables.MotionContent
import com.singleoriginapps.animatables.SuccessCheck
import com.singleoriginapps.animatables.samples.MaterialSymbols
import kotlinx.coroutines.delay

private enum class Collection(val title: String) {
    All("All"), Text("Text"), Feedback("Feedback"), Activity(
        "Activity"
    ),
    Layout("Layout"), Loading("Loading"), Audio("Audio")
}

private data class Demo(
    val id: String,
    val title: String,
    val note: String,
    val group: Collection,
    val api: String
)

private val demos = listOf(
    Demo(
        "async-action",
        "Action feedback",
        "Save, working, success and retry in one stable control.",
        Collection.Feedback,
        "AsyncActionButton"
    ),
    Demo(
        "refresh",
        "Refresh and recover",
        "Refresh saved places, simulate being offline, then retry without losing your selection.",
        Collection.Loading,
        "RefreshableContent"
    ),
    Demo(
        "waveform",
        "Audio waveform",
        "Anchored energy bands respond to speech attacks and pauses.",
        Collection.Audio,
        "AudioWaveform"
    ),
    Demo(
        "audio-icon",
        "Audio level icon",
        "Compact audio activity inside an existing control.",
        Collection.Audio,
        "AudioLevelIcon"
    ),
    Demo(
        "audio-ring",
        "Recording activity ring",
        "A reactive halo keeps the recording button in place.",
        Collection.Audio,
        "AudioActivityRing"
    ),
    Demo(
        "number",
        "Number pop-in",
        "Changed digits roll in stable place-value columns.",
        Collection.Text,
        "NumberPopIn"
    ),
    Demo(
        "text-swap",
        "Text states swap",
        "An outgoing blur hands over to a rising new label.",
        Collection.Text,
        "TextStateSwap"
    ),
    Demo(
        "icon",
        "Icon swap",
        "Two icons scale and resolve around a shared centre.",
        Collection.Feedback,
        "IconSwap"
    ),
    Demo(
        "success",
        "Success check",
        "A rising fade and focus resolve, followed by a drawn check.",
        Collection.Feedback,
        "SuccessCheck"
    ),
    Demo(
        "spinner",
        "Completion spinner",
        "Material progress becomes a check when the task completes.",
        Collection.Loading,
        "CompletionSpinner"
    ),
    Demo(
        "reveal",
        "Texts reveal",
        "Lines arrive in a staggered upward sweep.",
        Collection.Text,
        "TextReveal"
    ),
    Demo(
        "shimmer",
        "Shimmer text",
        "A narrow highlight travels through the letterforms.",
        Collection.Text,
        "ShimmerText"
    ),
    Demo(
        "thinking",
        "Thinking states",
        "Shimmering activity labels change with the task.",
        Collection.Activity,
        "ThinkingStates"
    ),
    Demo(
        "reasoning",
        "Reasoning stream",
        "Recent activity moves through a fading viewport.",
        Collection.Activity,
        "ReasoningStream"
    ),
    Demo(
        "streaming",
        "Streaming text",
        "New words resolve while earlier words stay still.",
        Collection.Activity,
        "StreamingText"
    ),
    Demo(
        "01",
        "Room to unfold",
        "Expansion that keeps its place.",
        Collection.Layout,
        "ExpandableCard"
    ),
    Demo(
        "02",
        "A change in status",
        "Short labels hand off without losing their place.",
        Collection.Feedback,
        "AnimatedStatusText"
    ),
    Demo(
        "03",
        "Little things add up",
        "Counts roll as the badge grows and shrinks.",
        Collection.Feedback,
        "AnimatedCountBadge"
    ),
    Demo(
        "04",
        "Worth the wait",
        "A structural placeholder, then the content.",
        Collection.Loading,
        "LoadingContent"
    ),
    Demo(
        "06",
        "One place, new content",
        "Related states share a single surface.",
        Collection.Layout,
        "MotionContent"
    ),
)

@Composable
fun AnimatablesCatalog(initialEffect: String? = null) {
    val systemDark = isSystemInDarkTheme()
    var dark by rememberSaveable { mutableStateOf(systemDark) }
    var motion by rememberSaveable { mutableStateOf(true) }
    var inspected by rememberSaveable { mutableStateOf(initialEffect) }
    LaunchedEffect(initialEffect) { if (initialEffect != null) inspected = initialEffect }
    CatalogTheme(dark) {
        AnimatablesMotion(enabled = motion) {
            val selected = demos.find { it.id == inspected }
            if (selected != null) {
                BackHandler { inspected = null }
                EffectInspector(
                    selected,
                    motion,
                    { motion = it },
                    dark,
                    { dark = it },
                    onClose = { inspected = null })
            } else {
                CatalogScreen(
                    dark,
                    { dark = it },
                    motion,
                    { motion = it },
                    onInspect = { inspected = it })
            }
        }
    }
}

@Composable
private fun CatalogScreen(
    dark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    motion: Boolean,
    onMotionChange: (Boolean) -> Unit,
    onInspect: (String) -> Unit = {}
) {
    var filter by rememberSaveable { mutableStateOf(Collection.All) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(320.dp),
            modifier = Modifier.safeDrawingPadding(),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OriginMark(Modifier.size(28.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "SINGLE ORIGIN",
                            style = MaterialTheme.typography.labelLarge,
                            letterSpacing = 2.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { onDarkChange(!dark) }) { Text(if (dark) "Light theme" else "Dark theme") }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "THE COMPOSE MOTION LIBRARY",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.6.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Motion, made tangible.",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-1.5).sp
                        ),
                        modifier = Modifier.semantics { heading() })
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Play, interrupt and replay each transition.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (motion) "Library animations on" else "Library animations off",
                                style = MaterialTheme.typography.titleMedium
                            )
                            SystemMotionLabel()
                        }
                        Switch(
                            checked = motion,
                            onCheckedChange = onMotionChange,
                            modifier = Modifier.semantics { contentDescription = "Library motion" })
                    }
                    Spacer(Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "The collection",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.semantics { heading() })
                        Text(
                            "${demos.size} motion demos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 10.dp)
                    ) {
                        items(Collection.entries.size) { i ->
                            val item = Collection.entries[i]
                            FilterChip(
                                selected = filter == item,
                                onClick = { filter = item },
                                label = { Text(item.title) })
                        }
                    }
                }
            }
            items(
                demos.filter { filter == Collection.All || it.group == filter },
                key = { it.id }) { demo -> DemoCard(demo, onInspect) }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(vertical = 16.dp)) {
                    Text("Motion with purpose.", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Theme tokens · Hoisted state · System animation scale\nPrototype 0.1 · singleoriginapps.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun OriginMark(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        rotate(45f) {
            drawRoundRect(
                color,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * .28f)
            )
        }
        drawCircle(Color.White, size.minDimension * .12f, Offset(size.width / 2, size.height / 2))
    }
}

@Composable
private fun DemoCard(demo: Demo, onInspect: (String) -> Unit) {
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
                    .padding(16.dp), contentAlignment = Alignment.Center
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
                    Text(
                        "Inspect motion"
                    )
                }
                TextButton(onClick = {
                    showApi = !showApi
                }) { Text(if (showApi) "Hide API" else "View API") }
            }
            if (showApi) {
                Text(
                    demo.api,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    effectApiExample(demo.id) ?: apiExample(demo.id),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun DemoContent(
    id: String,
    spinnerStep: Int = 0,
    spinnerWavy: Boolean = false,
    spinnerDeterminate: Boolean = true,
    customSuccessIcon: Boolean = false,
) {
    when (id) {
        "async-action" -> AsyncActionDemo()
        "refresh" -> RefreshRecoveryDemo()
        "waveform" -> AudioDemo("waveform")
        "audio-icon" -> AudioDemo("icon")
        "audio-ring" -> AudioDemo("ring")
        "01" -> ExpansionDemo()
        "02" -> StatusDemo()
        "03" -> BadgeDemo()
        "04" -> LoadingDemo()
        "06" -> ContentDemo()
        else -> MotionEffectDemo(
            id,
            spinnerStep,
            spinnerWavy,
            spinnerDeterminate,
            customSuccessIcon
        )
    }
}

@Composable
private fun EffectInspector(
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
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TextButton(onClick = onClose) {
                Icon(
                    MaterialSymbols.ArrowBack,
                    contentDescription = null, modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Back to collection")
            }
            Text(
                demo.title,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() })
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
                    modifier = Modifier.semantics { contentDescription = "Library motion" })
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
                    label = { Text("Centre guide") })
                TextButton(onClick = { onDarkChange(!dark) }) { Text(if (dark) "Light theme" else "Dark theme") }
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
                        if (centreGuide) drawLine(
                            guideColor,
                            Offset(size.width / 2f, 0f),
                            Offset(size.width / 2f, size.height),
                            1.dp.toPx()
                        )
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
                        label = { Text("Determinate") })
                    FilterChip(
                        !spinnerDeterminate,
                        { spinnerDeterminate = false },
                        label = { Text("Indeterminate") })
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
                effectApiExample(demo.id) ?: apiExample(demo.id),
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

private fun apiExample(id: String) = when (id) {
    "async-action" -> "AsyncActionButton(\n  state = saveState, onClick = ::save,\n  modifier = Modifier.width(240.dp),\n  label = \"Save\", workingLabel = \"Saving\",\n  successLabel = \"Saved\", errorLabel = \"Retry\",\n)"
    "refresh" -> "RefreshableContent(\n  hasContent = items.isNotEmpty(), loading = loading,\n  error = error, onRetry = ::refresh,\n  placeholder = { Skeleton() }, empty = { EmptyState() },\n) { Collection(items) }"
    "waveform" -> "AudioWaveform(levels = sampledLevels, active = recording,\n  label = \"Recording\")"
    "audio-icon" -> "AudioLevelIcon(level = audioLevel, active = recording,\n  label = \"Recording\")"
    "audio-ring" -> "AudioActivityRing(level = audioLevel, active = recording) {\n  IconButton(onClick = ::toggleRecording) { MicIcon() }\n}"
    "01" -> "ExpandableCard(\n  expanded = expanded,\n  onExpandedChange = { expanded = it },\n  title = \"Details\",\n) { Text(\"More to discover\") }"
    "02" -> "AnimatedStatusText(text = status)"
    "03" -> "AnimatedCountBadge(count = unread)"
    "04" -> "LoadingContent(loading = loading,\n  placeholder = { Skeleton() },\n) { Profile() }"
    "06" -> "MotionContent(targetState = selected)\n  { item -> ItemContent(item) }"
    else -> error("Unknown demo: $id")
}

@Composable
private fun ExpansionDemo() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExpandableCard(expanded, { expanded = it }, "A weekend away", Modifier.fillMaxWidth()) {
        Text(
            "Slow mornings. New places.\nA little room for possibility.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun StatusDemo() {
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
private fun BadgeDemo() {
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

@Composable
private fun LoadingDemo() {
    var loading by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(loading) {
        if (loading) {
            delay(1800); loading = false
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
                ) { Text("SO", style = MaterialTheme.typography.labelLarge) }
                Column {
                    Text("Something good", style = MaterialTheme.typography.titleMedium); Text(
                    "Made at your own pace",
                    style = MaterialTheme.typography.bodySmall
                )
                }
            }
        }
        FilledTonalButton(onClick = {
            loading = !loading
        }) { Text(if (loading) "Show content" else "Replay loading") }
    }
}

@Composable
private fun ProfileSkeleton() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .width(148.dp)
                    .height(16.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
            Box(
                Modifier
                    .width(124.dp)
                    .height(12.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
        }
    }
}

@Composable
private fun ContentDemo() {
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
                if (detail) Text(
                    "Make a little space for your next idea.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        FilledTonalButton(onClick = {
            selected = !selected
        }) { Text(if (selected) "Less detail" else "More detail") }
    }
}

@Preview(name = "Phone · Light", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun CatalogLightPreview() {
    CatalogTheme { AnimatablesMotion(false) { CatalogScreen(false, {}, true, {}) } }
}

@Preview(name = "Phone · Dark", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun CatalogDarkPreview() {
    CatalogTheme(true) { AnimatablesMotion(false) { CatalogScreen(true, {}, true, {}) } }
}

@Preview(name = "Tablet", widthDp = 1000, heightDp = 1000, showBackground = true)
@Composable
private fun CatalogTabletPreview() {
    CatalogTheme { AnimatablesMotion(false) { CatalogScreen(false, {}, true, {}) } }
}

@Preview(
    name = "Large text",
    widthDp = 412,
    heightDp = 915,
    fontScale = 1.5f,
    showBackground = true
)
@Composable
private fun CatalogLargeTextPreview() {
    CatalogTheme { AnimatablesMotion(false) { CatalogScreen(false, {}, false, {}) } }
}

@Preview(name = "RTL", widthDp = 412, heightDp = 915, locale = "ar", showBackground = true)
@Composable
private fun CatalogRtlPreview() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        CatalogTheme { AnimatablesMotion(false) { CatalogScreen(false, {}, true, {}) } }
    }
}

@Preview(name = "Component resting states", widthDp = 412, heightDp = 1000, showBackground = true)
@Composable
private fun RestingStatesPreview() {
    CatalogTheme {
        AnimatablesMotion(false) {
            Surface {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Text("Ready at rest.", style = MaterialTheme.typography.headlineLarge)
                    ExpandableCard(
                        true,
                        {},
                        "An expanded card"
                    ) { Text("This content remains readable with animations disabled.") }
                    AnimatedStatusText("All changes saved")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AnimatedCountBadge(0); AnimatedCountBadge(
                        3
                    ); AnimatedCountBadge(123)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        SuccessCheck(true, Modifier.size(32.dp))
                        SuccessCheck(true, Modifier.size(64.dp))
                    }
                    LoadingContent(
                        false,
                        placeholder = { ProfileSkeleton() }) { Text("Content has arrived") }
                    Text(
                        "Structural loading placeholder",
                        style = MaterialTheme.typography.labelMedium
                    )
                    ProfileSkeleton()
                    MotionContent(true) { Text("Related content") }
                }
            }
        }
    }
}
