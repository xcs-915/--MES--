/**
 * Dictionaries View
 * Data dictionary management - master-detail split layout
 * Left: dictionary types, Right: dictionary items of selected type
 */

import { $, $$, esc, escVal, icon, renderIcons } from '../utils/dom.js';
import { state, ps } from '../store/index.js';
import { api } from '../api/request.js';
import { t } from '../i18n/index.js';
import { pageHead, btn, filterField, toolbar } from '../components/toolbar.js';
import { dataTable, paginationHTML } from '../components/table.js';
import { detailGrid, emptyState, statusPill, sectionTitle } from '../components/feedback.js';
import { openDrawer, closeDrawer } from '../components/drawer.js';
import { toast } from '../utils/ui.js';
import { formatDate } from '../utils/format.js';

let currentType = '';
let typeList = [];

export function renderDictionaries() {
  $('#page').innerHTML = pageHead(t('foundation'), t('dataDictionary'), t('accessSubtitle'),
    btn('dict-type-add', icon('plus') + t('add') + ' ' + t('dictType'), 'primary', 'USER_ADMIN'))
    + `<div class="split-layout">
        <div class="split-panel">
          <div class="panel-header">
            <h3>${icon('folder-tree')} ${esc(t('dictType'))}</h3>
            <input id="dict-type-search" type="text" placeholder="${esc(t('search'))}..." class="input-sm">
          </div>
          <div class="panel-body" id="dict-type-list"></div>
        </div>
        <div class="split-panel">
          <div class="panel-header">
            <h3 id="dict-items-title">${icon('list')} ${esc(t('dataDictionary'))}</h3>
            <div class="toolbar-actions">
              ${btn('dict-item-add', icon('plus') + t('add'), 'secondary', 'USER_ADMIN')}
              ${btn('refresh-data', icon('refresh-cw') + t('refresh'), 'secondary')}
            </div>
          </div>
          <div class="panel-body" id="dict-items-table"></div>
        </div>
      </div>`;
  $('#dict-type-search')?.addEventListener('input', e => renderTypeList(e.target.value));
  loadDictionaryTypes();
}

async function loadDictionaryTypes() {
  const node = $('#dict-type-list');
  try {
    const all = (await api('/system/dictionaries')).data || [];
    state.data.dictionaries = all;
    // Group by dictType
    const typeMap = {};
    all.forEach(item => {
      if (!typeMap[item.dictType]) typeMap[item.dictType] = [];
      typeMap[item.dictType].push(item);
    });
    typeList = Object.keys(typeMap).sort().map(typeCode => ({
      type: typeCode,
      count: typeMap[typeCode].length,
      // Try to find a representative label from items with sortOrder 0 or similar
      name: typeMap[typeCode].find(i => i.labelZh)?.labelZh || typeCode
    }));
    renderTypeList();
    // Auto-select first type
    if (typeList.length > 0 && !currentType) {
      currentType = typeList[0].type;
      loadDictItems();
    }
  } catch (e) {
    node.innerHTML = emptyState(e.message);
  }
  renderIcons();
}

function renderTypeList(filter) {
  const node = $('#dict-type-list');
  if (!node) return;
  const keyword = (filter || '').toLowerCase().trim();
  const filtered = keyword ? typeList.filter(t => t.type.toLowerCase().includes(keyword) || (t.name || '').toLowerCase().includes(keyword)) : typeList;
  if (!filtered.length) {
    node.innerHTML = emptyState(t('noData'));
    return;
  }
  node.innerHTML = `<ul class="dict-type-list">${filtered.map(t => `
    <li data-type="${esc(t.type)}" class="${currentType === t.type ? 'active' : ''}">
      <div class="type-info" data-action="dict-type-select">
        <div class="type-name">${escVal(t.name || t.type)}</div>
        <div class="type-code">${escVal(t.type)}</div>
      </div>
      <div class="type-actions">
        <span class="type-count">${t.count}</span>
        ${btn('dict-type-edit', icon('pencil'), 'ghost', 'USER_ADMIN')}
        ${btn('dict-type-delete', icon('trash-2'), 'ghost', 'USER_ADMIN')}
      </div>
    </li>
  `).join('')}</ul>`;
  // Bind click events for type selection
  $$('#dict-type-list .type-info').forEach(el => {
    el.addEventListener('click', () => {
      const li = el.closest('li');
      currentType = li.dataset.type;
      $$('#dict-type-list li').forEach(x => x.classList.remove('active'));
      li.classList.add('active');
      loadDictItems();
    });
  });
}

async function loadDictItems() {
  const node = $('#dict-items-table');
  const title = $('#dict-items-title');
  if (!node || !currentType) {
    if (node) node.innerHTML = emptyState(t('noData'));
    return;
  }
  const typeInfo = typeList.find(t => t.type === currentType);
  if (title) title.innerHTML = `${icon('list')} ${esc(t('dictData'))} - ${escVal(typeInfo?.name || currentType)}`;
  try {
    const items = (await api('/system/dictionaries?type=' + encodeURIComponent(currentType))).data || [];
    const headers = [
      { key: 'dictCode', label: t('dictCode'), sortable: false },
      { key: 'label', label: t('label'), sortable: false },
      { key: 'dictValue', label: t('dictValue'), sortable: false },
      { key: 'sortOrder', label: t('sortOrder'), sortable: false },
      { key: 'status', label: t('status'), sortable: false },
      { key: 'actions', label: t('actions'), sortable: false }
    ];
    const rows = items.map(v => `<tr>
      <td class="code">${escVal(v.dictCode)}</td>
      <td>${escVal(v.labelZh)}<span class="cell-sub">${escVal(v.labelEn || v.labelAr)}</span></td>
      <td>${escVal(v.dictValue)}</td>
      <td>${escVal(v.sortOrder)}</td>
      <td>${statusPill(v.status)}</td>
      <td>${btn('dictionary-edit', icon('edit-3'), 'ghost', 'USER_ADMIN')} ${btn('dictionary-delete', icon('trash-2'), 'ghost', 'USER_ADMIN')}</td>
    </tr>`);
    node.innerHTML = items.length ? dataTable(headers, rows) : emptyState();
  } catch (e) {
    node.innerHTML = emptyState(e.message);
  }
  renderIcons();
}

export async function loadDictionaries() {
  await loadDictionaryTypes();
}

export function setCurrentType(type) {
  currentType = type;
}

export function openDictionaryCreate() {
  openDrawer(t('dataDictionary'), t('accessSubtitle'),
    `<div class="drawer-body"><div class="form-grid"><label><span>${t('dictType')}</span><input id="new-dict-type" value="${escVal(currentType || '')}" required></label><label><span>${t('dictCode')}</span><input id="new-dict-code" required></label><label><span>${t('label')}</span><input id="new-dict-label-zh" required></label><label><span>English</span><input id="new-dict-label-en"></label><label><span>العربية</span><input id="new-dict-label-ar"></label><label><span>${t('dictValue')}</span><input id="new-dict-value" required></label></div></div><div class="form-footer"><button class="btn secondary" data-action="close-drawer">${t('cancel')}</button><button class="btn primary" data-action="create-dictionary">${t('save')}</button></div>`);
}

export function openDictionaryEdit(item) {
  openDrawer(t('dataDictionary'), t('accessSubtitle'),
    `<div class="drawer-body"><div class="form-grid">
      <label><span>${t('dictType')}</span><input id="edit-dict-type" value="${escVal(item.dictType)}" required></label>
      <label><span>${t('dictCode')}</span><input id="edit-dict-code" value="${escVal(item.dictCode)}" required></label>
      <label><span>${t('label')}</span><input id="edit-dict-label-zh" value="${escVal(item.labelZh)}" required></label>
      <label><span>English</span><input id="edit-dict-label-en" value="${escVal(item.labelEn || '')}"></label>
      <label><span>العربية</span><input id="edit-dict-label-ar" value="${escVal(item.labelAr || '')}"></label>
      <label><span>${t('dictValue')}</span><input id="edit-dict-value" value="${escVal(item.dictValue)}" required></label>
      <label><span>${t('sortOrder')}</span><input id="edit-dict-sort" type="number" value="${escVal(item.sortOrder || 0)}"></label>
      <label><span>${t('status')}</span><select id="edit-dict-status"><option value="ACTIVE">${t('enabled')}</option><option value="INACTIVE">${t('disabled')}</option></select></label>
    </div></div>
    <div class="form-footer">
      <button class="btn secondary" data-action="close-drawer">${t('cancel')}</button>
      <button class="btn primary" data-action="save-dictionary" data-id="${item.id}">${t('save')}</button>
    </div>`);
  // Set status
  setTimeout(() => {
    const sel = $('#edit-dict-status');
    if (sel) sel.value = item.status || 'ACTIVE';
  }, 0);
}

export function openDictTypeCreate() {
  openDrawer(t('add') + ' · ' + t('dictType'), t('accessSubtitle'),
    `<div class="drawer-body"><div class="form-grid">
      <label><span>${t('dictType')}</span><input id="new-type-code" placeholder="${esc(t('dictType'))}" required></label>
      <label><span>${t('label')}</span><input id="new-type-name" placeholder="${esc(t('label'))}"></label>
    </div></div>
    <div class="form-footer">
      <button class="btn secondary" data-action="close-drawer">${t('cancel')}</button>
      <button class="btn primary" data-action="create-dict-type">${t('save')}</button>
    </div>`);
}

export function openDictTypeEdit(typeInfo) {
  openDrawer(t('edit') + ' · ' + t('dictType'), t('accessSubtitle'),
    `<div class="drawer-body"><div class="form-grid">
      <label><span>${t('dictType')}</span><input id="edit-type-old" value="${escVal(typeInfo.type)}" disabled></label>
      <label><span>${t('newType')}</span><input id="edit-type-new" value="${escVal(typeInfo.type)}" required></label>
      <label><span>${t('label')}</span><input id="edit-type-label" value="${escVal(typeInfo.name || '')}" placeholder="${esc(t('label'))}"></label>
    </div></div>
    <div class="form-footer">
      <button class="btn secondary" data-action="close-drawer">${t('cancel')}</button>
      <button class="btn primary" data-action="save-dict-type" data-old-type="${escVal(typeInfo.type)}">${t('save')}</button>
    </div>`);
}
