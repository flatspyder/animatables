import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins { kotlin("multiplatform") }

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { nodejs() }
    sourceSets {
        commonMain {
            kotlin.srcDir("../../animatables/src/commonMain/kotlin")
            kotlin.include("**/PlatformUnicode.kt")
        }
        wasmJsMain {
            // Exercise the shipped adapter without loading the browser-only Skia runtime in Node.
            kotlin.srcDir("../../animatables/src/wasmJsMain/kotlin")
            kotlin.include("**/Unicode.web.kt")
        }
        wasmJsTest.dependencies { implementation(kotlin("test")) }
    }
}
