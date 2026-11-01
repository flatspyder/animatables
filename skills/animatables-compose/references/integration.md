# Integration

This reference accompanies `com.singleoriginapps:animatables:0.1.0-SNAPSHOT`. It is a development snapshot, not a released Maven Central coordinate.

## Dependency setup

Inside the library checkout, build the project-local Maven repository:

```sh
./gradlew :animatables:publishAllPublicationsToProjectLocalRepository
```

In a consuming app's `settings.gradle.kts`, add that specific directory to the existing repositories block. Replace the example absolute path with the real checkout path. Keep `google()` and `mavenCentral()` for transitive AndroidX dependencies.

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("/absolute/path/to/Animatables/build/repo")
            content { includeGroup("com.singleoriginapps") }
        }
    }
}
```

Then add:

```kotlin
implementation("com.singleoriginapps:animatables:0.1.0-SNAPSHOT")
```

The snapshot uses Compose 1.12.1 and Material 3 1.5.0-beta02 with minimum Android API 24 and compile SDK 37.0. Its own build uses Java 17 bytecode. Material 3 is deliberately a beta dependency to access the requested Expressive APIs; this is a compatibility constraint for consumers. Resolve dependency version conflicts in the host app deliberately rather than forcing an older Material artifact. The library checkout's version catalog is authoritative for the exact snapshot being consumed.

In a multi-module checkout, a Gradle project dependency can reference `:animatables` directly. Keep repository configuration scoped to the consuming build.

## Component map

All functions are in `com.singleoriginapps.animatables`. Check source for optional parameters and their current defaults.

| Need | API | State belongs to |
| --- | --- | --- |
| Async action with working/success/retry feedback | `AsyncActionButton(state, onClick, label)` | Caller; `AsyncActionState`, work, errors, enabled/cancellation policy |
| Initial loading, retained refresh and error recovery | `RefreshableContent(hasContent, loading, placeholder, empty) { ... }` | Caller; retained data, error and retry operation |
| Large audio waveform | `AudioWaveform(levels, active)` | Caller; immutable normalized samples, capture and stop/pause |
| Compact audio activity | `AudioLevelIcon(level, active)` | Caller; normalized level, actual state and localized label |
| Activity around a recording button | `AudioActivityRing(level, active) { ... }` | Caller; button, recording semantics and interaction |
| App preference to disable optional motion | `AnimatablesMotion(enabled) { ... }` | App settings/state holder |
| Small in-place content swap with size reflow | `MotionContent(targetState) { state -> ... }` | Caller; render the supplied lambda value |
| Related detail revealed from a titled card | `ExpandableCard(expanded, onExpandedChange, title) { ... }` | Caller; keep expansion saveable if user-owned |
| Short status string replacement | `AnimatedStatusText(text)` | Caller; do not stream a new value every frame |
| Actual notification/item count | `AnimatedCountBadge(count)` | Caller; supply the actual count, not a capped display string |
| Placeholder to loaded content | `LoadingContent(loading, placeholder = { ... }) { ... }` | Caller; loading state, data, error/retry |
| Material progress becoming a completion check | `CompletionSpinner(progress = { fraction }, style = CompletionSpinnerStyle.Wavy)` | Caller; progress/completion, cancellation and long-running pause/hide |
| Foreground completion feedback | `SuccessCheck(complete, modifier = Modifier.size(32.dp))` | Caller; real task state and deliberate replay identity |
| Formatted metric with place-value continuity | `NumberPopIn(value, decimalSeparator = '.', anchor = NumberFlowAnchor.Center)` | Caller; formatted value, locale separator, deliberate replay identity |
| Deliberate text handoff | `TextStateSwap(text, replayKey = eventId)` | Caller; real label state |
| Icon swap inside a labeled control | `IconSwap(targetState) { state -> ... }` | Caller; keep button semantics/hit target on host |
| Short grouped introduction | `TextReveal(lines, replayKey = eventId)` | Caller; lines wrap naturally |
| Finite text highlight | `ShimmerText(text, active = true)` | Caller; plays two two-second sweeps, then rests |
| Truthful activity state | `ThinkingStates(text, active = working)` | Caller; state changes only with actual work |
| Appended response text | `StreamingText(text, streamKey = responseId)` | Caller; actual text, pause/stop, stream identity |
| Compact activity transcript | `ReasoningStream(entries, maxVisibleEntries = 2)` | Caller; actual entries and expanded/pause controls |

For navigation, anchored menus, tooltips, switches, checkboxes, tabs, snackbars, and modal sheets, begin with the native Material/Navigation components. Neither `MotionContent` nor `ExpandableCard` implements navigation lifecycle or predictive back.

## Actions, refresh and audio

```kotlin
AsyncActionButton(
    state = saveState,
    onClick = ::saveOrCancel,
    modifier = Modifier.width(240.dp),
    label = "Save changes",
    workingLabel = "Saving · cancel",
    successLabel = "Changes saved",
    errorLabel = "Retry save",
    progress = { saveProgress }, // Omit for indeterminate work.
)

RefreshableContent(
    hasContent = collection != null,
    loading = refreshing,
    error = refreshError,
    onRetry = ::refresh,
    placeholder = { CollectionSkeleton() },
    empty = { Text("No saved items") },
) {
    CollectionContent(collection!!)
}

AudioWaveform(levels = sampledLevels, active = recording, label = "Recording")
AudioLevelIcon(level = audioLevel, active = recording, label = "Recording")
AudioActivityRing(level = audioLevel, active = recording) {
    FilledIconButton(onClick = ::toggleRecording) {
        Icon(micPainter, contentDescription = if (recording) "Stop recording" else "Start recording")
    }
}
```

These are host integration examples: the state holders, callbacks, painter and collection/skeleton
composables come from the app. Import all new APIs and `AsyncActionState` from
`com.singleoriginapps.animatables`; `FilledIconButton`, `Icon` and `Text` are Material 3.

`AsyncActionButton` keeps a single native button with a fixed leading 20 dp indicator slot and an
equal trailing slot. Icons crossfade in place; labels stay centered on the whole button and enter
with an 8 dp vertical rise plus fade. Text length never moves the icon.
Set width with a modifier to fit the longest localized label (the catalog uses 240 dp); without an
explicit width it wraps current content. It does not measure hidden copies of every label. Long
labels wrap. `enabled` remains caller-owned: the motion never disables
input, and the caller can disable duplicate submission or turn a working click into cancellation.
Set `AsyncActionState.Success` only when the operation succeeds; normalized progress at 1 is still
Working. Invalid progress is clamped (NaN becomes zero). Success and error do not auto-reset. Put a
useful failure explanation outside the retry button. Localize all four labels.

Keep previous data while refreshing. As long as `hasContent` stays true, refresh/error changes
retain the content subtree and do not reset a text field, scroll state or focus. Without content,
the existing 150 ms placeholder delay and 350 ms minimum hold apply; after loading, an error with
optional retry or the empty slot appears. During retry, loading hides the previous error. Clear
the caller's error after success. Refresh indicators settle to static partial tracks with motion
disabled. There is no data cache, request, automatic retry or navigation inside the container.

Audio levels are normalized 0..1; NaN is zero, infinities and other out-of-range values are clamped.
`AudioWaveform` linearly resamples ordered input to `barCount` (default 32, allowed 3..96). Empty
input is silence. It defaults to 240 × 72 dp; `AudioLevelIcon` defaults to 24 dp. Inactive waveform
and icon states are baselines. With reduced motion the active waveform/profile and ring stay fixed
even if the host continues providing samples. State labels remain accessible without per-sample
live announcements. These visuals contain no audio capture, microphone permission or sample clock.
For a decorative icon in an already labelled control, pass `label = null`. `AudioActivityRing`
adds a constant 12 dp on each side of the supplied circular button and handles no input/semantics.
Retain a native button with a localized action/state label and a stop/pause control in the host.

## Minimal usage

Install the theme once at the app boundary. If the app already has an Expressive theme, use it. This example intentionally keeps the domain state outside the components:

```kotlin
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.singleoriginapps.animatables.AnimatablesMotion
import com.singleoriginapps.animatables.AnimatedCountBadge
import com.singleoriginapps.animatables.AnimatedStatusText
import com.singleoriginapps.animatables.ExpandableCard

@Composable
fun InboxSummary(unreadCount: Int, status: String, motionEnabled: Boolean) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    MaterialExpressiveTheme {
        AnimatablesMotion(enabled = motionEnabled) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AnimatedStatusText(text = status)
                AnimatedCountBadge(count = unreadCount)
                ExpandableCard(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    title = "Inbox details",
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Unread messages remain available when animations are off.")
                }
            }
        }
    }
}
```

In a localized app, pass translated labels/state descriptions where the API exposes them and localize any surrounding user-facing strings. Do not assume an English default is sufficient for public app use.

## Choosing a state transition

Use stable immutable target values and render the parameter passed by `MotionContent`. Capturing the outer `targetState` inside its lambda makes outgoing and incoming content render the same new value and destroys the transition's identity.

```kotlin
MotionContent(targetState = selectedSummary) { summary ->
    Text(summary.title)
}
```

A state swap is suitable for a small result, label, or icon. Full destinations need the app's navigation machinery. A changing counter should remain readable and should not perform a celebratory bounce for every refresh.


## Explicit reveal and streaming effects

`TextReveal` treats each item in `lines` as a separate wrapping block. Its 500ms rise/fade/focus uses 12dp travel, 3dp blur, and 40ms line spacing capped at a 300ms stagger window. Keep ordinary revisits still unless the user explicitly requests replay.

`TextStateSwap` centers labels by default and hands off through 4dp travel and 2dp soft focus: the 150ms exit completes before the 150ms entrance begins. An explicitly supplied `TextStyle.textAlign` is retained. `AnimatedStatusText` uses this handoff as its convenience API. `ThinkingStates` centers its activity label, uses 8dp travel and 150ms phases with a 50ms incoming offset, and combines the transition with a bounded highlight. Neither component generates status messages.

`IconSwap` uses a centered 250ms scale/focus handoff with a 2dp blur accent; preserve the host button's semantics and target. `SuccessCheck` stays full size and arrives through a 500ms upward fade with 40dp travel, 80° rotation, and 10dp soft focus. Its 500ms check stroke begins after an 80ms lead-in. Rapid reversal retargets the visible mark continuously rather than restarting its spatial position. Use `Modifier.size(...)` to choose a compact or larger presentation; the default size is 48dp. Blur is an API 31+ accent; earlier Android retains the other transformations and a sharp resting state.

Android soft-focus layers reserve temporary drawing outsets during blur and clear them at rest without changing measured layout. Skia uses its normal filtered layer path: expanded outsets in the current backend can cache the initial opacity and leave animated content invisible. Both adapters clear blur at rest; inspect repeated playback on each shipped target.

`CompletionSpinner` keeps the native Material circular indicator while pending (`CompletionSpinnerStyle.Standard` or `.Wavy`). Supply `progress = { fraction }` for determinate loading, or omit it for indeterminate loading. Either 100% or `complete = true` uses the same shared success entrance as `SuccessCheck`, with no shape morph. The initial completed spinner stays still; resetting both inputs returns to loading quietly. State is not latched. NaN progress is zero and out-of-range values are clamped. The completion fill/content use theme `primaryContainer`/`onPrimaryContainer`; customize `completeColor` and `completeContentColor` together for the host app’s meaning. Localize `label`/`completeLabel`. Motion disabled shows the target check immediately and indeterminate loading as a static partial indicator, preserving indeterminate semantics. Keep timers, cancellation and any long-running pause/hide control in the host.

Both `SuccessCheck` and `CompletionSpinner` accept an optional `successIcon: (@Composable () -> Unit)?`. Omitting it retains the delayed drawn check. Custom content is centered in half the indicator bounds, inherits `LocalContentColor`, and fades in after the same delay without changing its shape. The parent owns accessible outcome labels, so a decorative `Icon` should use `contentDescription = null`. Size the whole component using `Modifier.size(...)`; a compact success uses `Modifier.size(32.dp)`.

```kotlin
CompletionSpinner(
    progress = { uploadFraction },
    complete = uploadSucceeded,
    style = CompletionSpinnerStyle.Wavy,
    label = "Uploading",
    completeLabel = "Upload complete",
)
```

For a custom icon, supply the host’s `Painter` as `successPainter`:

```kotlin
SuccessCheck(
    complete = uploadSucceeded,
    modifier = Modifier.size(32.dp),
    label = "Upload complete",
    successIcon = {
        Icon(painter = successPainter, contentDescription = null)
    },
)
```

`Icon` is from Material 3; `size` and `dp` are the standard Compose layout/unit imports. The same slot works on `CompletionSpinner`.

`ShimmerText` performs two two-second neutral luminance sweeps at normal scale, then leaves ordinary static text. The moving peak uses the original glyph color. Shaded regions move that color toward `MaterialTheme.colorScheme.surface`; when the supplied color/alpha already exceeds 4.5:1 calculated contrast against that surface, shading is bounded to retain at least 4.5:1. A supplied color already at or below 4.5:1 is preserved rather than faded further. `SrcIn` retains caller glyph alpha. The contrast calculation is specific to the theme surface: verify the actual background when the component is placed elsewhere. `ThinkingStates` uses the same highlight. `active = false` stops it, and Android/app-disabled animation leaves a readable label. Do not change replay keys repeatedly to create a perpetual loop.

### Number flow

`NumberPopIn` starts with the exact value at rest. Subsequent changes roll changed digits in stable place-value columns, and unchanged places remain still. Repeated input retargets the current reels; replay requests a deliberate one-step roll/fade. The supplied string is not converted to floating point or reformatted. Scientific notation is displayed rather than interpreted.

Its optional parameters after `style` are `decimalSeparator: Char = '.'` and `anchor: NumberFlowAnchor = NumberFlowAnchor.Center`. Pass the locale's actual decimal separator; grouping characters alone cannot establish it. `Center` keeps the complete run centered. `Decimal` anchors the fraction boundary (or units-column edge for integers), keeping unchanged place columns at exact horizontal positions as the number gains or loses digits. Give aligned numeric columns an adequate explicit width; values clip rather than automatically shrinking to fit.

```kotlin
NumberPopIn(
    value = "€ 1.234,50",
    modifier = Modifier.width(220.dp),
    decimalSeparator = ',',
    anchor = NumberFlowAnchor.Decimal,
)
```

`NumberFlowAnchor` is in `com.singleoriginapps.animatables`; `width` is the standard Compose layout modifier. Use number flow for readable value changes, not a repeated celebratory entrance on routine refreshes.

### Streaming paragraphs and activity

`StreamingText` accepts the complete current response and displays an ordered sequence of word opacity fades: 350ms per word, starts paced 60ms apart, with no travel or blur. It draws one native measured paragraph through the same word-opacity path throughout. There is no full native-text fallback or base/overlay handoff during append, layout, or settling. Whitespace, native wrapping, RTL shaping, and paragraph breaks remain part of that shared layout.

Appended batches join the existing queue; settled words do not restart and extending the current partial word preserves opacity. A non-prefix replacement resets presentation identity; change `streamKey` for another response. `animate = false` displays the full current text immediately. The accessible value is the full latest text, without live-region announcements for every token. Paginate very large transcripts in the host app. Longer streaming queues can exceed the 300ms group-entrance budget because this is paced incoming content; retain a host pause/stop control.

`ReasoningStream` shows the last `maxVisibleEntries` activity entries. A single measured column scrolls by actual displaced entry heights using a 500ms decelerating curve. Surviving entries keep a continuous path when other entries wrap or use larger text. A 28dp gradient overlaps visible glyphs only at an edge with hidden content beyond it. The bottom edge becomes sharp when the newest entry settles, and a fully expanded transcript has no edge mask. Entries wrap, so the parameter is not an exact visual-line limit. Its semantic value contains the full supplied transcript.

`ReasoningStream.active = false` removes motion while still accepting updated entries. To pause updates, freeze the host's displayed snapshot and provide an expanded transcript view. Pass user-visible activity such as "Searching saved documents" rather than fabricated internal reasoning. Neither stream component owns a timer or network request.

For any effect with `replayKey`, keep the key stable through ordinary recomposition. Change it for a new event or an explicit Replay action. System duration scale zero and `AnimatablesMotion(false)` eliminate animation and choreography delays. Verify actual playback and the rendered disabled state: semantic text alone can remain present while a rendering defect makes glyphs invisible. Frame inspection should establish sequential fades, steady prefixes through append, stable number anchors, and sharp expanded activity content. Record pending checks honestly rather than treating these descriptions as evidence of passing tests.


## Compose Multiplatform consumers

Use the same root coordinate in a KMP host; Gradle resolves Android, JVM, iOS or Wasm metadata:

```kotlin
kotlin {
    sourceSets.commonMain.dependencies {
        implementation("com.singleoriginapps:animatables:0.1.0-SNAPSHOT")
    }
}
```

Public component signatures and package names match Android. Keep an Expressive Material theme at the host boundary. The shared Material 3 API currently uses JetBrains 1.12.0-alpha03; Android selects Jetpack Material 3 1.5.0-beta02. Do not use Android-only Material APIs from common code. iOS and web reductions are observed automatically; desktop hosts must bridge their own motion preference with `AnimatablesMotion(enabled = ...)`. Motion opt-outs always retain the requested target state. Blur is an accent; platform fonts and Unicode versions can differ, so verify layout and semantics on each platform being shipped.
