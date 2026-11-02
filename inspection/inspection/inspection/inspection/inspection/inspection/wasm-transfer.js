/* Prefer precompressed Wasm even on hosts (including project Pages) without encoding rules.
   Ordinary resources and older browsers keep native fetch behavior. */
(() => {
  const nativeFetch = window.fetch.bind(window);
  if (!('DecompressionStream' in window)) return;
  window.fetch = async (input, init) => {
    const url = new URL(input instanceof Request ? input.url : String(input), location.href);
    if (url.origin !== location.origin || !url.pathname.endsWith('.wasm') ||
        (input instanceof Request && input.method !== 'GET') || (init?.method && init.method !== 'GET')) {
      return nativeFetch(input, init);
    }
    try {
      const compressedUrl = new URL(url);
      compressedUrl.pathname += '.gz';
      const request = input instanceof Request ? new Request(compressedUrl, input) : compressedUrl;
      const compressed = await nativeFetch(request, init);
      if (!compressed.ok || !compressed.body) return nativeFetch(input, init);
      // Some hosts apply Content-Encoding themselves. Detect bytes after the browser's
      // automatic decoding so the payload is never decompressed twice.
      const [probe, payload] = compressed.body.tee();
      const reader = probe.getReader();
      const prefix = [];
      while (prefix.length < 2) {
        const chunk = await reader.read();
        if (chunk.done) break;
        for (const byte of chunk.value.subarray(0, 2 - prefix.length)) prefix.push(byte);
      }
      reader.cancel().catch(() => {});
      const encoded = prefix[0] === 0x1f && prefix[1] === 0x8b;
      const body = encoded ? payload.pipeThrough(new DecompressionStream('gzip')) : payload;
      return new Response(body, {headers: {'Content-Type': 'application/wasm'}});
    } catch {
      return nativeFetch(input, init);
    }
  };
})();
