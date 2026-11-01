// Optional build-time snapshots. Requires Playwright in web-gallery/node_modules.
// The gallery itself has no npm runtime dependency on Playwright.
import { createHash } from 'node:crypto';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const gallery = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const samples = JSON.parse(await readFile(path.join(gallery, 'gallery-samples.json'), 'utf8'));
const resources = path.join(gallery, 'src/wasmJsMain/resources');
const root = path.dirname(gallery);
const base = new URL(process.argv[2] || 'http://127.0.0.1:8080/');
if (!['http:', 'https:'].includes(base.protocol)) throw new Error('Serve the built gallery over HTTP.');
const { chromium } = await import('playwright');
const browser = await chromium.launch();
const output = path.join(gallery, 'build/generated/gallery/previews');
const manifest = { viewport: { width: 640, height: 360 }, motion: false, components: {} };
try {
  const provenance = await fetch(new URL('preview-sources.json', base));
  if (!provenance.ok) throw new Error('Build and serve the current gallery before capturing previews.');
  const builtSources = await provenance.json();
  await mkdir(output, { recursive: true });
  const page = await browser.newPage({ viewport: manifest.viewport, deviceScaleFactor: 1,
    colorScheme: 'light', reducedMotion: 'reduce' });
  for (const { id } of samples) {
    const hash = createHash('sha256');
    for (const file of [
      path.join(root, `animatables/src/commonMain/kotlin/com/singleoriginapps/animatables/${id}.kt`),
      path.join(gallery, 'src/wasmJsMain/kotlin/Gallery.kt'),
      path.join(resources, 'playground.html'),
    ]) hash.update(await readFile(file));
    const digest = hash.digest('hex');
    if (builtSources[id] !== digest) throw new Error(`Built source differs for ${id}. Rebuild before capturing.`);
    const url = new URL('playground.html', base);
    url.searchParams.set('component', id);
    url.searchParams.set('motion', 'false');
    await page.goto(url.href);
    await page.locator(`html[data-rendered-component="${id}"]`).waitFor({ state: 'attached', timeout: 45000 });
    await page.screenshot({ path: path.join(output, `${id}.jpg`), type: 'jpeg', quality: 90 });
    manifest.components[id] = digest;
    console.log(`Captured ${id}`);
  }
  await writeFile(path.join(output, 'manifest.json'), JSON.stringify(manifest, null, 2) + '\n');
} finally {
  await browser.close();
}
