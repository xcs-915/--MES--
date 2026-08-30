/**
 * Scheduled Jobs View
 * SAP synchronization jobs: schedules, endpoints, run history and failures.
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

export function renderJobs() {
  const fields = [
    filterField('job-search', t('search'), 'text', null, t('jobCode') + '/' + t('name')),
    filterField('job-status', t('status'), 'select', [
      { value: '', label: t('all') },
      { value: 'ENABLED', label: t('enabled') },
      { value: 'DISABLED', label: t('disabled') },
      { value: 'SUCCESS', label: 'SUCCESS' },
      { value: 'FAILED', label: 'FAILED' }
    ])
  ];
  $('#page').innerHTML = pageHead(t('integrationCenter'), t('scheduledJobs'), t('jobSubtitle')) + `<div class="panel">${toolbar(fields)}<div id="job-table"></div></div>`;
  loadJobs();
}

export async function loadJobs() {
  const node = $('#job-table');
  if (!node) return;
  try {
    const params = new URLSearchParams();
    const kw = $('#job-search')?.value, st = $('#job-status')?.value;
    if (kw) params.set('keyword', kw);
    if (st) params.set('status', st);
    const data = await api('/integrations/sync-jobs?' + params);
    const items = data.data || [];
    state.data.jobs = items;
    node.innerHTML = items.length ? dataTable(
      [t('jobCode'), t('name'), t('system'), t('cron'), t('endpoint'), t('status'), t('lastRun'), t('nextRun'), t('actions')],
      items.map(v => `<tr><td class="code">${escVal(v.code)}</td><td><span class="cell-title">${escVal(v.nameZh)}</span><span class="cell-sub">${escVal(v.nameEn || v.nameAr)}</span></td><td>${escVal(v.systemCode)}</td><td class="code">${escVal(v.cronExpression)}</td><td class="code">${escVal(v.endpoint)}</td><td>${statusPill(v.status)}</td><td>${formatDate(v.lastRunAt)}</td><td>${formatDate(v.nextRunAt)}</td><td class="table-actions">${btn('job-detail', icon('eye'), 'ghost')} ${btn('job-run', icon('play'), 'ghost', 'INTEGRATION_WRITE')} ${btn('job-toggle', icon(v.enabled ? 'pause' : 'play'), 'ghost', 'INTEGRATION_WRITE')}</td></tr>`)
    ) : emptyState();
  } catch (e) {
    node.innerHTML = emptyState(e.message);
  }
  renderIcons();
}

export async function openJobDetail(job) {
  // Fetch the full job record (includes recent run history).
  try { job = (await api('/integrations/sync-jobs/' + job.id)).data; } catch {}
  const runs = job.runs || [];
  const historyRows = runs.map(v => `<tr><td>${escVal(v.triggerType)}</td><td>${statusPill(v.status)}</td><td class="code">${escVal(v.endpoint)}</td><td>${formatDate(v.startedAt)}</td><td>${escVal(v.received)}</td><td>${escVal(v.created)}</td><td>${escVal(v.updated)}</td><td>${escVal(v.failed)}</td></tr>`);
  openDrawer(`${t('scheduledJobs')} · ${job.code}`, t('jobSubtitle'),
    `<div class="drawer-body"><div class="job-detail"><div class="job-meta"><dl><dt>${t('jobCode')}</dt><dd>${escVal(job.code)}</dd><dt>${t('system')}</dt><dd>${escVal(job.systemCode)}</dd><dt>${t('cron')}</dt><dd>${escVal(job.cronExpression)}</dd><dt>${t('status')}</dt><dd>${statusPill(job.status)}</dd><dt>${t('lastRun')}</dt><dd>${formatDate(job.lastRunAt)}</dd><dt>${t('nextRun')}</dt><dd>${formatDate(job.nextRunAt)}</dd></dl></div><div class="job-meta"><dl><dt>${t('endpoint')}</dt><dd>${escVal(job.endpoint)}</dd><dt>HTTP</dt><dd>${escVal(job.httpMethod)}</dd><dt>${t('description')}</dt><dd>${escVal(job.description)}</dd></dl></div></div>${sectionTitle(t('history'), runs.length)}<div class="subtable">${dataTable([t('trigger'), t('status'), t('endpoint'), t('lastRun'), t('received'), t('created'), t('updated'), t('failed')], historyRows)}</div></div>`);
}
