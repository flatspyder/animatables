// Browser regression checks against the built gallery. Requires Playwright/Chromium;
// the same PLAYWRIGHT_MODULE and PLAYWRIGHT_CHROMIUM_EXECUTABLE overrides as captures apply.
import assert from 'node:assert/strict';
import { mkdir, readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const gallery = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const samples = JSON.parse(await readFile(path.join(gallery, 'gallery-samples.json'), 'utf8'));
const base = new URL(process.argv[2] || 'http://127.0.0.1:8080/');
const focus = process.argv[3] || 'all';
assert.match(focus,/^(all|spinner)$/,'Optional focus must be all or spinner.');
assert.ok(['http:', 'https:'].includes(base.protocol), 'Serve the current gallery over HTTP.');
const output = path.join(gallery, 'build/generated/gallery/verification');
await mkdir(output, {recursive:true});
const { chromium } = await import(process.env.PLAYWRIGHT_MODULE || 'playwright');
const browser = await chromium.launch({executablePath:process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE || undefined});
try {
  const page = await browser.newPage({viewport:{width:1440,height:1100},deviceScaleFactor:1});
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  await page.goto(base.href);
  assert.equal(await page.locator('article[data-component]').count(),samples.length);
  assert.equal(await page.locator('.card-launch, .open-preview, .card-visual button').count(),0,
    'Static previews must not contain a second play control.');
  for (const image of await page.locator('.card-visual img').all()) {
    assert.ok(await image.getAttribute('alt'), 'Static preview images need descriptive alternatives.');
  }

  // Decode screenshots through the browser instead of comparing PNG encodings or
  // counting static DOM descriptions. The central region must contain visible ink.
  async function pixels(bytes) {
    return page.evaluate(async source => {
      const image = new Image(); image.src = source; await image.decode();
      const canvas = document.createElement('canvas');
      canvas.width=image.width; canvas.height=image.height;
      const context = canvas.getContext('2d'); context.drawImage(image,0,0);
      const {data,width,height} = context.getImageData(0,0,image.width,image.height);
      const bg = (Math.min(5,height-1)*width+Math.min(5,width-1))*4;
      let ink=0, hash=2166136261;
      for (let y=Math.floor(height*.3);y<height*.7;y++) {
        for (let x=Math.floor(width*.15);x<width*.85;x++) {
          const i=(y*width+x)*4;
          if (Math.abs(data[i]-data[bg])+Math.abs(data[i+1]-data[bg+1])+Math.abs(data[i+2]-data[bg+2])>30) ink++;
          for (let c=0;c<3;c++) hash=Math.imul(hash^data[i+c],16777619)>>>0;
        }
      }
      return {ink,hash,width,height};
    }, 'data:image/png;base64,'+bytes.toString('base64'));
  }
  async function shot(frame, name) {
    const bytes = await frame.screenshot(name ? {path:path.join(output,name+'.png')} : {});
    return pixels(bytes);
  }
  async function settled(frame, name) {
    await page.waitForTimeout(1100);
    const signature = await shot(frame,name);
    assert.ok(signature.ink>50,`${name || 'Preview'} is blank at rest (${signature.ink} non-background pixels).`);
    return signature;
  }
  async function open(id) {
    await page.locator(`article[data-component="${id}"] .card-details`).click();
    await page.locator('#runtime.ready').waitFor({state:'attached',timeout:45000});
    await page.frameLocator('#playground').locator(`html[data-rendered-component="${id}"]`).waitFor({state:'attached'});
  }
  async function close() { await page.locator('#close-dialog').click(); }

  // Smoke-test actual pixels after two successive actions for every component.
  // Protocol unit tests alone cannot detect an empty graphics layer.
  for (const {id} of focus==='all'?samples:[]) {
    await open(id);
    await settled(page.locator('#playground'),`${id}-initial`);
    for (let action=1;action<=2;action++) {
      await page.locator('#advance').click();
      await settled(page.locator('#playground'),`${id}-action-${action}`);
    }
    await close();
    console.log(`PASS visible ${id} pixels after repeated detail-page actions`);
  }

  // Four complete cycles in both entry points catch one-shot playback and caches
  // that retain an alpha-zero frame. Compare actual settled target pixels.
  for (const [id,period] of focus==='all'?[['TextStateSwap',3],['IconSwap',2]]:[]) {
    for (const route of ['detail','card']) {
      const card = page.locator(`article[data-component="${id}"]`);
      if (route==='detail') await open(id);
      else {
        await card.locator('.card-play').click();
        await page.locator('#inline-preview.live').waitFor({state:'attached',timeout:45000});
        await page.frameLocator('#inline-renderer').locator(`html[data-rendered-component="${id}"]`).waitFor({state:'attached'});
      }
      const frame=page.locator(route==='detail'?'#playground':'#inline-renderer');
      const button=route==='detail'?page.locator('#advance'):card.locator('.card-play');
      const targets=new Map();
      let previous=await settled(frame,`${id}-${route}-before`);
      targets.set(0,previous.hash);
      for (let action=1;action<=period*4;action++) {
        await button.click();
        if (action===1) {
          await page.waitForTimeout(80);
          await shot(frame,`${id}-${route}-intermediate`);
        }
        const current=await settled(frame,action===1?`${id}-${route}-after`:undefined);
        assert.notEqual(current.hash,previous.hash,`${id} ${route} action ${action} did not change visible content.`);
        const target=action%period;
        if (targets.has(target)) assert.equal(current.hash,targets.get(target),`${id} ${route} failed to return to the same visible target.`);
        else targets.set(target,current.hash);
        previous=current;
      }
      if (route==='detail') {
        for (let replay=1;replay<=2;replay++) {
          await page.locator('#replay').click();
          await page.waitForTimeout(80);
          await shot(frame,`${id}-detail-replay-${replay}-intermediate`);
          const current=await settled(frame,`${id}-detail-replay-${replay}-settled`);
          assert.equal(current.hash,previous.hash,`${id} explicit replay ${replay} changed or lost its resting target.`);
        }
        await close();
      }
      console.log(`PASS ${id} ${route}: four full visible cycles with identical resting targets`);
    }
  }

  await open('CompletionSpinner');
  assert.equal(await page.locator('#spinner-options').isVisible(),true);
  assert.equal(await page.locator('#spinner-style').inputValue(),'standard');
  assert.equal(await page.locator('#spinner-mode').inputValue(),'determinate');
  await page.locator('.motion-preference .switch-label').click();
  assert.equal(await page.locator('#motion').isChecked(),true);
  await page.waitForTimeout(100);
  const standardPending=new Map();
  for (const style of ['standard','wavy']) {
    for (const mode of ['determinate','indeterminate']) {
      await page.locator('#spinner-style').selectOption(style);
      await page.locator('#spinner-mode').selectOption(mode);
      await page.locator('#runtime.ready').waitFor({state:'attached',timeout:45000});
      const frame=page.frameLocator('#playground');
      assert.equal(await frame.getByRole('button').count(),0,'Spinner canvas must contain only the indicator and its label.');
      for (let action=1;action<=4;action++) {
        await page.locator('#advance').click();
        const current=await settled(page.locator('#playground'),`CompletionSpinner-${style}-${mode}-${action}`);
        const label=action%2?'Task complete':mode==='determinate'?'35% complete':'Working…';
        await frame.getByText(label,{exact:true}).waitFor();
        if (action===4) {
          if (style==='standard') standardPending.set(mode,current.hash);
          else assert.notEqual(current.hash,standardPending.get(mode),`${mode} style option did not change visible spinner geometry.`);
        }
      }
    }
  }
  await page.screenshot({path:path.join(output,'spinner-native-detail-options.png')});
  await close();
  const spinnerCard=page.locator('article[data-component="CompletionSpinner"]');
  await spinnerCard.locator('.card-play').click();
  await page.locator('#inline-preview.live').waitFor({state:'attached',timeout:45000});
  await page.frameLocator('#inline-renderer').getByText('Task complete',{exact:true}).waitFor();
  await spinnerCard.locator('.card-play').click();
  await page.frameLocator('#inline-renderer').getByText('35% complete',{exact:true}).waitFor();
  assert.equal(await page.frameLocator('#inline-renderer').getByRole('button').count(),0);
  await settled(page.locator('#inline-renderer'),'spinner-card-default-clean');
  await open('TextStateSwap');
  assert.equal(await page.locator('#spinner-options').isVisible(),false,'Spinner controls belong only to its detail page.');
  await close();
  for (const id of ['SuccessCheck','CompletionSpinner']) {
    await open(id);
    assert.equal(await page.locator('#success-options').isVisible(),true);
    const frame=page.frameLocator('#playground');
    const outcome=id==='SuccessCheck'?'All changes saved':'Task complete';
    if (id==='CompletionSpinner') await page.locator('#advance').click();
    await frame.getByText(outcome,{exact:true}).waitFor();
    await page.locator('#custom-success-icon').uncheck();
    await page.locator('#runtime.ready').waitFor({state:'attached',timeout:45000});
    const defaultIcon=await settled(page.locator('#playground'),`${id}-default-success-icon`);
    await page.locator('#custom-success-icon').check();
    await page.locator('#runtime.ready').waitFor({state:'attached',timeout:45000});
    const customIcon=await settled(page.locator('#playground'),`${id}-custom-success-icon`);
    assert.notEqual(customIcon.hash,defaultIcon.hash,`${id} custom success icon must change rendered pixels.`);
    await frame.getByText(outcome,{exact:true}).waitFor();
    assert.equal(await frame.getByRole('checkbox').count(),0,'Custom icon options belong outside the canvas.');
    await page.locator('#custom-success-icon').uncheck();
    await close();
  }
  assert.deepEqual(errors,[],'The gallery must not emit browser page errors.');
  console.log('PASS native spinner detail options, simple canvas, four completion/reset actions per style/mode, single card play control');
} finally {
  await browser.close();
}
