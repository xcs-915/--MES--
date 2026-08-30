/**
 * Table Components
 * Data table with sortable headers and pagination
 */

import { esc, icon } from '../utils/dom.js';
import { t } from '../i18n/index.js';

export function dataTable(headers, rows, sortKey, sortDir) {
  const thead = headers.map((h, i) => {
    const key = typeof h === 'object' ? h.key : null;
    const label = typeof h === 'object' ? h.label : h;
    const sortable = typeof h === 'object' && h.sortable;
    if (!sortable) return `<th>${esc(label)}</th>`;
    const active = sortKey === key;
    const dir = active && sortDir === 'desc' ? 'asc' : 'desc';
    return `<th class="sortable ${active ? 'sort-' + sortDir : ''}" data-sort="${key}" data-next-dir="${dir}">${esc(label)}${active ? icon(sortDir === 'asc' ? 'arrow-up' : 'arrow-down') : icon('arrow-up-down')}</th>`;
  }).join('');
  return `<div class="table-wrap"><table class="data-table"><thead><tr>${thead}</tr></thead><tbody>${rows.length ? rows.join('') : `<tr><td colspan="${headers.length}" class="empty">${t('noData')}</td></tr>`}</tbody></table></div>`;
}

export function paginationHTML(ps) {
  if (!ps) return '';
  const cur = ps.page || 0;
  const total = ps.totalPages || 1;
  // Always show pagination bar with total count and page size selector
  const pages = [];
  if (total > 1) {
    for (let i = 0; i < total; i++) {
      if (i === 0 || i === total - 1 || Math.abs(i - cur) <= 1) pages.push(i);
      else if (pages[pages.length - 1] !== -1) pages.push(-1);
    }
  }
  const start = ps.total === 0 ? 0 : cur * ps.size + 1;
  const end = Math.min((cur + 1) * ps.size, ps.total);
  const pageButtons = total > 1 ? `<button data-page="${Math.max(0, cur - 1)}" ${cur === 0 ? 'disabled' : ''}>${icon('chevron-left')}</button>${pages.map(p => p === -1 ? '<span class="ellipsis">…</span>' : `<button data-page="${p}" class="${p === cur ? 'active' : ''}">${p + 1}</button>`).join('')}<button data-page="${Math.min(total - 1, cur + 1)}" ${cur >= total - 1 ? 'disabled' : ''}>${icon('chevron-right')}</button>` : '';
  return `<div class="pagination"><span class="total">${t('total')} <strong>${ps.total}</strong> ${t('items')} · ${start}-${end}</span>${pageButtons}<select data-page-size><option value="10">10 ${t('perPage')}</option><option value="20" ${ps.size === 20 ? 'selected' : ''}>20 ${t('perPage')}</option><option value="50" ${ps.size === 50 ? 'selected' : ''}>50 ${t('perPage')}</option><option value="100">100 ${t('perPage')}</option></select></div>`;
}
