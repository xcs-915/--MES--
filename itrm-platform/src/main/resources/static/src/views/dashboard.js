import { $, $$, esc, formatDateTime } from '../utils/dom.js';
import { state, getUser } from '../store/index.js';
import { api } from '../api/request.js';
import { statusPill, priorityPill, emptyState, loadingState } from './_ui.js';

// ==================== 自定义样式注入 ====================
const dashboardStyles = `
/* 欢迎区 */
.dashboard-welcome {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding: 24px;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  border-radius: 12px;
  color: #fff;
}
.welcome-greeting {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 6px;
}
.welcome-date {
  font-size: 13px;
  opacity: 0.85;
}
.welcome-actions {
  display: flex;
  gap: 10px;
}
.welcome-actions .btn {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
  color: #fff;
  backdrop-filter: blur(8px);
}
.welcome-actions .btn:hover {
  background: rgba(255, 255, 255, 0.3);
}
.welcome-actions .btn.primary {
  background: #fff;
  border-color: #fff;
  color: #3b82f6;
}
.welcome-actions .btn.primary:hover {
  background: #f8fafc;
}

/* 统计卡片补充样式 */
.stat-card-footer {
  font-size: 12px;
  color: var(--it-color-text-tertiary);
  margin-top: 8px;
}
.stat-card-progress {
  margin-top: 8px;
}
.progress-bar-track {
  height: 6px;
  background: var(--it-color-bg-hover);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 6px;
}
.progress-bar-fill {
  height: 100%;
  background: var(--it-color-purple);
  border-radius: 3px;
  transition: width 0.6s ease;
}
.cursor-pointer {
  cursor: pointer;
}
.trend-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  background: var(--it-color-danger);
  border-radius: 50%;
  margin-right: 4px;
}

/* 图表卡片补充 */
.chart-card-sub {
  font-size: 12px;
  color: var(--it-color-text-tertiary);
  font-weight: 400;
}
.chart-body {
  min-height: 200px;
}

/* 环形图 */
.donut-chart-wrapper {
  display: flex;
  align-items: center;
  gap: 24px;
}
.donut-chart {
  position: relative;
  width: 160px;
  height: 160px;
  flex-shrink: 0;
}
.donut-ring {
  width: 100%;
  height: 100%;
  border-radius: 50%;
}
.donut-inner {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100px;
  height: 100px;
  background: var(--it-color-bg-card);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.donut-total {
  font-size: 24px;
  font-weight: 700;
  color: var(--it-color-text-primary);
  line-height: 1.2;
}
.donut-label {
  font-size: 12px;
  color: var(--it-color-text-tertiary);
  margin-top: 2px;
}
.donut-legend {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.donut-legend .legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}
.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  flex-shrink: 0;
}
.legend-label {
  flex: 1;
  color: var(--it-color-text-secondary);
}
.legend-value {
  font-weight: 600;
  color: var(--it-color-text-primary);
  min-width: 24px;
  text-align: right;
}
.legend-percent {
  color: var(--it-color-text-tertiary);
  font-size: 12px;
  min-width: 36px;
  text-align: right;
}

/* 柱状图 */
.bar-chart-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.bar-chart-bars {
  flex: 1;
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  gap: 8px;
  padding-bottom: 8px;
  min-height: 180px;
}
.bar-group {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.bar-pair {
  display: flex;
  align-items: flex-end;
  gap: 3px;
  height: 160px;
}
.bar {
  width: 16px;
  border-radius: 3px 3px 0 0;
  position: relative;
  transition: height 0.5s ease;
  min-height: 4px;
}
.bar-new {
  background: linear-gradient(180deg, #3b82f6 0%, #60a5fa 100%);
}
.bar-done {
  background: linear-gradient(180deg, #10b981 0%, #34d399 100%);
}
.bar-value {
  position: absolute;
  top: -18px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 10px;
  color: var(--it-color-text-tertiary);
  white-space: nowrap;
}
.bar-label {
  font-size: 11px;
  color: var(--it-color-text-tertiary);
}
.bar-chart-legend {
  display: flex;
  justify-content: center;
  gap: 20px;
  padding-top: 8px;
  border-top: 1px solid var(--it-color-border-lighter);
}
.bar-chart-legend .legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--it-color-text-secondary);
}
.legend-dot-new {
  background: #3b82f6;
  border-radius: 2px;
}
.legend-dot-done {
  background: #10b981;
  border-radius: 2px;
}

/* 水平柱状图 */
.h-bar-chart-wrapper {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 4px 0;
}
.h-bar-item {
  display: flex;
  align-items: center;
  gap: 12px;
}
.h-bar-label {
  width: 60px;
  font-size: 13px;
  color: var(--it-color-text-secondary);
  flex-shrink: 0;
  font-weight: 500;
}
.h-bar-track {
  flex: 1;
  height: 20px;
  background: var(--it-color-bg-hover);
  border-radius: 4px;
  overflow: hidden;
}
.h-bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.6s ease;
}
.h-bar-value {
  width: 36px;
  text-align: right;
  font-size: 13px;
  font-weight: 600;
  color: var(--it-color-text-primary);
  flex-shrink: 0;
}

/* 工时统计 */
.workhours-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.workhours-item {
  padding: 12px 16px;
  background: var(--it-color-bg-hover);
  border-radius: 8px;
  text-align: center;
}
.workhours-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--it-color-text-primary);
  line-height: 1.2;
  margin-bottom: 4px;
}
.workhours-accent {
  color: var(--it-color-primary);
}
.workhours-success {
  color: var(--it-color-success);
}
.workhours-label {
  font-size: 12px;
  color: var(--it-color-text-tertiary);
}

/* 待办区 */
.dashboard-todo-card {
  margin-bottom: 24px;
}
.todo-list-container {
  margin-top: 4px;
}
.todo-list {
  display: flex;
  flex-direction: column;
}
.todo-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 0;
  border-bottom: 1px solid var(--it-color-border-light);
}
.todo-item:last-child {
  border-bottom: none;
}
.todo-main {
  flex: 1;
  min-width: 0;
}
.todo-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--it-color-text-primary);
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.todo-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--it-color-text-tertiary);
}
.todo-side {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  margin-left: 16px;
}
.todo-actions {
  display: flex;
  gap: 6px;
}
.btn-sm {
  padding: 4px 12px;
  font-size: 12px;
}

/* 项目概览区 */
.project-overview-section {
  margin-bottom: 0;
}
`;

let stylesInjected = false;
function injectStyles() {
  if (stylesInjected) return;
  const style = document.createElement('style');
  style.id = 'dashboard-styles';
  style.textContent = dashboardStyles;
  document.head.appendChild(style);
  stylesInjected = true;
}

// ==================== 模拟数据 ====================
const mockStats = {
  myRequirements: 128,
  myRequirementsTrend: 12,
  inProgress: 36,
  inProgressRate: 28,
  pendingMe: 8,
  completed: 64,
  completionRate: 72,
  statusDistribution: [
    { label: '待接收', value: 12, color: '#f59e0b' },
    { label: '待排期', value: 18, color: '#3b82f6' },
    { label: '开发中', value: 24, color: '#8b5cf6' },
    { label: '测试中', value: 16, color: '#06b6d4' },
    { label: '已完成', value: 38, color: '#10b981' },
  ],
  weeklyTrend: [
    { day: '周一', new: 8, done: 5 },
    { day: '周二', new: 12, done: 9 },
    { day: '周三', new: 6, done: 10 },
    { day: '周四', new: 10, done: 7 },
    { day: '周五', new: 5, done: 8 },
    { day: '周六', new: 2, done: 3 },
    { day: '周日', new: 1, done: 2 },
  ],
  systemDistribution: [
    { name: 'MES', count: 42 },
    { name: 'WMS', count: 28 },
    { name: 'SAP', count: 24 },
    { name: 'OA', count: 18 },
    { name: 'SCADA', count: 16 },
  ],
  workHours: {
    total: 1286,
    thisWeek: 86,
    avgPerDay: 17.2,
  },
};

const mockTodos = {
  approval: [
    { id: 1, title: 'MES生产报工功能优化需求审批', priority: 'HIGH', deadline: '2024-01-15', submitter: '张三' },
    { id: 2, title: 'WMS库存盘点模块升级', priority: 'MEDIUM', deadline: '2024-01-18', submitter: '李四' },
    { id: 3, title: 'SAP接口对接需求评审', priority: 'URGENT', deadline: '2024-01-12', submitter: '王五' },
  ],
  tasks: [
    { id: 1, title: '生产看板前端开发', status: 'IN_DEVELOPMENT', priority: 'HIGH', deadline: '2024-01-20' },
    { id: 2, title: '库存预警功能测试', status: 'IN_TESTING', priority: 'MEDIUM', deadline: '2024-01-22' },
    { id: 3, title: '报表导出接口优化', status: 'PENDING_PLAN', priority: 'LOW', deadline: '2024-01-25' },
    { id: 4, title: '用户权限管理重构', status: 'IN_DEVELOPMENT', priority: 'HIGH', deadline: '2024-01-30' },
  ],
  submitted: [
    { id: 1, title: '设备管理模块新增', status: 'PENDING_RECEIVE', priority: 'MEDIUM', createdAt: '2024-01-08' },
    { id: 2, title: '质检流程数字化改造', status: 'IN_DEVELOPMENT', priority: 'HIGH', createdAt: '2024-01-05' },
    { id: 3, title: '能耗数据分析平台', status: 'PENDING_PLAN', priority: 'LOW', createdAt: '2024-01-02' },
  ],
};

// ==================== 统计卡片配置 ====================
const statCardConfigs = [
  {
    key: 'myRequirements',
    label: '我的需求',
    icon: '📋',
    color: 'primary',
    trendKey: 'myRequirementsTrend',
    trendLabel: '较上周',
    footer: '我负责的所有需求',
    action: 'requirements',
  },
  {
    key: 'inProgress',
    label: '进行中需求',
    icon: '⚙️',
    color: 'purple',
    progressKey: 'inProgressRate',
    footer: '开发与测试中',
    action: 'requirements',
  },
  {
    key: 'pendingMe',
    label: '待我处理',
    icon: '🔔',
    color: 'warning',
    hasDot: true,
    footer: '待审批 & 待处理',
    action: 'requirements',
  },
  {
    key: 'completed',
    label: '已完成需求',
    icon: '✅',
    color: 'success',
    rateKey: 'completionRate',
    footer: '本月完成率',
    action: 'requirements',
  },
];

// ==================== 主渲染函数 ====================
export async function renderDashboard() {
  injectStyles();
  const page = $('#page');
  const user = getUser();
  const userName = user?.nickname || user?.username || '用户';
  const todayStr = formatToday();

  page.innerHTML = `
    <div class="page-container">
      <!-- 欢迎区 -->
      <div class="dashboard-welcome">
        <div class="welcome-left">
          <div class="welcome-greeting">你好，${esc(userName)} 👋</div>
          <div class="welcome-date">${todayStr}</div>
        </div>
        <div class="welcome-actions">
          <button class="btn primary" data-action="new-req">
            <span>＋</span> 新建需求
          </button>
          <button class="btn" data-action="my-tasks">
            <span>📋</span> 我的任务
          </button>
          <button class="btn" data-action="todos">
            <span>✅</span> 待办事项
          </button>
        </div>
      </div>

      <!-- 统计卡片行 -->
      <div class="stats-grid">
        ${statCardConfigs.map(c => renderStatCard(c)).join('')}
      </div>

      <!-- 状态分布图表区 -->
      <div class="chart-section">
        <div class="chart-card">
          <div class="chart-card-header">
            <div class="chart-card-title">需求状态分布</div>
            <div class="chart-card-sub">全部需求</div>
          </div>
          <div class="chart-body" id="status-chart">
            ${loadingState()}
          </div>
        </div>
        <div class="chart-card">
          <div class="chart-card-header">
            <div class="chart-card-title">本周新增 vs 完成</div>
            <div class="chart-card-sub">趋势对比</div>
          </div>
          <div class="chart-body" id="weekly-chart">
            ${loadingState()}
          </div>
        </div>
      </div>

      <!-- 我的待办区 -->
      <div class="chart-card dashboard-todo-card">
        <div class="chart-card-header">
          <div class="chart-card-title">我的待办</div>
        </div>
        <div class="tab-nav" id="todo-tabs">
          <div class="tab-item active" data-tab="approval">
            待我审批 <span class="tab-count">${mockTodos.approval.length}</span>
          </div>
          <div class="tab-item" data-tab="tasks">
            我的任务 <span class="tab-count">${mockTodos.tasks.length}</span>
          </div>
          <div class="tab-item" data-tab="submitted">
            我提交的需求 <span class="tab-count">${mockTodos.submitted.length}</span>
          </div>
        </div>
        <div class="todo-list-container" id="todo-list">
          ${renderTodoList('approval')}
        </div>
      </div>

      <!-- 项目概览区 -->
      <div class="chart-section project-overview-section">
        <div class="chart-card">
          <div class="chart-card-header">
            <div class="chart-card-title">各系统需求分布</div>
            <div class="chart-card-sub">按系统分类</div>
          </div>
          <div class="chart-body" id="system-chart">
            ${loadingState()}
          </div>
        </div>
        <div class="chart-card">
          <div class="chart-card-header">
            <div class="chart-card-title">工时统计</div>
            <div class="chart-card-sub">本月累计</div>
          </div>
          <div class="workhours-stats" id="workhours-stats">
            ${loadingState()}
          </div>
        </div>
      </div>
    </div>
  `;

  // 绑定事件
  bindEvents();

  // 加载数据
  loadAllData();
}

// ==================== 渲染函数 ====================

function renderStatCard(config) {
  const trendHtml = config.trendKey
    ? `<div class="stat-card-trend up" data-trend="${config.trendKey}">
         <span class="trend-icon">↑</span>
         <span data-trend-value="${config.trendKey}">0%</span>
         <span class="trend-label">${config.trendLabel}</span>
       </div>`
    : config.progressKey
    ? '' // 进度条在底部
    : config.hasDot
    ? `<div class="stat-card-trend" style="color: var(--it-color-danger)">
         <span class="trend-dot"></span>
         <span class="trend-label">待处理</span>
       </div>`
    : config.rateKey
    ? `<div class="stat-card-trend up" data-trend="${config.rateKey}">
         <span class="trend-icon">↑</span>
         <span data-trend-value="${config.rateKey}">0%</span>
         <span class="trend-label">完成率</span>
       </div>`
    : '';

  const bottomProgress = config.progressKey
    ? `<div class="stat-card-progress">
         <div class="progress-bar-track">
           <div class="progress-bar-fill" data-progress="${config.progressKey}" style="width: 0%"></div>
         </div>
         <div class="stat-card-footer">${config.footer} · <span data-progress-label="${config.progressKey}">0%</span></div>
       </div>`
    : `<div class="stat-card-footer">${config.footer}</div>`;

  return `
    <div class="stat-card cursor-pointer" data-card-action="${config.action}">
      <div class="stat-card-header">
        <div class="stat-card-icon ${config.color}">
          <span class="icon">${config.icon}</span>
        </div>
        ${trendHtml}
      </div>
      <div class="stat-card-value" data-stat="${config.key}">0</div>
      <div class="stat-card-label">${config.label}</div>
      ${bottomProgress}
    </div>
  `;
}

// 环形图（纯 CSS 模拟）
function renderDonutChart(data) {
  const total = data.reduce((sum, d) => sum + d.value, 0);
  let cumulativePercent = 0;

  // 用 conic-gradient 绘制环形图
  const gradientParts = [];
  let currentAngle = 0;
  data.forEach(d => {
    const percent = (d.value / total) * 100;
    const startAngle = currentAngle;
    currentAngle += percent * 3.6; // 1% = 3.6deg
    const endAngle = currentAngle;
    gradientParts.push(`${d.color} ${startAngle}deg ${endAngle}deg`);
  });

  const conicGradient = `conic-gradient(${gradientParts.join(', ')})`;

  return `
    <div class="donut-chart-wrapper">
      <div class="donut-chart">
        <div class="donut-ring" style="background: ${conicGradient}"></div>
        <div class="donut-inner">
          <div class="donut-total">${total}</div>
          <div class="donut-label">需求总数</div>
        </div>
      </div>
      <div class="donut-legend">
        ${data.map(d => `
          <div class="legend-item">
            <span class="legend-dot" style="background: ${d.color}"></span>
            <span class="legend-label">${d.label}</span>
            <span class="legend-value">${d.value}</span>
            <span class="legend-percent">${Math.round((d.value / total) * 100)}%</span>
          </div>
        `).join('')}
      </div>
    </div>
  `;
}

// 柱状图（本周新增 vs 完成）
function renderWeeklyBarChart(data) {
  const maxValue = Math.max(...data.map(d => Math.max(d.new, d.done))) * 1.2;

  return `
    <div class="bar-chart-wrapper">
      <div class="bar-chart-bars">
        ${data.map(d => {
          const newHeight = (d.new / maxValue) * 100;
          const doneHeight = (d.done / maxValue) * 100;
          return `
            <div class="bar-group">
              <div class="bar-pair">
                <div class="bar bar-new" style="height: ${newHeight}%" title="新增: ${d.new}">
                  <span class="bar-value">${d.new}</span>
                </div>
                <div class="bar bar-done" style="height: ${doneHeight}%" title="完成: ${d.done}">
                  <span class="bar-value">${d.done}</span>
                </div>
              </div>
              <div class="bar-label">${d.day}</div>
            </div>
          `;
        }).join('')}
      </div>
      <div class="bar-chart-legend">
        <div class="legend-item">
          <span class="legend-dot legend-dot-new"></span>
          <span class="legend-label">新增</span>
        </div>
        <div class="legend-item">
          <span class="legend-dot legend-dot-done"></span>
          <span class="legend-label">完成</span>
        </div>
      </div>
    </div>
  `;
}

// 系统分布柱状图
function renderSystemBarChart(data) {
  const maxValue = Math.max(...data.map(d => d.count)) * 1.2;
  const colors = ['#3b82f6', '#8b5cf6', '#10b981', '#f59e0b', '#06b6d4'];

  return `
    <div class="h-bar-chart-wrapper">
      ${data.map((d, i) => {
        const width = (d.count / maxValue) * 100;
        const color = colors[i % colors.length];
        return `
          <div class="h-bar-item">
            <div class="h-bar-label">${d.name}</div>
            <div class="h-bar-track">
              <div class="h-bar-fill" style="width: ${width}%; background: ${color}"></div>
            </div>
            <div class="h-bar-value">${d.count}</div>
          </div>
        `;
      }).join('')}
    </div>
  `;
}

// 工时统计卡片
function renderWorkHoursStats(data) {
  return `
    <div class="workhours-grid">
      <div class="workhours-item">
        <div class="workhours-value">${data.total}</div>
        <div class="workhours-label">本月总工时(h)</div>
      </div>
      <div class="workhours-item">
        <div class="workhours-value workhours-accent">${data.thisWeek}</div>
        <div class="workhours-label">本周工时(h)</div>
      </div>
      <div class="workhours-item">
        <div class="workhours-value workhours-success">${data.avgPerDay}</div>
        <div class="workhours-label">日均工时(h)</div>
      </div>
    </div>
  `;
}

// 待办列表
function renderTodoList(tab) {
  const items = mockTodos[tab] || [];
  if (!items.length) return emptyState('暂无数据');

  if (tab === 'approval') {
    return `
      <div class="todo-list">
        ${items.map(item => `
          <div class="todo-item" data-id="${item.id}">
            <div class="todo-main">
              <div class="todo-title">${esc(item.title)}</div>
              <div class="todo-meta">
                <span>提交人：${esc(item.submitter)}</span>
                <span>截止：${esc(item.deadline)}</span>
              </div>
            </div>
            <div class="todo-side">
              ${priorityPill(item.priority)}
              <div class="todo-actions">
                <button class="btn btn-sm primary">通过</button>
                <button class="btn btn-sm">驳回</button>
              </div>
            </div>
          </div>
        `).join('')}
      </div>
    `;
  }

  if (tab === 'tasks') {
    return `
      <table class="data-table">
        <thead><tr>
          <th>任务名称</th>
          <th>状态</th>
          <th>优先级</th>
          <th>截止日期</th>
          <th>操作</th>
        </tr></thead>
        <tbody>
          ${items.map(item => `
            <tr data-id="${item.id}">
              <td class="text-ellipsis">${esc(item.title)}</td>
              <td>${statusPill(item.status)}</td>
              <td>${priorityPill(item.priority)}</td>
              <td>${esc(item.deadline)}</td>
              <td class="actions-cell">
                <button class="btn btn-sm">查看</button>
              </td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    `;
  }

  // submitted
  return `
    <table class="data-table">
      <thead><tr>
        <th>需求标题</th>
        <th>状态</th>
        <th>优先级</th>
        <th>提交时间</th>
        <th>操作</th>
      </tr></thead>
      <tbody>
        ${items.map(item => `
          <tr data-id="${item.id}">
            <td class="text-ellipsis">${esc(item.title)}</td>
            <td>${statusPill(item.status)}</td>
            <td>${priorityPill(item.priority)}</td>
            <td>${esc(item.createdAt)}</td>
            <td class="actions-cell">
              <button class="btn btn-sm">查看</button>
            </td>
          </tr>
        `).join('')}
      </tbody>
    </table>
  `;
}

// ==================== 数据加载 ====================

async function loadAllData() {
  // 加载统计数据
  loadStatsData();
  // 加载图表数据
  loadChartData();
}

async function loadStatsData() {
  const page = $('#page');
  let stats = mockStats;

  try {
    const res = await api('/requirements/stats');
    if (res && res.data) {
      stats = { ...mockStats, ...res.data };
    }
  } catch (e) {
    // 接口失败使用模拟数据
  }

  // 更新统计卡片数值
  statCardConfigs.forEach(c => {
    const el = page.querySelector(`[data-stat="${c.key}"]`);
    if (el) el.textContent = stats[c.key] ?? 0;

    // 趋势
    if (c.trendKey) {
      const trendEl = page.querySelector(`[data-trend-value="${c.trendKey}"]`);
      const trendWrap = page.querySelector(`[data-trend="${c.trendKey}"]`);
      const val = stats[c.trendKey] ?? 0;
      if (trendEl) trendEl.textContent = `${val > 0 ? '+' : ''}${val}%`;
      if (trendWrap) {
        trendWrap.className = `stat-card-trend ${val >= 0 ? 'up' : 'down'}`;
        trendWrap.querySelector('.trend-icon').textContent = val >= 0 ? '↑' : '↓';
      }
    }

    // 进度条
    if (c.progressKey) {
      const progressEl = page.querySelector(`[data-progress="${c.progressKey}"]`);
      const labelEl = page.querySelector(`[data-progress-label="${c.progressKey}"]`);
      const val = stats[c.progressKey] ?? 0;
      if (progressEl) progressEl.style.width = `${val}%`;
      if (labelEl) labelEl.textContent = `${val}%`;
    }

    // 完成率
    if (c.rateKey) {
      const rateEl = page.querySelector(`[data-trend-value="${c.rateKey}"]`);
      const val = stats[c.rateKey] ?? 0;
      if (rateEl) rateEl.textContent = `${val}%`;
    }
  });
}

async function loadChartData() {
  let data = mockStats;

  try {
    const res = await api('/requirements/stats');
    if (res && res.data) {
      data = { ...mockStats, ...res.data };
    }
  } catch (e) {
    // 使用模拟数据
  }

  // 状态分布环形图
  const statusChart = $('#status-chart');
  if (statusChart) {
    statusChart.innerHTML = renderDonutChart(data.statusDistribution);
  }

  // 本周趋势柱状图
  const weeklyChart = $('#weekly-chart');
  if (weeklyChart) {
    weeklyChart.innerHTML = renderWeeklyBarChart(data.weeklyTrend);
  }

  // 系统分布柱状图
  const systemChart = $('#system-chart');
  if (systemChart) {
    systemChart.innerHTML = renderSystemBarChart(data.systemDistribution);
  }

  // 工时统计
  const workhoursStats = $('#workhours-stats');
  if (workhoursStats) {
    workhoursStats.innerHTML = renderWorkHoursStats(data.workHours);
  }
}

// ==================== 事件绑定 ====================

function bindEvents() {
  const page = $('#page');

  // 快捷操作按钮
  page.querySelectorAll('[data-action]').forEach(btn => {
    btn.addEventListener('click', () => {
      const action = btn.dataset.action;
      if (action === 'new-req') {
        window.location.hash = '#/requirements/new';
      } else if (action === 'my-tasks') {
        window.location.hash = '#/tasks';
      } else if (action === 'todos') {
        const todoCard = page.querySelector('.dashboard-todo-card');
        if (todoCard) {
          todoCard.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
      }
    });
  });

  // 统计卡片点击跳转
  page.querySelectorAll('[data-card-action]').forEach(card => {
    card.addEventListener('click', () => {
      const action = card.dataset.cardAction;
      if (action === 'requirements') {
        window.location.hash = '#/requirements';
      }
    });
  });

  // Tab 切换
  const tabs = page.querySelectorAll('#todo-tabs .tab-item');
  tabs.forEach(tab => {
    tab.addEventListener('click', () => {
      tabs.forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      const tabName = tab.dataset.tab;
      const listContainer = $('#todo-list');
      if (listContainer) {
        listContainer.innerHTML = renderTodoList(tabName);
      }
    });
  });
}

// ==================== 工具函数 ====================

function formatToday() {
  const now = new Date();
  const weekDays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
  const year = now.getFullYear();
  const month = now.getMonth() + 1;
  const day = now.getDate();
  const weekDay = weekDays[now.getDay()];
  return `${year}年${month}月${day}日 ${weekDay}`;
}
