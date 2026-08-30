/**
 * Feedback Components
 * Stat cards, detail grids, section titles, progress bars, status pills
 */

import { esc, escVal, icon } from '../utils/dom.js';
import { t } from '../i18n/index.js';

export function statCard(label, value, iconName) {
  return `<div class="stat-card"><small>${esc(label)}</small><strong>${esc(value)}</strong><span class="stat-icon">${icon(iconName)}</span></div>`;
}

/** Key info card: prominent summary of critical fields at the top of detail views */
export function keyInfoCard(items) {
  const cards = items.map(({ label, value, icon: iconName }) => {
    const display = (typeof value === 'string' && value.startsWith('<')) ? value : escVal(value);
    return `<div class="key-info-item">${iconName ? `<span class="key-info-icon">${icon(iconName)}</span>` : ''}<div><small>${esc(label)}</small><strong>${display || '—'}</strong></div></div>`;
  }).join('');
  return `<div class="key-info-card">${cards}</div>`;
}

export function detailGrid(fields) {
  return `<div class="detail-grid">${fields.map(([label, v]) => `<div class="detail-item"><small>${esc(label)}</small><strong>${typeof v === 'string' && v.startsWith('<') ? v : escVal(v)}</strong></div>`).join('')}</div>`;
}

/** Grouped detail grid: renders multiple sections with titled field groups */
export function groupedDetailGrid(sections) {
  return sections.map(section => {
    const fields = section.fields || [];
    const grid = fields.length ? `<div class="detail-grid">${fields.map(([label, v]) => `<div class="detail-item"><small>${esc(label)}</small><strong>${typeof v === 'string' && v.startsWith('<') ? v : escVal(v)}</strong></div>`).join('')}</div>` : `<div class="empty">${esc(t('noData'))}</div>`;
    return `<div class="detail-section"><div class="section-title"><h3>${esc(section.title)}</h3></div>${grid}</div>`;
  }).join('');
}

export function sectionTitle(title, count) {
  return `<div class="section-title"><h2>${esc(title)}</h2><span>${count}</span></div>`;
}

export function emptyState(msg) {
  return `<div class="empty">${esc(msg || t('noData'))}</div>`;
}

export function progressBar(value, total) {
  const pct = total ? Math.round(Number(value || 0) / Number(total) * 100) : 0;
  return `<span class="progress-bar"><span class="bar"><span class="fill" style="width:${pct}%"></span></span><span class="pct">${pct}%</span></span>`;
}

export function statusPill(value) {
  const label = t('status.' + value) || value;
  const cls = ['ACTIVE','RELEASED','IN_PROGRESS','COMPLETED','SUCCESS','IDLE','ENABLED','UNREST'].includes(String(value)) ? 'success'
    : ['DRAFT','RUNNING','PARTIAL'].includes(String(value)) ? 'warn'
    : ['FAILED','CANCELLED','DISABLED','INACTIVE','RESTRICTED'].includes(String(value)) ? 'danger' : '';
  return `<span class="status ${cls}">${esc(label || '—')}</span>`;
}
