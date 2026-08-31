/**
 * API Logs View
 * Read-only log of every SAP interface call: request, response and duration.
 */

import { $, esc, escVal, icon, renderIcons } from '../utils/dom.js';
import { state, ps } from '../store/index.js';
import { api, getMenuConfig } from '../api/request.js';
import { t } from '../i18n/index.js';
import { pageHead, btn, filterField, toolbar } from '../components/toolbar.js';
import { dataTable, paginationHTML } from '../components/table.js';
import { detailGrid, emptyState, statusPill, progressBar, sectionTitle } from '../components/feedback.js';
import { openDrawer, closeDrawer } from '../components/drawer.js';
import { toast } from '../utils/ui.js';
import { formatDate, readPath, localizedMeta } from '../utils/format.js';

export function renderApiLogs() {
  const fields = [
    filterField('log-search', t('search'), 'text', null, t('endpoint')),
    filterField('log-system', t('system'), 'select', [
      { value: '', label: t('all') },
      { value: 'SAP', label: 'SAP' }
    ])
  ];
  $('#page').innerHTML = pageHead(t('integrationCenter'), t('apiLogs'), t('apiLogsSubtitle')) + `<div class="panel">${toolbar(fields)}<div id="log-table"></div></div>`;
  loadApiLogs();
}

export async function loadApiLogs(page) {
  const p = ps('apiLogs'), node = $('#log-table');
  if (page !== undefined) p.page = page;
  if (!node) return;
  try {
    const params = new URLSearchParams({ page: String(p.page), size: String(p.size) });
    const ep = $('#log-search')?.value;
    const sys = $('#log-system')?.value;
    if (ep) params.set('endpoint', ep);
    if (sys) params.set('system', sys);
    const data = await api('/integrations/logs?' + params);
    p.items = data.data.items || [];
    p.total = data.data.total || 0;
    p.totalPages = data.data.totalPages || 0;
    node.innerHTML = dataTable(
      ['#', t('systemName'), t('httpMethod'), t('endpoint'), t('status'), `${t('duration')}(ms)`, t('lastSync'), t('actions')],
      p.items.map((v, i) => `<tr><td>${p.page * p.size + i + 1}</td><td>${statusPill(v.systemCode)}</td><td><span class="status ${v.success ? 'success' : 'danger'}">${escVal(v.httpMethod)}</span></td><td class="code" style="max-width:300px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${esc(v.endpoint || '')}">${escVal(v.endpoint)}</td><td>${statusPill(v.responseStatus)}</td><td>${escVal(v.durationMs)}</td><td>${formatDate(v.createdAt)}</td><td class="table-actions">${btn('log-detail', icon('eye'), 'ghost')}</td></tr>`)
    ) + paginationHTML(p);
  } catch (e) {
    node.innerHTML = emptyState(e.message);
  }
  renderIcons();
}

export function openLogDetail(log) {
  openDrawer(`API Log · ${log.id}`, log.endpoint || '',
    `<div class="drawer-body">${detailGrid([
      [t('httpMethod'), log.httpMethod],
      [t('systemName'), log.systemCode],
      [t('status'), statusPill(log.responseStatus)],
      [`${t('duration')}(ms)`, log.durationMs + ' ms'],
      [t('lastSync'), formatDate(log.createdAt)],
      [t('details'), log.success ? '<span class="status success">SUCCESS</span>' : '<span class="status danger">FAILED</span>']
    ])}${log.errorMessage ? `<div class="section-title"><h2>${t('error')}</h2></div><pre style="white-space:pre-wrap;background:#FEF2F2;padding:12px;border-radius:8px;font-size:12px;color:#DC2626;max-height:200px;overflow:auto">${esc(log.errorMessage)}</pre>` : ''}
      <div class="section-title"><h2>${t('requestParams')}</h2></div>
      <pre style="white-space:pre-wrap;background:#F8FAFC;padding:12px;border-radius:8px;font-size:12px;max-height:200px;overflow:auto">${esc(log.requestParams || '{}')}</pre>
      ${log.requestBody ? `<div class="section-title"><h2>${t('requestBody')}</h2></div><pre style="white-space:pre-wrap;background:#F8FAFC;padding:12px;border-radius:8px;font-size:12px;max-height:200px;overflow:auto">${esc(log.requestBody)}</pre>` : ''}
      <div class="section-title"><h2>${t('response')}</h2></div>
      <pre style="white-space:pre-wrap;background:#F0FDF4;padding:12px;border-radius:8px;font-size:12px;max-height:300px;overflow:auto">${esc(log.responseBody || '—')}</pre>
      </div>`);
}
