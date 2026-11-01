#!/usr/bin/env python3
"""Generate the HTML collection and browser data from public Kotlin declarations.

Only demo configuration belongs in gallery-samples.json. KDoc and signatures come
from commonMain. This intentionally small extractor fails on missing declarations;
it is not a general-purpose Kotlin parser or a replacement for Dokka.
"""
import argparse
import html
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
GALLERY = ROOT / "web-gallery"
RESOURCES = GALLERY / "src/wasmJsMain/resources"
OUTPUT = GALLERY / "build/generated/gallery"
SOURCE = ROOT / "animatables/src/commonMain/kotlin/com/singleoriginapps/animatables"


def preview_hash(name):
    files = [SOURCE / f"{name}.kt", GALLERY / "src/wasmJsMain/kotlin/Gallery.kt", RESOURCES / "playground.html"]
    return hashlib.sha256(b''.join(file.read_bytes() for file in files)).hexdigest()


def declaration(text, name):
    match = re.search(r"/\*\*((?:(?!/\*\*).)*?)\*/\s*@Composable\s+public fun\s+(?:<[^>]+>\s*)?" + re.escape(name) + r"\(", text, re.S)
    if not match:
        raise ValueError(f"No documented public composable: {name}")
    start = match.end() - 1
    depth, quote, escaped = 0, None, False
    for index in range(start, len(text)):
        char = text[index]
        if quote:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == quote:
                quote = None
        elif char in "\"'":
            quote = char
        elif char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                signature = text[text.index("public fun", match.start()):index + 1].removeprefix("public ")
                break
    else:
        raise ValueError(f"Unbalanced declaration: {name}")
    doc = "\n".join(re.sub(r"^\s*\* ?", "", line) for line in match.group(1).splitlines()).strip()
    paragraphs = [re.sub(r"\s+", " ", p).strip() for p in doc.split("\n\n")]
    # Display KDoc references as readable plain text. The complete original stays in source.
    paragraphs = [re.sub(r"\[([^\]]+)\]", r"\1", p).replace("`", "") for p in paragraphs]
    return signature, paragraphs


def generate(output_dir=OUTPUT):
    samples = json.loads((GALLERY / "gallery-samples.json").read_text())
    if len({s['id'] for s in samples}) != len(samples):
        raise ValueError("Duplicate gallery component")
    components, cards = [], []
    for i, sample in enumerate(samples):
        name = sample['id']
        source = SOURCE / f"{name}.kt"
        signature, docs = declaration(source.read_text(), name)
        title = re.sub(r"(?<=[a-z])(?=[A-Z])", " ", name)
        item = dict(sample, title=title, description=docs[0], guidance=docs[1:], signature=signature,
                    source=f"animatables/src/commonMain/kotlin/com/singleoriginapps/animatables/{name}.kt")
        components.append(item)
        esc = html.escape
        if (output_dir / f"previews/{name}.jpg").exists():
            preview = f'<img src="previews/{esc(name)}.jpg" alt="" loading="{"eager" if i < 3 else "lazy"}" width="640" height="360">'
        else:
            preview = '<span class="preview-placeholder" aria-hidden="true"><span>▷</span>Play to explore</span>'
        cards.append(f'''<article class="component tone-{esc(sample['tone'])}" data-component="{esc(name)}">
  <div class="card-visual"><button class="card-launch" aria-controls="inline-renderer" data-play="{esc(name)}" aria-label="Play {esc(title)} animation">{preview}<span class="preview-label">COMPOSE PREVIEW</span><span class="open-preview" aria-hidden="true">▶</span></button></div>
  <div class="card-info"><span class="card-meta"><span>{esc(sample['group'])}</span><span class="component-index">{i+1:02d}</span></span><h3>{esc(title)}</h3><p class="component-description">{esc(docs[0])}</p><span class="component-api">{esc(name)}<span aria-hidden="true">()</span></span><div class="card-actions"><button class="card-play" aria-controls="inline-renderer" data-play="{esc(name)}" aria-label="Play {esc(title)} animation">▶ Play</button><button class="card-details" data-details="{esc(name)}" aria-haspopup="dialog" aria-label="API and usage for {esc(title)}">API &amp; usage ↗</button></div></div>
</article>''')
    template = (GALLERY / "index.template.html").read_text()
    output = template.replace("<!-- COMPONENT_CARDS -->", "\n".join(cards)).replace("{{COMPONENT_COUNT}}", str(len(components)))
    for group in {c['group'] for c in components}:
        output = output.replace("{{COUNT_" + group + "}}", str(sum(c['group'] == group for c in components)))
    # Escape HTML-significant characters even though this JSON is an external script.
    payload = json.dumps(components, ensure_ascii=False, indent=2).replace("<", "\\u003c").replace(">", "\\u003e")
    return {output_dir / "index.html": output,
            output_dir / "preview-sources.json": json.dumps({c['id']: preview_hash(c['id']) for c in components}, indent=2) + '\n',
            output_dir / "components-data.js": "// Generated by scripts/generate-gallery.py; edit Kotlin KDoc or gallery-samples.json.\nwindow.galleryComponents = " + payload + ";\n"}


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="fail when generated resources have drifted")
    parser.add_argument("--check-previews", action="store_true", help="also check the source provenance of captured previews")
    parser.add_argument("--output-dir", type=Path, default=OUTPUT, help="generated resource directory (default: build/generated/gallery)")
    args = parser.parse_args()
    for path, content in generate(args.output_dir).items():
        if args.check:
            if not path.exists() or path.read_text() != content:
                raise SystemExit(f"Stale gallery resource: {path.relative_to(ROOT)}")
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content)
    if args.check_previews:
        manifest = json.loads((args.output_dir / "previews/manifest.json").read_text())
        for sample in json.loads((GALLERY / "gallery-samples.json").read_text()):
            name = sample['id']
            if manifest['components'].get(name) != preview_hash(name) or not (args.output_dir / f"previews/{name}.jpg").exists():
                raise SystemExit(f"Stale or missing component preview: {name}")
    print("Gallery content is current." if args.check else "Generated gallery from Kotlin KDoc and signatures.")
