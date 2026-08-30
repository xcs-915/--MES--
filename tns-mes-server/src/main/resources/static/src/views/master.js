/**
 * Master Data View
 * CRUD for factories, workshops, departments, warehouses, etc.
 */

import { $, esc, escVal, icon, renderIcons } from '../utils/dom.js';
import { state } from '../store/index.js';
import { api } from '../api/request.js';
import { t } from '../i18n/index.js';
import { pageHead, btn, filterField, toolbar } from '../components/toolbar.js';
import { dataTable, paginationHTML } from '../components/table.js';
import { emptyState, detailGrid, statusPill } from '../components/feedback.js';
import { openDrawer, closeDrawer } from '../components/drawer.js';
import { toast } from '../utils/ui.js';

export function renderMaster() {
  const fields = [
    filterField('master-search', t('search'), 'text', null, t('code') + '/' + t('name')),
    filterField('master-type', t('type'), 'select', [
      {value:'FACTORY', label:'Factory'}, {value:'WORKSHOP', label:'Workshop'},
      {value:'DEPARTMENT', label:'Department'}, {value:'WAREHOUSE', label:'Warehouse'},
      {value:'WORK_CENTER', label:'Work center'}, {value:'PRODUCTION_LINE', label:'Production line'},
      {value:'WORKSTATION', label:'Workstation'}, {value:'PERSON', label:'Person'},
      {value:'POSITION', label:'Position'}, {value:'CUSTOMER', label:'Customer'},
      {value:'SUPPLIER', label:'Supplier'}, {value:'MANUFACTURER', label:'Manufacturer'}
    ])
  ];
  $('#page').innerHTML = pageHead(t('foundation'), t('masterData'), t('masterSubtitle'), btn('master-add', icon('plus') + t('add'), 'primary', 'BASIC_DATA_WRITE')) + `<div class="panel">${toolbar(fields)}<div id="master-table"></div></div>`;
  $('#master-type').addEventListener('change', loadMaster);
  loadMaster();
}

export async function loadMaster() {
  const type = $('#master-type')?.value || 'FACTORY', node = $('#master-table');
  if (!node) return;
  try {
    const data = await api(`/master-data/${type}?size=100&keyword=${encodeURIComponent($('#master-search')?.value || '')}`);
    const items = data.data.items || [];
    state.data.master = items;
    node.innerHTML = items.length ? dataTable([t('code'), t('name'), t('status'), t('actions')], items.map(v => `<tr><td class="code">${escVal(v.code)}</td><td><span class="cell-title">${escVal(v.nameZh)}</span><span class="cell-sub">${escVal(v.nameEn || v.nameAr)}</span></td><td>${statusPill(v.status)}</td><td class="table-actions">${btn('master-detail', icon('eye'), 'ghost')} ${btn('master-edit', icon('pencil'), 'ghost', 'BASIC_DATA_WRITE')} ${btn('master-delete', icon('trash-2'), 'ghost', 'BASIC_DATA_WRITE')}</td></tr>`)) : emptyState();
  } catch (e) { node.innerHTML = emptyState(e.message); }
  renderIcons();
}

function getMasterParentTypes(type) {
  switch (type) {
    case 'FACTORY': return ['ENTERPRISE'];
    case 'WORKSHOP': return ['FACTORY'];
    case 'DEPARTMENT': return ['ENTERPRISE', 'FACTORY', 'WORKSHOP'];
    case 'WAREHOUSE': return ['FACTORY'];
    case 'WORK_CENTER': return ['FACTORY', 'WORKSHOP'];
    case 'PRODUCTION_LINE': return ['FACTORY', 'WORKSHOP'];
    case 'WORKSTATION': return ['PRODUCTION_LINE', 'WORK_CENTER'];
    case 'PERSON': return ['DEPARTMENT'];
    case 'POSITION': return ['DEPARTMENT'];
    default: return [];
  }
}

async function loadMasterParents(type) {
  const parentTypes = getMasterParentTypes(type);
  if (!parentTypes.length) return [];
  const results = await Promise.all(parentTypes.map(pt => api(`/master-data/${pt}?size=200`).catch(() => ({ data: { items: [] } }))));
  return results.flatMap(r => r.data?.items || []);
}

export async function openMasterDetail(item) {
  const type = $('#master-type')?.value || 'FACTORY';
  let value = item;
  try { value = (await api(`/master-data/${type}/${item.id}`)).data; } catch {}
  const fields = [
    [t('code'), value.code], [t('name'), value.nameZh],
    ['English', value.nameEn], ['العربية', value.nameAr],
    [t('status'), statusPill(value.status)], [t('sortOrder'), value.sortOrder],
    [t('description'), value.description]
  ];
  openDrawer(`${t('masterData')} · ${value.code}`, t('masterSubtitle'), `<div class="drawer-body">${detailGrid(fields)}</div>`);
}

export async function openMasterCreate() {
  const type = $('#master-type')?.value || 'FACTORY';
  const parents = await loadMasterParents(type);
  const parentOptions = parents.length ? `<option value="">—</option>` + parents.map(p => `<option value="${p.id}">${esc(p.code)} - ${esc(p.nameZh)}</option>`).join('') : '';
  const parentField = getMasterParentTypes(type).length ? `<label><span>${t('parent')}</span><select id="md-parent">${parentOptions}</select></label>` : '';
  openDrawer(t('add') + ' · ' + type, t('masterSubtitle'),
    `<div class="drawer-body"><div class="form-grid">
      <label><span>${t('code')}</span><input id="md-code" required></label>
      <label><span>${t('name')}</span><input id="md-name-zh" required></label>
      <label><span>English</span><input id="md-name-en"></label>
      <label><span>العربية</span><input id="md-name-ar"></label>
      ${parentField}
      <label><span>${t('status')}</span><select id="md-status"><option value="ACTIVE">${t('enabled')}</option><option value="INACTIVE">${t('disabled')}</option></select></label>
      <label><span>${t('sortOrder')}</span><input id="md-sort-order" type="number" value="0"></label>
      <label class="full"><span>${t('description')}</span><textarea id="md-description" rows="3"></textarea></label>
    </div></div>
    <div class="form-footer"><button class="btn secondary" data-action="close-drawer">${t('cancel')}</button><button class="btn primary" data-action="save-master" data-mode="create">${t('save')}</button></div>`);
}

export async function openMasterEdit(item) {
  const type = $('#master-type')?.value || 'FACTORY';
  const parents = await loadMasterParents(type);
  const parentOptions = parents.length ? `<option value="">—</option>` + parents.map(p => `<option value="${p.id}" ${p.id === item.parentId ? 'selected' : ''}>${esc(p.code)} - ${esc(p.nameZh)}</option>`).join('') : '';
  const parentField = getMasterParentTypes(type).length ? `<label><span>${t('parent')}</span><select id="md-parent">${parentOptions}</select></label>` : '';
  openDrawer(t('edit') + ' · ' + item.code, t('masterSubtitle'),
    `<div class="drawer-body"><div class="form-grid">
      <label><span>${t('code')}</span><input id="md-code" value="${esc(item.code)}" required></label>
      <label><span>${t('name')}</span><input id="md-name-zh" value="${esc(item.nameZh)}" required></label>
      <label><span>English</span><input id="md-name-en" value="${esc(item.nameEn || '')}"></label>
      <label><span>العربية</span><input id="md-name-ar" value="${esc(item.nameAr || '')}"></label>
      ${parentField}
      <label><span>${t('status')}</span><select id="md-status"><option value="ACTIVE" ${item.status === 'ACTIVE' ? 'selected' : ''}>${t('enabled')}</option><option value="INACTIVE" ${item.status === 'INACTIVE' ? 'selected' : ''}>${t('disabled')}</option></select></label>
      <label><span>${t('sortOrder')}</span><input id="md-sort-order" type="number" value="${item.sortOrder ?? 0}"></label>
      <label class="full"><span>${t('description')}</span><textarea id="md-description" rows="3">${esc(item.description || '')}</textarea></label>
    </div></div>
    <div class="form-footer"><button class="btn secondary" data-action="close-drawer">${t('cancel')}</button><button class="btn primary" data-action="save-master" data-mode="edit" data-id="${item.id}">${t('save')}</button></div>`);
}
