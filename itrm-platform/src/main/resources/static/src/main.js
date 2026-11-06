/**
 * ITRM Platform - Main entry
 * 登录校验、路由、导航、初始渲染
 */

import { $, $$ } from './utils/dom.js';
import { state, setToken, setUser, getUser, getToken } from './store/index.js';
import { api } from './api/request.js';
import * as UI from './views/_ui.js';

// 将 UI 组件暴露到全局
window.UI = UI;

import { renderDashboard } from './views/dashboard.js';
import { renderRequirements } from './views/requirements.js';
import { renderTasks } from './views/tasks.js';
import { renderBoard } from './views/board.js';
import { renderTesting } from './views/testing.js';
import { renderTimelog } from './views/timelog.js';

// ---------- 视图标题映射（面包屑用） ----------

const viewTitles = {
  dashboard: '工作台',
  requirements: '需求池',
  tasks: '任务管理',
  board: '项目看板',
  testing: '测试验收',
  timelog: '工时管理',
  templates: '审批模板',
};

// ---------- 视图渲染器映射 ----------

const viewRenderers = {
  dashboard: renderDashboard,
  requirements: renderRequirements,
  tasks: renderTasks,
  board: renderBoard,
  testing: renderTesting,
  timelog: renderTimelog,
  templates: renderTemplates,
};

// ---------- 角色映射 ----------

function roleLabel(role) {
  const map = {
    ADMIN: '系统管理员',
    BUSINESS_USER: '业务用户',
    IT_REVIEWER: '需求评审',
    IT_PLANNER: '项目排期',
    DEVELOPER: '开发人员',
    TESTER: '测试人员',
    RELEASE_ENGINEER: '发布工程师',
  };
  return map[role] || role;
}

// ---------- 登录/登出 ----------

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

async function doLogin() {
  const username = $('#login-username').value.trim();
  const password = $('#login-password').value;
  if (!username || !password) {
    UI.showToast('请输入用户名和密码', 'warning');
    return;
  }
  const btn = $('#login-btn');
  btn.disabled = true;
  btn.textContent = '登录中...';
  try {
    const res = await api('/auth/login', { method: 'POST', body: { username, password } });
    setToken(res.data.token);
    setUser(res.data.user);
    UI.showToast('登录成功', 'success');
    showApp();
  } catch (e) {
    UI.showToast(e.message || '登录失败', 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = '登 录';
  }
}

function doLogout() {
  UI.confirm({
    title: '退出登录',
    message: '确定要退出登录吗？',
    onOk: () => {
      setToken(null);
      setUser(null);
      showLogin();
    },
  });
}

// ---------- 导航 & 面包屑 ----------

function updateBreadcrumb(view) {
  const title = viewTitles[view] || view;
  const rootEl = $('#breadcrumb-root');
  const currentEl = $('#breadcrumb-current');

  if (rootEl) {
    rootEl.textContent = '首页';
  }
  if (currentEl) {
    currentEl.textContent = title;
  }

  // 兼容旧版面包屑（单元素）
  const legacyEl = $('#breadcrumb');
  if (legacyEl) {
    legacyEl.textContent = title;
  }
}

function renderView(view) {
  state.view = view;

  // 更新导航激活状态
  $$('.nav-link').forEach(n => n.classList.toggle('active', n.dataset.view === view));

  // 更新面包屑
  updateBreadcrumb(view);

  // 渲染视图
  const renderer = viewRenderers[view] || renderDashboard;
  try {
    renderer();
  } catch (e) {
    UI.showToast(`加载视图失败: ${e.message}`, 'error');
  }
}

// ---------- 占位视图：审批模板 ----------

function renderTemplates() {
  const content = $('#view-content');
  if (!content) return;
  content.innerHTML = `
    <div class="view-header">
      <h2>审批模板</h2>
    </div>
    ${UI.emptyState('&#9776;', '审批模板功能开发中，敬请期待')}
  `;
}

// ---------- 全局容器事件绑定 ----------

function initGlobalContainers() {
  // 全局弹窗关闭按钮事件委托
  document.addEventListener('click', e => {
    // 关闭按钮
    if (e.target.closest('[data-modal-close]')) {
      UI.closeModal();
    }
    if (e.target.closest('[data-drawer-close]')) {
      UI.closeDrawer();
    }
  });

  // ESC 键关闭弹窗/抽屉
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape') {
      UI.closeModal();
      UI.closeDrawer();
    }
  });
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

// ---------- Boot ----------

document.addEventListener('DOMContentLoaded', () => {
  // 登录相关
  $('#login-btn')?.addEventListener('click', doLogin);
  $('#login-password')?.addEventListener('keydown', e => {
    if (e.key === 'Enter') doLogin();
  });
  $('#logout-btn')?.addEventListener('click', doLogout);

  // 全局容器初始化
  initGlobalContainers();

  // 导航初始化
  initNavigation();

  // 判断是否已登录
  if (getToken() && getUser()) {
    showApp();
  } else {
    showLogin();
  }
});
