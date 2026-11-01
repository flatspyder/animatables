#!/usr/bin/env python3
"""Deploy the Kotlin/Wasm web gallery to GitHub Pages.

Builds the static distribution and pushes it to the repository's 'gh-pages' branch.
The 'main' branch remains clean and does not track generated web assets.

Usage:
  python3 scripts/deploy-gallery.py
  python3 scripts/deploy-gallery.py --custom-domain motion.singleoriginapps.com
"""
from __future__ import annotations

import argparse
from pathlib import Path
import shutil
import subprocess
import sys


def run(cmd, cwd=None, check=True):
    print(f"==> {' '.join(cmd)}")
    res = subprocess.run(cmd, cwd=cwd)
    if check and res.returncode != 0:
        print(f"Error: command failed with code {res.returncode}", file=sys.stderr)
        sys.exit(res.returncode)
    return res.returncode


def get_git_config(root: Path, key: str) -> str | None:
    try:
        val = subprocess.check_output(["git", "config", key], cwd=root).decode().strip()
        if val:
            return val
    except Exception:
        pass
    return None


def get_remote_url(root: Path) -> str:
    try:
        url = subprocess.check_output(["git", "config", "--get", "remote.origin.url"], cwd=root).decode().strip()
        if url:
            return url
    except Exception:
        pass
    print("Error: No git remote 'origin' found. Add a remote origin first.", file=sys.stderr)
    sys.exit(1)


def main():
    parser = argparse.ArgumentParser(description="Build and deploy Animatables web gallery to GitHub Pages.")
    parser.add_argument("--custom-domain", help="Custom domain/subdomain (e.g. motion.singleoriginapps.com)")
    parser.add_argument("--skip-build", action="store_true", help="Skip gradle assembleGallery build step")
    args = parser.parse_args()

    root = Path(__file__).resolve().parents[1]
    gradlew = str(root / "gradlew")
    dist_dir = root / "web-gallery" / "build" / "dist" / "wasmJs" / "productionExecutable"
    remote_url = get_remote_url(root)

    print("=== Deploying Animatables Web Gallery to GitHub Pages ===\n")

    # 1. Assemble gallery
    if not args.skip_build:
        print("Building static gallery and Kotlin/Wasm distribution...")
        run([gradlew, ":web-gallery:assembleGallery"], cwd=root)

    if not dist_dir.exists() or not (dist_dir / "index.html").exists():
        print(f"Error: Distribution directory not found at {dist_dir}", file=sys.stderr)
        sys.exit(1)

    # 2. Add .nojekyll (prevents GitHub Pages from running Jekyll on Wasm/static files)
    (dist_dir / ".nojekyll").touch()

    # 3. Add CNAME if custom domain provided
    if args.custom_domain:
        (dist_dir / "CNAME").write_text(args.custom_domain.strip() + "\n")
        print(f"Configured CNAME for: {args.custom_domain.strip()}")

    # 4. Push directly to gh-pages branch using an isolated git instance
    print(f"\nPushing distribution to 'gh-pages' branch on {remote_url}...")
    shutil.rmtree(dist_dir / ".git", ignore_errors=True)
    author_name = get_git_config(root, "user.name")
    author_email = get_git_config(root, "user.email")
    run(["git", "init"], cwd=dist_dir)
    if author_name:
        run(["git", "config", "user.name", author_name], cwd=dist_dir)
    if author_email:
        run(["git", "config", "user.email", author_email], cwd=dist_dir)
    run(["git", "checkout", "-B", "gh-pages"], cwd=dist_dir)
    run(["git", "add", "-A"], cwd=dist_dir)
    run(["git", "commit", "-m", "Deploy web gallery to GitHub Pages [skip ci]"], cwd=dist_dir)
    run(["git", "push", "-f", remote_url, "gh-pages"], cwd=dist_dir)

    # Clean up the temporary .git directory in build dist
    shutil.rmtree(dist_dir / ".git", ignore_errors=True)

    print("\n=== Deployment complete! ===")
    print("In your GitHub repository:")
    print("  1. Go to Settings -> Pages")
    print("  2. Ensure Source is set to 'Deploy from a branch'")
    print("  3. Set Branch to 'gh-pages' and Folder to '/ (root)'")
    if args.custom_domain:
        print(f"  4. Set Custom Domain to '{args.custom_domain}' and enable HTTPS")
        print(f"\nYour gallery will be live at: https://{args.custom_domain}/")
    else:
        print("\nYour gallery will be live at: https://<your-username>.github.io/animatables/")


if __name__ == "__main__":
    main()
