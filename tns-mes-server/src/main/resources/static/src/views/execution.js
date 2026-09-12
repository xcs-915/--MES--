import { $, $$, esc, escVal, icon, renderIcons } from '../utils/dom.js';
import { api } from '../api/request.js';
import { t } from '../i18n/index.js';
import { pageHead, btn } from '../components/toolbar.js';
import { dataTable } from '../components/table.js';
import { emptyState, statusPill } from '../components/feedback.js';
import { openDrawer, closeDrawer } from '../components/drawer.js';
import { toast } from '../utils/ui.js';
import { formatDate } from '../utils/format.js';

let activeTab = 'issues';
let currentItems = [];

const tabs = [
  ['issues', 'materialIssue'],
  ['reports', 'workReport'],
  ['inspections', 'inspection'],
  ['receipts', 'goodsReceipt'],
];

export function renderExecution() {
  const actions = btn('execution-add-' + tabAction(activeTab), icon('plus') + t('add'), 'primary', activeTab === 'inspections' ? 'QUALITY_WRITE' : 'EXECUTION_WRITE');
  $('#page').innerHTML = pageHead(t('production'), t('productionOperations'), t('productionOperationsSubtitle'), actions)
    + `<div class="panel" id="execution-root">
        <div class="tabs execution-tabs">${tabs.map(([key, label]) => `<button class="tab ${key === activeTab ? 'active' : ''}" data-execution-tab="${key}">${esc(t(label))}</button>`).join('')}</div>
        <div class="toolbar">
          <label class="grow"><span>${esc(t('search'))}</span><input id="execution-search" placeholder="${esc(t('documentNo') + '/' + t('orderNo'))}"></label>
          <label><span>${esc(t('status'))}</span><select id="execution-status"><option value="">${esc(t('all'))}</option>${statusOptions(activeTab)}</select></label>
          ${activeTab === 'inspections' ? `<label><span>${esc(t('inspectionType'))}</span><select id="execution-type"><option value="">${esc(t('all'))}</option>${inspectionOptions()}</select></label>` : ''}
          <div class="toolbar-actions">${btn('execution-query', icon('search') + t('query'), 'primary')}${btn('execution-refresh', icon('refresh-cw') + t('refresh'))}</div>
        </div>
        <div id="execution-table"></div>
      </div>`;
  bindPage();
  loadExecution();
}

function bindPage() {
  const root = $('#page');
  root?.addEventListener('click', async event => {
    const tab = event.target.closest('[data-execution-tab]');
    if (tab) { activeTab = tab.dataset.executionTab; renderExecution(); return; }
    const action = event.target.closest('[data-action]')?.dataset.action;
    if (!action) return;
    if (action === 'execution-query' || action === 'execution-refresh') { await loadExecution(); return; }
    if (action.startsWith('execution-add-')) { await openCreate(activeTab); return; }
    const row = event.target.closest('tr[data-id]');
    const value = currentItems.find(item => String(item.id) === row?.dataset.id);
    if (!value) return;
    if (action === 'execution-post') { await postDocument(value, event.target.closest('button')); return; }
    if (action === 'execution-inspect') { openInspectionResult(value); return; }
    if (action === 'execution-detail') { openDetail(value); }
  });
  $('#execution-search')?.addEventListener('keydown', event => { if (event.key === 'Enter') loadExecution(); });
  $('#execution-status')?.addEventListener('change', loadExecution);
  $('#execution-type')?.addEventListener('change', loadExecution);
}

export async function loadExecution() {
  const node = $('#execution-table');
  if (!node) return;
  const endpoint = { issues: 'material-issues', reports: 'work-reports', inspections: 'inspections', receipts: 'goods-receipts' }[activeTab];
  const params = new URLSearchParams({ page: '0', size: '100' });
  const keyword = $('#execution-search')?.value?.trim();
  const status = $('#execution-status')?.value;
  const type = $('#execution-type')?.value;
  if (keyword) params.set('keyword', keyword);
  if (status) params.set('status', status);
  if (type) params.set('type', type);
  try {
    const response = await api('/execution/' + endpoint + '?' + params);
    currentItems = response.data?.items || [];
    node.innerHTML = tableFor(activeTab, currentItems);
  } catch (error) { node.innerHTML = emptyState(error.message); }
  renderIcons();
}

function tableFor(tab, items) {
  if (tab === 'issues') return dataTable([t('documentNo'), t('orderNo'), t('operation'), t('material'), t('quantity'), t('operator'), t('postingDate'), t('status'), t('actions')], items.map(value => {
    const quantity = (value.items || []).reduce((sum, item) => sum + Number(item.quantity || 0), 0);
    const materials = (value.items || []).map(item => item.productCode).join(', ');
    return `<tr data-id="${value.id}"><td class="code">${escVal(value.issueNo)}</td><td class="code">${escVal(value.orderNo)}</td><td>${escVal(value.operationCode)}</td><td>${escVal(materials)}</td><td>${escVal(quantity)}</td><td>${escVal(value.operatorCode)}</td><td>${formatDate(value.postingDate)}</td><td>${statusPill(value.status)}</td><td class="table-actions">${rowActions(value, true)}</td></tr>`;
  }));
  if (tab === 'reports') return dataTable([t('documentNo'), t('orderNo'), t('product'), t('operation'), t('qualifiedQuantity'), t('unqualifiedQuantity'), t('operator'), t('reportedAt'), t('status'), t('actions')], items.map(value => `<tr data-id="${value.id}"><td class="code">${escVal(value.reportNo)}</td><td class="code">${escVal(value.orderNo)}</td><td><span class="cell-title">${escVal(value.productCode)}</span><span class="cell-sub">${escVal(value.productName)}</span></td><td>${escVal(value.operationCode)}</td><td>${escVal(value.qualifiedQuantity)}</td><td>${escVal(value.unqualifiedQuantity)}</td><td>${escVal(value.operatorCode)}</td><td>${formatDate(value.reportedAt)}</td><td>${statusPill(value.status)}</td><td class="table-actions">${rowActions(value, true)}</td></tr>`));
  if (tab === 'inspections') return dataTable([t('documentNo'), t('inspectionType'), t('orderNo'), t('product'), t('operation'), t('sampleQuantity'), t('inspector'), t('requestedAt'), t('status'), t('actions')], items.map(value => `<tr data-id="${value.id}"><td class="code">${escVal(value.inspectionNo)}</td><td>${escVal(t('inspection.' + value.inspectionType))}</td><td class="code">${escVal(value.orderNo)}</td><td><span class="cell-title">${escVal(value.productCode)}</span><span class="cell-sub">${escVal(value.productName)}</span></td><td>${escVal(value.operationCode)}</td><td>${escVal(value.sampleQuantity)}</td><td>${escVal(value.inspectorCode || value.requestedBy)}</td><td>${formatDate(value.requestedAt)}</td><td>${statusPill(value.status)}</td><td class="table-actions">${rowActions(value, false)}</td></tr>`));
  return dataTable([t('documentNo'), t('orderNo'), t('product'), t('inspection'), t('quantity'), t('batchNo'), t('storageLocation'), t('postingDate'), t('status'), t('actions')], items.map(value => `<tr data-id="${value.id}"><td class="code">${escVal(value.receiptNo)}</td><td class="code">${escVal(value.orderNo)}</td><td><span class="cell-title">${escVal(value.productCode)}</span><span class="cell-sub">${escVal(value.productName)}</span></td><td class="code">${escVal(value.inspectionNo)}</td><td>${escVal(value.quantity)} ${escVal(value.unit)}</td><td>${escVal(value.batchNo)}</td><td>${escVal(value.storageLocation)}</td><td>${formatDate(value.postingDate)}</td><td>${statusPill(value.status)}</td><td class="table-actions">${rowActions(value, true)}</td></tr>`));
}

function rowActions(value, postable) {
  let html = btn('execution-detail', icon('eye'), 'ghost', activeTab === 'inspections' ? 'QUALITY_READ' : 'EXECUTION_READ');
  if (activeTab === 'inspections' && ['PENDING', 'IN_PROGRESS'].includes(value.status)) html += btn('execution-inspect', icon('clipboard-check'), 'ghost', 'QUALITY_WRITE');
  if (postable && ['DRAFT', 'FAILED'].includes(value.status)) html += btn('execution-post', icon('send'), 'ghost', 'EXECUTION_POST');
  return html;
}

function statusOptions(tab) {
  const values = tab === 'inspections' ? ['PENDING', 'IN_PROGRESS', 'PASSED', 'FAILED'] : ['DRAFT', 'POSTING', 'POSTED', 'FAILED'];
  return values.map(value => `<option value="${value}">${esc(t('status.' + value))}</option>`).join('');
}

function inspectionOptions() {
  return ['FIRST', 'LAST', 'PATROL', 'COMPLETION', 'GP'].map(value => `<option value="${value}">${esc(t('inspection.' + value))}</option>`).join('');
}

function tabAction(tab) { return { issues: 'issue', reports: 'report', inspections: 'inspection', receipts: 'receipt' }[tab]; }

async function openCreate(tab) {
  if (tab === 'receipts') { await openReceiptCreate(); return; }
  const orders = await activeOrders();
  if (!orders.length) { toast(t('noExecutableOrders'), true); return; }
  if (tab === 'issues') openIssueCreate(orders);
  if (tab === 'reports') openReportCreate(orders);
  if (tab === 'inspections') openInspectionCreate(orders);
}

async function activeOrders() {
  const response = await api('/work-orders?page=0&size=200');
  return (response.data?.items || []).filter(value => ['RELEASED', 'IN_PROGRESS'].includes(value.status));
}

function orderSelect(orders, id = 'execution-order') {
  return `<select id="${id}" required>${orders.map(value => `<option value="${value.id}">${esc(value.orderNo)} · ${esc(value.productCode)} · ${esc(value.productNameZh || '')}</option>`).join('')}</select>`;
}

function formFooter(saveAction) {
  return `<div class="form-footer"><button class="btn secondary" data-action="close-drawer">${esc(t('cancel'))}</button><button class="btn primary" id="${saveAction}">${icon('save')} ${esc(t('save'))}</button></div>`;
}

function openIssueCreate(orders) {
  const body = `<div class="drawer-body"><div class="form-grid">
    <label><span>${esc(t('workOrder'))}</span>${orderSelect(orders, 'issue-order')}</label>
    <label><span>${esc(t('operation'))}</span><select id="issue-operation"><option value="">${esc(t('all'))}</option></select></label>
    <label><span>${esc(t('operator'))}</span><input id="issue-operator" required></label>
    <label><span>${esc(t('warehouse'))}</span><input id="issue-warehouse" value="TK10"></label>
    <label class="span-2"><span>${esc(t('remark'))}</span><input id="issue-remark"></label>
  </div><div class="section-title"><h2>${esc(t('materials'))}</h2><button class="btn secondary" id="issue-add-line">${icon('plus')} ${esc(t('add'))}</button></div>
  <div id="issue-lines">${materialLine(1)}</div></div>${formFooter('issue-save')}`;
  openDrawer(t('materialIssue'), t('productionOperationsSubtitle'), body);
  bindOrderOperations('issue-order', 'issue-operation');
  $('#issue-add-line')?.addEventListener('click', () => {
    const host = $('#issue-lines');
    host.insertAdjacentHTML('beforeend', materialLine(host.querySelectorAll('.material-line').length + 1));
    renderIcons();
  });
  $('#issue-lines')?.addEventListener('click', event => event.target.closest('[data-remove-line]')?.closest('.material-line')?.remove());
  $('#issue-save')?.addEventListener('click', saveIssue);
  renderIcons();
}

function materialLine(line) {
  return `<div class="config-row material-line"><span class="code">${line}</span><input data-key="productCode" placeholder="${esc(t('materialCode'))}" required><input data-key="productName" placeholder="${esc(t('materialName'))}"><input data-key="batchNo" placeholder="${esc(t('batchNo'))}"><input data-key="quantity" type="number" min="0.000001" step="0.000001" placeholder="${esc(t('quantity'))}" required><input data-key="unit" value="PCS" placeholder="${esc(t('unit'))}"><input data-key="storageLocation" placeholder="${esc(t('storageLocation'))}"><input data-key="reservationNo" placeholder="${esc(t('reservation'))}"><input data-key="reservationItem" placeholder="${esc(t('lineNo'))}"><button class="icon-btn danger-icon" data-remove-line title="${esc(t('delete'))}">${icon('trash-2')}</button></div>`;
}

async function saveIssue() {
  const items = $$('.material-line', '#issue-lines').map(row => Object.fromEntries($$('[data-key]', row).map(input => [input.dataset.key, input.dataset.key === 'quantity' ? Number(input.value) : input.value.trim()]))).filter(item => item.productCode && item.quantity > 0);
  if (!items.length || !$('#issue-operator')?.value.trim()) { toast(t('requiredFields'), true); return; }
  await mutate('/execution/material-issues', 'POST', {
    workOrderId: Number($('#issue-order').value), operationId: numberOrNull($('#issue-operation').value), operatorCode: $('#issue-operator').value.trim(),
    warehouseCode: $('#issue-warehouse').value.trim(), remark: $('#issue-remark').value.trim(), items,
  });
}

function openReportCreate(orders) {
  const body = `<div class="drawer-body"><div class="form-grid">
    <label><span>${esc(t('workOrder'))}</span>${orderSelect(orders, 'report-order')}</label>
    <label><span>${esc(t('operation'))}</span><select id="report-operation"><option value="">${esc(t('all'))}</option></select></label>
    <label><span>${esc(t('qualifiedQuantity'))}</span><input id="report-qualified" type="number" min="0.000001" step="0.000001" required></label>
    <label><span>${esc(t('unqualifiedQuantity'))}</span><input id="report-unqualified" type="number" min="0" step="0.000001" value="0" required></label>
    <label><span>${esc(t('operator'))}</span><input id="report-operator" required></label>
    <label><span>${esc(t('shift'))}</span><input id="report-shift"></label>
    <label><span>${esc(t('equipment'))}</span><input id="report-equipment"></label>
    <label><span>${esc(t('remark'))}</span><input id="report-remark"></label>
  </div></div>${formFooter('report-save')}`;
  openDrawer(t('workReport'), t('productionOperationsSubtitle'), body);
  bindOrderOperations('report-order', 'report-operation');
  $('#report-save')?.addEventListener('click', async () => {
    if (!$('#report-operator').value.trim() || Number($('#report-qualified').value) <= 0) { toast(t('requiredFields'), true); return; }
    await mutate('/execution/work-reports', 'POST', { workOrderId:Number($('#report-order').value), operationId:numberOrNull($('#report-operation').value), qualifiedQuantity:Number($('#report-qualified').value), unqualifiedQuantity:Number($('#report-unqualified').value || 0), operatorCode:$('#report-operator').value.trim(), shiftCode:$('#report-shift').value.trim(), equipmentCode:$('#report-equipment').value.trim(), remark:$('#report-remark').value.trim() });
  });
}

function openInspectionCreate(orders) {
  const body = `<div class="drawer-body"><div class="form-grid">
    <label><span>${esc(t('inspectionType'))}</span><select id="inspection-type">${inspectionOptions()}</select></label>
    <label><span>${esc(t('workOrder'))}</span>${orderSelect(orders, 'inspection-order')}</label>
    <label><span>${esc(t('operation'))}</span><select id="inspection-operation"><option value="">${esc(t('all'))}</option></select></label>
    <label><span>${esc(t('sampleQuantity'))}</span><input id="inspection-sample" type="number" min="0.000001" step="0.000001" value="1" required></label>
    <label><span>${esc(t('requestedBy'))}</span><input id="inspection-requester" required></label>
    <label><span>${esc(t('remark'))}</span><input id="inspection-remark"></label>
    <label><span>${esc(t('inspectionItem'))}</span><input id="inspection-item-code"></label>
    <label><span>${esc(t('itemName'))}</span><input id="inspection-item-name"></label>
    <label class="span-2"><span>${esc(t('specification'))}</span><input id="inspection-specification"></label>
  </div></div>${formFooter('inspection-save')}`;
  openDrawer(t('inspectionRequest'), t('productionOperationsSubtitle'), body);
  bindOrderOperations('inspection-order', 'inspection-operation');
  $('#inspection-save')?.addEventListener('click', async () => {
    const itemCode = $('#inspection-item-code').value.trim();
    const items = itemCode ? [{ itemCode, itemName:$('#inspection-item-name').value.trim() || itemCode, specification:$('#inspection-specification').value.trim() }] : [];
    if (!$('#inspection-requester').value.trim() || Number($('#inspection-sample').value) <= 0) { toast(t('requiredFields'), true); return; }
    await mutate('/execution/inspections/requests', 'POST', { workOrderId:Number($('#inspection-order').value), operationId:numberOrNull($('#inspection-operation').value), inspectionType:$('#inspection-type').value, sampleQuantity:Number($('#inspection-sample').value), requestedBy:$('#inspection-requester').value.trim(), remark:$('#inspection-remark').value.trim(), items });
  });
}

async function openReceiptCreate() {
  const response = await api('/execution/inspections?type=COMPLETION&status=PASSED&page=0&size=200');
  const passed = response.data?.items || [];
  if (!passed.length) { toast(t('noPassedCompletionInspection'), true); return; }
  const body = `<div class="drawer-body"><div class="form-grid">
    <label class="span-2"><span>${esc(t('completionInspection'))}</span><select id="receipt-inspection">${passed.map(value => `<option value="${value.id}" data-order-id="${value.workOrderId}" data-quantity="${value.qualifiedQuantity}">${esc(value.inspectionNo)} · ${esc(value.orderNo)} · ${esc(value.productCode)}</option>`).join('')}</select></label>
    <label><span>${esc(t('quantity'))}</span><input id="receipt-quantity" type="number" min="0.000001" step="0.000001" value="${esc(passed[0].qualifiedQuantity)}" required></label>
    <label><span>${esc(t('unit'))}</span><input id="receipt-unit" value="PCS" required></label>
    <label><span>${esc(t('batchNo'))}</span><input id="receipt-batch"></label>
    <label><span>${esc(t('warehouse'))}</span><input id="receipt-warehouse" value="TK10" required></label>
    <label><span>${esc(t('storageLocation'))}</span><input id="receipt-location" required></label>
    <label><span>${esc(t('movementType'))}</span><input id="receipt-movement" value="1101P" required></label>
    <label><span>${esc(t('operator'))}</span><input id="receipt-operator" required></label>
    <label><span>${esc(t('remark'))}</span><input id="receipt-remark"></label>
  </div></div>${formFooter('receipt-save')}`;
  openDrawer(t('goodsReceipt'), t('productionOperationsSubtitle'), body);
  $('#receipt-inspection')?.addEventListener('change', event => { $('#receipt-quantity').value = event.target.selectedOptions[0]?.dataset.quantity || ''; });
  $('#receipt-save')?.addEventListener('click', async () => {
    const option = $('#receipt-inspection').selectedOptions[0];
    if (!$('#receipt-operator').value.trim() || !$('#receipt-location').value.trim() || Number($('#receipt-quantity').value) <= 0) { toast(t('requiredFields'), true); return; }
    await mutate('/execution/goods-receipts', 'POST', { workOrderId:Number(option.dataset.orderId), inspectionId:Number(option.value), quantity:Number($('#receipt-quantity').value), unit:$('#receipt-unit').value.trim(), batchNo:$('#receipt-batch').value.trim(), warehouseCode:$('#receipt-warehouse').value.trim(), storageLocation:$('#receipt-location').value.trim(), movementType:$('#receipt-movement').value.trim(), operatorCode:$('#receipt-operator').value.trim(), remark:$('#receipt-remark').value.trim() });
  });
  renderIcons();
}

function openInspectionResult(value) {
  const itemFields = (value.items || []).map((item, index) => `<div class="config-row inspection-result-line" data-index="${index}"><span class="code">${escVal(item.itemCode)}</span><span>${escVal(item.itemName)}</span><input data-key="measuredValue" value="${esc(item.measuredValue || '')}" placeholder="${esc(t('measuredValue'))}"><select data-key="result"><option value="PASS">${esc(t('status.PASSED'))}</option><option value="FAIL">${esc(t('status.FAILED'))}</option></select></div>`).join('');
  const body = `<div class="drawer-body"><div class="form-grid">
    <label><span>${esc(t('inspector'))}</span><input id="result-inspector" required></label>
    <label><span>${esc(t('overallResult'))}</span><select id="result-overall"><option value="PASS">${esc(t('status.PASSED'))}</option><option value="FAIL">${esc(t('status.FAILED'))}</option></select></label>
    <label><span>${esc(t('qualifiedQuantity'))}</span><input id="result-qualified" type="number" min="0" step="0.000001" value="${esc(value.sampleQuantity)}"></label>
    <label><span>${esc(t('unqualifiedQuantity'))}</span><input id="result-unqualified" type="number" min="0" step="0.000001" value="0"></label>
    <label><span>${esc(t('defectCode'))}</span><input id="result-defect"></label>
    <label><span>${esc(t('disposition'))}</span><select id="result-disposition"><option value=""></option><option value="REWORK">REWORK</option><option value="SCRAP">SCRAP</option><option value="CONCESSION">CONCESSION</option><option value="HOLD">HOLD</option></select></label>
    <label class="span-2"><span>${esc(t('remark'))}</span><input id="result-remark"></label>
  </div>${itemFields ? `<div class="section-title"><h2>${esc(t('inspectionItems'))}</h2></div><div>${itemFields}</div>` : ''}</div>${formFooter('inspection-result-save')}`;
  openDrawer(`${t('inspection')} · ${value.inspectionNo}`, t('inspection.' + value.inspectionType), body);
  $('#result-overall')?.addEventListener('change', event => {
    if (event.target.value === 'PASS') { $('#result-qualified').value = value.sampleQuantity; $('#result-unqualified').value = 0; }
    else { $('#result-qualified').value = 0; $('#result-unqualified').value = value.sampleQuantity; }
  });
  $('#inspection-result-save')?.addEventListener('click', async () => {
    const items = $$('.inspection-result-line').map((row, index) => ({
      itemCode:value.items[index].itemCode, itemName:value.items[index].itemName, specification:value.items[index].specification,
      minValue:value.items[index].minValue, maxValue:value.items[index].maxValue, unit:value.items[index].unit,
      measuredValue:row.querySelector('[data-key="measuredValue"]').value.trim(), result:row.querySelector('[data-key="result"]').value,
    }));
    await mutate('/execution/inspections/' + value.id + '/result', 'PUT', { inspectorCode:$('#result-inspector').value.trim(), qualifiedQuantity:Number($('#result-qualified').value), unqualifiedQuantity:Number($('#result-unqualified').value), overallResult:$('#result-overall').value, defectCode:$('#result-defect').value.trim(), disposition:$('#result-disposition').value, remark:$('#result-remark').value.trim(), items });
  });
  renderIcons();
}

async function bindOrderOperations(orderId, operationId) {
  const update = async () => {
    const select = $('#' + operationId);
    if (!select) return;
    try {
      const order = (await api('/work-orders/' + $('#' + orderId).value)).data;
      select.innerHTML = `<option value="">${esc(t('all'))}</option>` + (order.operations || []).map(value => `<option value="${value.id}">${esc(value.operationCode || value.sequenceNo)} · ${esc(value.operationName || '')}</option>`).join('');
    } catch { select.innerHTML = `<option value="">${esc(t('all'))}</option>`; }
  };
  $('#' + orderId)?.addEventListener('change', update);
  await update();
}

async function postDocument(value, button) {
  const endpoint = { issues:'material-issues', reports:'work-reports', receipts:'goods-receipts' }[activeTab];
  if (!endpoint) return;
  button.disabled = true;
  try {
    await api(`/execution/${endpoint}/${value.id}/post`, { method:'POST', headers:idempotencyHeaders() });
    toast(t('postedSuccessfully'));
    await loadExecution();
  } catch (error) { toast(error.message, true); }
  finally { button.disabled = false; }
}

async function mutate(path, method, body) {
  try {
    await api(path, { method, body, headers:idempotencyHeaders() });
    closeDrawer();
    toast(t('saved'));
    await loadExecution();
  } catch (error) { toast(error.message, true); }
}

function openDetail(value) {
  const hidden = new Set(['id', 'items', 'workOrderId', 'operationId', 'workReportId', 'inspectionId']);
  const fields = Object.entries(value).filter(([key, field]) => !hidden.has(key) && field !== null && field !== '').map(([key, field]) => `<div class="detail-item"><small>${esc(t(key))}</small><strong>${key.endsWith('At') || key.endsWith('Date') ? formatDate(field) : escVal(field)}</strong></div>`).join('');
  const lines = (value.items || []).map(item => `<tr>${Object.values(item).slice(1).map(field => `<td>${escVal(field)}</td>`).join('')}</tr>`).join('');
  const label = { issues:'materialIssue', reports:'workReport', inspections:'inspection', receipts:'goodsReceipt' }[activeTab];
  openDrawer(value.issueNo || value.reportNo || value.inspectionNo || value.receiptNo, t(label), `<div class="drawer-body"><div class="detail-grid">${fields}</div>${lines ? `<div class="section-title"><h2>${esc(t('details'))}</h2></div><div class="table-wrap"><table><tbody>${lines}</tbody></table></div>` : ''}</div>`);
}

function idempotencyHeaders() { return { 'X-Idempotency-Key': crypto.randomUUID() }; }
function numberOrNull(value) { return value ? Number(value) : null; }
