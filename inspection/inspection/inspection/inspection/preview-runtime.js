"use strict";
// A persistent renderer with an explicit prepare → draw → play handshake.
// Callers may keep sending input while preparation is in flight.
window.GalleryPreview = class GalleryPreview {
  constructor(frame, callbacks = {}) {
    this.frame = frame;
    this.callbacks = callbacks;
    this.connected = false;
    this.prepared = false;
    this.failed = false;
    this.request = 0;
    this.pending = [];
    this.context = null;
    this.step = 0;
    this.replay = 0;
    window.addEventListener("message", event => this.receive(event));
  }
  key(context) {
    return `${context.component}:${context.dark}:${context.tone || ""}:${!!context.spinnerWavy}:${context.spinnerDeterminate !== false}:${!!context.customSuccessIcon}`;
  }
  load() {
    if (this.frame.hasAttribute("src") && !this.failed) return;
    this.failed = false;
    this.connected = false;
    this.prepared = false;
    const url = new URL(this.frame.dataset.src, location.href);
    this.frame.src = url.href;
    this.armTimeout();
  }
  armTimeout() {
    clearTimeout(this.timeout);
    this.timeout = setTimeout(() => this.fail("The live preview could not finish preparing. Try again in a current browser."), 45000);
  }
  prepare(context) {
    const same = this.context && this.key(context) === this.key(this.context);
    if (same && !this.failed) {
      this.context = {...context};
      return;
    }
    const sameComponent = this.context?.component === context.component;
    this.context = {...context};
    if (!sameComponent) {
      this.step = context.initialStep || 0;
      this.replay = 0;
      this.pending = [];
    }
    this.prepared = false;
    this.request++;
    this.callbacks.preparing?.(this.context);
    this.load();
    if (this.connected) this.sendPreparation();
  }
  sendPreparation() {
    this.armTimeout();
    this.post(true, false);
  }
  action(context, action = "advance") {
    this.prepare(context);
    if (!this.prepared) this.pending.push(action);
    else { this.apply(action); this.post(false, context.motion); }
  }
  apply(action) {
    if (action === "replay" || ["TextReveal", "ShimmerText"].includes(this.context.component)) this.replay++;
    else this.step++;
  }
  setMotion(enabled) {
    if (!this.context) return;
    this.context.motion = enabled;
    if (this.connected && this.prepared) this.post(false, enabled);
  }
  post(prepare, motion) {
    this.frame.contentWindow.postMessage({type:"animatables-config", ...this.context,
      step:this.step, replay:this.replay, prepare, motion, request:this.request}, location.origin);
  }
  receive(event) {
    if (event.origin !== location.origin || event.source !== this.frame.contentWindow) return;
    if (event.data?.type === "animatables-ready") {
      this.connected = true;
      if (this.context) this.sendPreparation();
    }
    if (event.data?.type === "animatables-prepared" && event.data.request === this.request &&
        event.data.component === this.context?.component && !this.prepared) {
      clearTimeout(this.timeout);
      this.prepared = true;
      const hadInput = this.pending.length > 0;
      for (const action of this.pending) this.apply(action);
      this.pending = [];
      this.callbacks.prepared?.(this.context);
      // Two drawn frames of the resting state have completed before advancing the clock.
      if (hadInput) this.post(false, this.context.motion);
    }
    if (event.data?.type === "animatables-error") this.fail("The Compose renderer could not start. Static previews and API guidance remain available.");
  }
  fail(message) {
    clearTimeout(this.timeout);
    this.connected = false;
    this.prepared = false;
    this.failed = true;
    this.callbacks.error?.(message);
  }
};
