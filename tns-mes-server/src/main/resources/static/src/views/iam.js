/**
 * IAM View (Identity & Access Management)
 * Roles and users management with permission tree editor
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

export function renderIam() {
  $('#page').innerHTML = pageHead(t('foundation'), t('iam'), t('accessSubtitle')) + `<div class="tabs"><button class="tab active" data-tab="roles">${t('rolesManagement')}</button><button class="tab" data-tab="users">${t('usersManagement')}</button></div><div class="panel"><div class="toolbar"><div class="muted">${t('accessSubtitle')}</div><div class="toolbar-actions">${btn('add-role', icon('plus') + t('addRole'), 'primary', 'USER_ADMIN')} ${btn('add-user', icon('user-plus') + t('addUser'), 'secondary', 'USER_ADMIN')} ${btn('refresh-data', icon('refresh-cw') + t('refresh'))}</div></div><div id="iam-table"></div></div>`;
  loadRoles();
}

export async function loadRoles() {
  const node = $('#iam-table');
  try {
    const data = await api('/iam/roles?size=200');
    state.data.roles = data.data.items || [];
    node.innerHTML = dataTable([t('code'), t('name'), t('permission'), t('status'), t('actions')], state.data.roles.map(v => `<tr><td class="code">${escVal(v.code)}</td><td>${escVal(v.nameZh)}<span class="cell-sub">${escVal(v.nameEn || v.nameAr)}</span></td><td>${(v.permissions || []).length}</td><td>${statusPill(v.status)}</td><td class="table-actions">${btn('role-detail', icon('shield-check'), 'ghost', 'USER_ADMIN')} ${btn('role-delete', icon('trash-2'), 'ghost', 'USER_ADMIN')}</td></tr>`));
  } catch (e) { node.innerHTML = emptyState(e.message); }
  renderIcons();
}

export async function loadUsers() {
  const node = $('#iam-table');
  try {
    const data = await api('/iam/users?size=200');
    state.data.users = data.data.items || [];
    node.innerHTML = dataTable([t('username'), t('name'), t('email'), t('language'), t('role'), t('status'), t('actions')], state.data.users.map(v => `<tr><td class="code">${escVal(v.username)}</td><td>${escVal(v.displayName)}</td><td>${escVal(v.email)}</td><td>${escVal(v.languageCode)}</td><td>${escVal((v.roles || []).join(', '))}</td><td>${statusPill(v.status)}</td><td class="table-actions">${btn('user-toggle', icon(v.status === 'ACTIVE' ? 'user-round-x' : 'user-round-check'), 'ghost', 'USER_ADMIN')} ${btn('user-delete', icon('trash-2'), 'ghost', 'USER_ADMIN')}</td></tr>`));
  } catch (e) { node.innerHTML = emptyState(e.message); }
  renderIcons();
}

// Role detail with permission checkboxes (tree layout for scalability)
export async function openRoleDetail(role) {
  const permissions = (await api('/iam/permissions')).data || [];
  const groups = permissions.reduce((map, p) => { (map[p.groupCode] = map[p.groupCode] || []).push(p); return map; }, {});
  const rolePerms = new Set(role.permissions || []);
  const groupNames = { MASTER_DATA: t('foundation'), ENGINEERING: t('engineering'), PRODUCTION: t('production'), INTEGRATION: t('integrationCenter'), QUALITY: t('quality'), SECURITY: t('iam'), SYSTEM: t('system'), OVERVIEW: t('overview') };
  const groupHTML = Object.entries(groups).map(([group, list]) => {
    const allChecked = list.every(p => rolePerms.has(p.code));
    const someChecked = list.some(p => rolePerms.has(p.code));
    return `<div class="perm-group" data-group="${esc(group)}">
      <div class="perm-group-header" style="display:flex;align-items:center;gap:8px;padding:8px 12px;background:var(--bg-table-header);border-radius:var(--r-sm);cursor:pointer;margin-bottom:4px;">
        <input type="checkbox" class="perm-group-all" ${allChecked ? 'checked' : ''} style="accent-color:var(--primary)">
        <i data-lucide="chevron-down" style="width:14px;height:14px"></i>
        <strong>${esc(groupNames[group] || group)}</strong>
        <span class="muted" style="margin-left:auto;font-size:12px">${list.filter(p => rolePerms.has(p.code)).length}/${list.length}</span>
      </div>
      <div class="perm-group-body" style="padding:4px 0 8px 28px;display:flex;flex-wrap:wrap;gap:4px;">
        ${list.map(p => `<label style="display:inline-flex;align-items:center;gap:4px;padding:4px 8px;background:var(--bg-page);border-radius:var(--r-sm);cursor:pointer;font-size:12px;"><input type="checkbox" value="${esc(p.code)}" class="perm-item" ${rolePerms.has(p.code) ? 'checked' : ''} style="accent-color:var(--primary)"><span>${esc(p.nameZh)}</span><span class="cell-sub" style="font-size:10px;color:var(--text-tertiary)">${esc(p.permissionType)}</span></label>`).join('')}
      </div>
    </div>`;
  }).join('');
  openDrawer(`${t('role')} · ${role.code}`, t('accessSubtitle'),
    `<div class="drawer-body"><div class="form-grid"><label><span>${t('name')}</span><input id="role-name-zh" value="${esc(role.nameZh)}"></label><label><span>English</span><input id="role-name-en" value="${esc(role.nameEn)}"></label><label><span>العربية</span><input id="role-name-ar" value="${esc(role.nameAr)}"></label></div>${sectionTitle(t('permission'), (role.permissions || []).length)}<div class="permission-groups">${groupHTML}</div></div><div class="form-footer"><button class="btn secondary" data-action="close-drawer">${t('cancel')}</button><button class="btn primary" data-action="save-role" data-id="${role.id}">${t('save')}</button></div>`);
  // Add group toggle and select-all behavior
  $$('.perm-group-header').forEach(header => {
    header.addEventListener('click', e => {
      if (e.target.tagName !== 'INPUT') {
        const body = header.nextElementSibling;
        body.style.display = body.style.display === 'none' ? 'flex' : 'none';
        const chevron = header.querySelector('[data-lucide]');
        if (chevron) { chevron.style.transform = body.style.display === 'none' ? 'rotate(-90deg)' : ''; }
      }
    });
    const groupAll = header.querySelector('.perm-group-all');
    if (groupAll) {
      groupAll.addEventListener('change', e => {
        const body = header.nextElementSibling;
        body.querySelectorAll('.perm-item').forEach(item => { item.checked = e.target.checked; });
      });
    }
  });
  renderIcons();
}

export function openRoleCreate() {
  openDrawer(t('addRole'), t('accessSubtitle'),
    `<div class="drawer-body"><div class="form-grid"><label><span>${t('code')}</span><input id="new-role-code" required></label><label><span>${t('name')}</span><input id="new-role-name-zh" required></label><label><span>English</span><input id="new-role-name-en"></label><label><span>العربية</span><input id="new-role-name-ar"></label><label><span>${t('status')}</span><select id="new-role-status"><option value="ACTIVE">${t('enabled')}</option><option value="INACTIVE">${t('disabled')}</option></select></label></div></div><div class="form-footer"><button class="btn secondary" data-action="close-drawer">${t('cancel')}</button><button class="btn primary" data-action="create-role">${t('save')}</button></div>`);
}

export function openUserCreate() {
  openDrawer(t('addUser'), t('accessSubtitle'),
    `<div class="drawer-body"><div class="form-grid"><label><span>${t('username')}</span><input id="new-user-username" required></label><label><span>${t('password')}</span><input id="new-user-password" type="password" minlength="8" placeholder="${esc(t('passwordHint'))}" required></label><label><span>${t('displayName')}</span><input id="new-user-display-name" required></label><label><span>${t('email')}</span><input id="new-user-email" type="email"></label><label><span>${t('language')}</span><select id="new-user-language"><option value="zh-CN">中文</option><option value="en">English</option><option value="ar-TN">العربية</option></select></label><label><span>${t('status')}</span><select id="new-user-status"><option value="ACTIVE">${t('enabled')}</option><option value="INACTIVE">${t('disabled')}</option></select></label><label><span>${t('roleCodes')}</span><input id="new-user-roles" placeholder="MES_ADMIN, MES_OPERATOR"></label></div></div><div class="form-footer"><button class="btn secondary" data-action="close-drawer">${t('cancel')}</button><button class="btn primary" data-action="create-user">${t('save')}</button></div>`);
}
