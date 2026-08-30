/**
 * TNS-MES Application Entry Point
 * Bootstraps the application: event handlers, initialization, routing
 * Similar to D:\TNS src/main.js but for vanilla JS modular architecture
 */

// Utils
import { $, $$, esc, escVal, icon, renderIcons } from './utils/dom.js';
import { toast } from './utils/ui.js';
import { formatDate, readPath, localizedMeta } from './utils/format.js';

// Config & Store
import { config } from './config/index.js';
import { state, ps, setToken, setUser } from './store/index.js';
import { applyPermissions } from './store/permission.js';

// i18n
import { t, setLanguage } from './i18n/index.js';

// API
import { api, getMenuConfig } from './api/request.js';

// Components
import { pageHead, btn, filterField, toolbar } from './components/toolbar.js';
import { dataTable, paginationHTML } from './components/table.js';
import { detailGrid, emptyState, statusPill, progressBar, statCard, sectionTitle } from './components/feedback.js';
import { openDrawer, closeDrawer } from './components/drawer.js';

// Layouts
import { loadNavigation, renderNavigation, navLabel } from './layouts/navigation.js';
import { login, logout, restoreSession, showWorkspace } from './layouts/auth.js';

// Router
import { renderView, reloadCurrentView } from './router/index.js';

// Views - renderers
import { renderOverview } from './views/overview.js';
import { renderMaster, loadMaster, openMasterDetail, openMasterCreate, openMasterEdit } from './views/master.js';
import { renderProducts, loadProducts, openProductDetail } from './views/products.js';
import { renderOrders, loadOrders, openOrderDetail } from './views/orders.js';
import { renderBatches, loadBatches, openBatchDetail } from './views/batches.js';
import { renderIntegration, runSync, runSingleSync } from './views/integration.js';
import { renderApiLogs, loadApiLogs, openLogDetail } from './views/api-logs.js';
import { renderJobs, loadJobs, openJobDetail } from './views/jobs.js';
import { renderIam, loadRoles, loadUsers, openRoleDetail, openRoleCreate, openUserCreate } from './views/iam.js';
import { renderMenus, loadMenus, openMenuCreate, openMenuEdit, openMenuConfig } from './views/menus.js';
import { renderDictionaries, loadDictionaries, setCurrentType, openDictionaryCreate, openDictionaryEdit, openDictTypeCreate, openDictTypeEdit } from './views/dictionaries.js';
import { renderInterfaces, loadInterfaces, openCategoryCreate, openCategoryEdit, openSystemCreate, openSystemEdit, openDefCreate, openDefEdit } from './views/interfaces.js';

/* ================================================================
   EVENT HANDLERS (consolidated single click handler)
   ================================================================ */
document.addEventListener('click', async event => {
  // Nav group toggle (accordion: collapse others when expanding)
  const groupToggle = event.target.closest('.nav-group-toggle');
  if (groupToggle) {
    const group = groupToggle.closest('.nav-group');
    const wasCollapsed = group.classList.contains('collapsed');
    $$('.nav-group').forEach(g => g.classList.add('collapsed'));
    if (wasCollapsed) group.classList.remove('collapsed');
    return;
  }

  // Tab switch
  const tab = event.target.closest('[data-tab]');
  if (tab) {
    $$('.tab', tab.closest('.tabs')).forEach(x => x.classList.remove('active'));
    tab.classList.add('active');
    const tabName = tab.dataset.tab;
    if (state.view === 'iam') { tabName === 'users' ? loadUsers() : loadRoles(); }
    return;
  }

  // Nav view switch
  const viewLink = event.target.closest('[data-view]');
  if (viewLink) { renderView(viewLink.dataset.view); if (innerWidth < 800) $('#sidebar').classList.remove('mobile-open'); return; }

  // Pagination page button
  const pageBtn = event.target.closest('[data-page]');
  if (pageBtn && pageBtn.dataset.page !== undefined) {
    const newPage = parseInt(pageBtn.dataset.page);
    const p = state.data[state.view];
    if (p && newPage >= 0 && newPage < p.totalPages) {
      const loader = ({ products: loadProducts, orders: loadOrders, batches: loadBatches, apiLogs: loadApiLogs })[state.view];
      if (loader) await loader(newPage);
    }
    return;
  }

  // Table header sort
  const sortTh = event.target.closest('th[data-sort]');
  if (sortTh) {
    const key = sortTh.dataset.sort;
    const dir = sortTh.dataset.nextDir || 'asc';
    const p = state.data[state.view];
    if (p) {
      p.sortKey = key;
      p.sortDir = dir;
      const loader = ({ products: loadProducts, orders: loadOrders, batches: loadBatches })[state.view];
      if (loader) await loader();
    }
    return;
  }

  // Action buttons
  const actionNode = event.target.closest('[data-action]');
  if (!actionNode) return;

  // Drawer click handling
  const action = actionNode.dataset.action;
  if (action === 'close-drawer') {
    // Close if clicked a close button inside the drawer, or clicked the backdrop outside the drawer
    if (actionNode.closest('[data-stop-close]') || !event.target.closest('[data-stop-close]')) { closeDrawer(); return; }
    // Click was inside drawer on non-action element (bubbled to backdrop) — don't close
    return;
  }

  // Query / Reset / Refresh
  if (action === 'query') { const p = state.data[state.view]; if (p) p.page = 0; await reloadCurrentView(); return; }
  if (action === 'reset') {
    $$('input, select', '#page .toolbar').forEach(el => { if (el.id !== 'master-type') el.value = ''; });
    const p = state.data[state.view]; if (p) { p.page = 0; p.sortKey = null; p.sortDir = 'asc'; }
    await reloadCurrentView();
    return;
  }
  if (action === 'refresh-data') { await reloadCurrentView(); return; }

  // Logout
  if (action === 'logout') { logout(true); return; }

  // Sync actions
  if (action === 'sync-products') { await runSync('products', actionNode); return; }
  if (action === 'sync-orders') { await runSync('orders', actionNode); return; }
  if (action === 'sync-batches') { await runSync('batches', actionNode); return; }

  // Single item sync actions
  if (action === 'sync-single-product') { const val = $('#single-product')?.value?.trim(); if (val) await runSingleSync('products', val, actionNode); else toast(t('pleaseEnter') + t('productCode'), true); return; }
  if (action === 'sync-single-order') { const val = $('#single-order')?.value?.trim(); if (val) await runSingleSync('orders', val, actionNode); else toast(t('pleaseEnter') + t('workOrderNo'), true); return; }
  if (action === 'sync-single-batch') { const val = $('#single-batch')?.value?.trim(); if (val) await runSingleSync('batches', val, actionNode); else toast(t('pleaseEnter') + t('batchNo'), true); return; }

  // Product row actions
  if (action === 'product-detail') { const row = actionNode.closest('tr'); const item = state.data.products?.items?.find(v => v.code === row.querySelector('.code').textContent.trim()); if (item) await openProductDetail(item); return; }
  if (action === 'product-sync') { const row = actionNode.closest('tr'); await runSingleSync('products', row.querySelector('.code').textContent.trim(), actionNode); return; }

  // Order row actions
  if (action === 'order-detail') { const row = actionNode.closest('tr'); const item = state.data.orders?.items?.find(v => v.orderNo === row.querySelector('.code').textContent.trim()); if (item) await openOrderDetail(item); return; }
  if (action === 'batch-detail') { const row = actionNode.closest('tr'); const item = state.data.batches?.items?.find(v => v.batchNo === row.querySelector('.code').textContent.trim()); if (item) openBatchDetail(item); return; }
  if (action === 'order-sync') { const row = actionNode.closest('tr'); await runSingleSync('work-orders', row.querySelector('.code').textContent.trim(), actionNode); return; }

  // Job row actions
  if (action === 'job-detail') { const row = actionNode.closest('tr'); const item = (state.data.jobs || []).find(v => v.code === row.querySelector('.code').textContent.trim()); if (item) await openJobDetail(item); return; }
  if (action === 'job-run') { const code = actionNode.closest('tr').querySelector('.code').textContent.trim(); const job = (state.data.jobs || []).find(v => v.code === code); if (job) { actionNode.disabled = true; await api('/integrations/sync-jobs/' + encodeURIComponent(job.id) + '/run', { method: 'POST' }).then(() => toast(t('started'))).catch(e => toast(e.message, true)).finally(() => { actionNode.disabled = false; loadJobs(); }); } return; }
  if (action === 'job-toggle') { const code = actionNode.closest('tr').querySelector('.code').textContent.trim(); const job = (state.data.jobs || []).find(v => v.code === code); if (job) { await api('/integrations/sync-jobs/' + job.id + '/enabled?value=' + (!job.enabled), { method: 'PUT' }); toast(t('saved')); loadJobs(); } return; }

  // API Log actions
  if (action === 'log-detail') { const idx = parseInt(actionNode.closest('tr').querySelector('td').textContent.trim()) - 1; const p = state.data.apiLogs; if (p && p.items && p.items[idx]) openLogDetail(p.items[idx]); return; }

  // Master data actions
  if (action === 'master-add') { await openMasterCreate(); return; }
  if (action === 'master-detail') { const row = actionNode.closest('tr'); const item = (state.data.master || []).find(v => v.code === row.querySelector('.code').textContent.trim()); if (item) await openMasterDetail(item); return; }
  if (action === 'master-edit') { const row = actionNode.closest('tr'); const item = (state.data.master || []).find(v => v.code === row.querySelector('.code').textContent.trim()); if (item) await openMasterEdit(item); return; }
  if (action === 'master-delete') { const type = $('#master-type')?.value || 'FACTORY'; const id = (state.data.master || []).find(v => v.code === actionNode.closest('tr').querySelector('.code').textContent.trim())?.id; if (id && confirm(t('confirmDelete'))) { await api(`/master-data/${type}/${id}`, { method: 'DELETE' }).then(() => { toast(t('saved')); loadMaster(); }); } return; }
  if (action === 'save-master') {
    const type = $('#master-type')?.value || 'FACTORY';
    const mode = actionNode.dataset.mode;
    const parentVal = $('#md-parent')?.value;
    const body = {
      code: $('#md-code').value, nameZh: $('#md-name-zh').value, nameEn: $('#md-name-en').value || '',
      nameAr: $('#md-name-ar').value || '', parentId: parentVal && parentVal !== '' ? Number(parentVal) : null,
      description: $('#md-description').value || '', status: $('#md-status').value,
      sortOrder: Number($('#md-sort-order').value || 0)
    };
    if (mode === 'create') { await api(`/master-data/${type}`, { method: 'POST', body }); }
    else { await api(`/master-data/${type}/${actionNode.dataset.id}`, { method: 'PUT', body }); }
    closeDrawer(); toast(t('saved')); loadMaster(); return;
  }

  // Role actions
  if (action === 'role-detail') { const code = actionNode.closest('tr').querySelector('.code').textContent.trim(); const role = (state.data.roles || []).find(v => v.code === code); if (role) await openRoleDetail(role); return; }
  if (action === 'save-role') { const role = (state.data.roles || []).find(v => String(v.id) === actionNode.dataset.id); const permissionCodes = $$('.perm-item:checked').map(input => input.value); await api('/iam/roles/' + role.id, { method: 'PUT', body: { nameZh: $('#role-name-zh').value, nameEn: $('#role-name-en').value, nameAr: $('#role-name-ar').value, permissionCodes } }); closeDrawer(); toast(t('permissionSaved')); loadRoles(); return; }
  if (action === 'role-delete') { const id = (state.data.roles || []).find(v => v.code === actionNode.closest('tr').querySelector('.code').textContent.trim())?.id; if (id && confirm(t('confirmDelete'))) { await api('/iam/roles/' + id, { method: 'DELETE' }).then(() => { toast(t('saved')); loadRoles(); }); } return; }

  // User actions
  if (action === 'user-toggle') { const row = actionNode.closest('tr'); const u = (state.data.users || []).find(v => v.username === row.querySelector('.code').textContent.trim()); if (u) { await api('/iam/users/' + u.id + '/status?value=' + (u.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'), { method: 'PUT' }).then(() => { toast(t('saved')); loadUsers(); }); } return; }
  if (action === 'user-delete') { const id = (state.data.users || []).find(v => v.username === actionNode.closest('tr').querySelector('.code').textContent.trim())?.id; if (id && confirm(t('confirmDelete'))) { await api('/iam/users/' + id, { method: 'DELETE' }).then(() => { toast(t('saved')); loadUsers(); }); } return; }

  // Menu actions
  if (action === 'menu-config') { const code = actionNode.closest('tr').querySelector('.code').textContent.trim(); const menu = (state.data.menus || []).find(v => v.code === code); if (menu) await openMenuConfig(menu); return; }
  if (action === 'menu-edit') { const code = actionNode.closest('tr').querySelector('.code').textContent.trim(); const menu = (state.data.menus || []).find(v => v.code === code); if (menu) openMenuEdit(menu); return; }
  if (action === 'menu-toggle') { const code = actionNode.closest('tr').querySelector('.code').textContent.trim(); const menu = (state.data.menus || []).find(v => v.code === code); if (menu) { await api('/system/menus/' + menu.id, { method: 'PUT', body: { code: menu.code, nameZh: menu.nameZh, nameEn: menu.nameEn, nameAr: menu.nameAr, parentCode: menu.parentCode, path: menu.path, icon: menu.icon, permissionCode: menu.permissionCode, sortOrder: menu.sortOrder, status: menu.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' } }).then(() => { toast(t('saved')); loadMenus(); }); } return; }
  if (action === 'menu-delete') { const id = (state.data.menus || []).find(v => v.code === actionNode.closest('tr').querySelector('.code').textContent.trim())?.id; if (id && confirm(t('confirmDelete'))) { await api('/system/menus/' + id, { method: 'DELETE' }).then(() => { toast(t('saved')); loadMenus(); }); } return; }

  // Dictionary type actions
  if (action === 'dict-type-add') { openDictTypeCreate(); return; }
  if (action === 'dict-type-edit') { const li = actionNode.closest('li'); const typeCode = li?.dataset.type; const typeName = li?.querySelector('.type-name')?.textContent || typeCode; if (typeCode) openDictTypeEdit({ type: typeCode, name: typeName }); return; }
  if (action === 'dict-type-delete') { const li = actionNode.closest('li'); const typeCode = li.dataset.type; if (typeCode && confirm(t('confirmDelete'))) { await api('/system/dictionaries/type/' + encodeURIComponent(typeCode), { method: 'DELETE' }).then(() => { toast(t('saved')); setCurrentType(''); loadDictionaries(); }); } return; }
  if (action === 'dict-type-select') { return; } // handled by direct click binding

  // Dictionary item actions
  if (action === 'dict-item-add' || action === 'dictionary-add') { openDictionaryCreate(); return; }
  if (action === 'dictionary-edit') { const row = actionNode.closest('tr'); const code = row.querySelector('.code').textContent.trim(); const item = (state.data.dictionaries || []).find(v => v.dictCode === code); if (item) openDictionaryEdit(item); return; }
  if (action === 'dictionary-delete') { const id = (state.data.dictionaries || []).find(v => v.dictCode === actionNode.closest('tr').querySelector('.code').textContent.trim())?.id; if (id && confirm(t('confirmDelete'))) { await api('/system/dictionaries/' + id, { method: 'DELETE' }).then(() => { toast(t('saved')); loadDictionaries(); }); } return; }

  // Create drawers
  if (action === 'add-role') { openRoleCreate(); return; }
  if (action === 'add-user') { openUserCreate(); return; }
  if (action === 'menu-add') { openMenuCreate(); return; }
  if (action === 'dictionary-add') { openDictionaryCreate(); return; }

  // Create submissions
  if (action === 'create-role') { await api('/iam/roles', { method: 'POST', body: { code: $('#new-role-code').value, nameZh: $('#new-role-name-zh').value, nameEn: $('#new-role-name-en').value, nameAr: $('#new-role-name-ar').value, status: $('#new-role-status').value, permissionCodes: [] } }); closeDrawer(); toast(t('saved')); loadRoles(); return; }
  if (action === 'create-user') { const roles = $('#new-user-roles').value.split(',').map(v => v.trim()).filter(Boolean); await api('/iam/users', { method: 'POST', body: { username: $('#new-user-username').value, password: $('#new-user-password').value, displayName: $('#new-user-display-name').value, email: $('#new-user-email').value, languageCode: $('#new-user-language').value, status: $('#new-user-status').value, roleCodes: roles } }); closeDrawer(); toast(t('saved')); loadUsers(); return; }
  if (action === 'create-menu') { await api('/system/menus', { method: 'POST', body: { code: $('#new-menu-code').value, nameZh: $('#new-menu-name-zh').value, nameEn: $('#new-menu-name-en').value, nameAr: $('#new-menu-name-ar').value, parentCode: $('#new-menu-parent').value || null, path: $('#new-menu-path').value, icon: $('#new-menu-icon').value, permissionCode: $('#new-menu-permission').value, sortOrder: parseInt($('#new-menu-sort').value) || 0, status: $('#new-menu-status').value } }); closeDrawer(); toast(t('saved')); loadMenus(); return; }
  if (action === 'save-menu') { const id = actionNode.dataset.id; await api('/system/menus/' + id, { method: 'PUT', body: { code: $('#edit-menu-code').value, nameZh: $('#edit-menu-name-zh').value, nameEn: $('#edit-menu-name-en').value, nameAr: $('#edit-menu-name-ar').value, parentCode: $('#edit-menu-parent').value || null, path: $('#edit-menu-path').value, icon: $('#edit-menu-icon').value, permissionCode: $('#edit-menu-permission').value, sortOrder: parseInt($('#edit-menu-sort').value) || 0, status: $('#edit-menu-status').value } }); closeDrawer(); toast(t('saved')); loadMenus(); return; }
  if (action === 'config-add-field') { const host = $('#menu-fields-config'); host.querySelector('.empty')?.remove(); host.insertAdjacentHTML('beforeend', `<div class="config-row new-config" data-config="field"><input data-key="fieldCode" placeholder="${t('fieldCode')}"><input data-key="fieldPath" placeholder="${t('fieldPath')}"><input data-key="labelZh" placeholder="中文"><input data-key="labelEn" placeholder="English"><input data-key="labelAr" placeholder="العربية"><select data-key="fieldType"><option>TEXT</option><option>NUMBER</option><option>DATE</option><option>STATUS</option></select><label class="check"><input data-key="visibleList" type="checkbox" checked>${t('listVisible')}</label><label class="check"><input data-key="visibleDetail" type="checkbox" checked>${t('detailVisible')}</label><label class="check"><input data-key="queryable" type="checkbox">${t('queryable')}</label><input data-key="sortOrder" type="number" value="0" placeholder="${t('sortOrder')}"><button class="icon-btn danger-icon" data-action="config-remove-row"><i data-lucide="trash-2"></i></button></div>`); renderIcons(); return; }
  if (action === 'config-add-action') { const host = $('#menu-actions-config'); host.querySelector('.empty')?.remove(); host.insertAdjacentHTML('beforeend', `<div class="config-row new-config" data-config="action"><input data-key="actionCode" placeholder="${t('actionCode')}"><input data-key="nameZh" placeholder="中文"><input data-key="nameEn" placeholder="English"><input data-key="nameAr" placeholder="العربية"><input data-key="permissionCode" placeholder="${t('permissionCode')}"><select data-key="actionType"><option>BUTTON</option><option>LINK</option><option>EXPORT</option></select><input data-key="sortOrder" type="number" value="0"><button class="icon-btn danger-icon" data-action="config-remove-row"><i data-lucide="trash-2"></i></button></div>`); renderIcons(); return; }
  if (action === 'config-remove-row') { actionNode.closest('.config-row')?.remove(); return; }
  if (action === 'config-delete-field' || action === 'config-delete-action') { const row = actionNode.closest('.config-row'); const id = row?.dataset.id; if (id && confirm(t('confirmDelete'))) { await api('/system/' + (action === 'config-delete-field' ? 'menu-fields/' : 'menu-actions/') + id, { method: 'DELETE' }); row.remove(); toast(t('saved')); } return; }
  if (action === 'save-menu-config') {
    const editor = actionNode.closest('.config-editor'); const menuId = editor.dataset.menuId;
    const readValue = (row, key) => { const n = row.querySelector(`[data-key="${key}"]`); return n?.type === 'checkbox' ? n.checked : (n?.value || ''); };
    const requests = [];
    $$('.config-row', editor).forEach(row => { const type = row.dataset.config; const body = type === 'field' ? { fieldCode: readValue(row,'fieldCode'), fieldPath: readValue(row,'fieldPath'), labelZh: readValue(row,'labelZh'), labelEn: readValue(row,'labelEn'), labelAr: readValue(row,'labelAr'), fieldType: readValue(row,'fieldType'), visibleList: readValue(row,'visibleList'), visibleDetail: readValue(row,'visibleDetail'), queryable: readValue(row,'queryable'), defaultVisible: true, sortOrder: Number(readValue(row,'sortOrder') || 0), status:'ACTIVE' } : { actionCode: readValue(row,'actionCode'), nameZh: readValue(row,'nameZh'), nameEn: readValue(row,'nameEn'), nameAr: readValue(row,'nameAr'), permissionCode: readValue(row,'permissionCode'), actionType: readValue(row,'actionType'), sortOrder: Number(readValue(row,'sortOrder') || 0), status:'ACTIVE' }; if (!body[type === 'field' ? 'fieldCode' : 'actionCode']) return; requests.push(api(type === 'field' ? (row.dataset.id ? '/system/menu-fields/' + row.dataset.id : '/system/menus/' + menuId + '/fields') : (row.dataset.id ? '/system/menu-actions/' + row.dataset.id : '/system/menus/' + menuId + '/actions'), { method: row.dataset.id ? 'PUT' : 'POST', body })); });
    await Promise.all(requests); closeDrawer(); toast(t('menuConfigSaved')); return;
  }
  if (action === 'create-dictionary') { await api('/system/dictionaries', { method: 'POST', body: { dictType: $('#new-dict-type').value, dictCode: $('#new-dict-code').value, labelZh: $('#new-dict-label-zh').value, labelEn: $('#new-dict-label-en').value, labelAr: $('#new-dict-label-ar').value, dictValue: $('#new-dict-value').value } }); closeDrawer(); toast(t('saved')); loadDictionaries(); return; }
  if (action === 'save-dictionary') { const id = actionNode.dataset.id; await api('/system/dictionaries/' + id, { method: 'PUT', body: { dictType: $('#edit-dict-type').value, dictCode: $('#edit-dict-code').value, labelZh: $('#edit-dict-label-zh').value, labelEn: $('#edit-dict-label-en').value, labelAr: $('#edit-dict-label-ar').value, dictValue: $('#edit-dict-value').value, sortOrder: parseInt($('#edit-dict-sort').value) || 0, status: $('#edit-dict-status').value } }); closeDrawer(); toast(t('saved')); loadDictionaries(); return; }
  if (action === 'create-dict-type') { const typeCode = $('#new-type-code')?.value?.trim(); const typeName = $('#new-type-name')?.value?.trim(); if (!typeCode) { toast(t('pleaseEnter') + t('dictType'), true); return; } await api('/system/dictionaries', { method: 'POST', body: { dictType: typeCode, dictCode: 'DEFAULT', labelZh: typeName || typeCode, dictValue: 'DEFAULT' } }); closeDrawer(); toast(t('saved')); setCurrentType(typeCode); loadDictionaries(); return; }
  if (action === 'save-dict-type') { const oldType = actionNode.dataset.oldType; const newType = $('#edit-type-new')?.value?.trim(); const newLabel = $('#edit-type-label')?.value?.trim(); if (!newType) { toast(t('pleaseEnter') + t('newType'), true); return; } await api('/system/dictionaries/type/' + encodeURIComponent(oldType), { method: 'PUT', body: { newType, newLabel } }); closeDrawer(); toast(t('saved')); setCurrentType(newType); loadDictionaries(); return; }

  // ===== Interface Management =====
  if (action === 'interface-category-add') { openCategoryCreate(); return; }
  if (action === 'interface-system-add') { openSystemCreate(); return; }
  if (action === 'interface-def-add') { openDefCreate(); return; }
  if (action === 'iface-cat-select') { return; }
  if (action === 'iface-cat-edit') { const li = actionNode.closest('li'); const code = li?.dataset.code; const cat = (await api('/interfaces/categories')).data?.find(c => c.code === code); if (cat) openCategoryEdit(cat); return; }
  if (action === 'iface-cat-delete') { const li = actionNode.closest('li'); const code = li?.dataset.code; const cat = (await api('/interfaces/categories')).data?.find(c => c.code === code); if (cat && confirm(t('confirmDelete'))) { await api('/interfaces/categories/' + cat.id, { method: 'DELETE' }).then(() => { toast(t('saved')); loadInterfaces(); }); } return; }
  if (action === 'iface-def-edit') { const row = actionNode.closest('tr'); const code = row.querySelector('.code').textContent.trim(); const def = (await api('/interfaces/definitions')).data?.find(d => d.code === code); if (def) openDefEdit(def); return; }
  if (action === 'iface-def-delete') { const row = actionNode.closest('tr'); const code = row.querySelector('.code').textContent.trim(); const def = (await api('/interfaces/definitions')).data?.find(d => d.code === code); if (def && confirm(t('confirmDelete'))) { await api('/interfaces/definitions/' + def.id, { method: 'DELETE' }).then(() => { toast(t('saved')); loadInterfaces(); }); } return; }

  if (action === 'create-iface-category') { await api('/interfaces/categories', { method: 'POST', body: { code: $('#new-cat-code').value, nameZh: $('#new-cat-name-zh').value, nameEn: $('#new-cat-name-en').value, sortOrder: parseInt($('#new-cat-sort').value) || 0, status: 'ACTIVE' } }); closeDrawer(); toast(t('saved')); loadInterfaces(); return; }
  if (action === 'save-iface-category') { const id = actionNode.dataset.id; await api('/interfaces/categories/' + id, { method: 'PUT', body: { nameZh: $('#edit-cat-name-zh').value, nameEn: $('#edit-cat-name-en').value, sortOrder: parseInt($('#edit-cat-sort').value) || 0, status: $('#edit-cat-status').value } }); closeDrawer(); toast(t('saved')); loadInterfaces(); return; }
  if (action === 'create-iface-system') { await api('/interfaces/systems', { method: 'POST', body: { code: $('#new-sys-code').value, nameZh: $('#new-sys-name-zh').value, nameEn: $('#new-sys-name-en').value, baseUrl: $('#new-sys-url').value, authType: $('#new-sys-auth').value, sortOrder: parseInt($('#new-sys-sort').value) || 0, status: 'ACTIVE' } }); closeDrawer(); toast(t('saved')); loadInterfaces(); return; }
  if (action === 'save-iface-system') { const id = actionNode.dataset.id; await api('/interfaces/systems/' + id, { method: 'PUT', body: { nameZh: $('#edit-sys-name-zh').value, nameEn: $('#edit-sys-name-en').value, baseUrl: $('#edit-sys-url').value, authType: $('#edit-sys-auth').value, sortOrder: parseInt($('#edit-sys-sort').value) || 0, status: $('#edit-sys-status').value } }); closeDrawer(); toast(t('saved')); loadInterfaces(); return; }
  if (action === 'create-iface-def') { await api('/interfaces/definitions', { method: 'POST', body: { categoryCode: $('#new-def-cat').value, systemCode: $('#new-def-sys').value, code: $('#new-def-code').value, nameZh: $('#new-def-name-zh').value, nameEn: $('#new-def-name-en').value, method: $('#new-def-method').value, path: $('#new-def-path').value, syncDirection: $('#new-def-dir').value, scheduleCron: $('#new-def-cron').value, description: $('#new-def-desc').value, sortOrder: 0, status: 'ACTIVE' } }); closeDrawer(); toast(t('saved')); loadInterfaces(); return; }
  if (action === 'save-iface-def') { const id = actionNode.dataset.id; await api('/interfaces/definitions/' + id, { method: 'PUT', body: { categoryCode: $('#edit-def-cat').value, systemCode: $('#edit-def-sys').value, nameZh: $('#edit-def-name-zh').value, nameEn: $('#edit-def-name-en').value, method: $('#edit-def-method').value, path: $('#edit-def-path').value, syncDirection: $('#edit-def-dir').value, scheduleCron: $('#edit-def-cron').value, description: $('#edit-def-desc').value, status: $('#edit-def-status').value } }); closeDrawer(); toast(t('saved')); loadInterfaces(); return; }
});

/* ================================================================
   INITIALIZATION
   ================================================================ */

// Login form
$('#login-form').addEventListener('submit', event => {
  event.preventDefault();
  const button = event.target.querySelector('button[type="submit"]');
  button.disabled = true;
  $('#login-error').textContent = '';
  login().then(() => loadNavigation().then(() => renderView('overview'))).catch(e => $('#login-error').textContent = e.message).finally(() => button.disabled = false);
});

// Sidebar
$('#sidebar-collapse').addEventListener('click', () => {
  $('#sidebar').classList.toggle('collapsed');
  $('#workspace-view').classList.toggle('sidebar-collapsed');
});
$('#mobile-menu').addEventListener('click', () => $('#sidebar').classList.toggle('mobile-open'));
$('#global-refresh').addEventListener('click', () => renderView(state.view));

// Language switcher
$$('[data-lang]').forEach(node => node.addEventListener('click', () => setLanguage(node.dataset.lang)));

// Nav search
$('#nav-search').addEventListener('input', event => {
  const term = event.target.value.toLowerCase().trim();
  $$('.nav-link, .nav-group').forEach(node => {
    const match = !term || node.textContent.toLowerCase().includes(term);
    node.classList.toggle('hidden', !match);
  });
});

// Page size selector (delegated change event)
document.addEventListener('change', event => {
  const sizeSelect = event.target.closest('[data-page-size]');
  if (!sizeSelect) return;
  const p = state.data[state.view];
  if (p) {
    p.size = parseInt(sizeSelect.value);
    p.page = 0;
    const loader = ({ products: loadProducts, orders: loadOrders, batches: loadBatches, apiLogs: loadApiLogs })[state.view];
    if (loader) loader(0);
  }
});

// Escape closes drawer; Enter triggers search
document.addEventListener('keydown', event => {
  if (event.key === 'Escape') { closeDrawer(); return; }
  if (event.key !== 'Enter') return;
  const input = event.target.closest('#page input[id$="-search"]');
  if (!input) return;
  const p = state.data[state.view];
  if (p) p.page = 0;
  reloadCurrentView();
});

// Listen for reload events from integration sync
document.addEventListener('tns:reload', (e) => {
  reloadCurrentView();
});

// Initialize
setLanguage(state.lang);
if (state.token) {
  restoreSession()
    .then(() => loadNavigation().finally(() => renderView('overview')))
    .catch(() => logout(false));
}
