#!/usr/bin/env python3
"""Reject local development material and generated gallery output in publishable source.

Default checks the index and non-ignored new files (the inputs to a normal git add).
--history also checks branch/tag commits. Private reflogs and app checkpoint refs
remain local and are intentionally outside the normal GitHub push namespace.
"""
import argparse
from pathlib import PurePosixPath
import subprocess


def git(*args):
    return subprocess.check_output(['git', *args]).decode()


def local_only(path):
    parts = PurePosixPath(path).parts
    if not parts:
        return False
    if parts[0] in {'.local', 'docs', 'captures'} or path in {'AGENTS.md', 'MOTION.md'}:
        return True
    if any(part in {'build', 'node_modules', '.gradle', '.kotlin'} for part in parts):
        return True
    filename = parts[-1]
    if filename in {'local.properties', 'keystore.properties', 'signing.properties'} or filename.startswith('.env'):
        return True
    if filename.endswith(('.jks', '.keystore', '.gpg', '.asc', '.key', '.pem', '.p12')):
        return True
    if not path.startswith('web-gallery/'):
        return False
    if path == 'web-gallery/performance-samples.json':
        return True
    if path.endswith(('.wasm', '.wasm.gz', '.ttf', '.otf', '.woff', '.woff2')):
        return True
    prefix = 'web-gallery/src/wasmJsMain/resources/'
    if path.startswith(prefix):
        relative = path[len(prefix):]
        return (relative.startswith(('previews/', 'fonts/')) or relative in {
            'index.html', 'components-data.js', 'preview-sources.json',
            'profile.html', 'preview-profile.js',
        })
    return False


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--history', action='store_true')
    args = parser.parse_args()
    failures = []
    candidates = set(git('ls-files', '--cached', '--others', '--exclude-standard', '-z').split('\0')) - {''}
    failures.extend(f'Git candidate: {path}' for path in sorted(candidates) if local_only(path))
    if args.history:
        for commit in git('rev-list', '--branches', '--tags').splitlines():
            paths = git('ls-tree', '-r', '--name-only', '-z', commit).split('\0')
            failures.extend(f'{commit[:12]}: {path}' for path in paths if path and local_only(path))
    if failures:
        raise SystemExit('Local-only material in publishable source:\n' + '\n'.join(failures))
    print('Public source candidates and branch/tag history are clean.' if args.history else 'Public source candidates are clean.')


if __name__ == '__main__':
    main()
