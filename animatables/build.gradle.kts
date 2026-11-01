import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    `maven-publish`
    signing
}

group = "com.singleoriginapps"
version = providers.environmentVariable("RELEASE_VERSION")
    .orElse(providers.gradleProperty("version"))
    .getOrElse("0.1.0-SNAPSHOT")

kotlin {
    android {
        namespace = "com.singleoriginapps.animatables"
        compileSdk = 37
        minSdk = 24
        androidResources { enable = true }
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            execution = "HOST"
        }
    }
    jvm { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    iosArm64()
    iosSimulatorArm64()
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            api(libs.multiplatform.ui)
            api(libs.multiplatform.foundation)
            api(libs.multiplatform.animation)
            api(libs.multiplatform.material3)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        androidMain.dependencies {
            // Android consumers retain the newer Jetpack Material implementation.
            api(libs.material3)
        }
        val skiaMain = create("skiaMain") { dependsOn(commonMain.get()) }
        jvmMain.get().dependsOn(skiaMain)
        iosMain.get().dependsOn(skiaMain)
        wasmJsMain.get().dependsOn(skiaMain)
        getByName("androidDeviceTest").dependencies {
            implementation(libs.compose.test)
            implementation(libs.compose.test.manifest)
            implementation(libs.test.runner)
            implementation(libs.test.junit)
            implementation(libs.test.espresso)
        }
    }
}

publishing {
    repositories {
        maven {
            name = "projectLocal"; url =
            rootProject.layout.buildDirectory.dir("repo").get().asFile.toURI()
        }
    }
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("Animatables")
            description.set("Considered Material 3 Expressive motion for Compose Multiplatform")
            url.set("https://github.com/flatspyder/animatables")
            licenses { license { name.set("The Apache License, Version 2.0"); url.set("https://www.apache.org/licenses/LICENSE-2.0.txt") } }
            developers { developer { id.set("flatspyder"); name.set("Single Origin Apps") } }
            scm { url.set("https://github.com/flatspyder/animatables"); connection.set("scm:git:https://github.com/flatspyder/animatables.git") }
        }
    }
}

val signingKey = System.getenv("SIGNING_KEY") ?: findProperty("signing.key") as? String
val signingPassword =
    System.getenv("SIGNING_PASSWORD") ?: findProperty("signing.password") as? String
val signingKeyId = System.getenv("SIGNING_KEY_ID") ?: findProperty("signing.keyId") as? String

signing {
    if (!signingKey.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    } else {
        useGpgCmd()
    }
    isRequired =
        providers.environmentVariable("RELEASE_VERSION").isPresent || !signingKey.isNullOrBlank()
    sign(publishing.publications)
}

tasks.withType<Sign>().configureEach {
    onlyIf {
        gradle.startParameter.taskNames.any { it.contains("publish", ignoreCase = true) }
    }
}
