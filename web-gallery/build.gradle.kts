import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenExec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import java.util.zip.GZIPOutputStream

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig { outputFileName = "gallery.js" }
        }
        binaries.executable()
    }
    sourceSets {
        wasmJsMain {
            dependencies { implementation(project(":animatables")) }
            resources.srcDir(project.layout.buildDirectory.dir("generated/gallery"))
        }
    }
}

// Keep Kotlin's feature flags and production DCE; prefer size in the final Binaryen pass.
tasks.withType<BinaryenExec>().configureEach {
    binaryenArguments.addAll("-Oz", "--strip-debug")
}

val generateGalleryContent = tasks.register<Exec>("generateGalleryContent") {
    group = "build"
    description = "Generate gallery descriptions and signatures from commonMain Kotlin source."
    inputs.dir(rootProject.layout.projectDirectory.dir("animatables/src/commonMain/kotlin"))
    inputs.dir(rootProject.layout.projectDirectory.dir("catalog/src/main/kotlin"))
    inputs.files("gallery-samples.json", "index.template.html", "scripts/generate-gallery.py")
    inputs.files("src/wasmJsMain/kotlin/Gallery.kt", "src/wasmJsMain/resources/playground.html")
    inputs.files(fileTree(layout.buildDirectory.dir("generated/gallery/previews")) { include("*.jpg") })
    outputs.files(
        layout.buildDirectory.file("generated/gallery/index.html"),
        layout.buildDirectory.file("generated/gallery/components-data.js"),
        layout.buildDirectory.file("generated/gallery/preview-sources.json"),
    )
    commandLine(
        "python3", layout.projectDirectory.file("scripts/generate-gallery.py").asFile,
        "--output-dir", layout.buildDirectory.dir("generated/gallery").get().asFile
    )
}

tasks.named("wasmJsProcessResources") { dependsOn(generateGalleryContent) }

val distribution = layout.buildDirectory.dir("dist/wasmJs/productionExecutable")
tasks.register("assembleGallery") {
    group = "build"
    description = "Build the static gallery and reproducible gzip payloads for any static host."
    dependsOn("wasmJsBrowserDistribution")
    inputs.dir(distribution)
    // Packaging is intentionally rerun: webpack owns/cleans the same output directory.
    doLast {
        val directory = distribution.get().asFile
        val assets = directory.listFiles()!!
            .filter { it.extension in setOf("wasm", "js", "css", "html", "svg") }
        val rows = assets.sortedBy { it.name }.map { file ->
            val compressed = file.resolveSibling(file.name + ".gz")
            compressed.outputStream().use { output ->
                object : GZIPOutputStream(output) { init {
                    def.setLevel(9)
                }
                }.use { gzip ->
                    file.inputStream().use { it.copyTo(gzip) }
                }
            }
            "  {\"file\":\"${file.name}\",\"bytes\":${file.length()},\"gzipBytes\":${compressed.length()}}"
        }
        directory.resolve("payload-sizes.json")
            .writeText("[\n" + rows.joinToString(",\n") + "\n]\n")
        logger.lifecycle("Compressed gallery: ${directory.absolutePath}")
    }
}

tasks.register<Exec>("testWasmTransfer") {
    group = "verification"
    description = "Check static-host gzip loading and raw-file fallbacks."
    dependsOn("kotlinWasmNodeJsSetup")
    val node = project.extensions.getByType<WasmNodeJsEnvSpec>()
    commandLine(
        node.executable.get(),
        layout.projectDirectory.file("scripts/test-wasm-transfer.mjs").asFile
    )
}

tasks.register<Exec>("testPreviewRuntime") {
    group = "verification"
    description = "Check live-card preparation, queued input and persistent renderer reuse."
    dependsOn("kotlinWasmNodeJsSetup")
    val node = project.extensions.getByType<WasmNodeJsEnvSpec>()
    commandLine(
        node.executable.get(),
        layout.projectDirectory.file("scripts/test-preview-runtime.mjs").asFile
    )
}
