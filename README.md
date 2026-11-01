# Animatables

Theme-aware Compose Multiplatform components for readable state changes and Material 3 Expressive motion. The library uses the package `com.singleoriginapps.animatables`. No public Maven artifact is currently available; build from source or publish to a local Maven repository.

## Components

| Component | Purpose |
| --- | --- |
| `AsyncActionButton` | Native action control with a fixed leading icon slot and working, success and retry feedback |
| `RefreshableContent` | Initial loading, empty/error recovery and refresh that retains existing content |
| `AudioWaveform` | Anchored audio-energy bars with quick attacks and gentle release |
| `AudioLevelIcon` | Compact five-bar audio activity indicator |
| `AudioActivityRing` | Level-reactive halo around an existing recording button |
| `MotionContent` | In-place content replacement with a fade, vertical entrance and size reflow |
| `ExpandableCard` | State-hoisted disclosure with an accessible, interactive header |
| `AnimatedStatusText` | Short status-label changes |
| `AnimatedCountBadge` | Capped visual counts with the full accessible value |
| `LoadingContent` | Structural placeholder timing and content handoff |
| `CompletionSpinner` | Standard or wavy Material progress with the shared success entrance |
| `NumberPopIn` | Formatted digit rolls with centre or decimal anchoring |
| `TextStateSwap` | Two-phase label handoff with soft focus |
| `IconSwap` | Icon replacement inside the caller's existing control |
| `SuccessCheck` | Completion feedback with modifier sizing, explicit replay and an optional icon |
| `TextReveal` | Staggered introduction of a short group of lines |
| `ShimmerText` | Continuous glyph highlights, or two sweeps with `continuous = false` |
| `ThinkingStates` | Caller-owned activity labels |
| `StreamingText` | Overlapping word fades that preserve settled text |
| `ReasoningStream` | A compact window of caller-supplied activity entries |
| `AnimatablesMotion` | An additional application-level motion preference |

Components inherit the host Material theme. Keep state, events, data arrival and business logic in the caller. Prefer native Material controls for the interactions they already implement.

Async actions keep one native Material button mounted with a fixed leading icon slot and centred labels. Size the
button with a modifier to fit its localized labels. The caller decides
whether working can be clicked again or cancelled; success never resets on a timer. For refresh,
retain the previous data and pass `hasContent = true` while loading or recovering from an error.
`RefreshableContent` keeps that content's local state and focus intact.

Audio components consume normalized levels from the host; they do not capture audio or request
microphone permissions. Use `AudioWaveform` for a larger presentation, `AudioLevelIcon` in compact
spaces, or wrap an existing circular recording control with `AudioActivityRing`. The ring reserves
space without intercepting clicks or replacing the button's semantics. With reduced motion, active
audio has a fixed readable profile and inactive audio has a quiet baseline.

```kotlin
var expanded by rememberSaveable { mutableStateOf(false) }

ExpandableCard(
    expanded = expanded,
    onExpandedChange = { expanded = it },
    title = "Delivery details",
) {
    Text("Arriving tomorrow")
}
```

See the [integration guide](skills/animatables-compose/references/integration.md) for imports, theme setup, localization and component selection. The [Animatables Compose skill](skills/animatables-compose/SKILL.md) provides reusable integration guidance for coding agents.

## Platform support

Shared components live in [`animatables/src/commonMain`](animatables/src/commonMain/kotlin/com/singleoriginapps/animatables/). Targets include Android API 24+, JVM desktop, iOS arm64/simulator and Kotlin/Wasm.

Android uses its platform animation clock and ICU text handling. Soft focus uses Android RenderEffect on API 31+; earlier versions retain sharp travel/fade effects. Other targets use Compose/Skia blur and platform Unicode segmentation. Web and iOS observe system reduced motion; desktop hosts pass their preference through `AnimatablesMotion`. App-disabled motion presents readable target states.

## Build

Use JDK 17 or later and Python 3, and configure the Android SDK through `local.properties` or the normal SDK environment. iOS builds require macOS and Xcode.

```sh
./gradlew :animatables:assembleAndroidMain :catalog:assembleDebug
./gradlew :animatables:jvmTest :unicode-tests:wasmJsNodeTest
python3 scripts/check-public-content.py --history
```

The Android catalog depends directly on `:animatables`:

```sh
./gradlew :catalog:installDebug
```

## Local Maven integration

Publish the library and source artifacts:

```sh
./gradlew :animatables:publishAllPublicationsToProjectLocalRepository
```

Add the generated repository directory to the consuming application's repository configuration:

```kotlin
maven {
    url = uri("/absolute/path/to/Animatables/build/repo")
    content { includeGroup("com.singleoriginapps") }
}
```

Then add the dependency to the consuming Android module or KMP `commonMain` source set:

```kotlin
implementation("com.singleoriginapps:animatables:0.1.0-SNAPSHOT")
```

## Web gallery

The [web gallery](web-gallery/README.md) demonstrates the same shared library with interactive cards, state controls, API examples, search, themes and reduced motion.

```sh
./gradlew :web-gallery:assembleGallery
python3 -m http.server 8080 --bind 127.0.0.1 \
  --directory web-gallery/build/dist/wasmJs/productionExecutable
```

Open `http://localhost:8080`. Generated HTML, component data, preview captures, Wasm and compressed deployment files belong under `web-gallery/build/`; commit their source templates and generators.

## Source hygiene

Keep implementation notes and generated build output local. Enable the repository's pre-commit guard to reject local-only files in the Git index:

```sh
git config --local core.hooksPath .githooks
```

`python3 scripts/check-public-content.py --history` also checks publishable branch/tag history.

## License

[Apache-2.0](LICENSE).

Icons use Google Material Symbols Rounded as generated Kotlin vectors, shared across supported
Compose targets. The web page loads the matching font through Google Fonts. See
[third-party notices](THIRD_PARTY_NOTICES.md) and the [generator](scripts/generate-material-symbols.py).
Use native Material controls directly for switches, checkboxes and menus.
