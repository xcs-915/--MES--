/**
 * Toolbar Components
 * Page header, buttons, filter fields, and toolbar layout
 */

import { esc, icon } from '../utils/dom.js';
import { t } from '../i18n/index.js';

export function pageHead(section, title, subtitle, actions = '') {
  return `<div class="page-head"><div><h1>${esc(title)}</h1>${subtitle ? `<p class="page-subtitle">${esc(subtitle)}</p>` : ''}</div><div class="page-actions">${actions}</div></div>`;
}

export function btn(action, label, kind = 'secondary', permission = '') {
  return `<button class="btn ${kind}" data-action="${action}"${permission ? ` data-permission="${permission}"` : ''}>${label}</button>`;
}

export function filterField(id, label, type, options, placeholder) {
  if (type === 'select') {
    return `<label><span>${esc(label)}</span><select id="${id}">${options.map(o => typeof o === 'string' ? `<option value="${esc(o)}">${esc(o)}</option>` : `<option value="${esc(o.value)}">${esc(o.label)}</option>`).join('')}</select></label>`;
  }
  return `<label><span>${esc(label)}</span><input id="${id}" type="${type || 'text'}" placeholder="${esc(placeholder || '')}"></label>`;
}

export function toolbar(fields, extraActions) {
  const defaults = btn('query', icon('search') + t('query'), 'primary') + btn('reset', icon('rotate-ccw') + t('reset')) + btn('refresh-data', icon('refresh-cw') + t('refresh'));
  const actions = defaults + (extraActions || '');
  return `<div class="toolbar">${fields.join('')}<div class="toolbar-actions">${actions}</div></div>`;
}
