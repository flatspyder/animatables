#!/usr/bin/env python3
"""Local release script for Animatables.

Builds and signs the Android catalog APK and Maven Central multiplatform artifacts
entirely on your local machine. No private keys or passwords leave your computer.

Usage:
  python3 scripts/release.py 0.1.0-alpha01
"""
from __future__ import annotations

import argparse
import base64
import getpass
import os
from pathlib import Path
import shutil
import subprocess
import sys
import urllib.error
import urllib.request
import zipfile


def get_keychain_secret(service: str, account: str = None) -> str | None:
    """Retrieve a password from macOS Keychain. Prompts for Touch ID / Keychain access if set."""
    if sys.platform != "darwin":
        return None
    if not account:
        account = os.environ.get("USER", "")
    cmd = ["security", "find-generic-password", "-s", service, "-a", account, "-w"]
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, check=False)
        if res.returncode == 0 and res.stdout.strip():
            return res.stdout.strip()
    except Exception:
        pass
    return None


def set_keychain_secret(service: str, secret: str, account: str = None) -> bool:
    """Save a password securely into macOS Keychain."""
    if sys.platform != "darwin":
        return False
    if not account:
        account = os.environ.get("USER", "")
    cmd = ["security", "add-generic-password", "-U", "-s", service, "-a", account, "-w", secret]
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, check=False)
        return res.returncode == 0
    except Exception:
        return False


def run(cmd, cwd=None, env=None, check=True):
    print(f"\n==> Running: {' '.join(cmd)}")
    result = subprocess.run(cmd, cwd=cwd, env=env)
    if check and result.returncode != 0:
        print(f"Error: command failed with exit code {result.returncode}", file=sys.stderr)
        sys.exit(result.returncode)
    return result.returncode


def create_bundle_zip(repo_dir: Path, zip_dest: Path, version: str):
    print(f"\n==> Packaging Maven Central bundle: {zip_dest}")
    if zip_dest.exists():
        zip_dest.unlink()
    matched_count = 0
    with zipfile.ZipFile(zip_dest, "w", zipfile.ZIP_DEFLATED) as zf:
        for file in repo_dir.rglob("*"):
            if file.is_file():
                # Sonatype Central requires:
                # 1. No maven-metadata.xml files in the bundle
                # 2. Only release version artifacts (no SNAPSHOTs or unrelated versions)
                if "maven-metadata.xml" in file.name:
                    continue
                if f"/{version}/" not in file.as_posix():
                    continue
                zf.write(file, arcname=file.relative_to(repo_dir))
                matched_count += 1
    print(f"Bundle created with {matched_count} files ({zip_dest.stat().st_size:,} bytes).")


def main():
    parser = argparse.ArgumentParser(description="Build, sign, and publish an Animatables release locally.")
    parser.add_argument("version", help="Release version (e.g. 0.1.0-alpha01)")
    parser.add_argument("--skip-tests", action="store_true", help="Skip test suite execution")
    args = parser.parse_args()

    version = args.version.lstrip("v")
    tag = f"v{version}"
    root = Path(__file__).resolve().parents[1]
    gradlew = str(root / "gradlew")

    print(f"=== Animatables Local Release: {version} (tag: {tag}) ===\n")

    # 1. Hygiene & Status check
    print("Checking repository cleanliness...")
    git_status = subprocess.check_output(["git", "status", "--porcelain"], cwd=root).decode().strip()
    if git_status:
        print("\nWarning: You have uncommitted or untracked changes in your git working tree:")
        print(git_status)
        proceed = input("\nContinue anyway? [y/N]: ").strip().lower()
        if proceed != "y":
            print("Aborted.")
            sys.exit(1)

    # 2. Run hygiene check
    run([sys.executable, str(root / "scripts" / "check-public-content.py"), "--history"], cwd=root)

    # 3. Run test suite
    if not args.skip_tests:
        print("\n==> Running test suite...")
        run([gradlew, ":animatables:jvmTest", ":unicode-tests:wasmJsNodeTest"], cwd=root)

    # 4. Keystore password for catalog APK
    keystore_path = root / "catalog-release.jks"
    env_keystore = {}
    if keystore_path.exists():
        print(f"\nFound Android release keystore: {keystore_path.name}")
        password = get_keychain_secret("animatables-keystore")
        if password:
            print("Retrieved keystore password securely from macOS Keychain (Touch ID).")
        else:
            password = getpass.getpass("Enter keystore password (will not echo): ")
            if password:
                save_kc = input("Save keystore password to macOS Keychain for Touch ID on future releases? [Y/n]: ").strip().lower()
                if save_kc != "n":
                    if set_keychain_secret("animatables-keystore", password):
                        print("Saved keystore password to macOS Keychain.")

        if password:
            env_keystore["KEYSTORE_PASSWORD"] = password
            env_keystore["KEY_ALIAS"] = "catalog"
    else:
        print(f"\nNote: {keystore_path.name} not found in project root. Catalog APK will be built unsigned.")

    # 5. Build Catalog Release APK
    print(f"\n==> Assembling Catalog Release APK (version {version})...")
    import os
    build_env = os.environ.copy()
    build_env.update(env_keystore)
    build_env["RELEASE_VERSION"] = version

    run([gradlew, ":catalog:assembleRelease"], cwd=root, env=build_env)

    apk_dir = root / "catalog" / "build" / "outputs" / "apk" / "release"
    apks = list(apk_dir.glob("*.apk")) if apk_dir.exists() else []
    if apks:
        for apk in apks:
            print(f"  Artifact: {apk.name} ({apk.stat().st_size:,} bytes)")
    else:
        print("  Warning: No release APK found.")

    # 6. Build and sign Maven Publications using local GPG
    print(f"\n==> Building & signing multiplatform library publications (version {version})...")
    print("Gradle will invoke your local GPG to sign artifacts.")
    run([gradlew, ":animatables:publishAllPublicationsToProjectLocalRepository"], cwd=root, env=build_env)

    repo_dir = root / "build" / "repo"
    bundle_zip = root / "build" / f"animatables-{version}-bundle.zip"
    create_bundle_zip(repo_dir, bundle_zip, version)

    # 7. Upload to Sonatype Central Portal (Optional)
    print("\n-----------------------------------------------------------")
    upload_central = input("Upload bundle to Sonatype Central Portal now? [y/N]: ").strip().lower()
    if upload_central == "y":
        token_username = get_keychain_secret("sonatype-portal-username")
        token_password = get_keychain_secret("sonatype-portal-password")

        if token_username and token_password:
            print("Retrieved Sonatype Central Portal token from macOS Keychain (Touch ID).")
        else:
            token_username = input("Enter Sonatype Central Portal token username: ").strip()
            token_password = getpass.getpass("Enter Sonatype Central Portal token password: ").strip()
            if token_username and token_password:
                save_token = input("Save Sonatype token to macOS Keychain for future releases? [Y/n]: ").strip().lower()
                if save_token != "n":
                    set_keychain_secret("sonatype-portal-username", token_username)
                    set_keychain_secret("sonatype-portal-password", token_password)
                    print("Saved Sonatype token to macOS Keychain.")

        if token_username and token_password:
            auth_str = base64.b64encode(f"{token_username}:{token_password}".encode()).decode()
            url = "https://central.sonatype.com/api/v1/publisher/upload?publishingType=AUTOMATIC"
            print(f"Uploading {bundle_zip.name} to Sonatype Central Portal...")

            # Upload using curl to handle multipart/form-data cleanly
            curl_cmd = [
                "curl", "--fail-with-body", "--request", "POST",
                "--url", url,
                "--header", f"Authorization: Bearer {auth_str}",
                "--form", f"bundle=@{bundle_zip}"
            ]
            run(curl_cmd, cwd=root)
            print("Successfully uploaded to Sonatype Central Portal!")
        else:
            print("Skipped upload: token not provided.")

    # 8. Git Tag
    print("\n-----------------------------------------------------------")
    tag_confirm = input(f"Create git tag '{tag}' and push to origin? [y/N]: ").strip().lower()
    if tag_confirm == "y":
        run(["git", "tag", "-a", tag, "-m", f"Release {tag}"], cwd=root)
        run(["git", "push", "origin", tag], cwd=root)
        print(f"Pushed tag {tag} to origin.")

    # 9. Optional Web Gallery Deploy
    print("\n-----------------------------------------------------------")
    deploy_web = input("Deploy updated web gallery to GitHub Pages now? [y/N]: ").strip().lower()
    if deploy_web == "y":
        custom_domain = input("Enter custom domain (press Enter to keep default GitHub Pages URL): ").strip()
        cmd = [sys.executable, str(root / "scripts" / "deploy-gallery.py")]
        if custom_domain:
            cmd.extend(["--custom-domain", custom_domain])
        run(cmd, cwd=root)

    print("\n=== Release steps completed! ===")
    if apks:
        print(f"Signed Catalog APK: {apks[0]}")
        print("Attach this APK to your GitHub Release in your browser.")
    print(f"Maven Central Bundle: {bundle_zip}")


if __name__ == "__main__":
    main()
