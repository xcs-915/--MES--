/**
 * Batches View
 * SAP batch master data with status, shelf life, and inspection info
 */

import { $, $$, esc, escVal, icon, renderIcons } from '../utils/dom.js';
import { state, ps } from '../store/index.js';
import { api, getMenuConfig } from '../api/request.js';
import { t } from '../i18n/index.js';
import { pageHead, btn, filterField, toolbar } from '../components/toolbar.js';
import { dataTable, paginationHTML } from '../components/table.js';
import { detailGrid, groupedDetailGrid, keyInfoCard, emptyState, statusPill, progressBar, sectionTitle } from '../components/feedback.js';
import { openDrawer, closeDrawer } from '../components/drawer.js';
import { toast } from '../utils/ui.js';
import { formatDate, readPath, localizedMeta } from '../utils/format.js';

export function renderBatches() {
  const fields = [
    filterField('batch-search', t('search'), 'text', null, t('batchNo') + '/' + t('productCode')),
    filterField('batch-status', t('batchStatus'), 'select', [
      { value: 'all', label: t('all') },
      { value: 'RELEASED', label: 'RELEASED' },
      { value: 'RESTRICTED', label: 'RESTRICTED' },
      { value: 'UNREST', label: 'UNREST' }
    ]),
    filterField('batch-plant', t('plant'), 'text', null, t('plant'))
  ];
  const actions = btn('sync-batches', icon('refresh-cw') + t('syncBatches'), 'primary', 'INTEGRATION_WRITE');
  $('#page').innerHTML = pageHead(t('quality'), t('batches'), t('batchSubtitle'), '')
    + `<div class="panel">${toolbar(fields, actions)}<div id="batch-table"></div></div>`;
  $('#batch-status')?.addEventListener('change', () => loadBatches(0));
  $('#batch-search')?.addEventListener('keydown', e => { if (e.key === 'Enter') loadBatches(0); });
  $('#batch-plant')?.addEventListener('keydown', e => { if (e.key === 'Enter') loadBatches(0); });
  loadBatches();
}

export async function loadBatches(page) {
  const p = ps('batches');
  if (page !== undefined) p.page = page;
  const node = $('#batch-table');
  if (!node) return;
  const params = new URLSearchParams();
  params.set('page', p.page);
  params.set('size', p.size);
  params.set('keyword', $('#batch-search')?.value || '');
  const statusVal = $('#batch-status')?.value || 'all';
  if (statusVal !== 'all') params.set('batchStatus', statusVal);
  const plantVal = $('#batch-plant')?.value || '';
  if (plantVal) params.set('plant', plantVal);
  try {
    const data = await api('/quality/batches?' + params);
    let items = data.data.items || [];
    p.items = items;
    p.total = data.data.total || 0;
    p.totalPages = data.data.totalPages || Math.ceil(p.total / p.size) || 1;
    state.data.batches = p;

    const headers = [
      { key: 'batchNo', label: t('batchNo'), sortable: true },
      { key: 'productCode', label: t('productCode'), sortable: true },
      { key: 'plant', label: t('plant'), sortable: true },
      { key: 'batchStatus', label: t('batchStatus'), sortable: true },
      { key: 'quantity', label: t('quantity'), sortable: true },
      { key: 'manufactureDate', label: t('manufactureDate'), sortable: true },
      { key: 'expirationDate', label: t('expirationDate'), sortable: true },
      { key: 'shelfLifeExpirationDate', label: t('shelfLifeExpirationDate'), sortable: true },
      { key: 'supplierBatch', label: t('supplierBatch'), sortable: true },
      { key: 'inspectionLot', label: t('inspectionLot'), sortable: true },
      { key: 'countryOfOrigin', label: t('countryOfOrigin'), sortable: true },
      { key: 'nextInspectionDate', label: t('nextInspectionDate'), sortable: true },
      { key: 'lastSync', label: t('lastSync'), sortable: false },
      { key: 'actions', label: t('actions'), sortable: false }
    ];

    // client-side sort
    let sorted = items;
    if (p.sortKey && p.sortKey !== 'actions' && p.sortKey !== 'lastSync') {
      const dir = p.sortDir === 'desc' ? -1 : 1;
      sorted = [...items].sort((a, b) => {
        const va = readPath(a, p.sortKey), vb = readPath(b, p.sortKey);
        if (va == null) return 1;
        if (vb == null) return -1;
        if (typeof va === 'number' && typeof vb === 'number') return (va - vb) * dir;
        return String(va).localeCompare(String(vb)) * dir;
      });
    }

    const rows = sorted.map(v => `<tr data-id="${esc(v.id)}">
      <td class="code">${escVal(v.batchNo)}</td>
      <td><span class="cell-title">${escVal(v.productCode)}</span><span class="cell-sub">${escVal(v.productNameZh || v.productNameEn || v.productNameAr || v.productName)}</span></td>
      <td>${escVal(v.plant)}</td>
      <td>${statusPill(v.batchStatus)}</td>
      <td>${escVal(v.quantity)} ${escVal(v.unit)}</td>
      <td>${formatDate(v.manufactureDate)}</td>
      <td>${formatDate(v.expirationDate)}</td>
      <td>${formatDate(v.shelfLifeExpirationDate)}</td>
      <td>${escVal(v.supplierBatch)}</td>
      <td>${escVal(v.inspectionLot)}</td>
      <td>${escVal(v.countryOfOrigin)}</td>
      <td>${formatDate(v.nextInspectionDate)}</td>
      <td>${formatDate(v.sapLastSyncAt)}</td>
      <td class="table-actions">${btn('batch-detail', icon('eye'), 'ghost', 'QUALITY_READ')}</td>
    </tr>`);

    node.innerHTML = dataTable(headers, rows, p.sortKey, p.sortDir) + paginationHTML(p);
  } catch (e) {
    node.innerHTML = emptyState(e.message);
  }
  renderIcons();
}

export function openBatchDetail(value) {
  const sections = [
    {
      title: t('basicInfo'),
      fields: [
        [t('batchNo'), value.batchNo],
        [t('product'), [value.productCode, value.productNameZh || value.productNameEn || value.productNameAr || value.productName].filter(Boolean).join(' ') || null],
        [t('plant'), value.plant],
        [t('batchStatus'), statusPill(value.batchStatus)],
        [t('quantity'), value.quantity != null ? `${value.quantity} ${value.unit || ''}`.trim() : null],
        [t('material'), value.material],
        [t('batchIdentifyingPlant'), value.batchIdentifyingPlant]
      ]
    },
    {
      title: t('dateInfo'),
      fields: [
        [t('manufactureDate'), formatDate(value.manufactureDate)],
        [t('expirationDate'), formatDate(value.expirationDate)],
        [t('shelfLifeExpirationDate'), formatDate(value.shelfLifeExpirationDate)],
        [t('nextInspectionDate'), formatDate(value.nextInspectionDate)],
        [t('lastGoodsReceiptDate'), formatDate(value.lastGoodsReceiptDate)],
        [t('batchCertificationDate'), formatDate(value.batchCertificationDate)],
        [t('freeDefinedDate1'), formatDate(value.freeDefinedDate1)],
        [t('freeDefinedDate2'), formatDate(value.freeDefinedDate2)],
        [t('freeDefinedDate3'), formatDate(value.freeDefinedDate3)]
      ]
    },
    {
      title: t('supplierOrigin'),
      fields: [
        [t('supplierBatch'), value.supplierBatch],
        [t('countryOfOrigin'), value.countryOfOrigin],
        [t('regionOfOrigin'), value.regionOfOrigin]
      ]
    },
    {
      title: t('statusFlags'),
      fields: [
        [t('batchMarkedForDeletion'), value.batchMarkedForDeletion ? t('yes') : t('no')],
        [t('inspectionLot'), value.inspectionLot]
      ]
    },
    {
      title: t('syncInfo'),
      fields: [
        [t('lastSync'), formatDate(value.sapLastSyncAt)]
      ]
    }
  ];
  openDrawer(`${t('batches')} · ${value.batchNo}`, t('batchSubtitle'), `<div class="drawer-body">${keyInfoCard([
    { label: t('batchNo'), value: value.batchNo, icon: 'boxes' },
    { label: t('product'), value: [value.productCode, value.productNameZh || value.productNameEn || value.productNameAr || value.productName].filter(Boolean).join(' ') || null, icon: 'package' },
    { label: t('batchStatus'), value: statusPill(value.batchStatus), icon: 'check-circle' },
    { label: t('quantity'), value: value.quantity != null ? `${value.quantity} ${value.unit || ''}`.trim() : null, icon: 'scale' },
    { label: t('plant'), value: value.plant, icon: 'building-2' },
    { label: t('expirationDate'), value: formatDate(value.expirationDate), icon: 'calendar-clock' }
  ])}${groupedDetailGrid(sections)}</div>`);
}
