import { $, $$, esc, formatDate, formatDateTime } from '../utils/dom.js';
import { state } from '../store/index.js';
import { api } from '../api/request.js';
import { statusPill, priorityPill, progressBar, pagination, openModal, emptyState, loadingState } from './_ui.js';

const taskStatusOptions = [
  { value: 'TODO', label: '待办' },
  { value: 'IN_PROGRESS', label: '进行中' },
  { value: 'TESTING', label: '测试中' },
  { value: 'DONE', label: '已完成' },
  { value: 'BLOCKED', label: '阻塞' },
];

const priorityOptions = [
  { value: 'URGENT', label: '紧急' },
  { value: 'HIGH', label: '高' },
  { value: 'MEDIUM', label: '中' },
  { value: 'LOW', label: '低' },
];

let currentPage = 0;
let currentFilters = { keyword: '', status: '', assignee: '' };
let allTasks = [];
let allRequirements = [];

export async function renderTasks() {
  const page = $('#page');
  page.innerHTML = `
    <div class="page-container">
      <div class="page-header">
        <div><h2>开发任务</h2><div class="page-subtitle">任务由需求评审后拆分而来，此处统一查看所有需求的执行项</div></div>
        <button class="btn primary" data-action="go-requirements" data-permission="requirement:view">去需求池拆分任务</button>
      </div>

      <div class="task-origin-banner"><span class="task-origin-icon">↳</span><div><strong>任务从需求来</strong><p>需求审批通过并排期后，在需求子页的“开发任务”页签中拆分；本页用于跨需求跟踪执行。</p></div></div>
      <div class="task-kpis" id="task-kpis"></div>

      <div class="filter-bar">
        <input type="text" id="f-keyword" placeholder="搜索任务标题、编号..." />
        <select id="f-status">
          <option value="">全部状态</option>
          ${taskStatusOptions.map(s => `<option value="${s.value}">${s.label}</option>`).join('')}
        </select>
        <input type="text" id="f-assignee" placeholder="负责人" />
        <button class="btn" data-action="search-task">搜索</button>
        <button class="btn" data-action="reset-task">重置</button>
      </div>

      <div class="table-wrap" id="task-table">
          ${emptyState('开发任务需从需求详情中拆分')}
      </div>

      <div class="pagination-wrap" id="task-pagination"></div>
    </div>
  `;

  bindEvents();
  await loadRequirementsForSelect();
  loadTasks(0);
}

async function loadRequirementsForSelect() {
  try {
    const res = await api('/requirements?size=500');
    allRequirements = res?.data?.items || res?.data?.content || [];
  } catch (e) {
    allRequirements = [];
  }
}

function bindEvents() {
  const container = $('.page-container');

  container.addEventListener('click', e => {
    const btn = e.target.closest('[data-action]');
    if (!btn) return;
    const action = btn.dataset.action;
    const id = btn.dataset.id;

    if (action === 'search-task') {
      currentFilters = {
        keyword: $('#f-keyword').value.trim(),
        status: $('#f-status').value,
        assignee: $('#f-assignee').value.trim(),
      };
      loadTasks(0);
    } else if (action === 'reset-task') {
      currentFilters = { keyword: '', status: '', assignee: '' };
      $('#f-keyword').value = '';
      $('#f-status').value = '';
      $('#f-assignee').value = '';
      loadTasks(0);
    } else if (action === 'go-requirements') {
      document.querySelector('[data-view="requirements"]')?.click();
    } else if (action === 'edit-task') {
      openTaskForm(id);
    } else if (action === 'delete-task') {
      if (confirm('确定要删除这条任务吗？')) deleteTask(id);
    } else if (action === 'view-task-requirement') {
      sessionStorage.setItem('itrm_current_req_id', btn.dataset.requirementId);
      document.querySelector('[data-view="requirements"]')?.click();
    }
  });
}

async function loadTasks(pageNum) {
  currentPage = pageNum ?? 0;
  const table = $('#task-table');
  const pagWrap = $('#task-pagination');

  if (!allRequirements.length) {
    try {
      const res = await api('/requirements?size=500');
      allRequirements = res?.data?.items || res?.data?.content || [];
    } catch (e) {
      table.innerHTML = emptyState('请从需求详情查看任务');
      return;
    }
  }

  table.innerHTML = loadingState();

  try {
    const taskPromises = allRequirements.map(req =>
      api(`/tasks/by-requirement/${req.id}`)
        .then(res => (res?.data || res || []).map(t => ({ ...t, _reqTitle: req.title, _reqNo: req.reqNo || req.id })))
        .catch(() => [])
    );
    const results = await Promise.all(taskPromises);
    allTasks = results.flat();

    // 筛选
    let filtered = allTasks;
    if (currentFilters.keyword) {
      const kw = currentFilters.keyword.toLowerCase();
      filtered = filtered.filter(t => (t.title || '').toLowerCase().includes(kw) || (t.taskNo || '').toLowerCase().includes(kw));
    }
    if (currentFilters.status) {
      filtered = filtered.filter(t => t.status === currentFilters.status);
    }
    if (currentFilters.assignee) {
      const a = currentFilters.assignee.toLowerCase();
      filtered = filtered.filter(t => (t.assignee || '').toLowerCase().includes(a));
    }
    updateTaskSummary(filtered);

    const total = filtered.length;
    const size = 20;
    const totalPages = Math.ceil(total / size) || 1;
    const paged = filtered.slice(currentPage * size, (currentPage + 1) * size);

    if (!paged.length) {
      table.innerHTML = emptyState('暂无数据');
      pagWrap.innerHTML = '';
      return;
    }

    renderTaskTable(paged);
    pagWrap.innerHTML = pagination({ page: currentPage, total, totalPages }, 'task');
    bindPagination();
  } catch (e) {
    table.innerHTML = emptyState('请从需求详情查看任务');
    pagWrap.innerHTML = '';
  }
}

function renderTaskTable(items) {
  const table = $('#task-table');
  table.innerHTML = `<table class="data-table">
    <thead><tr>
      <th>任务编号</th>
      <th style="min-width:220px">任务</th>
      <th>所属需求</th>
      <th>状态</th>
      <th>紧急程度</th>
      <th>负责人</th>
      <th>预估工时</th>
      <th>实际工时</th>
      <th>进度</th>
      <th>截止日期</th>
      <th>操作</th>
    </tr></thead>
    <tbody>
      ${items.map(item => `
        <tr data-id="${esc(item.id)}">
          <td>${esc(item.taskNo || item.id)}</td>
          <td><div class="task-title-cell"><strong>${esc(item.title)}</strong><small>${esc(item.description || '暂无任务说明')}</small></div></td>
          <td><button class="requirement-chip" data-action="view-task-requirement" data-requirement-id="${esc(item.requirementId)}"><span>${esc(item._reqNo || '')}</span>${esc(item._reqTitle || '-')}</button></td>
          <td>${statusPill(item.status)}</td>
          <td>${priorityPill(item.priority || item.urgency)}</td>
          <td>${item.assignee ? `<div class="person-cell"><span class="avatar avatar-sm">${esc(item.assignee[0])}</span><strong>${esc(item.assignee)}</strong></div>` : '—'}</td>
          <td>${esc(item.estimatedHours || '-')}</td>
          <td>${esc(item.actualHours || '-')}</td>
          <td><div style="display:flex;align-items:center;min-width:80px">${progressBar(item.progress || 0)}</div></td>
          <td>${formatDate(item.dueDate)}</td>
          <td class="actions">
            <button class="btn-link" data-action="edit-task" data-id="${esc(item.id)}" data-permission="task:edit">编辑</button>
            <button class="btn-link danger" data-action="delete-task" data-id="${esc(item.id)}" data-permission="task:delete">删除</button>
          </td>
        </tr>
      `).join('')}
    </tbody>
  </table>`;
}

function updateTaskSummary(items) {
  const wrap = $('#task-kpis'); if (!wrap) return;
  const today = new Date().toISOString().slice(0, 10);
  const metrics = [
    ['任务总数', items.length, '全部开发任务', 'blue'],
    ['进行中', items.filter(item => item.status === 'IN_PROGRESS').length, '当前正在开发', 'violet'],
    ['待测试', items.filter(item => item.status === 'TESTING').length, '等待测试验证', 'amber'],
    ['逾期风险', items.filter(item => item.dueDate && item.dueDate < today && item.status !== 'DONE').length, '超过截止日期', 'red'],
  ];
  wrap.innerHTML = metrics.map(([label, value, note, tone]) => `<div class="task-kpi ${tone}"><span>${label}</span><strong>${value}</strong><small>${note}</small></div>`).join('');
}

function bindPagination() {
  $$('#task-pagination button[data-page]').forEach(btn => {
    btn.onclick = () => {
      if (btn.disabled) return;
      loadTasks(parseInt(btn.dataset.page));
    };
  });
}

function openTaskForm(id) {
  const isEdit = !!id;
  const title = '编辑开发任务';

  const reqOptions = allRequirements.length
    ? allRequirements.map(r => `<option value="${esc(r.id)}">${esc(r.reqNo || r.id)} - ${esc(r.title)}</option>`).join('')
    : `<option value="">暂无需求</option>`;

  const bodyHtml = `
    <form class="form">
      <div class="form-row">
        <label>所属需求 <span class="required">*</span></label>
        <select name="requirementId" required disabled>
          <option value="">请选择</option>
          ${reqOptions}
        </select>
      </div>
      <div class="form-row">
        <label>任务标题 <span class="required">*</span></label>
        <input type="text" name="title" required />
      </div>
      <div class="form-row">
        <label>任务描述</label>
        <textarea name="description" rows="3"></textarea>
      </div>
      <div class="form-row">
        <label>紧急程度</label>
        <select name="priority">
          ${priorityOptions.map(p => `<option value="${p.value}" ${p.value === 'MEDIUM' ? 'selected' : ''}>${p.label}</option>`).join('')}
        </select>
      </div>
      <div class="form-row">
        <label>负责人</label>
        <input type="text" name="assignee" />
      </div>
      <div class="form-row">
        <label>预估工时 (小时)</label>
        <input type="number" name="estimatedHours" min="0" step="0.5" />
      </div>
      <div class="form-row">
        <label>开始日期</label>
        <input type="date" name="startDate" />
      </div>
      <div class="form-row">
        <label>截止日期</label>
        <input type="date" name="dueDate" />
      </div>
    </form>
  `;

  const modal = openModal(title, bodyHtml, async (overlay) => {
    const form = overlay.querySelector('form');
    const titleVal = form.title.value.trim();
    const reqId = form.requirementId.value;
    if (!titleVal || !reqId) { alert('请填写必填项'); return; }

    const body = {
      requirementId: reqId,
      title: titleVal,
      description: form.description.value.trim(),
      priority: form.priority.value,
      assignee: form.assignee.value.trim(),
      estimatedHours: parseFloat(form.estimatedHours.value) || 0,
      startDate: form.startDate.value,
      dueDate: form.dueDate.value,
    };

    await api(`/tasks/${id}`, { method: 'PUT', body });
    loadTasks(currentPage);
  }, '保存');

  if (isEdit) {
    api(`/tasks/${id}`).then(res => {
      const d = res?.data || res;
      const form = modal.querySelector('form');
      if (d) {
        form.requirementId.value = d.requirementId || '';
        form.title.value = d.title || '';
        form.description.value = d.description || '';
        form.priority.value = d.priority || 'MEDIUM';
        form.assignee.value = d.assignee || '';
        form.estimatedHours.value = d.estimatedHours || '';
        form.startDate.value = d.startDate ? d.startDate.slice(0, 10) : '';
        form.dueDate.value = d.dueDate ? d.dueDate.slice(0, 10) : '';
      }
    }).catch(e => alert(e.message));
  }
}

async function deleteTask(id) {
  try {
    await api(`/tasks/${id}`, { method: 'DELETE' });
    loadTasks(currentPage);
  } catch (e) {
    alert(e.message);
  }
}
