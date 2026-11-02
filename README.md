# Animatables

[![Maven Central](https://img.shields.io/maven-central/v/com.singleoriginapps/animatables?label=Maven%20Central&color=blue)](https://central.sonatype.com/artifact/com.singleoriginapps/animatables)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin%20Multiplatform-Android%20%7C%20iOS%20%7C%20Desktop%20%7C%20Wasm-purple.svg)](https://kotlinlang.org)

Animatables is a Compose Multiplatform library providing theme-aware motion components based on Material 3 Expressive motion tokens.

An interactive preview of all components is available in the **[Web Gallery](https://flatspyder.github.io/animatables/)**.

---

## Supported Platforms

| Platform | Target | Details |
| :--- | :--- | :--- |
| **Android** | API 24+ | Uses platform animation clock and ICU text handling. API 31+ uses native `RenderEffect` for soft-focus effects; API 24–30 falls back to travel and fade. |
| **iOS** | `iosArm64`, `iosSimulatorArm64` | Skia blur and platform Unicode segmentation. Observes system reduced motion. |
| **Desktop (JVM)** | Java 17+ (macOS, Linux, Windows) | Skia blur. Observes `AnimatablesMotion` preference. |
| **Web** | Kotlin/Wasm (`wasmJs`) | Skia blur. Observes CSS `prefers-reduced-motion`. |

---

## Download

Animatables is published to Maven Central under `com.singleoriginapps`.

### Compose Multiplatform

Add the dependency to your `commonMain` source set:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.singleoriginapps:animatables:0.1.0-alpha02")
        }
    }
}
```

### Android

For Android-only projects, add the dependency to `dependencies`:

```kotlin
dependencies {
    implementation("com.singleoriginapps:animatables:0.1.0-alpha02")
}
```

---

## Theme Setup

Components inherit colors, shapes, typography, and motion tokens directly from `MaterialTheme` (or `MaterialExpressiveTheme`). No custom theme wrapper is required:

```kotlin
MaterialTheme {
    AppContent()
}
```

---

## Usage

### Action button with state feedback

`AsyncActionButton` is a native Material button that coordinates `Idle`, `Working`, `Success`, and `Error` states. An anchored leading icon slot and vertical label transitions prevent button width shifts:

```kotlin
var state by remember { mutableStateOf(AsyncActionState.Idle) }

AsyncActionButton(
    state = state,
    onClick = {
        state = AsyncActionState.Working
        viewModel.save(
            onSuccess = { state = AsyncActionState.Success },
            onError = { state = AsyncActionState.Error },
        )
    },
    modifier = Modifier.width(220.dp),
    label = "Save changes",
    workingLabel = "Saving…",
    successLabel = "Saved",
    errorLabel = "Retry",
)
```

### Streamed text

`StreamingText` displays text updates with word-level opacity fades on a single measured paragraph layout, preserving native text wrapping and RTL shaping without layout reflow:

```kotlin
StreamingText(
    text = responseText,
    streamKey = messageId,
    style = MaterialTheme.typography.bodyLarge,
)
```

### Place-value rolling numbers

`NumberPopIn` rolls changed digits directionally while keeping unchanged digits stationary. Setting `anchor = NumberFlowAnchor.Decimal` maintains column alignment as values change:

```kotlin
NumberPopIn(
    value = "$1,250.00",
    anchor = NumberFlowAnchor.Decimal,
    style = MaterialTheme.typography.headlineMedium,
)
```

### Retained refresh and error recovery

`RefreshableContent` manages pull-to-refresh and initial loading states. Existing content, scroll position, and focus are retained while refreshing:

```kotlin
RefreshableContent(
    hasContent = items.isNotEmpty(),
    loading = isRefreshing,
    error = errorMessage,
    onRetry = { viewModel.refresh() },
    placeholder = { ListSkeleton() },
    empty = { Text("No items") },
) {
    ItemList(items)
}
```

### Audio activity and waveforms

`AudioActivityRing` displays an energy ring around an existing button without modifying its click handling or accessibility semantics. `AudioWaveform` resamples caller-supplied levels into an anchored 24-bar waveform:

```kotlin
// Energy halo around an existing recording button
AudioActivityRing(
    level = audioLevel, // Normalized 0f..1f
    active = isRecording,
) {
    FilledIconButton(onClick = ::toggleRecording) {
        Icon(Icons.Rounded.Mic, contentDescription = "Record")
    }
}

// 24-bar audio energy waveform
AudioWaveform(
    levels = frequencyLevels,
    active = isRecording,
    modifier = Modifier.fillMaxWidth().height(64.dp),
)
```

---

## Components

| Component | Description |
| :--- | :--- |
| **`AsyncActionButton`** | Native Material button coordinating `Idle`, `Working`, `Success`, and `Error` states with an anchored icon slot. |
| **`CompletionSpinner`** | Circular Material progress indicator (standard or wavy) that transitions into a completion mark. |
| **`SuccessCheck`** | Standalone completion checkmark with animated stroke reveal and soft-focus accent. |
| **`IconSwap`** | Scale and crossfade transition for replacing icons inside an existing control. |
| **`AnimatedStatusText`** | Two-phase text transition for status string changes. |
| **`StreamingText`** | Word-level opacity fades for streamed text on a single measured paragraph layout. |
| **`ReasoningStream`** | Scrollable viewport for activity log entries with scroll-coordinated edge masking. |
| **`ThinkingStates`** | Activity label with a bounded luminance sweep during background processing. |
| **`ShimmerText`** | Text with an animated luminance sweep (continuous or finite two-sweep mode). |
| **`TextReveal`** | Staggered entrance for multi-line text blocks. |
| **`TextStateSwap`** | Two-phase text transition with vertical travel and soft focus. |
| **`NumberPopIn`** | Place-value digit reels for numeric text with decimal or center anchoring. |
| **`AnimatedCountBadge`** | Directional rolling digit badge with capsule width adjustment and full screen reader value. |
| **`AudioWaveform`** | Anchored 24-bar audio energy display with fast attack and default release specs. |
| **`AudioActivityRing`** | Level-reactive halo around a circular recording button that preserves host semantics. |
| **`AudioLevelIcon`** | Compact 5-bar live audio indicator. |
| **`RefreshableContent`** | Pull-to-refresh container that retains existing content, scroll state, and focus during reloads. |
| **`LoadingContent`** | Placeholder container with minimum visible hold and appearance delay to prevent layout flicker. |
| **`ExpandableCard`** | Disclosure card with animated height expansion and accessible header controls. |
| **`MotionContent`** | In-place content replacement with vertical slide, fade, and size adjustment. |
| **`AnimatablesMotion`** | CompositionLocal provider to inspect or toggle motion app-wide. |

---

## Reduced Motion & Accessibility

* **System reduced motion**: Observes Android's Animator duration scale, iOS's `prefers-reduced-motion`, and CSS `prefers-reduced-motion`. When reduced motion is requested, transitions resolve immediately to their resting states without timing delays.
* **App-level control**: Motion can be disabled programmatically across a composition subtree:
  ```kotlin
  AnimatablesMotion(enabled = false) {
      AppContent()
  }
  ```
* **Screen readers**: Accessibility semantics expose final meaningful states (such as full count badge values or complete streamed text) rather than intermediate animation frames.
* **Input handling**: Controls remain interactive during transitions; gestures and taps are not blocked.

---

## Samples

* **[Web Gallery](https://flatspyder.github.io/animatables/)**: Browser-based interactive catalog running via Kotlin/Wasm.
* **Android Catalog**: Run `./gradlew :catalog:installDebug` to install the demo app on a connected device.
* **Local Web Gallery**:
  ```sh
  ./gradlew :web-gallery:assembleGallery
  python3 -m http.server 8080 --directory web-gallery/build/dist/wasmJs/productionExecutable
  ```

---

## Building from Source

```sh
# Build library and catalog
./gradlew :animatables:assembleAndroidMain :catalog:assembleDebug

# Run unit and multiplatform tests
./gradlew :animatables:jvmTest :unicode-tests:wasmJsNodeTest
```


---

## License

```
Copyright 2026 Single Origin Apps

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

Icons use Google Material Symbols Rounded as generated Kotlin vector graphics. See [Third-Party Notices](THIRD_PARTY_NOTICES.md).
