/**
 * Drawer Component
 * Slide-out panel for details and forms
 * Uses direct event binding (not document-level delegation) for reliable close behavior
 */

import { esc, icon, renderIcons, $ } from '../utils/dom.js';

export function openDrawer(title, subtitle, content) {
  const root = $('#overlay-root');
  root.innerHTML = `<div class="drawer-backdrop"><aside class="drawer" data-stop-close><div class="drawer-header"><div><h2>${esc(title)}</h2><p>${esc(subtitle || '')}</p></div><button class="icon-btn" data-action="close-drawer">${icon('x')}</button></div>${content}</aside></div>`;

  // Direct click handler on backdrop — closes when clicking backdrop or any close-drawer button
  const backdrop = root.querySelector('.drawer-backdrop');
  if (backdrop) {
    backdrop.addEventListener('click', e => {
      // Close if clicked directly on backdrop (outside drawer)
      if (e.target === backdrop) { closeDrawer(); return; }
      // Close if clicked a close button (X icon or Cancel/Save-dict-type buttons with data-action="close-drawer")
      const btn = e.target.closest('[data-action="close-drawer"]');
      if (btn && btn !== backdrop) { closeDrawer(); return; }
    });
  }

  renderIcons();
}

export function closeDrawer() {
  $('#overlay-root').innerHTML = '';
}
