---
name: animatables-compose
description: Integrate Animatables components into Android and Compose Multiplatform screens, choosing restrained Material 3 Expressive motion, hoisted state, and accessible resting states. Use when a project already uses or explicitly requests Animatables; do not select it for unrelated animation libraries or pointer-only effects.
---

# Animatables Compose

Use Animatables to clarify an interaction or state change. Components live in `commonMain`, with Android, JVM desktop, iOS arm64/simulator and Kotlin/Wasm targets. Android retains its ICU and API 31+ blur adapters; other platforms use native Unicode segmentation and Skia blur. Web and iOS observe reduced-motion preferences automatically. Desktop hosts provide an OS/app preference using `AnimatablesMotion`. If browser grapheme segmentation is unavailable, formatted values remain intact as readable literals. See the repository README's platform support section for target requirements. Preserve the consuming app's Material theme and ownership of business state.

Read [references/integration.md](references/integration.md) for dependency setup, API selection, and a working example. Confirm the installed version and its signatures in source or generated API docs before changing code; this skill ships with the initial snapshot and is not a promise of future API compatibility.

## Choose the smallest useful component

- Prefer native Material 3 menus, dialogs, sheets, tabs, switches, checkboxes, progress indicators, and snackbars when they already supply the interaction. Animatables supplements them.
- Use an expansion to reveal related detail, a content swap for a meaningful state change, a badge for an actual count, and completion feedback for a foreground task. The named reveal/swap effects are explicit emphasis choices: reserve them for occasional events. Do not animate every label or list row.
- For streamed text, pass the actual current text to `StreamingText`; its ordered opacity-only fades share one native paragraph path. Do not layer a second native text copy behind it or restart existing words on append. Pass actual activity entries to `ReasoningStream`. Keep arrival, pause/stop, and transcript expansion in the caller. Never fabricate work states or hidden reasoning. `ReasoningStream` limits entries rather than visual lines.
- `replayKey` deliberately restarts finite effects. Use it for an explicit replay or a new event; do not generate a new key every recomposition. `streamKey` identifies a separate text stream. Shimmer ends after two two-second neutral glyph sweeps, retaining caller alpha and calculating shaded-color contrast against the theme surface; do not restart it continuously as a loading loop.
- `NumberPopIn` is initially static and rolls changed digit places. Keep its default `NumberFlowAnchor.Center` for a centered value; use `.Decimal` and adequate width for stable place-column alignment. Pass the locale's `decimalSeparator` explicitly when it differs from `'.'`; do not infer it from grouping characters.
- For an interaction used hundreds of times daily, preserve the native state layer. For tens of uses, keep added motion fast; occasional events can use the default tier. Reserve celebration for rare achievements.
- Keep server requests, timers, navigation, and domain logic outside these composables. Pass state and callbacks; use the app's state holder or `rememberSaveable` for user choices that should survive recreation.
- Avoid hard-coded colors, typography, spring values, and durations at call sites. General components obtain motion from `MaterialTheme.motionScheme`. Named reference effects have their own choreography tokens; keep those choices inside the library. Install a compatible Expressive theme at the app boundary. Inherit the app's existing theme rather than nesting a competing theme around each component.

## Make the resting state sufficient

Every important state needs readable text, a count, icon, or expanded content when animation is disabled. Compose animation APIs use the system animation clock; do not replace them with manual frame loops. `AnimatablesMotion(enabled = false)` is an additional app-level opt-out, not a replacement for Android's setting.

Keep touch targets, focus, click semantics, and labels intact. Do not announce every frame of a changing value; announce the final meaningful state. Make loading placeholders match the eventual content and retain separate error/retry states in the app. Avoid automatic decorative loops.

## Verify the integration

Build the consuming module. Inspect the result at compact and expanded widths, light/dark themes, larger text, and RTL where relevant. Exercise rapid reversal, configuration recreation, keyboard/TalkBack interaction, loading completion, and animation disabled. Inspect real playback at the start, midpoint, and settled state: a readable final screenshot does not prove a reveal moved. Compare rendered pixels for app-disabled versus settled text, not just semantics. API 31+ adds blur to the effects that use soft focus; verify their travel/fade fallback on older Android. Streaming is opacity-only. Check two finite shimmer sweeps and contrast against the actual host background, stable number anchoring, and compact activity gradients only at edges with hidden content. The newest activity entry should be sharp after settling; rapid success reversal should stay spatially continuous. Preview PNGs establish layout evidence; release builds on physical hardware establish motion/performance evidence. Report which checks ran and which remain.

This skill authorizes no publishing, repository creation, DNS change, or account action. Follow the user's current task scope for those operations.
