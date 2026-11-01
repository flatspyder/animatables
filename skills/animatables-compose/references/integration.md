# Initial snapshot integration

This reference accompanies `com.singleoriginapps:animatables:0.1.0-SNAPSHOT`. It is a development snapshot, not a released Maven Central coordinate.

## Dependency setup

Inside the library checkout, build the project-local Maven repository:

```sh
./gradlew :animatables:publishReleasePublicationToProjectLocalRepository
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

The owner's current workflows may alternatively use a Gradle project dependency. Do not add `mavenLocal()` globally or publish artifacts as a side effect of a UI edit. Public hosting remains a separate release task.

## Component map

All functions are in `com.singleoriginapps.animatables`. Check source for optional parameters and their current defaults.

| Need | API | State belongs to |
| --- | --- | --- |
| App preference to disable optional motion | `AnimatablesMotion(enabled) { ... }` | App settings/state holder |
| Small in-place content swap with size reflow | `MotionContent(targetState) { state -> ... }` | Caller; render the supplied lambda value |
| Related detail revealed from a titled card | `ExpandableCard(expanded, onExpandedChange, title) { ... }` | Caller; keep expansion saveable if user-owned |
| Short status string replacement | `AnimatedStatusText(text)` | Caller; do not stream a new value every frame |
| Actual notification/item count | `AnimatedCountBadge(count)` | Caller; supply the actual count, not a capped display string |
| Placeholder to loaded content | `LoadingContent(loading, placeholder = { ... }) { ... }` | Caller; loading state, data, error/retry |
| Foreground completion feedback | `CompletionIndicator(complete)` | Caller; real task state, no fake completion timer |
| Formatted metric with place-value continuity | `NumberPopIn(value, decimalSeparator = '.', anchor = NumberFlowAnchor.Center)` | Caller; formatted value, locale separator, deliberate replay identity |
| Deliberate text handoff | `TextStateSwap(text, replayKey = eventId)` | Caller; real label state |
| Icon swap inside a labeled control | `IconSwap(targetState) { state -> ... }` | Caller; keep button semantics/hit target on host |
| Expressive foreground success | `SuccessCheck(complete, replayKey = eventId)` | Caller; actual result, replay only deliberately |
| Short grouped introduction | `TextReveal(lines, replayKey = eventId)` | Caller; lines wrap naturally |
| Finite text highlight | `ShimmerText(text, active = true)` | Caller; plays two two-second sweeps, then rests |
| Truthful activity state | `ThinkingStates(text, active = working)` | Caller; state changes only with actual work |
| Appended response text | `StreamingText(text, streamKey = responseId)` | Caller; actual text, pause/stop, stream identity |
| Compact activity transcript | `ReasoningStream(entries, maxVisibleEntries = 2)` | Caller; actual entries and expanded/pause controls |

For navigation, anchored menus, tooltips, switches, checkboxes, tabs, snackbars, and modal sheets, begin with the native Material/Navigation components. Neither `MotionContent` nor `ExpandableCard` implements navigation lifecycle or predictive back.

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

`IconSwap` uses a centered 250ms scale/focus handoff with a 2dp blur accent; preserve the host button's semantics and target. `SuccessCheck` stays full size and arrives through a 500ms upward fade with 40dp travel, 80° rotation, and 10dp soft focus. Its 500ms check stroke begins after an 80ms lead-in. Rapid reversal retargets the visible mark continuously rather than restarting its spatial position. Prefer the calmer `CompletionIndicator` when emphasis is unnecessary. Blur is an API 31+ accent; earlier Android retains the other transformations and a sharp resting state.

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
