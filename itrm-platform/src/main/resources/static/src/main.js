/**
 * ITRM Platform - Main entry
 * 登录校验、路由、导航、初始渲染
 */

import { $, $$ } from './utils/dom.js';
import { state, setToken, setUser, getUser, getToken } from './store/index.js';
import { api } from './api/request.js';

import { renderDashboard } from './views/dashboard.js';
import { renderRequirements } from './views/requirements.js';
import { renderTasks } from './views/tasks.js';
import { renderBoard } from './views/board.js';
import { renderTesting } from './views/testing.js';
import { renderTimelog } from './views/timelog.js';

const viewTitles = {
  dashboard: '工作台',
  requirements: '需求池',
  tasks: '任务管理',
  board: '项目看板',
  testing: '测试验收',
  timelog: '工时管理',
};

const viewRenderers = {
  dashboard: renderDashboard,
  requirements: renderRequirements,
  tasks: renderTasks,
  board: renderBoard,
  testing: renderTesting,
  timelog: renderTimelog,
};

function showLogin() {
  $('#login-view').classList.remove('hidden');
  $('#app-view').classList.add('hidden');
}

function showApp() {
  $('#login-view').classList.add('hidden');
  $('#app-view').classList.remove('hidden');
  const user = getUser();
  if (user) {
    $('#user-name').textContent = user.realName || user.username;
    $('#user-role').textContent = roleLabel(user.role);
    $('#user-avatar').textContent = (user.realName || user.username).charAt(0).toUpperCase();
  }
  renderView('dashboard');
}

function roleLabel(role) {
  const map = { ADMIN: '管理员', MANAGER: '项目经理', DEVELOPER: '开发人员', TESTER: '测试人员', REQUESTER: '需求提出人' };
  return map[role] || role;
}

function renderView(view) {
  state.view = view;
  $$('.nav-link').forEach(n => n.classList.toggle('active', n.dataset.view === view));
  $('#breadcrumb').textContent = viewTitles[view] || view;
  const renderer = viewRenderers[view] || renderDashboard;
  renderer();
}

async function doLogin() {
  const username = $('#login-username').value.trim();
  const password = $('#login-password').value;
  if (!username || !password) { alert('请输入用户名和密码'); return; }
  const btn = $('#login-btn');
  btn.disabled = true; btn.textContent = '登录中...';
  try {
    const res = await api('/auth/login', { method: 'POST', body: { username, password } });
    setToken(res.data.token);
    setUser(res.data.user);
    showApp();
  } catch (e) {
    alert(e.message);
  } finally {
    btn.disabled = false; btn.textContent = '登 录';
  }
}

function doLogout() {
  if (!confirm('确定要退出登录吗？')) return;
  setToken(null);
  setUser(null);
  showLogin();
}

function initNavigation() {
  $$('.nav-link').forEach(link => {
    link.addEventListener('click', e => {
      e.preventDefault();
      const view = link.dataset.view;
      if (view) renderView(view);
    });
  });
}

// Boot
document.addEventListener('DOMContentLoaded', () => {
  $('#login-btn')?.addEventListener('click', doLogin);
  $('#login-password')?.addEventListener('keydown', e => { if (e.key === 'Enter') doLogin(); });
  $('#logout-btn')?.addEventListener('click', doLogout);

  initNavigation();

  if (getToken() && getUser()) {
    showApp();
  } else {
    showLogin();
  }
});
