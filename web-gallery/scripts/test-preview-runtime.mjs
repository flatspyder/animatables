import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';

const source = readFileSync(new URL('../src/wasmJsMain/resources/preview-runtime.js', import.meta.url), 'utf8');
function harness() {
  const messages = [], callbacks = [], listeners = [];
  let loads = 0, src;
  const origin = 'https://gallery.test';
  const frame = {dataset:{src:'playground.html'},hasAttribute:()=>!!src,
    set src(value) { src=value;loads++; }, contentWindow:{postMessage:data=>messages.push(data)}};
  const window = {addEventListener:(_, listener)=>listeners.push(listener)};
  const scope = {window,location:{origin,href:origin+'/',search:''},URL,URLSearchParams,
    setTimeout:()=>1,clearTimeout:()=>{}};
  vm.runInNewContext(source, scope);
  const runtime = new window.GalleryPreview(frame, {prepared:()=>callbacks.push('prepared')});
  const receive = (data, overrides={}) => listeners.forEach(f=>f({origin,source:frame.contentWindow,data,...overrides}));
  return {runtime,messages,callbacks,receive,loads:()=>loads};
}
const number = {component:'NumberPopIn',dark:false,tone:'peach',motion:true};
const icon = {component:'IconSwap',dark:false,tone:'green',motion:true};
const ready = h=>h.receive({type:'animatables-ready'});
const prepared = h=>h.receive({type:'animatables-prepared',component:h.runtime.context.component,request:h.runtime.request});

{
  const h=harness();
  h.runtime.action(number);h.runtime.action(number);ready(h);
  assert.equal(h.messages.at(-1).prepare,true);
  assert.equal(h.messages.at(-1).motion,false);
  assert.equal(h.messages.at(-1).step,0);
  prepared(h);
  assert.equal(h.messages.at(-1).step,2,'input during preparation must not be dropped');
  assert.equal(h.messages.at(-1).prepare,false);
  assert.equal(h.messages.at(-1).motion,true);
  assert.equal(h.callbacks.length,1);
  prepared(h);assert.equal(h.callbacks.length,1,'duplicate readiness must not replay input');
}
{
  const h=harness();h.runtime.action(number);ready(h);
  const stale=h.runtime.request;
  h.runtime.action(icon);
  h.receive({type:'animatables-prepared',component:number.component,request:stale});
  assert.equal(h.runtime.prepared,false,'superseded preparation cannot reveal the wrong card');
  prepared(h);assert.equal(h.messages.at(-1).component,'IconSwap');
  assert.equal(h.messages.at(-1).step,1);
  assert.equal(h.loads(),1,'switching cards must reuse the browsing context');
}
{
  const h=harness();h.runtime.prepare(number);
  h.receive({type:'animatables-ready'},{origin:'https://other.test'});
  assert.equal(h.runtime.connected,false);
  ready(h);prepared(h);h.runtime.action({...number,motion:false});
  assert.equal(h.messages.at(-1).motion,false,'reduced motion must reach the renderer');
  h.runtime.setMotion(false);assert.equal(h.messages.at(-1).motion,false);
}
{
  const h=harness();h.runtime.action(number);ready(h);prepared(h);
  h.runtime.prepare({...number,dark:true});ready(h);prepared(h);
  assert.equal(h.runtime.step,1,'theme preparation must preserve sample state');
  assert.equal(h.loads(),1);
  h.runtime.fail('test');h.runtime.action({...number,dark:true});
  assert.equal(h.loads(),2,'retry must replace the failed context');
}
console.log('PASS queued input, stale responses, persistent renderer, reduced motion, theme state and retry');
