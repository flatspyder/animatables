plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
}

group = "com.singleoriginapps"
version = "0.1.0-SNAPSHOT"

android {
    namespace = "com.singleoriginapps.animatables"
    compileSdk { version = release(37) { minorApiLevel = 0 } }
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    publishing { singleVariant("release") { withSourcesJar() } }
}

dependencies {
    api(libs.compose.ui)
    api(libs.compose.foundation)
    api(libs.compose.animation)
    api(libs.material3)
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.junit)
    // Compose's older transitive Espresso uses a hidden input API removed on Android 17.
    androidTestImplementation(libs.test.espresso)
    debugImplementation(libs.compose.test.manifest)
}

publishing {
    repositories {
        // A shareable local repository without requiring credentials or a global install.
        maven { name = "projectLocal"; url = rootProject.layout.buildDirectory.dir("repo").get().asFile.toURI() }
    }
}
afterEvaluate {
    publishing.publications.create<MavenPublication>("release") {
        from(components["release"])
        artifactId = "animatables"
        pom {
            name.set("Animatables")
            description.set("Considered Material 3 Expressive motion for Jetpack Compose")
            url.set("https://github.com/flatspyder/animatables")
            licenses { license { name.set("The Apache License, Version 2.0"); url.set("https://www.apache.org/licenses/LICENSE-2.0.txt") } }
            developers { developer { id.set("flatspyder"); name.set("Single Origin Apps") } }
            scm { url.set("https://github.com/flatspyder/animatables"); connection.set("scm:git:https://github.com/flatspyder/animatables.git") }
        }
    }
}
