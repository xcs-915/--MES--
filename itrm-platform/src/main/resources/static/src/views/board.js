import { $, $$, esc } from '../utils/dom.js';
import { state } from '../store/index.js';
import { api } from '../api/request.js';
import { priorityPill, progressBar, emptyState, loadingState } from './_ui.js';

const boardColumns = [
  { key: 'TODO', label: '待办' },
  { key: 'IN_PROGRESS', label: '进行中' },
  { key: 'TESTING', label: '测试中' },
  { key: 'DONE', label: '已完成' },
  { key: 'BLOCKED', label: '阻塞' },
];

const priorityOptions = [
  { value: 'URGENT', label: '紧急' },
  { value: 'HIGH', label: '高' },
  { value: 'MEDIUM', label: '中' },
  { value: 'LOW', label: '低' },
];

let currentFilters = { assignee: '', priority: '' };

export async function renderBoard() {
  const page = $('#page');
  page.innerHTML = `
    <div class="page-container">
      <div class="page-header">
        <h2>项目看板</h2>
      </div>

      <div class="filter-bar">
        <input type="text" id="f-assignee" placeholder="负责人" />
        <select id="f-priority">
          <option value="">全部紧急程度</option>
          ${priorityOptions.map(p => `<option value="${p.value}">${p.label}</option>`).join('')}
        </select>
        <button class="btn" data-action="search-board">搜索</button>
        <button class="btn" data-action="reset-board">重置</button>
      </div>

      <div class="kanban-board" id="kanban-board">
        ${boardColumns.map(col => `
          <div class="kanban-col" data-status="${col.key}">
            <div class="kanban-col-header">
              <span class="col-title">${col.label}</span>
              <span class="col-count" data-count="${col.key}">0</span>
            </div>
            <div class="kanban-col-body" data-body="${col.key}">
              ${loadingState()}
            </div>
          </div>
        `).join('')}
      </div>
    </div>
  `;

  bindEvents();
  loadBoard();
}

function bindEvents() {
  const container = $('.page-container');

  container.addEventListener('click', e => {
    const btn = e.target.closest('[data-action]');
    if (!btn) return;
    const action = btn.dataset.action;

    if (action === 'search-board') {
      currentFilters = {
        assignee: $('#f-assignee').value.trim(),
        priority: $('#f-priority').value,
      };
      loadBoard();
    } else if (action === 'reset-board') {
      currentFilters = { assignee: '', priority: '' };
      $('#f-assignee').value = '';
      $('#f-priority').value = '';
      loadBoard();
    }
  });
}

async function loadBoard() {
  // 重置每列为加载状态
  boardColumns.forEach(col => {
    const body = $(`[data-body="${col.key}"]`);
    if (body) body.innerHTML = loadingState();
  });

  try {
    // 获取需求列表（前20条）
    const reqRes = await api('/requirements?size=20');
    const requirements = reqRes?.data?.content || reqRes?.data || [];

    // 逐个获取任务并汇总
    const taskPromises = requirements.map(req =>
      api(`/tasks/by-requirement/${req.id}`)
        .then(res => (res?.data || res || []).map(t => ({ ...t, _reqTitle: req.title, _reqNo: req.reqNo || req.id })))
        .catch(() => [])
    );
    const results = await Promise.all(taskPromises);
    let allTasks = results.flat();

    // 筛选
    if (currentFilters.assignee) {
      const a = currentFilters.assignee.toLowerCase();
      allTasks = allTasks.filter(t => (t.assignee || '').toLowerCase().includes(a));
    }
    if (currentFilters.priority) {
      allTasks = allTasks.filter(t => t.priority === currentFilters.priority);
    }

    // 按状态分组
    const grouped = {};
    boardColumns.forEach(col => { grouped[col.key] = []; });
    allTasks.forEach(t => {
      const status = t.status || 'TODO';
      if (grouped[status]) grouped[status].push(t);
      else grouped.TODO.push(t);
    });

    // 渲染各列
    boardColumns.forEach(col => {
      const body = $(`[data-body="${col.key}"]`);
      const count = $(`[data-count="${col.key}"]`);
      const tasks = grouped[col.key] || [];

      if (count) count.textContent = tasks.length;

      if (!tasks.length) {
        if (body) body.innerHTML = `<div class="empty-state small">暂无任务</div>`;
        return;
      }

      if (body) {
        body.innerHTML = tasks.map(task => `
          <div class="kanban-card" data-id="${esc(task.id)}">
            <div class="card-top">
              <span class="card-no">${esc(task.taskNo || task.id)}</span>
              ${priorityPill(task.priority || task.urgency)}
            </div>
            <div class="card-title" title="${esc(task.title)}">${esc(task.title)}</div>
            <div class="card-req" title="${esc(task._reqTitle || '')}">${esc(task._reqTitle || '-')}</div>
            <div class="card-bottom">
              <span class="card-assignee">${esc(task.assignee || '-')}</span>
              <div class="card-progress">${progressBar(task.progress || 0)}</div>
            </div>
          </div>
        `).join('');
      }
    });
  } catch (e) {
    boardColumns.forEach(col => {
      const body = $(`[data-body="${col.key}"]`);
      if (body) body.innerHTML = `<div class="empty-state small">暂无数据</div>`;
    });
  }
}
