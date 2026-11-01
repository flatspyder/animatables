plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}
val releaseVersion = providers.environmentVariable("RELEASE_VERSION")
    .orElse(providers.gradleProperty("version"))
    .getOrElse("0.1.0")

android {
    sourceSets.getByName("main").kotlin.srcDir("src/shared/kotlin")
    namespace = "com.singleoriginapps.animatables.catalog"
    compileSdk { version = release(37) { minorApiLevel = 0 } }
    defaultConfig {
        applicationId = "com.singleoriginapps.animatables.catalog"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = releaseVersion
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    signingConfigs {
        create("release") {
            val envFile = System.getenv("KEYSTORE_FILE")?.let { file(it) }
            val localFile = rootProject.file("catalog-release.jks")
            val targetFile = envFile ?: if (localFile.exists()) localFile else null

            if (targetFile != null && targetFile.exists()) {
                storeFile = targetFile
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS") ?: "catalog"
                keyPassword = System.getenv("KEY_PASSWORD") ?: System.getenv("KEYSTORE_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfigs.findByName("release")?.let { config ->
                if (config.storeFile != null) {
                    signingConfig = config
                }
            }
        }
    }
}
dependencies {
    implementation(project(":animatables"))
    implementation(libs.activity.compose)
    implementation(libs.compose.preview)
    debugImplementation(libs.compose.tooling)
}

val checkCatalogCoverage = tasks.register<Exec>("checkCatalogCoverage") {
    group = "verification"
    description = "Require every public component in both Android and web viewers."
    inputs.dir(rootProject.layout.projectDirectory.dir("animatables/src/commonMain/kotlin"))
    inputs.dir("src/main/kotlin")
    inputs.files(
        rootProject.layout.projectDirectory.file("web-gallery/gallery-samples.json"),
        rootProject.layout.projectDirectory.file("web-gallery/scripts/generate-gallery.py"),
        rootProject.layout.projectDirectory.file("web-gallery/src/wasmJsMain/kotlin/Gallery.kt"),
    )
    commandLine(
        "python3",
        rootProject.layout.projectDirectory.file("web-gallery/scripts/generate-gallery.py").asFile,
        "--check-coverage"
    )
}

tasks.named("preBuild") { dependsOn(checkCatalogCoverage) }
