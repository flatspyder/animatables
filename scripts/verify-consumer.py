#!/usr/bin/env python3
"""Compile the catalog as a separate consumer of the published Maven artifact."""
from pathlib import Path
import shutil
import subprocess
import tempfile


def main():
    root = Path(__file__).resolve().parents[1]
    subprocess.run(
        [str(root / "gradlew"), ":animatables:publishKotlinMultiplatformPublicationToProjectLocalRepository",
         ":animatables:publishAndroidPublicationToProjectLocalRepository", "--console=plain"],
        cwd=root, check=True,
    )
    with tempfile.TemporaryDirectory(prefix="animatables-consumer-") as directory:
        consumer = Path(directory)
        for name in ("gradlew", "gradlew.bat", "gradle.properties", "build.gradle.kts", "local.properties"):
            source = root / name
            if source.exists():
                shutil.copy2(source, consumer / name)
        shutil.copytree(root / "gradle", consumer / "gradle")
        shutil.copytree(root / "catalog" / "src" / "main", consumer / "catalog" / "src" / "main")
        module = (root / "catalog" / "build.gradle.kts").read_text()
        module = module.replace('implementation(project(":animatables"))',
                                'implementation("com.singleoriginapps:animatables:0.1.0-SNAPSHOT")')
        (consumer / "catalog" / "build.gradle.kts").write_text(module)
        repo_uri = (root / "build" / "repo").as_uri()
        (consumer / "settings.gradle.kts").write_text('''
pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google(); mavenCentral()
        maven { url = uri("%s"); content { includeGroup("com.singleoriginapps") } }
    }
}
rootProject.name = "AnimatablesConsumerCheck"
include(":catalog")
''' % repo_uri)
        # This app has no project dependency or access to library sources. Its compilation
        # verifies all sample API calls and the artifact's transitive dependency metadata.
        subprocess.run([str(consumer / "gradlew"), ":catalog:compileDebugKotlin", "--console=plain"],
                       cwd=consumer, check=True)
    print("Independent Maven consumer compiled successfully.")


if __name__ == "__main__":
    main()
