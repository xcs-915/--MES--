/**
 * Orders View
 * SAP work order master-detail with components and operations
 * Master-detail vertical split layout: master table on top, detail tabs below
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

export function renderOrders() {
  const fields = [
    filterField('order-search', t('search'), 'text', null, t('orderNo') + '/' + t('product')),
    filterField('order-status', t('status'), 'select', [
      { value: 'all', label: t('all') },
      { value: 'DRAFT', label: 'DRAFT' },
      { value: 'RELEASED', label: 'RELEASED' },
      { value: 'IN_PROGRESS', label: 'IN_PROGRESS' },
      { value: 'COMPLETED', label: 'COMPLETED' },
      { value: 'CANCELLED', label: 'CANCELLED' }
    ]),
    filterField('order-plant', t('plant'), 'text', null, t('plant'))
  ];
  const actions = btn('sync-orders', icon('refresh-cw') + t('syncOrders'), 'primary', 'INTEGRATION_WRITE');
  $('#page').innerHTML = pageHead(t('production'), t('workOrders'), t('orderSubtitle'), '')
    + `<div class="split-layout-vertical">
        <div class="split-master">
          ${toolbar(fields, actions)}
          <div id="order-table"></div>
        </div>
        <div class="split-detail">
          <div class="panel-header">
            <h3 id="order-detail-title">${icon('clipboard-list')} ${esc(t('details'))}</h3>
          </div>
          <div class="tabs" id="order-detail-tabs">
            <button class="tab active" data-tab="summary">${esc(t('details'))}</button>
            <button class="tab" data-tab="components">${esc(t('components'))}</button>
            <button class="tab" data-tab="operations">${esc(t('operations'))}</button>
          </div>
          <div id="order-detail-content"><div class="detail-empty">${esc(t('selectOrderHint'))}</div></div>
        </div>
      </div>`;
  $('#order-status')?.addEventListener('change', () => loadOrders(0));
  $('#order-search')?.addEventListener('keydown', e => { if (e.key === 'Enter') loadOrders(0); });
  $('#order-plant')?.addEventListener('keydown', e => { if (e.key === 'Enter') loadOrders(0); });
  bindDetailTabs();
  loadOrders();
}

export async function loadOrders(page) {
  const p = ps('orders');
  if (page !== undefined) p.page = page;
  const node = $('#order-table');
  if (!node) return;
  const params = new URLSearchParams();
  params.set('page', p.page);
  params.set('size', p.size);
  params.set('keyword', $('#order-search')?.value || '');
  const statusVal = $('#order-status')?.value || 'all';
  if (statusVal !== 'all') params.set('status', statusVal);
  const plantVal = $('#order-plant')?.value || '';
  if (plantVal) params.set('plant', plantVal);
  try {
    const data = await api('/work-orders?' + params);
    let items = data.data.items || [];
    p.items = items;
    p.total = data.data.total || 0;
    p.totalPages = data.data.totalPages || Math.ceil(p.total / p.size) || 1;
    state.data.orders = p;

    const headers = [
      { key: 'orderNo', label: t('orderNo'), sortable: true },
      { key: 'product', label: t('product'), sortable: true },
      { key: 'orderType', label: t('orderType'), sortable: true },
      { key: 'productionPlant', label: t('productionPlant'), sortable: true },
      { key: 'quantity', label: t('quantity'), sortable: true },
      { key: 'completedQuantity', label: t('completed'), sortable: true },
      { key: 'progress', label: t('progress'), sortable: false },
      { key: 'plannedStart', label: t('plannedStart'), sortable: true },
      { key: 'plannedEnd', label: t('plannedEnd'), sortable: true },
      { key: 'status', label: t('status'), sortable: true },
      { key: 'actions', label: t('actions'), sortable: false }
    ];

    // client-side sort (special: 'product' sorts by productCode, 'productionPlant' sorts by productionPlant or plant)
    let sorted = items;
    if (p.sortKey && p.sortKey !== 'actions' && p.sortKey !== 'progress') {
      const dir = p.sortDir === 'desc' ? -1 : 1;
      sorted = [...items].sort((a, b) => {
        let va, vb;
        if (p.sortKey === 'product') {
          va = a.productCode; vb = b.productCode;
        } else if (p.sortKey === 'productionPlant') {
          va = a.productionPlant || a.plant; vb = b.productionPlant || b.plant;
        } else {
          va = readPath(a, p.sortKey); vb = readPath(b, p.sortKey);
        }
        if (va == null) return 1;
        if (vb == null) return -1;
        if (typeof va === 'number' && typeof vb === 'number') return (va - vb) * dir;
        return String(va).localeCompare(String(vb)) * dir;
      });
    }

    const rows = sorted.map(v => `<tr data-id="${esc(v.id)}">
      <td class="code">${escVal(v.orderNo)}</td>
      <td><span class="cell-title">${escVal(v.productCode)}</span><span class="cell-sub">${escVal(v.productNameZh || v.productNameEn || v.productNameAr)}</span></td>
      <td>${escVal(v.orderType)}</td>
      <td>${escVal(v.productionPlant || v.plant)}</td>
      <td>${escVal(v.quantity)} ${escVal(v.productionUnit)}</td>
      <td>${escVal(v.completedQuantity)}</td>
      <td>${progressBar(v.completedQuantity, v.quantity)}</td>
      <td>${formatDate(v.plannedStart)}</td>
      <td>${formatDate(v.plannedEnd)}</td>
      <td>${statusPill(v.status)}</td>
      <td class="table-actions">${btn('order-detail', icon('eye'), 'ghost', 'PRODUCTION_READ')} ${btn('order-sync', icon('refresh-cw'), 'ghost', 'INTEGRATION_WRITE')}</td>
    </tr>`);

    node.innerHTML = dataTable(headers, rows, p.sortKey, p.sortDir) + paginationHTML(p);

    // Bind row click to load detail in the bottom panel (ignore clicks on action buttons)
    $$('#order-table tbody tr').forEach(tr => {
      tr.addEventListener('click', e => {
        if (e.target.closest('[data-action]')) return;
        const id = tr.dataset.id;
        if (id == null) return;
        $$('#order-table tbody tr').forEach(x => x.classList.remove('selected'));
        tr.classList.add('selected');
        loadOrderDetail(id);
      });
    });
  } catch (e) {
    node.innerHTML = emptyState(e.message);
  }
  renderIcons();
}

/**
 * Load a single work order's detail into the bottom split-detail panel.
 * Renders three tabs: summary (groupedDetailGrid), components, operations.
 */
export async function loadOrderDetail(id) {
  const node = $('#order-detail-content');
  const title = $('#order-detail-title');
  if (!node) return;

  // Loading state (use cached row data for immediate title feedback)
  const cached = state.data.orders?.items?.find(v => String(v.id) === String(id));
  if (title && cached) {
    title.innerHTML = `${icon('clipboard-list')} ${escVal(cached.orderNo)} <span class="cell-sub">${escVal(cached.productCode)}</span>`;
  }
  node.innerHTML = `<div class="detail-empty">${icon('loader')}</div>`;
  renderIcons();

  try {
    const value = (await api('/work-orders/' + encodeURIComponent(id))).data;

    const sections = [
      {
        title: t('basicInfo'),
        fields: [
          [t('orderNo'), value.orderNo],
          [t('product'), [value.productCode, value.productNameZh || value.productNameEn || value.productNameAr].filter(Boolean).join(' ') || null],
          [t('orderType'), value.orderType],
          [t('status'), statusPill(value.status)],
          [t('quantity'), value.quantity != null ? `${value.quantity} ${value.productionUnit || ''}`.trim() : null],
          [t('completed'), value.completedQuantity],
          [t('progress'), progressBar(value.completedQuantity, value.quantity)],
          [t('productionPlant'), value.productionPlant || value.plant],
          [t('storageLocation'), value.storageLocation],
          [t('source'), value.source || 'SAP']
        ]
      },
      {
        title: t('planningInfo'),
        fields: [
          [t('plannedStart'), formatDate(value.plannedStart)],
          [t('plannedEnd'), formatDate(value.plannedEnd)],
          [t('scheduledStart'), formatDate(value.scheduledStart)],
          [t('scheduledEnd'), formatDate(value.scheduledEnd)],
          [t('productionStartDate'), formatDate(value.productionStartDate)],
          [t('productionEndDate'), formatDate(value.productionEndDate)],
          [t('plannedOrderType'), value.plannedOrderType],
          [t('materialName'), value.materialName],
          [t('mrpPlant'), value.mrpPlant],
          [t('mrpController'), value.mrpController],
          [t('productionVersion'), value.productionVersion],
          [t('productionSupervisor'), value.productionSupervisor]
        ]
      },
      {
        title: t('customerSales'),
        fields: [
          [t('customer'), value.customer],
          [t('salesOrder'), value.salesOrder],
          [t('salesOrderItem'), value.salesOrderItem],
          [t('wbsElement'), value.wbsElement],
          [t('wbsDescription'), value.wbsDescription],
          [t('companyCode'), value.companyCode],
          [t('profitCenter'), value.profitCenter]
        ]
      },
      {
        title: t('procurement'),
        fields: [
          [t('purchasingGroup'), value.purchasingGroup],
          [t('purchasingOrganization'), value.purchasingOrganization],
          [t('fixedSupplier'), value.fixedSupplier],
          [t('supplierName'), value.supplierName],
          [t('goodsReceiptQty'), value.goodsReceiptQty],
          [t('issuedQuantity'), value.issuedQuantity]
        ]
      },
      {
        title: t('otherInfo'),
        fields: [
          [t('plannedOrder'), value.plannedOrder],
          [t('plannedOrderProfile'), value.plannedOrderProfile],
          [t('mrpArea'), value.mrpArea],
          [t('materialProcurementCategory'), value.materialProcurementCategory],
          [t('materialProcurementType'), value.materialProcurementType],
          [t('plannedScrapQtySap'), value.plannedScrapQtySap],
          [t('plannedOrderOpeningDate'), formatDate(value.plannedOrderOpeningDate)],
          [t('schedulingType'), value.schedulingType],
          [t('plannedOrderIsFirm'), value.plannedOrderIsFirm],
          [t('plannedOrderIsConvertible'), value.plannedOrderIsConvertible],
          [t('lastSync'), formatDate(value.sapLastSyncAt)]
        ]
      }
    ];

    const components = value.components || [];
    const operations = value.operations || [];

    const compHeaders = [t('sequence'), t('material'), t('materialGroup'), t('requiredQuantity'), t('withdrawn'), t('available'), t('unit'), t('reservation'), t('requirementDate'), t('storageLocation'), t('batch'), t('goodsMovement'), t('operation')];
    const compRows = components.map(c => `<tr>
      <td>${escVal(c.sequenceNo)}</td>
      <td><span class="cell-title">${escVal(c.productCode || c.componentProductCode)}</span><span class="cell-sub">${escVal(c.itemDescription || c.productName)}</span></td>
      <td>${escVal(c.materialGroup)}</td>
      <td>${escVal(c.requiredQuantity || c.quantity)}</td>
      <td>${escVal(c.withdrawnQuantity)}</td>
      <td>${escVal(c.availableQuantity)}</td>
      <td>${escVal(c.unit)}</td>
      <td>${escVal(c.reservationNo || c.reservationItem)}</td>
      <td>${formatDate(c.requirementDate)}</td>
      <td>${escVal(c.storageLocation)}</td>
      <td>${escVal(c.batch)}</td>
      <td>${escVal(c.goodsMovementType)}</td>
      <td>${escVal(c.operationCode || c.manufacturingOrderOperation)}</td>
    </tr>`);

    const opHeaders = [t('sequence'), t('operation'), t('workCenter'), t('plant'), t('controlKey'), t('plannedTotal'), t('plannedYield'), t('confirmedYield'), t('unit'), t('status')];
    const opRows = operations.map(o => `<tr>
      <td>${escVal(o.sequenceNo)}</td>
      <td><span class="cell-title">${escVal(o.operationCode)}</span><span class="cell-sub">${escVal(o.operationName || o.operationNameZh || o.operationNameEn)}</span></td>
      <td><span class="cell-title">${escVal(o.workCenterCode)}</span><span class="cell-sub">${escVal(o.workCenterDesc)}</span></td>
      <td>${escVal(o.plant)}</td>
      <td>${escVal(o.controlKey)}</td>
      <td>${escVal(o.plannedTotalQuantity || o.plannedQuantity)}</td>
      <td>${escVal(o.plannedYieldQuantity)}</td>
      <td>${escVal(o.confirmedYieldQuantity || o.completedQuantity)}</td>
      <td>${escVal(o.operationUnit)}</td>
      <td>${statusPill(o.status)}</td>
    </tr>`);

    if (title) {
      const productText = [value.productCode, value.productNameZh || value.productNameEn || value.productNameAr].filter(Boolean).join(' ');
      title.innerHTML = `${icon('clipboard-list')} ${escVal(value.orderNo)} <span class="cell-sub">${esc(productText)}</span>`;
    }

    node.innerHTML = `
      <div class="tab-content" id="tab-summary">
        ${keyInfoCard([
          { label: t('orderNo'), value: value.orderNo, icon: 'clipboard-list' },
          { label: t('product'), value: [value.productCode, value.productNameZh || value.productNameEn || value.productNameAr].filter(Boolean).join(' ') || null, icon: 'package' },
          { label: t('status'), value: statusPill(value.status), icon: 'activity' },
          { label: t('quantity'), value: value.quantity != null ? `${value.quantity} ${value.productionUnit || ''}`.trim() : null, icon: 'package-check' },
          { label: t('completed'), value: value.completedQuantity, icon: 'check-check' },
          { label: t('progress'), value: progressBar(value.completedQuantity, value.quantity), icon: 'trending-up' },
          { label: t('plannedStart'), value: formatDate(value.plannedStart), icon: 'calendar' },
          { label: t('plannedEnd'), value: formatDate(value.plannedEnd), icon: 'calendar-off' }
        ])}
        ${groupedDetailGrid(sections)}
      </div>
      <div class="tab-content hidden" id="tab-components">${sectionTitle(t('components'), components.length)}${dataTable(compHeaders, compRows)}</div>
      <div class="tab-content hidden" id="tab-operations">${sectionTitle(t('operations'), operations.length)}${dataTable(opHeaders, opRows)}</div>`;

    // Reset to the summary tab whenever a new order is loaded
    $$('#order-detail-tabs .tab').forEach(x => x.classList.toggle('active', x.dataset.tab === 'summary'));
  } catch (e) {
    node.innerHTML = `<div class="detail-empty">${esc(e.message)}</div>`;
  }
  renderIcons();
}

/** Bind tab switching for the bottom detail panel (summary / components / operations). */
function bindDetailTabs() {
  const tabs = $('#order-detail-tabs');
  if (!tabs) return;
  tabs.addEventListener('click', e => {
    const tab = e.target.closest('.tab');
    if (!tab) return;
    $$('#order-detail-tabs .tab').forEach(x => x.classList.remove('active'));
    tab.classList.add('active');
    const target = tab.dataset.tab;
    ['summary', 'components', 'operations'].forEach(name => {
      const el = $('#tab-' + name);
      if (el) el.classList.toggle('hidden', name !== target);
    });
  });
}

export async function openOrderDetail(order) {
  let value = order;
  try { value = (await api('/work-orders/' + order.id)).data; } catch {}

  const sections = [
    {
      title: t('basicInfo'),
      fields: [
        [t('orderNo'), value.orderNo],
        [t('product'), [value.productCode, value.productNameZh || value.productNameEn || value.productNameAr].filter(Boolean).join(' ') || null],
        [t('orderType'), value.orderType],
        [t('status'), statusPill(value.status)],
        [t('quantity'), value.quantity != null ? `${value.quantity} ${value.productionUnit || ''}`.trim() : null],
        [t('completed'), value.completedQuantity],
        [t('progress'), progressBar(value.completedQuantity, value.quantity)],
        [t('productionPlant'), value.productionPlant || value.plant],
        [t('storageLocation'), value.storageLocation],
        [t('source'), value.source || 'SAP']
      ]
    },
    {
      title: t('planningInfo'),
      fields: [
        [t('plannedStart'), formatDate(value.plannedStart)],
        [t('plannedEnd'), formatDate(value.plannedEnd)],
        [t('scheduledStart'), formatDate(value.scheduledStart)],
        [t('scheduledEnd'), formatDate(value.scheduledEnd)],
        [t('productionStartDate'), formatDate(value.productionStartDate)],
        [t('productionEndDate'), formatDate(value.productionEndDate)],
        [t('plannedOrderType'), value.plannedOrderType],
        [t('materialName'), value.materialName],
        [t('mrpPlant'), value.mrpPlant],
        [t('mrpController'), value.mrpController],
        [t('productionVersion'), value.productionVersion],
        [t('productionSupervisor'), value.productionSupervisor]
      ]
    },
    {
      title: t('customerSales'),
      fields: [
        [t('customer'), value.customer],
        [t('salesOrder'), value.salesOrder],
        [t('salesOrderItem'), value.salesOrderItem],
        [t('wbsElement'), value.wbsElement],
        [t('wbsDescription'), value.wbsDescription],
        [t('companyCode'), value.companyCode],
        [t('profitCenter'), value.profitCenter]
      ]
    },
    {
      title: t('procurement'),
      fields: [
        [t('purchasingGroup'), value.purchasingGroup],
        [t('purchasingOrganization'), value.purchasingOrganization],
        [t('fixedSupplier'), value.fixedSupplier],
        [t('supplierName'), value.supplierName],
        [t('goodsReceiptQty'), value.goodsReceiptQty],
        [t('issuedQuantity'), value.issuedQuantity]
      ]
    },
    {
      title: t('otherInfo'),
      fields: [
        [t('plannedOrder'), value.plannedOrder],
        [t('plannedOrderProfile'), value.plannedOrderProfile],
        [t('mrpArea'), value.mrpArea],
        [t('materialProcurementCategory'), value.materialProcurementCategory],
        [t('materialProcurementType'), value.materialProcurementType],
        [t('plannedScrapQtySap'), value.plannedScrapQtySap],
        [t('plannedOrderOpeningDate'), formatDate(value.plannedOrderOpeningDate)],
        [t('schedulingType'), value.schedulingType],
        [t('plannedOrderIsFirm'), value.plannedOrderIsFirm],
        [t('plannedOrderIsConvertible'), value.plannedOrderIsConvertible],
        [t('lastSync'), formatDate(value.sapLastSyncAt)]
      ]
    }
  ];

  const components = value.components || [];
  const operations = value.operations || [];

  const compHeaders = [t('sequence'), t('material'), t('materialGroup'), t('requiredQuantity'), t('withdrawn'), t('available'), t('unit'), t('reservation'), t('requirementDate'), t('storageLocation'), t('batch'), t('goodsMovement'), t('operation')];
  const compRows = components.map(c => `<tr>
    <td>${escVal(c.sequenceNo)}</td>
    <td><span class="cell-title">${escVal(c.productCode || c.componentProductCode)}</span><span class="cell-sub">${escVal(c.itemDescription || c.productName)}</span></td>
    <td>${escVal(c.materialGroup)}</td>
    <td>${escVal(c.requiredQuantity || c.quantity)}</td>
    <td>${escVal(c.withdrawnQuantity)}</td>
    <td>${escVal(c.availableQuantity)}</td>
    <td>${escVal(c.unit)}</td>
    <td>${escVal(c.reservationNo || c.reservationItem)}</td>
    <td>${formatDate(c.requirementDate)}</td>
    <td>${escVal(c.storageLocation)}</td>
    <td>${escVal(c.batch)}</td>
    <td>${escVal(c.goodsMovementType)}</td>
    <td>${escVal(c.operationCode || c.manufacturingOrderOperation)}</td>
  </tr>`);

  const opHeaders = [t('sequence'), t('operation'), t('workCenter'), t('plant'), t('controlKey'), t('plannedTotal'), t('plannedYield'), t('confirmedYield'), t('unit'), t('status')];
  const opRows = operations.map(o => `<tr>
    <td>${escVal(o.sequenceNo)}</td>
    <td><span class="cell-title">${escVal(o.operationCode)}</span><span class="cell-sub">${escVal(o.operationName || o.operationNameZh || o.operationNameEn)}</span></td>
    <td><span class="cell-title">${escVal(o.workCenterCode)}</span><span class="cell-sub">${escVal(o.workCenterDesc)}</span></td>
    <td>${escVal(o.plant)}</td>
    <td>${escVal(o.controlKey)}</td>
    <td>${escVal(o.plannedTotalQuantity || o.plannedQuantity)}</td>
    <td>${escVal(o.plannedYieldQuantity)}</td>
    <td>${escVal(o.confirmedYieldQuantity || o.completedQuantity)}</td>
    <td>${escVal(o.operationUnit)}</td>
    <td>${statusPill(o.status)}</td>
  </tr>`);

  const content = `<div class="drawer-body">
    ${keyInfoCard([
      { label: t('orderNo'), value: value.orderNo, icon: 'clipboard-list' },
      { label: t('product'), value: [value.productCode, value.productNameZh || value.productNameEn || value.productNameAr].filter(Boolean).join(' ') || null, icon: 'package' },
      { label: t('status'), value: statusPill(value.status), icon: 'activity' },
      { label: t('quantity'), value: value.quantity != null ? `${value.quantity} ${value.productionUnit || ''}`.trim() : null, icon: 'package-check' },
      { label: t('completed'), value: value.completedQuantity, icon: 'check-check' },
      { label: t('progress'), value: progressBar(value.completedQuantity, value.quantity), icon: 'trending-up' },
      { label: t('plannedStart'), value: formatDate(value.plannedStart), icon: 'calendar' },
      { label: t('plannedEnd'), value: formatDate(value.plannedEnd), icon: 'calendar-off' }
    ])}
    <div class="tabs" id="order-drawer-tabs">
      <button class="tab active" data-tab="summary">${t('details')}</button>
      <button class="tab" data-tab="components">${t('components')}</button>
      <button class="tab" data-tab="operations">${t('operations')}</button>
    </div>
    <div class="tab-content" id="tab-drawer-summary">${groupedDetailGrid(sections)}</div>
    <div class="tab-content hidden" id="tab-drawer-components">${sectionTitle(t('components'), components.length)}${dataTable(compHeaders, compRows)}</div>
    <div class="tab-content hidden" id="tab-drawer-operations">${sectionTitle(t('operations'), operations.length)}${dataTable(opHeaders, opRows)}</div>
  </div>`;

  openDrawer(`${t('workOrders')} · ${value.orderNo}`, t('orderSubtitle'), content);

  $$('#order-drawer-tabs .tab').forEach(tab => {
    tab.addEventListener('click', () => {
      $$('#order-drawer-tabs .tab').forEach(x => x.classList.remove('active'));
      tab.classList.add('active');
      const target = tab.dataset.tab;
      $('#tab-drawer-summary').classList.toggle('hidden', target !== 'summary');
      $('#tab-drawer-components').classList.toggle('hidden', target !== 'components');
      $('#tab-drawer-operations').classList.toggle('hidden', target !== 'operations');
    });
  });
}
