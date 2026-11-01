# Animatables web gallery

An HTML component gallery backed by the real `:animatables` Compose Multiplatform library. Play
cards directly to change state or replay finite effects. API examples, signatures and usage notes
are generated from Kotlin declarations and KDoc.

## Build and serve

From the repository root, with Python 3.9+ and JDK 17+:

```sh
./gradlew :web-gallery:assembleGallery
python3 -m http.server 8080 --bind 127.0.0.1 \
  --directory web-gallery/build/dist/wasmJs/productionExecutable
```

Open `http://localhost:8080`. Wasm loading requires HTTP. For development, use
`./gradlew :web-gallery:wasmJsBrowserDevelopmentRun`.

## Source and generated files

Commit these inputs:

- `index.template.html`: page structure and content slots.
- `gallery-samples.json`: component order, categories, sample calls and demo actions.
- `src/wasmJsMain/kotlin/Gallery.kt`: demonstrations of the shared library.
- `../catalog/src/shared/kotlin/`: deterministic speech-envelope samples shared with Android.
- `src/wasmJsMain/resources/`: handwritten CSS, JavaScript, HTML renderer and SVG mark.
- `scripts/`, Gradle configuration and dependency lockfiles: reproducible generation and automated
  checks.

The build generates `index.html`, `components-data.js` and `preview-sources.json` under
`build/generated/gallery/`. Component captures and their manifest also belong in that generated
directory. Do not place output files in source resources or commit them. Build products, preview
captures, measurement output and local development notes are excluded from Git.

Google Fonts supplies Bricolage Grotesque and DM Sans through the HTML header. Font binaries are not
bundled. System fallbacks keep text readable when those requests are unavailable.

Generate or verify the component content directly:

```sh
python3 web-gallery/scripts/generate-gallery.py
python3 web-gallery/scripts/generate-gallery.py --check
python3 web-gallery/scripts/generate-gallery.py --check-coverage
```

Both gallery generation and Android `preBuild` check that every public visual composable has web
sample metadata, a web dispatch case, and a registered, reachable Android demo. `AnimatablesMotion`
is exercised by each viewer's global motion control. Adding a component requires adding both demos.
The extractor handles this repository's documented public composables; it is not a general Kotlin
parser.

## Generate static card captures

Live cards work without captured images; a source-only build displays a Play prompt until captures
are generated. To generate the real resting-state images, build and serve the gallery first, then
run the optional browser tool:

```sh
npm install --prefix web-gallery --no-save --package-lock=false playwright
npm exec --prefix web-gallery -- playwright install chromium
node web-gallery/scripts/capture-previews.mjs http://127.0.0.1:8080/
./gradlew :web-gallery:assembleGallery
python3 web-gallery/scripts/generate-gallery.py --check --check-previews
```

The script writes to `build/generated/gallery/previews/`. The second build includes those images in
the distribution. A clean build removes captures; regenerate them when needed for deployment. None
of the npm installation files or browser captures belongs in Git.

To reuse an existing browser-tool installation, set `PLAYWRIGHT_MODULE` to its `index.mjs` path and
`PLAYWRIGHT_CHROMIUM_EXECUTABLE` to the Chromium executable instead of installing another copy.

The renderer's standalone capture route is `playground.html?component=NumberPopIn&motion=false`.
Capture at 640 × 360 CSS pixels and device scale 1, after
`html[data-rendered-component="NumberPopIn"]` appears. Source fingerprints prevent capturing an
outdated build; shared helper/dependency changes also require regeneration.

## Live preview behavior

One persistent renderer serves the cards; the optional API dialog uses a second renderer. A
component's sample states and graphics layers are prepared before queued input starts its visible
animation. Repeated input remains available during playback. Theme changes retain sample state, and
reduced motion presents target states directly.

Search and category filters combine. `/` focuses search. Component hashes such as `#StreamingText`
open details; Escape dismisses the dialog and restores focus. Static descriptions, examples and
setup links remain available if the live renderer cannot start.

## Checks

```sh
./gradlew :web-gallery:testWasmTransfer :web-gallery:testPreviewRuntime
./gradlew :animatables:jvmTest :unicode-tests:wasmJsNodeTest
python3 web-gallery/scripts/test-gallery-coverage.py
node web-gallery/scripts/test-viewer-controls.mjs http://127.0.0.1:8080/
node web-gallery/scripts/test-viewer-controls.mjs http://127.0.0.1:8080/ action-audio
```

These check source behavior and loading contracts. Assess motion performance on representative
hardware with an optimized build.

The optional browser check uses the capture script's Playwright setup and overrides. It checks
visible pixels after repeated actions in every component, four complete text/icon cycles through
both card and detail controls, and the spinner's native detail-page options. Verification captures
are written beneath `build/generated/gallery/verification/`.
The `action-audio` subset checks repeated action changes, a coherent refresh/offline/retry journey
with a retained pinned place, and audio autoplay from simulated samples. It also verifies sample
pause/resume, mute, and static reduced-motion profiles.

## Static hosting

Publish the complete `build/dist/wasmJs/productionExecutable/` directory. `assembleGallery` produces
gzip sidecars and a payload-size manifest. Raw `.wasm` files require `application/wasm`; gzip files
can be ordinary files without a Content-Encoding header. The loader handles host-decoded gzip and
falls back to raw Wasm when needed.

Keep relative paths intact for hosting under a project subdirectory. Deployment artifacts belong in
the hosting workflow, not repository source history.
