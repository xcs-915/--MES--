import { $, $$, esc, formatDateTime } from '../utils/dom.js';
import { state } from '../store/index.js';
import { api } from '../api/request.js';
import { statusPill, priorityPill, emptyState, loadingState } from './_ui.js';

const statCards = [
  { key: 'total', label: '需求总数', icon: '&#128196;', color: 'blue' },
  { key: 'pendingReceive', label: '待接收', icon: '&#128229;', color: 'orange' },
  { key: 'pendingPlan', label: '待排期', icon: '&#128197;', color: 'yellow' },
  { key: 'inDev', label: '开发中', icon: '&#128736;', color: 'cyan' },
  { key: 'inTest', label: '测试中', icon: '&#128269;', color: 'purple' },
  { key: 'completed', label: '已完成', icon: '&#127942;', color: 'green' },
];

export async function renderDashboard() {
  const page = $('#page');
  page.innerHTML = `
    <div class="page-container">
      <div class="page-header">
        <h2>工作台</h2>
      </div>

      <div class="stat-cards">
        ${statCards.map(c => `
          <div class="stat-card stat-${c.color}">
            <div class="stat-icon">${c.icon}</div>
            <div class="stat-info">
              <div class="stat-value" data-stat="${c.key}">0</div>
              <div class="stat-label">${c.label}</div>
            </div>
          </div>
        `).join('')}
      </div>

      <div class="dashboard-grid">
        <div class="dashboard-panel">
          <div class="panel-header">
            <h3>最新需求</h3>
          </div>
          <div class="panel-body" id="latest-reqs">
            ${loadingState()}
          </div>
        </div>

        <div class="dashboard-panel">
          <div class="panel-header">
            <h3>我的任务</h3>
          </div>
          <div class="panel-body" id="my-tasks">
            ${emptyState('请从需求详情查看任务')}
          </div>
        </div>
      </div>
    </div>
  `;

  // 加载统计数据
  loadStats();
  // 加载最新需求
  loadLatestReqs();
}

async function loadStats() {
  const page = $('#page');
  try {
    const stats = await api('/requirements/stats');
    if (stats && stats.data) {
      const d = stats.data;
      statCards.forEach(c => {
        const el = page.querySelector(`[data-stat="${c.key}"]`);
        if (el) el.textContent = d[c.key] ?? 0;
      });
    }
  } catch (e) {
    // 统计加载失败不影响主流程，保持 0
  }
}

async function loadLatestReqs() {
  const container = $('#latest-reqs');
  try {
    const res = await api('/requirements?size=5');
    const items = res?.data?.content || res?.data || [];
    if (!items.length) {
      container.innerHTML = emptyState('暂无数据');
      return;
    }
    container.innerHTML = `<table class="data-table">
      <thead><tr>
        <th>需求编号</th><th>需求标题</th>
        <th>紧急程度</th><th>当前状态</th>
        <th>创建时间</th>
      </tr></thead>
      <tbody>
        ${items.map(item => `
          <tr data-id="${esc(item.id)}">
            <td>${esc(item.reqNo || item.id)}</td>
            <td class="ellipsis" title="${esc(item.title)}">${esc(item.title)}</td>
            <td>${priorityPill(item.urgency || item.priority)}</td>
            <td>${statusPill(item.status)}</td>
            <td>${formatDateTime(item.createdAt)}</td>
          </tr>
        `).join('')}
      </tbody>
    </table>`;
  } catch (e) {
    container.innerHTML = emptyState('加载失败');
  }
}
