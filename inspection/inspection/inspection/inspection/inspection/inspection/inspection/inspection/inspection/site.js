"use strict";
const components = window.galleryComponents;
const byId = id => document.getElementById(id);
const frame = byId("playground");
const dialog = byId("component-dialog");
const prefersReduced = window.matchMedia("(prefers-reduced-motion: reduce)");
const prefersDark = window.matchMedia("(prefers-color-scheme: dark)");
let selected = components[0];
let filter = "All";
let copyTimeout;
let userReduceMotion = false;
let dark = prefersDark.matches;
let explicitTheme = false;
let opener;
let inlineCard;
let positionFrame;
const inlineHost = byId("inline-preview");
const inlineFrame = byId("inline-renderer");
const cardContext = item => ({component:item.id, dark, tone:item.tone,
  initialStep:item.id === "SuccessCheck" ? 1 : 0,
  spinnerWavy:false, spinnerDeterminate:true, customSuccessIcon:false,
  motion:!byId("motion").checked});
const supportsSuccessIcon = id => ["SuccessCheck", "CompletionSpinner"].includes(id);
const modalContext = () => ({component:selected.id, dark, tone:"", initialStep:0,
  spinnerWavy:selected.id === "CompletionSpinner" && byId("spinner-style").value === "wavy",
  spinnerDeterminate:selected.id !== "CompletionSpinner" || byId("spinner-mode").value === "determinate",
  customSuccessIcon:supportsSuccessIcon(selected.id) && byId("custom-success-icon").checked,
  motion:dialog.open && !byId("motion").checked});
const inlinePreview = new window.GalleryPreview(inlineFrame, {
  preparing() {
    inlineHost.classList.remove("live");
    inlineHost.setAttribute("aria-hidden", "true");
    inlineFrame.tabIndex = -1;
    if (inlineCard) {
      inlineCard.setAttribute("aria-busy", "true");
      inlineCard.querySelector(".card-play").textContent = "Preparing…";
      byId("preview-status").textContent = "Preparing the live component preview.";
    }
  },
  prepared() { if (inlineCard) revealInline(); },
  error(message) {
    stopInline();
    byId("preview-status").textContent = message;
  },
});
const modalPreview = new window.GalleryPreview(frame, {
  preparing() {
    byId("loading").hidden = false;
    byId("loading-message").textContent = "Preparing the Compose playground…";
    byId("retry").hidden = true;
    byId("runtime").textContent = "Preparing preview…";
    byId("runtime").classList.remove("ready");
    byId("advance").disabled = true;
    byId("replay").disabled = true;
  },
  prepared() {
    byId("loading").hidden = true;
    byId("runtime").textContent = "Live Compose preview";
    byId("runtime").classList.add("ready");
    byId("advance").disabled = false;
    byId("replay").disabled = false;
    modalPreview.setMotion(dialog.open && !byId("motion").checked);
  },
  error(message) {
    byId("loading").hidden = false;
    byId("loading-message").textContent = message;
    byId("runtime").textContent = "Preview unavailable";
    byId("runtime").classList.remove("ready");
    byId("retry").hidden = false;
    byId("advance").disabled = true;
    byId("replay").disabled = true;
  },
});
try {
  const saved = localStorage.getItem("animatables-theme");
  if (saved === "dark" || saved === "light") { dark = saved === "dark"; explicitTheme = true; }
} catch { /* Browser storage is optional. */ }
function syncMotionControl() {
  byId("motion").checked = prefersReduced.matches || userReduceMotion;
  byId("motion").disabled = prefersReduced.matches;
  byId("gallery-motion").checked = byId("motion").checked;
  byId("gallery-motion").disabled = prefersReduced.matches;
  byId("motion-note").textContent = prefersReduced.matches ? "Requested by your device." :
    userReduceMotion ? "Target states appear directly." : "Follows your device preference.";
}
function configure() {
  document.documentElement.classList.toggle("dark", dark);
  byId("theme").setAttribute("aria-pressed", String(dark));
  byId("theme").setAttribute("aria-label", `Switch to ${dark ? "light" : "dark"} theme`);
  document.querySelector('meta[name="theme-color"]').content = dark ? "#201b19" : "#fff8f4";
  if (dialog.open) modalPreview.prepare(modalContext());
  modalPreview.setMotion(dialog.open && !byId("motion").checked);
  if (inlineCard) {
    const item = components.find(c => c.id === inlineCard.dataset.component);
    inlinePreview.prepare(cardContext(item));
    inlinePreview.setMotion(!dialog.open && !byId("motion").checked);
  }
}
function choose(id, navigate = true) {
  const component = components.find(item => item.id === id);
  if (!component) return;
  selected = component;
  stopInline();
  const index = components.indexOf(component);
  byId("selected-name").textContent = component.title;
  byId("selected-description").textContent = component.description;
  byId("selected-group").textContent = component.group;
  byId("selected-position").textContent = `${String(index + 1).padStart(2, "0")} / ${components.length}`;
  byId("advance").textContent = `${component.action} ↗`;
  byId("code").textContent = component.code;
  byId("signature").textContent = component.signature;
  byId("guidance").replaceChildren(...component.guidance.map(text => {
    const paragraph = document.createElement("p"); paragraph.textContent = text; return paragraph;
  }));
  byId("source").href = `https://github.com/flatspyder/animatables/blob/main/${component.source}`;
  byId("replay").hidden = !component.replay;
  byId("spinner-options").hidden = component.id !== "CompletionSpinner";
  byId("success-options").hidden = !supportsSuccessIcon(component.id);
  byId("api-details").open = false;
  byId("copy").textContent = "Copy code";
  clearTimeout(copyTimeout);
  if (!dialog.open) {
    opener = document.activeElement;
    dialog.showModal();
    byId("close-dialog").focus();
    if (navigate) history.pushState(null, "", `#${id}`);
  } else if (navigate) history.replaceState(null, "", `#${id}`);
  dialog.scrollTop = 0;
  configure();
}
function search() {
  stopInline();
  const query = byId("search").value.trim().toLowerCase();
  let count = 0;
  document.querySelectorAll("[data-component]").forEach(button => {
    const item = components.find(item => item.id === button.dataset.component);
    const show = (filter === "All" || item.group === filter) &&
      `${item.id} ${item.title} ${item.description}`.toLowerCase().includes(query);
    button.hidden = !show;
    if (show) count++;
  });
  byId("result-count").textContent = `${count} ${count === 1 ? "component" : "components"}${filter === "All" && !query ? " to make your own" : " found"}`;
  byId("empty").hidden = count > 0;
}
function positionInline(immediate = false) {
  cancelAnimationFrame(positionFrame);
  const place = () => {
    if (!inlineCard || inlineCard.hidden) return;
    const rect = inlineCard.querySelector(".card-visual").getBoundingClientRect();
    const scale = Math.min(rect.width / 640, rect.height / 360);
    Object.assign(inlineHost.style, {left:`${rect.left + window.scrollX}px`, top:`${rect.top + window.scrollY}px`,
      width:`${rect.width}px`, height:`${rect.height}px`,
      backgroundColor:dark ? getComputedStyle(document.documentElement).getPropertyValue("--stage").trim() :
        getComputedStyle(inlineCard).getPropertyValue("--preview-bg").trim()});
    Object.assign(inlineFrame.style, {left:`${(rect.width - 640 * scale) / 2}px`,
      top:`${(rect.height - 360 * scale) / 2}px`, transform:`scale(${scale})`});
  };
  if (immediate === true) place();
  else positionFrame = requestAnimationFrame(place);
}
function revealInline() {
  if (!inlineCard || inlinePreview.context.component !== inlineCard.dataset.component) return;
  positionInline(true);
  inlineHost.classList.add("live");
  inlineHost.setAttribute("aria-hidden", "false");
  inlineFrame.tabIndex = 0;
  inlineCard.removeAttribute("aria-busy");
  const item = components.find(c => c.id === inlineCard.dataset.component);
  inlineCard.querySelector(".card-play").textContent = item.action;
  inlineCard.querySelector(".card-play").setAttribute("aria-label", `${item.action} for ${item.title}`);
  inlineFrame.title = `Interactive ${item.title} card`;
  byId("preview-status").textContent = `${item.title} is playing on its card.`;
}
function stopInline() {
  inlinePreview.setMotion(false);
  inlineHost.classList.remove("live");
  inlineHost.setAttribute("aria-hidden", "true");
  inlineFrame.tabIndex = -1;
  inlineHost.removeAttribute("style");
  if (inlineCard) {
    byId("preview-status").textContent = "Preview stopped.";
    inlineCard.classList.remove("playing");
    inlineCard.removeAttribute("aria-busy");
    inlineCard.querySelector(".card-play").textContent = "▶ Play";
    const item = components.find(c => c.id === inlineCard.dataset.component);
    inlineCard.querySelector(".card-play").setAttribute("aria-label", `Play ${item.title} animation`);
  }
  inlineCard = null;
}
function playInline(id) {
  const item = components.find(c => c.id === id);
  const card = document.querySelector(`[data-component="${id}"]`);
  if (inlineCard !== card) stopInline();
  inlineCard = card;
  inlineCard.classList.add("playing");
  positionInline(true);
  inlinePreview.action(cardContext(item));
  if (inlinePreview.prepared) revealInline();
}
document.querySelectorAll("[data-play]").forEach(button => button.addEventListener("click", () => playInline(button.dataset.play)));
document.querySelectorAll("[data-details]").forEach(button => button.addEventListener("click", () => choose(button.dataset.details)));
// Prepare during discovery without starting an animation or taking over an active card.
document.querySelectorAll("[data-component]").forEach(card => {
  const prepare = () => {
    if (!inlineCard && !dialog.open) inlinePreview.prepare(cardContext(components.find(c => c.id === card.dataset.component)));
  };
  card.addEventListener("pointerenter", prepare);
  card.addEventListener("focusin", prepare);
});
window.addEventListener("resize", positionInline);
document.fonts?.ready.then(positionInline);
new ResizeObserver(positionInline).observe(document.querySelector(".component-grid"));
const visibility = new IntersectionObserver(entries => {
  if (entries.some(entry => entry.target === inlineCard && !entry.isIntersecting)) stopInline();
});
document.querySelectorAll("[data-component]").forEach(card => visibility.observe(card));
document.addEventListener("visibilitychange", () => { if (document.hidden) stopInline(); });
const preload = new IntersectionObserver(entries => {
  if (entries.some(entry => entry.isIntersecting)) {
    if (!inlineCard) inlinePreview.prepare(cardContext(components[0]));
    preload.disconnect();
  }
}, {rootMargin:"400px"});
preload.observe(byId("gallery"));
document.querySelectorAll("[data-filter]").forEach(button => {
  button.querySelector("span").textContent = button.dataset.filter === "All" ? components.length : components.filter(c => c.group === button.dataset.filter).length;
  button.addEventListener("click", () => {
    filter = button.dataset.filter;
    document.querySelectorAll("[data-filter]").forEach(item => item.setAttribute("aria-pressed", String(item === button)));
    search();
  });
});
byId("search").addEventListener("input", search);
byId("clear-search").addEventListener("click", () => {
  byId("search").value = "";
  document.querySelector('[data-filter="All"]').click();
  byId("search").focus();
});
document.addEventListener("keydown", event => {
  if (event.key === "/" && !dialog.open && !["INPUT", "TEXTAREA", "SELECT"].includes(document.activeElement.tagName) && !event.ctrlKey && !event.metaKey && !event.altKey) {
    event.preventDefault(); byId("search").focus();
  }
});
byId("advance").addEventListener("click", () => {
  modalPreview.action(modalContext());
});
byId("replay").addEventListener("click", () => modalPreview.action(modalContext(), "replay"));
for (const id of ["spinner-style", "spinner-mode", "custom-success-icon"]) {
  byId(id).addEventListener("change", () => {
    if (dialog.open && supportsSuccessIcon(selected.id)) modalPreview.prepare(modalContext());
  });
}
byId("theme").addEventListener("click", () => {
  dark = !dark; explicitTheme = true;
  try { localStorage.setItem("animatables-theme", dark ? "dark" : "light"); } catch { /* Optional persistence. */ }
  configure();
});
prefersDark.addEventListener("change", () => { if (!explicitTheme) { dark = prefersDark.matches; configure(); } });
byId("motion").addEventListener("change", () => { userReduceMotion = byId("motion").checked; syncMotionControl(); configure(); });
byId("gallery-motion").addEventListener("change", () => { userReduceMotion = byId("gallery-motion").checked; syncMotionControl(); configure(); });
prefersReduced.addEventListener("change", () => { syncMotionControl(); configure(); });
byId("copy").addEventListener("click", async () => {
  const copied = selected.id;
  try {
    await navigator.clipboard.writeText(selected.code);
    if (selected.id !== copied) return;
    byId("copy").textContent = "Copied ✓";
    byId("copy-status").textContent = `${selected.id} example copied.`;
  } catch {
    const selection = window.getSelection(); const range = document.createRange();
    range.selectNodeContents(byId("code")); selection.removeAllRanges(); selection.addRange(range);
    byId("copy").textContent = "Code selected";
    byId("copy-status").textContent = "Copy unavailable. The code is selected for manual copying.";
  }
  clearTimeout(copyTimeout);
  copyTimeout = setTimeout(() => { byId("copy").textContent = "Copy code"; }, 2000);
});
byId("retry").addEventListener("click", () => {
  byId("loading-message").textContent = "Preparing the Compose playground…";
  byId("runtime").textContent = "Loading preview…";
  byId("retry").hidden = true;
  modalPreview.prepare(modalContext());
});
byId("close-dialog").addEventListener("click", () => dialog.close());
dialog.addEventListener("click", event => { if (event.target === dialog) {
  const bounds = dialog.getBoundingClientRect();
  if (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom) dialog.close();
} });
dialog.addEventListener("close", () => {
  if (components.some(item => `#${item.id}` === location.hash)) history.replaceState(null, "", "#gallery");
  configure();
  if (opener?.isConnected) opener.focus({ preventScroll: true });
});
byId("previous").addEventListener("click", () => choose(components[(components.indexOf(selected) + components.length - 1) % components.length].id));
byId("next").addEventListener("click", () => choose(components[(components.indexOf(selected) + 1) % components.length].id));
function route() {
  const id = location.hash.slice(1);
  if (components.some(item => item.id === id)) choose(id, false);
  else if (dialog.open) dialog.close();
}
window.addEventListener("hashchange", route);
syncMotionControl(); configure(); route();
