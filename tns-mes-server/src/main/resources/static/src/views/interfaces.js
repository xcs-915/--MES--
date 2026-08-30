/**
 * Interfaces View
 * Multi-system interface management - categories, external systems, and interface definitions.
 * Left: interface categories (大类), Right: interface definitions table
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

let categories = [];
let systems = [];
let currentCategory = '';
let currentSystemFilter = '';

export function renderInterfaces() {
  $('#page').innerHTML = pageHead(t('interfaceManagement'), t('interfaceManagementSubtitle'), t('interfaceManagementSubtitle'),
    btn('interface-category-add', icon('folder-plus') + t('add') + t('interfaceCategory'), 'primary', 'INTEGRATION_WRITE')
    + btn('interface-system-add', icon('server') + t('add') + t('externalSystem'), 'secondary', 'INTEGRATION_WRITE')
    + btn('interface-def-add', icon('plus') + t('add') + t('interfaceDefinition'), 'secondary', 'INTEGRATION_WRITE'))
    + `<div class="split-layout">
      <div class="split-panel">
        <div class="panel-header">
          <h3>${icon('folder-tree')} ${esc(t('interfaceCategory'))}</h3>
          <input id="iface-cat-search" type="text" placeholder="${esc(t('search'))}..." class="input-sm">
        </div>
        <div class="panel-body" id="iface-cat-list"></div>
      </div>
      <div class="split-panel">
        <div class="panel-header">
          <h3 id="iface-def-title">${icon('network')} ${esc(t('interfaceDefinition'))}</h3>
          <div class="toolbar-actions">
            <select id="iface-system-filter" class="input-sm">
              <option value="">${esc(t('all'))}${esc(t('externalSystem'))}</option>
            </select>
            ${btn('refresh-data', icon('refresh-cw') + t('refresh'), 'secondary')}
          </div>
        </div>
        <div class="panel-body" id="iface-def-table"></div>
      </div>
    </div>`;
  $('#iface-cat-search')?.addEventListener('input', e => renderCategoryList(e.target.value));
  $('#iface-system-filter')?.addEventListener('change', e => { currentSystemFilter = e.target.value; loadDefinitions(); });
  loadCategories();
  loadSystems();
}

async function loadCategories() {
  try {
    const res = await api('/interfaces/categories');
    categories = res.data || [];
    renderCategoryList();
    if (categories.length > 0 && !currentCategory) {
      currentCategory = categories[0].code;
      loadDefinitions();
    }
  } catch (e) {
    $('#iface-cat-list').innerHTML = emptyState(e.message);
  }
  renderIcons();
}

async function loadSystems() {
  try {
    const res = await api('/interfaces/systems');
    systems = res.data || [];
    // Populate system filter dropdown
    const sel = $('#iface-system-filter');
    if (sel) {
      const currentVal = sel.value;
      sel.innerHTML = `<option value="">${esc(t('all'))}${esc(t('externalSystem'))}</option>` +
        systems.map(s => `<option value="${escVal(s.code)}">${escVal(s.nameZh || s.nameEn || s.code)}</option>`).join('');
      sel.value = currentVal;
    }
  } catch (e) {
    // Systems load failed - filter just won't have options
  }
  renderIcons();
}

function renderCategoryList(filter) {
  const node = $('#iface-cat-list');
  if (!node) return;
  const keyword = (filter || '').toLowerCase().trim();
  const filtered = keyword ? categories.filter(c =>
    (c.code || '').toLowerCase().includes(keyword) || (c.nameZh || '').toLowerCase().includes(keyword)
  ) : categories;
  if (!filtered.length) {
    node.innerHTML = emptyState(t('noData'));
    return;
  }
  node.innerHTML = `<ul class="dict-type-list">${filtered.map(c => `
    <li data-code="${esc(c.code)}" class="${currentCategory === c.code ? 'active' : ''}">
      <div class="type-info" data-action="iface-cat-select">
        <div class="type-name">${escVal(c.nameZh || c.code)}</div>
        <div class="type-code">${escVal(c.code)}</div>
      </div>
      <div class="type-actions">
        ${btn('iface-cat-edit', icon('pencil'), 'ghost', 'INTEGRATION_WRITE')}
        ${btn('iface-cat-delete', icon('trash-2'), 'ghost', 'INTEGRATION_WRITE')}
      </div>
    </li>
  `).join('')}</ul>`;
  $$('#iface-cat-list .type-info').forEach(el => {
    el.addEventListener('click', () => {
      const li = el.closest('li');
      currentCategory = li.dataset.code;
      $$('#iface-cat-list li').forEach(x => x.classList.remove('active'));
      li.classList.add('active');
      loadDefinitions();
    });
  });
  renderIcons();
}

async function loadDefinitions() {
  const node = $('#iface-def-table');
  const title = $('#iface-def-title');
  if (!node) return;
  const catInfo = categories.find(c => c.code === currentCategory);
  if (title) title.innerHTML = `${icon('network')} ${esc(t('interfaceDefinition'))} - ${escVal(catInfo?.nameZh || currentCategory || t('all'))}`;
  try {
    let url = '/interfaces/definitions';
    const params = [];
    if (currentCategory) params.push('categoryCode=' + encodeURIComponent(currentCategory));
    if (currentSystemFilter) params.push('systemCode=' + encodeURIComponent(currentSystemFilter));
    if (params.length) url += '?' + params.join('&');
    const res = await api(url);
    const items = res.data || [];
    const headers = [
      { key: 'code', label: t('code'), sortable: false },
      { key: 'name', label: t('name'), sortable: false },
      { key: 'system', label: t('system'), sortable: false },
      { key: 'method', label: t('httpMethod'), sortable: false },
      { key: 'path', label: t('path'), sortable: false },
      { key: 'direction', label: t('syncDirection'), sortable: false },
      { key: 'cron', label: t('cron'), sortable: false },
      { key: 'status', label: t('status'), sortable: false },
      { key: 'actions', label: t('actions'), sortable: false }
    ];
    const sysMap = {};
    systems.forEach(s => { sysMap[s.code] = s.nameZh || s.nameEn || s.code; });
    const dirMap = { INBOUND: t('dirInbound'), OUTBOUND: t('dirOutbound'), BIDIRECTIONAL: t('dirBidirectional') };
    const methodColor = { GET: 'success', POST: 'warn', PUT: 'primary', DELETE: 'danger' };
    const rows = items.map(v => `<tr>
      <td class="code">${escVal(v.code)}</td>
      <td>${escVal(v.nameZh || v.nameEn || v.code)}${v.description ? `<span class="cell-sub">${escVal(v.description)}</span>` : ''}</td>
      <td>${escVal(sysMap[v.systemCode] || v.systemCode)}</td>
      <td><span class="method-badge ${methodColor[v.method] || ''}">${escVal(v.method)}</span></td>
      <td class="code" style="max-width:240px;overflow:hidden;text-overflow:ellipsis">${escVal(v.path || '-')}</td>
      <td>${escVal(dirMap[v.syncDirection] || v.syncDirection)}</td>
      <td>${escVal(v.scheduleCron || '-')}</td>
      <td>${statusPill(v.status)}</td>
      <td>${btn('iface-def-edit', icon('edit-3'), 'ghost', 'INTEGRATION_WRITE')} ${btn('iface-def-delete', icon('trash-2'), 'ghost', 'INTEGRATION_WRITE')}</td>
    </tr>`);
    node.innerHTML = items.length ? dataTable(headers, rows) : emptyState();
  } catch (e) {
    node.innerHTML = emptyState(e.message);
  }
  renderIcons();
}

export async function loadInterfaces() {
  await Promise.all([loadCategories(), loadSystems()]);
}

// ===== Drawer Forms =====

export function openCategoryCreate() {
  openDrawer(t('add') + ' · ' + t('interfaceCategory'), t('interfaceManagementSubtitle'),
    `<div class="drawer-body"><div class="form-grid">
      <label><span>${t('code')}</span><input id="new-cat-code" required></label>
      <label><span>${t('name')}</span><input id="new-cat-name-zh" required></label>
      <label><span>English</span><input id="new-cat-name-en"></label>
      <label><span>${t('sortOrder')}</span><input id="new-cat-sort" type="number" value="0"></label>
    </div></div>
    <div class="form-footer">
      <button class="btn secondary" data-action="close-drawer">${t('cancel')}</button>
      <button class="btn primary" data-action="create-iface-category">${t('save')}</button>
    </div>`);
}

export function openCategoryEdit(cat) {
  openDrawer(t('edit') + ' · ' + t('interfaceCategory'), t('interfaceManagementSubtitle'),
    `<div class="drawer-body"><div class="form-grid">
      <label><span>${t('code')}</span><input value="${escVal(cat.code)}" disabled></label>
      <label><span>${t('name')}</span><input id="edit-cat-name-zh" value="${escVal(cat.nameZh || '')}" required></label>
      <label><span>English</span><input id="edit-cat-name-en" value="${escVal(cat.nameEn || '')}"></label>
      <label><span>${t('sortOrder')}</span><input id="edit-cat-sort" type="number" value="${escVal(cat.sortOrder || 0)}"></label>
      <label><span>${t('status')}</span><select id="edit-cat-status"><option value="ACTIVE">${t('enabled')}</option><option value="INACTIVE">${t('disabled')}</option></select></label>
    </div></div>
    <div class="form-footer">
      <button class="btn secondary" data-action="close-drawer">${t('cancel')}</button>
      <button class="btn primary" data-action="save-iface-category" data-id="${cat.id}">${t('save')}</button>
    </div>`);
  setTimeout(() => { const s = $('#edit-cat-status'); if (s) s.value = cat.status || 'ACTIVE'; }, 0);
}

export function openSystemCreate() {
  openDrawer(t('add') + ' · ' + t('externalSystem'), t('interfaceManagementSubtitle'),
    `<div class="drawer-body"><div class="form-grid">
      <label><span>${t('code')}</span><input id="new-sys-code" required></label>
      <label><span>${t('name')}</span><input id="new-sys-name-zh" required></label>
      <label><span>English</span><input id="new-sys-name-en"></label>
      <label><span>Base URL</span><input id="new-sys-url" placeholder="https://..."></label>
      <label><span>${t('authType')}</span><select id="new-sys-auth"><option value="BASIC">BASIC</option><option value="BEARER">BEARER</option><option value="OAUTH2">OAUTH2</option><option value="NONE">NONE</option></select></label>
      <label><span>${t('sortOrder')}</span><input id="new-sys-sort" type="number" value="0"></label>
    </div></div>
    <div class="form-footer">
      <button class="btn secondary" data-action="close-drawer">${t('cancel')}</button>
      <button class="btn primary" data-action="create-iface-system">${t('save')}</button>
    </div>`);
}

export function openSystemEdit(sys) {
  openDrawer(t('edit') + ' · ' + t('externalSystem'), t('interfaceManagementSubtitle'),
    `<div class="drawer-body"><div class="form-grid">
      <label><span>${t('code')}</span><input value="${escVal(sys.code)}" disabled></label>
      <label><span>${t('name')}</span><input id="edit-sys-name-zh" value="${escVal(sys.nameZh || '')}" required></label>
      <label><span>English</span><input id="edit-sys-name-en" value="${escVal(sys.nameEn || '')}"></label>
      <label><span>Base URL</span><input id="edit-sys-url" value="${escVal(sys.baseUrl || '')}"></label>
      <label><span>${t('authType')}</span><select id="edit-sys-auth"><option value="BASIC">BASIC</option><option value="BEARER">BEARER</option><option value="OAUTH2">OAUTH2</option><option value="NONE">NONE</option></select></label>
      <label><span>${t('sortOrder')}</span><input id="edit-sys-sort" type="number" value="${escVal(sys.sortOrder || 0)}"></label>
      <label><span>${t('status')}</span><select id="edit-sys-status"><option value="ACTIVE">${t('enabled')}</option><option value="INACTIVE">${t('disabled')}</option></select></label>
    </div></div>
    <div class="form-footer">
      <button class="btn secondary" data-action="close-drawer">${t('cancel')}</button>
      <button class="btn primary" data-action="save-iface-system" data-id="${sys.id}">${t('save')}</button>
    </div>`);
  setTimeout(() => {
    $('#edit-sys-auth').value = sys.authType || 'BASIC';
    $('#edit-sys-status').value = sys.status || 'ACTIVE';
  }, 0);
}

export function openDefCreate() {
  const catOptions = categories.map(c => `<option value="${escVal(c.code)}"${c.code === currentCategory ? ' selected' : ''}>${escVal(c.nameZh || c.code)}</option>`).join('');
  const sysOptions = systems.map(s => `<option value="${escVal(s.code)}">${escVal(s.nameZh || s.code)}</option>`).join('');
  openDrawer(t('add') + ' · ' + t('interfaceDefinition'), t('interfaceManagementSubtitle'),
    `<div class="drawer-body"><div class="form-grid">
      <label><span>${t('interfaceCategory')}</span><select id="new-def-cat">${catOptions}</select></label>
      <label><span>${t('externalSystem')}</span><select id="new-def-sys">${sysOptions}</select></label>
      <label><span>${t('code')}</span><input id="new-def-code" required></label>
      <label><span>${t('name')}</span><input id="new-def-name-zh" required></label>
      <label><span>English</span><input id="new-def-name-en"></label>
      <label><span>${t('httpMethod')}</span><select id="new-def-method"><option>GET</option><option>POST</option><option>PUT</option><option>DELETE</option></select></label>
      <label><span>${t('path')}</span><input id="new-def-path" placeholder="/api/..."></label>
      <label><span>${t('syncDirection')}</span><select id="new-def-dir"><option value="INBOUND">${t('dirInbound')}</option><option value="OUTBOUND">${t('dirOutbound')}</option><option value="BIDIRECTIONAL">${t('dirBidirectional')}</option></select></label>
      <label><span>${t('cron')}</span><input id="new-def-cron" placeholder="0 */15 * * * *"></label>
      <label class="full"><span>${t('description')}</span><input id="new-def-desc"></label>
    </div></div>
    <div class="form-footer">
      <button class="btn secondary" data-action="close-drawer">${t('cancel')}</button>
      <button class="btn primary" data-action="create-iface-def">${t('save')}</button>
    </div>`);
}

export function openDefEdit(def) {
  const catOptions = categories.map(c => `<option value="${escVal(c.code)}"${c.code === def.categoryCode ? ' selected' : ''}>${escVal(c.nameZh || c.code)}</option>`).join('');
  const sysOptions = systems.map(s => `<option value="${escVal(s.code)}"${s.code === def.systemCode ? ' selected' : ''}>${escVal(s.nameZh || s.code)}</option>`).join('');
  openDrawer(t('edit') + ' · ' + t('interfaceDefinition'), t('interfaceManagementSubtitle'),
    `<div class="drawer-body"><div class="form-grid">
      <label><span>${t('code')}</span><input value="${escVal(def.code)}" disabled></label>
      <label><span>${t('interfaceCategory')}</span><select id="edit-def-cat">${catOptions}</select></label>
      <label><span>${t('externalSystem')}</span><select id="edit-def-sys">${sysOptions}</select></label>
      <label><span>${t('name')}</span><input id="edit-def-name-zh" value="${escVal(def.nameZh || '')}" required></label>
      <label><span>English</span><input id="edit-def-name-en" value="${escVal(def.nameEn || '')}"></label>
      <label><span>${t('httpMethod')}</span><select id="edit-def-method"><option>GET</option><option>POST</option><option>PUT</option><option>DELETE</option></select></label>
      <label class="full"><span>${t('path')}</span><input id="edit-def-path" value="${escVal(def.path || '')}"></label>
      <label><span>${t('syncDirection')}</span><select id="edit-def-dir"><option value="INBOUND">${t('dirInbound')}</option><option value="OUTBOUND">${t('dirOutbound')}</option><option value="BIDIRECTIONAL">${t('dirBidirectional')}</option></select></label>
      <label><span>${t('cron')}</span><input id="edit-def-cron" value="${escVal(def.scheduleCron || '')}"></label>
      <label class="full"><span>${t('description')}</span><input id="edit-def-desc" value="${escVal(def.description || '')}"></label>
      <label><span>${t('status')}</span><select id="edit-def-status"><option value="ACTIVE">${t('enabled')}</option><option value="INACTIVE">${t('disabled')}</option></select></label>
    </div></div>
    <div class="form-footer">
      <button class="btn secondary" data-action="close-drawer">${t('cancel')}</button>
      <button class="btn primary" data-action="save-iface-def" data-id="${def.id}">${t('save')}</button>
    </div>`);
  setTimeout(() => {
    $('#edit-def-method').value = def.method || 'GET';
    $('#edit-def-dir').value = def.syncDirection || 'INBOUND';
    $('#edit-def-status').value = def.status || 'ACTIVE';
  }, 0);
}
