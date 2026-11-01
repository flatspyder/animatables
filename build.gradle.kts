import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
}

// Keep FAIL_ON_PROJECT_REPOS: tool downloads use the module-filtered settings repositories.
allprojects {
    plugins.withType<WasmNodeJsPlugin> {
        extensions.configure<WasmNodeJsEnvSpec> {
            downloadBaseUrl.unset()
            downloadBaseUrl.unsetConvention()
        }
    }
    @OptIn(ExperimentalWasmDsl::class)
    plugins.withType<BinaryenPlugin> {
        extensions.configure<BinaryenEnvSpec> {
            downloadBaseUrl.unset()
            downloadBaseUrl.unsetConvention()
        }
    }
}
