/**
 * Navigation Layout
 * Renders dynamic menu from backend, handles sidebar logic
 */

import { $, $$, esc, icon, renderIcons } from '../utils/dom.js';
import { state } from '../store/index.js';
import { api } from '../api/request.js';
import { applyPermissions } from '../store/permission.js';

export function navLabel(item) {
  if (state.lang === 'ar-TN') return item.nameAr || item.nameEn || item.nameZh;
  if (state.lang === 'en') return item.nameEn || item.nameZh;
  return item.nameZh;
}

export function renderNavigation(items) {
  const active = items.filter(m => m.status !== 'INACTIVE');
  const byParent = active.reduce((map, item) => {
    const key = item.parentCode || '__root';
    (map[key] = map[key] || []).push(item);
    return map;
  }, {});

  const render = (parent, depth) => (byParent[parent] || []).sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0)).map(item => {
    const children = byParent[item.code] || [];
    const leaf = `<button class="nav-link" data-view="${esc(item.code)}" data-permission="${esc(item.permissionCode || '')}" data-depth="${depth}">${icon(item.icon || 'circle')}<span>${esc(navLabel(item))}</span></button>`;
    if (!children.length) return leaf;
    return `<div class="nav-group ${depth > 0 ? 'nav-group-nested' : ''}" data-group="${esc(item.code)}"><button class="nav-group-toggle" type="button">${icon(item.icon || 'folder')}<span>${esc(navLabel(item))}</span><i class="chevron" data-lucide="chevron-down"></i></button><div class="nav-children">${item.path ? leaf : ''}${render(item.code, depth + 1)}</div></div>`;
  }).join('');

  const html = render('__root', 0);
  if (html) {
    $('#main-nav').innerHTML = html;
    // Auto-expand the first group that contains children on initial load
    // so users immediately see sub-menus exist (especially important
    // when the active view is a top-level item like "overview").
    const firstGroup = document.querySelector('.nav-group');
    if (firstGroup && !document.querySelector('.nav-group:not(.collapsed)')) {
      firstGroup.classList.remove('collapsed');
    }
    renderIcons();
    applyPermissions();
  }
}

export async function loadNavigation() {
  try {
    const response = await api('/navigation');
    renderNavigation(response.data || []);
  } catch (e) {
    /* static fallback remains available */
  }
}
