/**
 * Menus View
 * Menu hierarchy management with field/action config editor
 */

import { $, $$, esc, escVal, icon, renderIcons } from '../utils/dom.js';
import { state } from '../store/index.js';
import { api } from '../api/request.js';
import { t } from '../i18n/index.js';
import { pageHead, btn, filterField, toolbar } from '../components/toolbar.js';
import { dataTable, paginationHTML } from '../components/table.js';
import { detailGrid, emptyState, statusPill, sectionTitle } from '../components/feedback.js';
import { openDrawer, closeDrawer } from '../components/drawer.js';
import { toast } from '../utils/ui.js';
import { formatDate } from '../utils/format.js';
import { localizedMeta } from '../utils/format.js';

export function renderMenus() {
  $('#page').innerHTML = pageHead(t('foundation'), t('menuManagement'), t('accessSubtitle'), btn('menu-add', icon('plus') + t('add'), 'primary', 'USER_ADMIN')) + `<div class="panel"><div class="toolbar"><label class="grow"><input id="menu-search" placeholder="${esc(t('search'))}"></label><div class="toolbar-actions">${btn('refresh-data', icon('refresh-cw') + t('refresh'))}</div></div><div id="menu-table"></div></div>`;
  loadMenus();
}

export async function loadMenus() {
  const node = $('#menu-table');
  try {
    state.data.menus = (await api('/system/menus')).data || [];
    // Build hierarchy display
    const buildMenuRows = (items, parentCode, depth) => {
      const children = items.filter(v => (v.parentCode || null) === parentCode);
      return children.flatMap(v => {
        const indent = depth > 0 ? '│&nbsp;'.repeat(depth) + '├&nbsp;' : '';
        const toggleBtn = v.status === 'ACTIVE' ? btn('menu-toggle', icon('circle-check'), 'ghost', 'USER_ADMIN') : btn('menu-toggle', icon('circle-x'), 'ghost', 'USER_ADMIN');
        return [`<tr><td class="code">${escVal(v.code)}</td><td>${indent}${escVal(localizedMeta(v, 'name'))}<span class="cell-sub">${escVal(v.nameZh || v.nameEn || v.nameAr)}</span></td><td class="code">${escVal(v.parentCode || '—')}</td><td class="code">${escVal(v.path || '—')}</td><td class="code">${escVal(v.permissionCode || '—')}</td><td>${escVal(v.sortOrder ?? 0)}</td><td>${statusPill(v.status)}</td><td class="table-actions">${btn('menu-config', icon('settings-2'), 'ghost', 'USER_ADMIN')} ${btn('menu-edit', icon('pencil'), 'ghost', 'USER_ADMIN')} ${toggleBtn} ${btn('menu-delete', icon('trash-2'), 'ghost', 'USER_ADMIN')}</td></tr>`, ...buildMenuRows(items, v.code, depth + 1)];
      });
    };
    const rows = buildMenuRows(state.data.menus, null, 0);
    node.innerHTML = rows.length ? dataTable([t('code'), t('name'), t('parentMenu'), t('path'), t('permissionCode'), t('sortOrder'), t('status'), t('actions')], rows) : emptyState();
  } catch (e) { node.innerHTML = emptyState(e.message); }
  renderIcons();
}

function menuParentOptions(selected) {
  return `<option value="">${t('all')}</option>` + (state.data.menus || []).filter(m => m.status === 'ACTIVE').map(m => `<option value="${esc(m.code)}" ${m.code === selected ? 'selected' : ''}>${esc(m.code)} · ${esc(localizedMeta(m, 'name'))}</option>`).join('');
}

function menuBaseForm(prefix, menu = {}) {
  const edit = !!menu.id;
  return `<div class="drawer-body"><div class="config-section"><div class="section-title"><h2>${t('menuManagement')}</h2><span>${edit ? esc(menu.code) : t('add')}</span></div><div class="form-grid">
    <label><span>${t('code')}</span><input id="${prefix}-menu-code" value="${esc(menu.code || '')}" ${edit ? 'disabled' : ''} required></label>
    <label><span>${t('name')}</span><input id="${prefix}-menu-name-zh" value="${esc(menu.nameZh || '')}" required></label>
    <label><span>English</span><input id="${prefix}-menu-name-en" value="${esc(menu.nameEn || '')}"></label>
    <label><span>العربية</span><input id="${prefix}-menu-name-ar" value="${esc(menu.nameAr || '')}"></label>
    <label><span>${t('parentMenu')}</span><select id="${prefix}-menu-parent">${menuParentOptions(menu.parentCode)}</select></label>
    <label><span>${t('path')}</span><input id="${prefix}-menu-path" value="${esc(menu.path || '')}" placeholder="/products"></label>
    <label><span>${t('iconName')}</span><input id="${prefix}-menu-icon" value="${esc(menu.icon || '')}" placeholder="layout-dashboard"></label>
    <label><span>${t('permissionCode')}</span><input id="${prefix}-menu-permission" value="${esc(menu.permissionCode || '')}" placeholder="PAGE_PRODUCT"></label>
    <label><span>${t('sortOrder')}</span><input id="${prefix}-menu-sort" type="number" value="${menu.sortOrder ?? 0}"></label>
    <label><span>${t('status')}</span><select id="${prefix}-menu-status"><option value="ACTIVE" ${menu.status !== 'INACTIVE' ? 'selected' : ''}>${t('enabled')}</option><option value="INACTIVE" ${menu.status === 'INACTIVE' ? 'selected' : ''}>${t('disabled')}</option></select></label>
  </div></div></div>`;
}

export function openMenuCreate() {
  openDrawer(t('add') + ' · ' + t('menuManagement'), t('accessSubtitle'), menuBaseForm('new') + `<div class="form-footer"><button class="btn secondary" data-action="close-drawer">${t('cancel')}</button><button class="btn primary" data-action="create-menu">${t('save')}</button></div>`);
}

export function openMenuEdit(menu) {
  openDrawer(`${t('edit')} · ${menu.code}`, t('accessSubtitle'), menuBaseForm('edit', menu) + `<div class="form-footer"><button class="btn secondary" data-action="close-drawer">${t('cancel')}</button><button class="btn primary" data-action="save-menu" data-id="${menu.id}">${t('save')}</button></div>`);
}

export async function openMenuConfig(menu) {
  let config = { fields: [], actions: [] };
  try { config = (await api('/system/menus/' + menu.id + '/config')).data || config; } catch (e) { toast(e.message, true); return; }
  const fieldRows = (config.fields || []).map(f => `<div class="config-row" data-config="field" data-id="${f.id}"><input data-key="fieldCode" value="${esc(f.fieldCode)}" placeholder="${t('fieldCode')}"><input data-key="fieldPath" value="${esc(f.fieldPath)}" placeholder="${t('fieldPath')}"><input data-key="labelZh" value="${esc(f.labelZh)}" placeholder="中文"><input data-key="labelEn" value="${esc(f.labelEn || '')}" placeholder="English"><input data-key="labelAr" value="${esc(f.labelAr || '')}" placeholder="العربية"><select data-key="fieldType"><option ${f.fieldType === 'TEXT' ? 'selected' : ''}>TEXT</option><option ${f.fieldType === 'NUMBER' ? 'selected' : ''}>NUMBER</option><option ${f.fieldType === 'DATE' ? 'selected' : ''}>DATE</option><option ${f.fieldType === 'STATUS' ? 'selected' : ''}>STATUS</option></select><label class="check"><input data-key="visibleList" type="checkbox" ${f.visibleList ? 'checked' : ''}>${t('listVisible')}</label><label class="check"><input data-key="visibleDetail" type="checkbox" ${f.visibleDetail ? 'checked' : ''}>${t('detailVisible')}</label><label class="check"><input data-key="queryable" type="checkbox" ${f.queryable ? 'checked' : ''}>${t('queryable')}</label><input data-key="sortOrder" type="number" value="${f.sortOrder || 0}" placeholder="${t('sortOrder')}"><button class="icon-btn danger-icon" data-action="config-delete-field"><i data-lucide="trash-2"></i></button></div>`).join('');
  const actionRows = (config.actions || []).map(a => `<div class="config-row" data-config="action" data-id="${a.id}"><input data-key="actionCode" value="${esc(a.actionCode)}" placeholder="${t('actionCode')}"><input data-key="nameZh" value="${esc(a.nameZh)}" placeholder="中文"><input data-key="nameEn" value="${esc(a.nameEn || '')}" placeholder="English"><input data-key="nameAr" value="${esc(a.nameAr || '')}" placeholder="العربية"><input data-key="permissionCode" value="${esc(a.permissionCode)}" placeholder="${t('permissionCode')}"><select data-key="actionType"><option ${a.actionType === 'BUTTON' ? 'selected' : ''}>BUTTON</option><option ${a.actionType === 'LINK' ? 'selected' : ''}>LINK</option><option ${a.actionType === 'EXPORT' ? 'selected' : ''}>EXPORT</option></select><input data-key="sortOrder" type="number" value="${a.sortOrder || 0}" placeholder="${t('sortOrder')}"><button class="icon-btn danger-icon" data-action="config-delete-action"><i data-lucide="trash-2"></i></button></div>`).join('');
  openDrawer(`${t('menuManagement')} · ${menu.code}`, t('actionConfig'), `<div class="drawer-body config-editor" data-menu-id="${menu.id}"><div class="config-section"><div class="section-title"><h2>${t('fieldConfig')}</h2><button class="btn secondary" data-action="config-add-field">${icon('plus')} ${t('addField')}</button></div><div class="config-head field-head"><span>${t('fieldCode')}</span><span>${t('fieldPath')}</span><span>${t('label')}</span><span>English</span><span>العربية</span><span>${t('fieldType')}</span><span>${t('listVisible')}</span><span>${t('detailVisible')}</span><span>${t('queryable')}</span><span>${t('sortOrder')}</span></div><div id="menu-fields-config">${fieldRows || `<div class="empty">${t('noFields')}</div>`}</div></div><div class="config-section"><div class="section-title"><h2>${t('actionConfig')}</h2><button class="btn secondary" data-action="config-add-action">${icon('plus')} ${t('addAction')}</button></div><div class="config-head action-head"><span>${t('actionCode')}</span><span>${t('actionName')}</span><span>English</span><span>العربية</span><span>${t('permissionCode')}</span><span>${t('actionType')}</span><span>${t('sortOrder')}</span></div><div id="menu-actions-config">${actionRows || `<div class="empty">${t('noActions')}</div>`}</div></div></div><div class="form-footer"><button class="btn secondary" data-action="close-drawer">${t('cancel')}</button><button class="btn primary" data-action="save-menu-config">${t('save')}</button></div>`);
  renderIcons();
}
