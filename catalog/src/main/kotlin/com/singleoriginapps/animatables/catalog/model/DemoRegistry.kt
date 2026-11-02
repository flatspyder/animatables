package com.singleoriginapps.animatables.catalog.model

internal val demos = listOf(
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

internal fun apiSnippetFor(id: String): String = when (id) {
    "async-action" -> "AsyncActionButton(\n  state = saveState, onClick = ::save,\n  modifier = Modifier.width(240.dp),\n  label = \"Save\", workingLabel = \"Saving\",\n  successLabel = \"Saved\", errorLabel = \"Retry\",\n)"
    "refresh" -> "RefreshableContent(\n  hasContent = items.isNotEmpty(), loading = loading,\n  error = error, onRetry = ::refresh,\n  placeholder = { Skeleton() }, empty = { EmptyState() },\n) { Collection(items) }"
    "waveform" -> "AudioWaveform(levels = sampledLevels, active = recording,\n  label = \"Recording\")"
    "audio-icon" -> "AudioLevelIcon(level = audioLevel, active = recording,\n  label = \"Recording\")"
    "audio-ring" -> "AudioActivityRing(level = audioLevel, active = recording) {\n  IconButton(onClick = ::toggleRecording) { MicIcon() }\n}"
    "number" -> "NumberPopIn(value = formattedValue,\n  replayKey = replay)"
    "text-swap" -> "TextStateSwap(text = status)"
    "icon" -> "IconSwap(targetState = expanded) {\n  state -> IconFor(state)\n}"
    "success" -> "SuccessCheck(complete = saved,\n  replayKey = replay)"
    "spinner" -> "CompletionSpinner(\n  progress = { progress },\n  complete = complete,\n  style = CompletionSpinnerStyle.Wavy,\n)"
    "reveal" -> "TextReveal(lines = lines,\n  replayKey = replay)"
    "shimmer" -> "ShimmerText(text = status,\n  active = running, replayKey = replay)"
    "thinking" -> "ThinkingStates(text = taskStatus,\n  active = running)"
    "reasoning" -> "ReasoningStream(entries = activityLog,\n  maxVisibleEntries = 2, active = running)"
    "streaming" -> "StreamingText(text = responseSoFar,\n  streamKey = responseId)"
    "01" -> "ExpandableCard(\n  expanded = expanded,\n  onExpandedChange = { expanded = it },\n  title = \"Details\",\n) { Text(\"More to discover\") }"
    "02" -> "AnimatedStatusText(text = status)"
    "03" -> "AnimatedCountBadge(count = unread)"
    "04" -> "LoadingContent(loading = loading,\n  placeholder = { Skeleton() },\n) { Profile() }"
    "06" -> "MotionContent(targetState = selected)\n  { item -> ItemContent(item) }"
    else -> error("Unknown demo: $id")
}
