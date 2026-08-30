/**
 * Integration View
 * SAP master-data synchronization (bulk + by-number) and connection health check.
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

/**
 * Ask the application shell to refresh whichever list view is currently on
 * screen (products / orders / batches) once a sync finishes. The shell listens
 * for the `tns:reload` event and invokes the matching loader; if nothing is
 * listening the dispatch is a safe no-op, which keeps this module decoupled
 * from the product/order/batch view modules.
 */
function reloadCurrentView() {
  document.dispatchEvent(new CustomEvent('tns:reload', { detail: { view: state.view } }));
}

/** Map a sync kind to its SAP endpoint segment. */
function syncEndpoint(kind) {
  return kind === 'products' ? 'products' : kind === 'batches' ? 'batches' : 'work-orders';
}

/** Render the by-number sync block used for single-record synchronization. */
function singleSyncHTML() {
  return `<div class="panel">
      <div class="toolbar">
        <div class="muted">${esc(t('singleSync'))}</div>
        <div class="toolbar-actions">
          <label><span>${esc(t('productCode'))}</span><input id="single-product" type="text" placeholder="TG123456"></label>
          ${btn('sync-single-product', icon('package-check') + t('sync'), 'secondary', 'INTEGRATION_WRITE')}
          <label><span>${esc(t('workOrderNo'))}</span><input id="single-order" type="text" placeholder="1000000"></label>
          ${btn('sync-single-order', icon('clipboard-sync') + t('sync'), 'secondary', 'INTEGRATION_WRITE')}
          <label><span>${esc(t('batchNo'))}</span><input id="single-batch" type="text" placeholder="B000123"></label>
          ${btn('sync-single-batch', icon('boxes') + t('sync'), 'secondary', 'INTEGRATION_WRITE')}
        </div>
      </div>
    </div>`;
}

export async function renderIntegration() {
  $('#page').innerHTML = pageHead(t('integrationCenter'), t('manualSync'), t('integrationSubtitle')) +
    `<div class="panel"><div class="toolbar"><div id="sync-hint" class="muted">${esc(t('syncHint'))}</div><div class="toolbar-actions">${btn('sync-products', icon('package-check') + t('syncProducts'), 'primary', 'INTEGRATION_WRITE')} ${btn('sync-orders', icon('clipboard-sync') + t('syncOrders'), 'secondary', 'INTEGRATION_WRITE')} ${btn('sync-batches', icon('boxes') + t('syncBatches'), 'secondary', 'INTEGRATION_WRITE')} ${btn('refresh-data', icon('refresh-cw') + t('refresh'))}</div></div><div id="sync-result" class="empty">${t('noData')}</div></div>` +
    singleSyncHTML();

  // Probe SAP connectivity by issuing a tiny product read through the server.
  try {
    const health = await api('/integrations/sap/request', { method: 'POST', body: { path: '/sap/opu/odata/sap/API_PRODUCT_SRV/A_Product', method: 'GET', query: { '$top': 1 } } });
    const hint = $('#sync-hint');
    if (hint) hint.textContent = `${t('syncHint')} · HTTP ${health.data.status}`;
  } catch {
    const hint = $('#sync-hint');
    if (hint) hint.textContent = t('sapDisabled');
  }
  renderIcons();
}

/** Render the result of a sync operation into the #sync-result container. */
function renderSyncResult(v, label) {
  const node = $('#sync-result');
  if (!node) return;
  node.innerHTML =
    sectionTitle(t('syncResult'), label) +
    detailGrid([
      [t('received'), v.received || 0],
      [t('created'), v.created || 0],
      [t('updated'), v.updated || 0],
      [t('failed'), v.failed || 0]
    ]) +
    (v.errors?.length ? `<pre style="white-space:pre-wrap;background:#f5f8fc;padding:12px;border-radius:6px;font-size:12px;margin-top:12px">${esc(v.errors.join('\n'))}</pre>` : '');
}

export async function runSync(kind, trigger) {
  const original = trigger.innerHTML;
  trigger.disabled = true;
  trigger.innerHTML = icon('loader-circle') + t('run') + '…';
  try {
    const endpoint = syncEndpoint(kind);
    const data = await api(`/integrations/sap/${endpoint}/sync`, { method: 'POST', body: { minutes: 15 } });
    const v = data.data || {};
    renderSyncResult(v, formatDate(new Date()) + ' · 15 min');
    toast(`${t('saved')} · ${v.received || 0} ${t('received')}`);
    // Refresh whatever list view (products/orders/batches) happens to be visible.
    if (['products', 'orders', 'batches'].includes(state.view)) reloadCurrentView();
  } catch (e) {
    toast(e.message, true);
  } finally {
    trigger.disabled = false;
    trigger.innerHTML = original;
    renderIcons();
  }
}

export async function runSingleSync(kind, value, trigger) {
  const original = trigger.innerHTML;
  trigger.disabled = true;
  trigger.innerHTML = icon('loader-circle');
  try {
    const endpoint = syncEndpoint(kind);
    const result = await api(`/integrations/sap/${endpoint}/${encodeURIComponent(value)}/sync`, { method: 'POST', body: {} });
    const v = result.data || {};
    renderSyncResult(v, formatDate(new Date()) + ' · ' + value);
    toast(`${t('saved')} · ${v.received || 0} ${t('received')}`);
    // Only reload when the currently visible view matches the synced kind.
    if (['products', 'orders', 'batches'].includes(kind) && state.view === kind) reloadCurrentView();
  } catch (e) {
    toast(e.message, true);
  } finally {
    trigger.disabled = false;
    trigger.innerHTML = original;
    renderIcons();
  }
}
