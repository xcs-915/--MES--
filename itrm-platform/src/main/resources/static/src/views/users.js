import { $, esc, formatDateTime } from '../utils/dom.js';
import { api } from '../api/request.js';
import { openModal, emptyState, loadingState, showToast, confirm, pagination } from './_ui.js';

const PAGE_SIZE = 50;
let users = [];
let roles = [];
let departments = [];
let keyword = '';
let currentPage = 0;

export async function renderUsers() {
  $('#page').innerHTML = `<div class="page-container">
    <div class="page-header">
      <div><div class="page-title">用户与部门</div><div class="page-subtitle">统一维护平台账号、组织部门和岗位角色</div></div>
      <div class="page-actions">
        <button class="btn btn-secondary" data-permission="user:create" data-action="new-department">＋ 新增部门</button>
        <button class="btn btn-primary" data-permission="user:create" data-action="new-user">＋ 新建用户</button>
      </div>
    </div>
    <div class="user-summary" id="user-summary"></div>
    <div class="card">
      <div class="page-toolbar">
        <div class="toolbar-left">
          <div class="search-bar"><span>⌕</span><input id="user-keyword" placeholder="搜索姓名、账号、部门"></div>
          <select id="user-department" class="form-select"><option value="">全部部门</option></select>
          <select id="user-status" class="form-select"><option value="">全部状态</option><option value="ACTIVE">正常</option><option value="DISABLED">已停用</option></select>
        </div>
        <div class="toolbar-right"><span class="table-count" id="user-count"></span></div>
      </div>
      <div id="user-table" class="table-wrapper">${loadingState()}</div>
      <div id="user-pagination" style="display:flex;justify-content:flex-end;margin-top:16px"></div>
    </div>
  </div>`;
  const root = $('.page-container');
  root.addEventListener('click', onClick);
  $('#user-keyword').oninput = event => { keyword = event.target.value.trim().toLowerCase(); currentPage = 0; draw(); };
  $('#user-status').onchange = () => { currentPage = 0; draw(); };
  $('#user-department').onchange = () => { currentPage = 0; draw(); };
  await load();
}

async function load() {
  try {
    const [userResult, roleResult, departmentResult] = await Promise.all([
      api('/admin/users'), api('/admin/roles'), api('/admin/departments'),
    ]);
    users = userResult?.data || [];
    roles = roleResult?.data || [];
    departments = departmentResult?.data || [];
    const selected = $('#user-department')?.value || '';
    $('#user-department').innerHTML = `<option value="">全部部门</option>${departmentOptions(selected)}`;
    draw();
  } catch (error) {
    $('#user-table').innerHTML = emptyState('!', error.message);
  }
}

function filteredUsers() {
  const status = $('#user-status')?.value || '';
  const department = $('#user-department')?.value || '';
  return users.filter(user => (!status || user.status === status)
    && (!department || user.department === department)
    && (!keyword || [user.username, user.realName, user.department, user.email]
      .some(value => String(value || '').toLowerCase().includes(keyword))));
}

function draw() {
  const rows = filteredUsers();
  const active = users.filter(item => item.status === 'ACTIVE').length;
  const activeDepartments = departments.filter(item => item.status === 'ACTIVE').length;
  $('#user-summary').innerHTML = `<div><span class="summary-icon blue">♙</span><p><strong>${users.length}</strong><span>全部用户</span></p></div><div><span class="summary-icon green">✓</span><p><strong>${active}</strong><span>正常账号</span></p></div><div><span class="summary-icon violet">◇</span><p><strong>${roles.filter(item => item.status === 'ACTIVE').length}</strong><span>可用角色</span></p></div><div><span class="summary-icon amber">⌂</span><p><strong>${activeDepartments}</strong><span>组织部门</span></p></div>`;
  $('#user-count').textContent = `共 ${rows.length} 人`;
  const totalPages = Math.max(1, Math.ceil(rows.length / PAGE_SIZE));
  currentPage = Math.min(currentPage, totalPages - 1);
  const pageRows = rows.slice(currentPage * PAGE_SIZE, (currentPage + 1) * PAGE_SIZE);
  $('#user-table').innerHTML = pageRows.length ? `<table class="data-table"><thead><tr><th>用户</th><th>账号</th><th>部门</th><th>角色</th><th>状态</th><th>创建时间</th><th style="width:220px">操作</th></tr></thead><tbody>${pageRows.map(userRow).join('')}</tbody></table>` : emptyState('♙', '没有符合条件的用户');
  const paginationRoot = $('#user-pagination');
  paginationRoot.innerHTML = pagination({ total: rows.length, totalPages, page: currentPage });
  paginationRoot.querySelectorAll('[data-page]').forEach(button => button.onclick = () => {
    currentPage = Number(button.dataset.page);
    draw();
  });
  document.dispatchEvent(new CustomEvent('itrm:permissions-rendered'));
}

function userRow(user) {
  return `<tr><td><div class="person-cell"><span class="avatar">${esc((user.realName || user.username)[0])}</span><div><strong>${esc(user.realName || '-')}</strong><small>${esc(user.email || '未设置邮箱')}</small></div></div></td><td><code>${esc(user.username)}</code></td><td>${departmentCell(user.department)}</td><td><span class="role-chip">${esc(roleName(user.role))}</span></td><td><span class="status-dot-label ${user.status === 'ACTIVE' ? 'active' : 'disabled'}"><i></i>${user.status === 'ACTIVE' ? '正常' : '已停用'}</span></td><td>${formatDateTime(user.createdAt)}</td><td class="actions"><button class="btn-link" data-permission="user:edit" data-action="edit" data-id="${user.id}">编辑</button><button class="btn-link" data-permission="user:reset-password" data-action="password" data-id="${user.id}">重置密码</button><button class="btn-link ${user.status === 'ACTIVE' ? 'danger' : ''}" data-permission="user:disable" data-action="status" data-id="${user.id}">${user.status === 'ACTIVE' ? '停用' : '启用'}</button></td></tr>`;
}

function departmentCell(path) {
  if (!path) return '—';
  const parts = path.split('/');
  const name = parts.pop();
  return `<div title="${esc(path)}"><strong>${esc(name)}</strong>${parts.length ? `<small style="display:block;color:var(--it-color-text-tertiary);margin-top:3px">${esc(parts.join(' / '))}</small>` : ''}</div>`;
}

function departmentOptions(selected = '') {
  return departments.filter(item => item.status === 'ACTIVE').map(item => {
    const depth = Math.max(0, item.fullPath.split('/').length - 1);
    const prefix = depth ? `${'　'.repeat(depth)}└ ` : '';
    return `<option value="${esc(item.fullPath)}" ${selected === item.fullPath ? 'selected' : ''}>${prefix}${esc(item.name)}</option>`;
  }).join('');
}

function roleName(code) {
  return roles.find(role => role.code === code)?.name || code || '-';
}

function onClick(event) {
  const button = event.target.closest('[data-action]');
  if (!button) return;
  const user = users.find(item => String(item.id) === button.dataset.id);
  if (button.dataset.action === 'new-user') form();
  if (button.dataset.action === 'new-department') departmentForm();
  if (button.dataset.action === 'edit') form(user);
  if (button.dataset.action === 'password') password(user);
  if (button.dataset.action === 'status') toggle(user);
}

function form(user) {
  const isEdit = !!user;
  const body = `<form class="form-grid user-form"><div class="form-item"><label class="form-label">账号 *</label><input class="form-input" name="username" ${isEdit ? 'disabled' : ''} value="${esc(user?.username || '')}" placeholder="英文账号"></div><div class="form-item"><label class="form-label">姓名 *</label><input class="form-input" name="realName" value="${esc(user?.realName || '')}"></div>${isEdit ? '' : `<div class="form-item"><label class="form-label">初始密码 *</label><input class="form-input" type="password" name="password" value="taikang123."></div>`}<div class="form-item"><label class="form-label">部门</label><select class="form-select" name="department"><option value="">未设置</option>${departmentOptions(user?.department || '')}</select></div><div class="form-item"><label class="form-label">角色 *</label><select class="form-select" name="role">${roles.filter(role => role.status === 'ACTIVE').map(role => `<option value="${role.code}" ${user?.role === role.code ? 'selected' : ''}>${esc(role.name)}</option>`).join('')}</select></div><div class="form-item"><label class="form-label">邮箱</label><input class="form-input" type="email" name="email" value="${esc(user?.email || '')}"></div></form>`;
  openModal(isEdit ? '编辑用户' : '新建用户', body, async overlay => {
    const formElement = overlay.querySelector('form');
    const data = Object.fromEntries(new FormData(formElement));
    if (!data.realName || (!isEdit && (!data.username || !data.password))) {
      showToast('请填写必填项', 'warning');
      return Promise.reject(new Error('必填项未填写'));
    }
    await api(isEdit ? `/admin/users/${user.id}` : '/admin/users', { method: isEdit ? 'PUT' : 'POST', body: data });
    showToast(isEdit ? '用户已更新' : '用户已创建', 'success');
    await load();
  }, isEdit ? '保存修改' : '创建用户');
}

function departmentForm() {
  const body = `<form><div class="form-item"><label class="form-label">上级部门</label><select class="form-select" name="parentPath"><option value="">无（一级部门）</option>${departmentOptions()}</select></div><div class="form-item"><label class="form-label required">部门名称</label><input class="form-input" name="name" placeholder="请输入部门名称" required></div></form>`;
  openModal('新增部门', body, async overlay => {
    const formElement = overlay.querySelector('form');
    const data = Object.fromEntries(new FormData(formElement));
    if (!data.name.trim()) return Promise.reject(new Error('请填写部门名称'));
    await api('/admin/departments', { method: 'POST', body: data });
    showToast('部门已新增', 'success');
    await load();
  }, '新增部门');
}

function password(user) {
  openModal('重置密码', `<div class="form-item"><label class="form-label">${esc(user.realName || user.username)} 的新密码</label><input id="new-password" class="form-input" type="password" value="taikang123."></div>`, async overlay => {
    const value = overlay.querySelector('#new-password').value;
    if (value.length < 6) return Promise.reject(new Error('密码至少 6 位'));
    await api(`/admin/users/${user.id}/password`, { method: 'PUT', body: { password: value } });
    showToast('密码已重置', 'success');
  }, '确认重置');
}

function toggle(user) {
  confirm({
    title: user.status === 'ACTIVE' ? '停用用户' : '启用用户',
    message: `确定${user.status === 'ACTIVE' ? '停用' : '启用'}“${user.realName || user.username}”吗？`,
    onOk: async () => {
      await api(`/admin/users/${user.id}/status`, { method: 'PUT', body: { status: user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE' } });
      showToast('账号状态已更新', 'success');
      await load();
    },
  });
}
