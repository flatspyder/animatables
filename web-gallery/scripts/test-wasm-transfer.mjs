import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { gzipSync } from 'node:zlib';
import vm from 'node:vm';

const source = await readFile(new URL('../src/wasmJsMain/resources/wasm-transfer.js', import.meta.url), 'utf8');
const wasm = Uint8Array.of(0, 97, 115, 109, 1, 0, 0, 0);
const root = 'https://example.test/animatables/';

async function check(name, response, {decompression = true, url = 'sample.wasm', expected = ['sample.wasm.gz']} = {}) {
  const calls = [];
  const window = {
    fetch: async input => {
      const href = input instanceof Request ? input.url : String(input);
      calls.push(href.replace(root, ''));
      return response(href);
    },
    ...(decompression ? {DecompressionStream} : {}),
  };
  vm.runInNewContext(source, {window, location: {href: root, origin: new URL(root).origin}, URL, Request, Response, DecompressionStream});
  const result = await window.fetch(new URL(url, root).href);
  assert.deepEqual(new Uint8Array(await result.arrayBuffer()), wasm, name);
  assert.deepEqual(calls, expected, name);
  // Verify the resulting bytes are still an executable Wasm module.
  await WebAssembly.compile(wasm);
  console.log(`PASS ${name}`);
}

await check('gzip file on an ordinary static host', () => new Response(gzipSync(wasm)));
await check('host already decoded Content-Encoding', () => new Response(wasm, {headers:{'Content-Encoding':'gzip'}}));
await check('gzip header split across network chunks', () => {
  const bytes = gzipSync(wasm);
  return new Response(new ReadableStream({start(controller) {
    controller.enqueue(bytes.subarray(0, 1));
    controller.enqueue(bytes.subarray(1));
    controller.close();
  }}));
});
await check('missing compressed file falls back', href => href.endsWith('.gz') ? new Response('', {status:404}) : new Response(wasm), {expected:['sample.wasm.gz','sample.wasm']});
await check('compression fetch failure falls back', href => {
  if (href.endsWith('.gz')) throw new Error('offline compressed file');
  return new Response(wasm);
}, {expected:['sample.wasm.gz','sample.wasm']});
await check('browser without decompression uses original', () => new Response(wasm), {decompression:false, expected:['sample.wasm']});
await check('ordinary resources are untouched', () => new Response(wasm), {url:'site.js', expected:['site.js']});
await check('cross-origin resources are untouched', () => new Response(wasm), {url:'https://other.test/sample.wasm', expected:['https://other.test/sample.wasm']});
