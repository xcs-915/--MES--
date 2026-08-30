/**
 * DOM Utilities
 * Core DOM manipulation helpers used throughout the application
 */

export const $ = (sel, root = document) => root.querySelector(sel);
export const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];

export const esc = v => String(v ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
export const escVal = v => (v === null || v === undefined || v === '') ? '—' : esc(v);

export const icon = name => `<i data-lucide="${name}"></i>`;

export function renderIcons() {
  window.lucide?.createIcons({ attrs: { 'stroke-width': 1.8 } });
}
