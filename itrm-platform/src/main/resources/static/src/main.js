import { $, $$, esc } from './utils/dom.js';
import { state, setToken, setUser, getUser, getToken, hasPermission, resetState } from './store/index.js';
import { api } from './api/request.js';
import * as UI from './views/_ui.js';
import { renderDashboard } from './views/dashboard.js';
import { renderRequirements } from './views/requirements.js';
import { renderTasks } from './views/tasks.js';
import { renderBoard } from './views/board.js';
import { renderTesting } from './views/testing.js';
import { renderSchedule } from './views/schedule.js';
import { renderReports } from './views/reports.js';
import { renderWorkflows } from './views/workflows.js';
import { renderUsers } from './views/users.js';
import { renderRoles } from './views/roles.js';

window.UI = UI;

const views = {
  dashboard: { title: '工作台', permission: 'dashboard:view', render: renderDashboard },
  requirements: { title: '需求池', permission: 'requirement:view', render: renderRequirements },
  board: { title: '研发看板', permission: 'board:view', render: renderBoard },
  tasks: { title: '开发任务', permission: 'task:view', render: renderTasks },
  schedule: { title: '排期与负载', permission: 'schedule:view', render: renderSchedule },
  testing: { title: '测试与缺陷', permission: 'test:view', render: renderTesting },
  reports: { title: '统计分析', permission: 'report:view', render: renderReports },
  workflows: { title: '审批流', permission: 'workflow:view', render: renderWorkflows },
  users: { title: '用户管理', permission: 'user:view', render: renderUsers },
  roles: { title: '角色权限', permission: 'role:view', render: renderRoles },
};

const roleLabels = {
  ADMIN: '系统管理员', BUSINESS_USER: '业务需求人', IT_REVIEWER: '需求评审人',
  IT_PLANNER: '项目经理', DEVELOPER: '开发人员', TESTER: '测试人员', RELEASE_ENGINEER: '发布工程师',
  THIRD_PARTY: '第三方协作用户', IT_APPROVER: '需求审批人', IT_APPROVER_DEVELOPER: '需求审批与开发',
};

function roleLabel(role) { return roleLabels[role] || role || '普通用户'; }

function showLogin() {
  $('#login-view')?.classList.remove('hidden');
  $('#app-view')?.classList.add('hidden');
  const remembered = localStorage.getItem('itrm_remembered_user');
  if (remembered && $('#login-username')) $('#login-username').value = remembered;
}

async function showApp() {
  $('#login-view')?.classList.add('hidden');
  $('#app-view')?.classList.remove('hidden');
  try {
    const me = await api('/auth/me', { silent: true });
    if (me?.data) setUser(me.data);
  } catch { /* cached identity remains usable until the API reports 401 */ }
  applyIdentity();
  applyPermissions();
  const first = $$('.nav-link:not(.hidden)')[0]?.dataset.view || 'dashboard';
  await renderView(first);
  updatePendingBadge();
  updateNotificationBadge();
}

function applyIdentity() {
  const user = getUser() || {};
  const name = user.realName || user.username || '用户';
  const avatar = name.slice(0, 1).toUpperCase();
  $('#user-name').textContent = name;
  $('#user-role').textContent = roleLabel(user.role);
  $('#user-avatar').textContent = avatar;
  $('#topbar-avatar').textContent = avatar;
  $('#topbar-user-name').textContent = name;
}

function applyPermissions(root = document) {
  root.querySelectorAll('[data-permission]').forEach(el => {
    const allowed = el.dataset.permission.split(',').some(hasPermission);
    el.classList.toggle('hidden', !allowed);
    el.setAttribute('aria-hidden', String(!allowed));
  });
  $$('.nav-group').forEach(group => {
    group.classList.toggle('hidden', !group.querySelector('.nav-link:not(.hidden)'));
  });
}

async function doLogin() {
  const username = $('#login-username').value.trim();
  const password = $('#login-password').value;
  const error = $('#login-error');
  error.classList.add('hidden');
  if (!username || !password) {
    error.textContent = '请输入账号和密码'; error.classList.remove('hidden'); return;
  }
  const btn = $('#login-btn'); btn.disabled = true; btn.innerHTML = '<span class="spinner spinner-sm"></span><span>正在登录</span>';
  try {
    const res = await api('/auth/login', { method: 'POST', body: { username, password }, silent: true });
    setToken(res.data.token); setUser(res.data.user);
    if ($('#remember-user').checked) localStorage.setItem('itrm_remembered_user', username);
    else localStorage.removeItem('itrm_remembered_user');
    await showApp();
  } catch (e) {
    error.textContent = e.message || '登录失败，请检查账号和密码'; error.classList.remove('hidden');
  } finally {
    btn.disabled = false; btn.innerHTML = '<span>登录平台</span><span aria-hidden="true">→</span>';
  }
}

function doLogout() {
  UI.confirm({ title: '退出登录', message: '确定退出当前账号吗？', okText: '退出', onOk: () => {
    resetState(); UI.closeModal(); showLogin();
  }});
}

export async function renderView(view) {
  const config = views[view] || views.dashboard;
  if (!hasPermission(config.permission)) {
    UI.showToast('你没有访问此模块的权限', 'warning'); return;
  }
  state.view = view;
  $$('.nav-link').forEach(link => link.classList.toggle('active', link.dataset.view === view));
  $('#breadcrumb-current').textContent = config.title;
  closeMobileSidebar();
  $('#page').innerHTML = '<div class="page-loading"><span class="spinner"></span><span>正在加载</span></div>';
  try {
    await config.render();
    applyPermissions($('#page'));
  } catch (e) {
    $('#page').innerHTML = `<div class="page-container">${UI.emptyState('!', esc(e.message || '页面加载失败'))}</div>`;
    UI.showToast(e.message || '页面加载失败', 'error');
  }
}

async function updatePendingBadge() {
  if (!hasPermission('requirement:view')) return;
  try {
    const res = await api('/requirements/stats', { silent: true });
    const d = res?.data || {};
    const value = d.pendingReceive ?? d.pending ?? d.PENDING_RECEIVE ?? 0;
    $('#nav-pending-count').textContent = value > 99 ? '99+' : value;
    $('#nav-pending-count').classList.toggle('hidden', !value);
  } catch { $('#nav-pending-count')?.classList.add('hidden'); }
}

function openGlobalSearch() {
  const modal = UI.showModal({ title: '全局搜索', body: `
    <div class="command-search"><span>⌕</span><input id="command-query" autofocus placeholder="输入需求标题、编号或负责人..."></div>
    <div id="command-results" class="command-results"><div class="command-hint">输入至少 2 个字开始搜索</div></div>`, footer: '' });
  const input = modal.querySelector('#command-query');
  const results = modal.querySelector('#command-results');
  let timer;
  input.addEventListener('input', () => {
    clearTimeout(timer); const q = input.value.trim();
    if (q.length < 2) { results.innerHTML = '<div class="command-hint">输入至少 2 个字开始搜索</div>'; return; }
    results.innerHTML = '<div class="command-hint">搜索中...</div>';
    timer = setTimeout(async () => {
      try {
        const res = await api(`/requirements?keyword=${encodeURIComponent(q)}&page=0&size=8`, { silent: true });
        const items = res?.data?.content || res?.data?.items || [];
        results.innerHTML = items.length ? items.map(r => `<button class="command-result" data-id="${r.id}"><span><strong>${esc(r.title)}</strong><small>${esc(r.reqNo || '')} · ${esc(r.sourceDept || '未分组')}</small></span><span>→</span></button>`).join('') : '<div class="command-hint">没有匹配的需求</div>';
        results.querySelectorAll('.command-result').forEach(btn => btn.onclick = async () => { sessionStorage.setItem('itrm_current_req_id', btn.dataset.id); UI.closeModal(); await renderView('requirements'); });
      } catch (e) { results.innerHTML = `<div class="command-hint">${esc(e.message)}</div>`; }
    }, 250);
  });
  setTimeout(() => input.focus(), 50);
}

async function updateNotificationBadge() {
  try {
    const result = await api('/notifications/unread-count', { silent: true });
    const count = Number(result?.data?.count || 0);
    $('#notification-btn')?.setAttribute('title', count ? `${count} 条未读消息` : '通知');
    $('#notification-btn')?.querySelector('.notification-dot')?.classList.toggle('hidden', count === 0);
  } catch { $('#notification-btn')?.querySelector('.notification-dot')?.classList.add('hidden'); }
}

async function openNotifications() {
  const drawer = UI.showDrawer({ title: '消息与待办', width: '440px', content: '<div class="loading-state"><span class="spinner"></span><span>加载消息...</span></div>' });
  const body = drawer.querySelector('.drawer-body');
  try {
    const result = await api('/notifications'); const items = result?.data || [];
    body.innerHTML = items.length ? `<div class="notification-list">${items.map(item => `<div class="notification-item ${item.readFlag ? '' : 'unread'}"><span class="notification-type ${item.readFlag ? 'green' : 'blue'}">${item.eventType?.includes('APPROVAL') ? '审' : '需'}</span><div><strong>${esc(item.title)}</strong><p>${esc(item.content || '')}</p><small>${formatNotificationTime(item.createdAt)}</small>${item.requirementId ? `<button class="btn btn-secondary btn-sm" data-notification-id="${item.id}" data-requirement-id="${item.requirementId}">查看需求</button>` : ''}</div></div>`).join('')}</div>` : UI.emptyState('♢', '暂无消息');
    body.querySelectorAll('[data-requirement-id]').forEach(button => button.onclick = async () => {
      await api(`/notifications/${button.dataset.notificationId}/read`, { method: 'PUT', body: {} });
      sessionStorage.setItem('itrm_current_req_id', button.dataset.requirementId); UI.closeDrawer(); await renderView('requirements'); updateNotificationBadge();
    });
  } catch (error) { body.innerHTML = UI.emptyState('!', esc(error.message)); }
}

function formatNotificationTime(value) { return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : ''; }

function quickCreate() {
  renderView('requirements').then(() => setTimeout(() => document.querySelector('[data-action="new-req"]')?.click(), 50));
}

function openMobileSidebar() { $('#sidebar').classList.add('open'); $('#sidebar-mask').classList.add('show'); }
function closeMobileSidebar() { $('#sidebar')?.classList.remove('open'); $('#sidebar-mask')?.classList.remove('show'); }

function bindEvents() {
  $('#login-btn')?.addEventListener('click', doLogin);
  $('#login-password')?.addEventListener('keydown', e => { if (e.key === 'Enter') doLogin(); });
  $('#logout-btn')?.addEventListener('click', doLogout);
  $$('.nav-link').forEach(link => link.addEventListener('click', e => { e.preventDefault(); renderView(link.dataset.view); }));
  $('#quick-create-btn')?.addEventListener('click', quickCreate);
  $('#global-search-btn')?.addEventListener('click', openGlobalSearch);
  $('#notification-btn')?.addEventListener('click', openNotifications);
  $('#mobile-menu-btn')?.addEventListener('click', openMobileSidebar);
  $('#sidebar-mask')?.addEventListener('click', closeMobileSidebar);
  document.addEventListener('keydown', e => {
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); openGlobalSearch(); }
    if (e.key === 'Escape') { UI.closeModal(); UI.closeDrawer(); closeMobileSidebar(); }
  });
  document.addEventListener('itrm:permissions-rendered', () => applyPermissions(document));
  const permissionObserver = new MutationObserver(mutations => {
    if (mutations.some(m => m.addedNodes.length)) applyPermissions(document);
  });
  permissionObserver.observe(document.body, { childList: true, subtree: true });
}

document.addEventListener('DOMContentLoaded', () => {
  bindEvents();
  if (getToken() && getUser()) showApp(); else showLogin();
});
