import { $, $$, esc, formatDate, formatDateTime } from '../utils/dom.js';
import { state, getUser } from '../store/index.js';
import { api } from '../api/request.js';
import { openModal, emptyState, loadingState } from './_ui.js';

const workTypeOptions = [
  { value: 'DEV', label: '开发' },
  { value: 'TEST', label: '测试' },
  { value: 'REVIEW', label: '评审' },
  { value: 'DOC', label: '文档' },
  { value: 'MEETING', label: '会议' },
  { value: 'OTHER', label: '其他' },
];

let currentFilters = { worker: '', startDate: '', endDate: '' };
let allLogs = [];
let allRequirements = [];
let allTasks = [];
let stats = { totalHours: 0, byType: {} };

export async function renderTimelog() {
  const page = $('#page');
  page.innerHTML = `
    <div class="page-container">
      <div class="page-header">
        <div><h2>工时管理</h2><div class="page-subtitle">工时必须登记到需求拆分的开发任务，并自动回写任务与需求实际工时</div></div>
        <button class="btn primary" data-action="new-log" data-permission="timelog:create">+ 登记工时</button>
      </div>

      <div class="timelog-layout">
        <div class="timelog-main">
          <div class="filter-bar">
            <input type="text" id="f-worker" placeholder="工作人员" />
            <input type="date" id="f-startDate" title="开始日期" />
            <input type="date" id="f-endDate" title="结束日期" />
            <button class="btn" data-action="search-log">搜索</button>
            <button class="btn" data-action="reset-log">重置</button>
          </div>

          <div class="table-wrap" id="log-table">
            ${emptyState('暂无工时记录，请点击"登记工时"添加')}
          </div>
        </div>

        <div class="timelog-side">
          <div class="stat-card stat-blue">
            <div class="stat-icon">&#9201;</div>
            <div class="stat-info">
              <div class="stat-value" id="stat-total-hours">0</div>
              <div class="stat-label">总工时</div>
            </div>
          </div>

          <div class="side-card">
            <div class="side-title">工作类型分布</div>
            <div class="side-content" id="stat-by-type">
              ${emptyState('暂无数据')}
            </div>
          </div>
        </div>
      </div>
    </div>
  `;

  bindEvents();
  await loadRequirementsForSelect();
  loadLogs();
}

async function loadRequirementsForSelect() {
  try {
    const res = await api('/requirements?size=500');
    allRequirements = res?.data?.content || res?.data || [];
    const taskGroups = await Promise.all(allRequirements.map(req =>
      api(`/tasks/by-requirement/${req.id}`)
        .then(result => (result?.data || result || []).map(task => ({ ...task, _reqTitle: req.title, _reqNo: req.reqNo, _requirementId: req.id })))
        .catch(() => [])
    ));
    allTasks = taskGroups.flat();
  } catch (e) {
    allRequirements = [];
    allTasks = [];
  }
}

function bindEvents() {
  const container = $('.page-container');

  container.addEventListener('click', e => {
    const btn = e.target.closest('[data-action]');
    if (!btn) return;
    const action = btn.dataset.action;
    const id = btn.dataset.id;

    if (action === 'search-log') {
      currentFilters = {
        worker: $('#f-worker').value.trim(),
        startDate: $('#f-startDate').value,
        endDate: $('#f-endDate').value,
      };
      loadLogs();
    } else if (action === 'reset-log') {
      currentFilters = { worker: '', startDate: '', endDate: '' };
      $('#f-worker').value = '';
      $('#f-startDate').value = '';
      $('#f-endDate').value = '';
      loadLogs();
    } else if (action === 'new-log') {
      openLogForm();
    } else if (action === 'delete-log') {
      if (confirm('确定删除这条工时记录？')) deleteLog(id);
    }
  });
}

async function loadLogs() {
  const table = $('#log-table');
  const worker = currentFilters.worker;

  table.innerHTML = loadingState();

  try {
    // 如果指定了工作人员，直接按人员查询
    if (worker) {
      const [logsRes, statsRes] = await Promise.all([
        api(`/time-logs/by-worker/${encodeURIComponent(worker)}`).catch(() => ({ data: [] })),
        api(`/time-logs/stats/worker/${encodeURIComponent(worker)}?startDate=${currentFilters.startDate || ''}&endDate=${currentFilters.endDate || ''}`).catch(() => ({ data: null })),
      ]);

      allLogs = logsRes?.data || logsRes || [];
      allLogs = allLogs.map(log => {
        const req = allRequirements.find(item => String(item.id) === String(log.requirementId));
        return { ...log, _reqTitle: req?.title, _reqNo: req?.reqNo };
      });
      const statsData = statsRes?.data || null;
      if (statsData) {
        stats.totalHours = statsData.totalHours || 0;
        stats.byType = statsData.byType || {};
      } else {
        // 手动计算
        stats.totalHours = allLogs.reduce((s, l) => s + (l.hours || 0), 0);
        stats.byType = {};
        allLogs.forEach(l => {
          const type = l.workType || 'other';
          stats.byType[type] = (stats.byType[type] || 0) + (l.hours || 0);
        });
      }
    } else {
      // 没指定人员，从需求列表聚合
      if (!allRequirements.length) {
        const res = await api('/requirements?size=500');
        allRequirements = res?.data?.content || res?.data || [];
      }

      const logPromises = allRequirements.map(req =>
        api(`/time-logs/by-requirement/${req.id}`)
          .then(res => (res?.data || res || []).map(l => ({ ...l, _reqTitle: req.title, _reqNo: req.reqNo || req.id })))
          .catch(() => [])
      );
      const results = await Promise.all(logPromises);
      allLogs = results.flat();

      // 计算统计
      stats.totalHours = allLogs.reduce((s, l) => s + (l.hours || 0), 0);
      stats.byType = {};
      allLogs.forEach(l => {
        const type = l.workType || 'other';
        stats.byType[type] = (stats.byType[type] || 0) + (l.hours || 0);
      });
    }

    // 按日期筛选
    let filtered = allLogs;
    if (currentFilters.startDate) {
      filtered = filtered.filter(l => l.workDate && l.workDate >= currentFilters.startDate);
    }
    if (currentFilters.endDate) {
      filtered = filtered.filter(l => l.workDate && l.workDate <= currentFilters.endDate);
    }

    // 按日期倒序
    filtered.sort((a, b) => (b.workDate || '').localeCompare(a.workDate || ''));

    if (!filtered.length) {
      table.innerHTML = emptyState('暂无数据');
    } else {
      table.innerHTML = `<table class="data-table">
        <thead><tr>
          <th>工作日期</th>
          <th>工作人员</th>
          <th>工作类型</th>
          <th>所属需求</th>
          <th>任务</th>
          <th>工时 (小时)</th>
          <th>工作描述</th>
          <th>操作</th>
        </tr></thead>
        <tbody>
          ${filtered.map(item => `
            <tr data-id="${esc(item.id)}">
              <td>${formatDate(item.workDate)}</td>
              <td>${esc(item.worker || '-')}</td>
              <td>${getWorkTypeLabel(item.workType)}</td>
              <td class="ellipsis" title="${esc(item._reqTitle || '')}">${esc(item._reqTitle || '-')}</td>
              <td>${esc(taskLabel(item.taskId))}</td>
              <td>${esc(item.hours || 0)}</td>
              <td class="ellipsis" title="${esc(item.description || '')}">${esc(item.description || '-')}</td>
              <td class="actions">
                <button class="btn-link danger" data-action="delete-log" data-id="${esc(item.id)}" data-permission="timelog:delete">删除</button>
              </td>
            </tr>
          `).join('')}
        </tbody>
      </table>`;
    }

    updateStats();
  } catch (e) {
    table.innerHTML = emptyState('暂无工时记录');
    updateStats();
  }
}

function getWorkTypeLabel(type) {
  const normalized = String(type || '').toUpperCase();
  const found = workTypeOptions.find(w => w.value === normalized);
  return found ? found.label : (type || '-');
}

function updateStats() {
  const totalEl = $('#stat-total-hours');
  const typeEl = $('#stat-by-type');

  if (totalEl) totalEl.textContent = stats.totalHours?.toFixed?.(1) || stats.totalHours || 0;

  if (typeEl) {
    const entries = Object.entries(stats.byType || {});
    if (!entries.length) {
      typeEl.innerHTML = `<div class="empty-state small">暂无数据</div>`;
      return;
    }
    const total = stats.totalHours || 1;
    typeEl.innerHTML = entries.map(([type, hours]) => {
      const pct = Math.round((hours / total) * 100);
      return `<div class="type-stat">
        <div class="type-stat-row">
          <span>${getWorkTypeLabel(type)}</span>
          <span>${hours?.toFixed?.(1) || hours} 小时</span>
        </div>
        <div class="progress-bar small"><div class="fill" style="width:${pct}%"></div></div>
      </div>`;
    }).join('');
  }
}

function openLogForm() {
  const taskOptions = allRequirements.map(req => {
    const tasks = allTasks.filter(task => String(task.requirementId) === String(req.id));
    return tasks.length ? `<optgroup label="${esc(req.reqNo || req.id)} · ${esc(req.title)}">${tasks.map(task => `<option value="${esc(task.id)}">${esc(task.taskNo || task.id)} - ${esc(task.title)}</option>`).join('')}</optgroup>` : '';
  }).join('');

  const bodyHtml = `
    <form class="form">
      <div class="form-row">
        <label>开发任务 <span class="required">*</span></label>
        <select name="taskId" id="log-task" required>
          <option value="">请选择</option>
          ${taskOptions || `<option value="" disabled>暂无可登记工时的任务</option>`}
        </select>
      </div>
      <div class="form-row">
        <label>所属需求</label>
        <div class="form-readonly" id="log-requirement-hint">选择任务后自动带出</div>
      </div>
      <div class="form-row">
        <label>工作人员 <span class="required">*</span></label>
        <input type="text" name="worker" required />
      </div>
      <div class="form-row">
        <label>工作日期 <span class="required">*</span></label>
        <input type="date" name="workDate" required />
      </div>
      <div class="form-row">
        <label>工时 (小时) <span class="required">*</span></label>
        <input type="number" name="hours" min="0" step="0.5" required />
      </div>
      <div class="form-row">
        <label>工作类型</label>
        <select name="workType">
          ${workTypeOptions.map(w => `<option value="${w.value}">${w.label}</option>`).join('')}
        </select>
      </div>
      <div class="form-row">
        <label>工作描述</label>
        <textarea name="description" rows="3"></textarea>
      </div>
    </form>
  `;

  const modal = openModal('登记工时', bodyHtml, async (overlay) => {
    const form = overlay.querySelector('form');
    const workerVal = form.worker.value.trim();
    const workDateVal = form.workDate.value;
    const hoursVal = parseFloat(form.hours.value);
    const taskId = form.taskId.value;
    if (!workerVal || !workDateVal || !hoursVal || !taskId) { alert('请填写必填项'); return; }

    const body = {
      taskId,
      worker: workerVal,
      workDate: workDateVal,
      hours: hoursVal,
      workType: form.workType.value,
      description: form.description.value.trim(),
    };

    await api('/time-logs', { method: 'POST', body });
    loadLogs();
  }, '保存');

  const currentUser = getUser() || {};
  modal.querySelector('input[name="worker"]').value = currentUser.realName || currentUser.username || '';
  modal.querySelector('input[name="workDate"]').value = new Date().toISOString().slice(0, 10);

  const taskSel = modal.querySelector('#log-task');
  const hint = modal.querySelector('#log-requirement-hint');
  taskSel.onchange = () => {
    const task = allTasks.find(item => String(item.id) === String(taskSel.value));
    hint.textContent = task ? `${task._reqNo || task.requirementId} - ${task._reqTitle || ''}` : '选择任务后自动带出';
  };
}

function taskLabel(taskId) {
  const task = allTasks.find(item => String(item.id) === String(taskId));
  return task ? `${task.taskNo || task.id} - ${task.title}` : (taskId || '-');
}

async function deleteLog(id) {
  try {
    await api(`/time-logs/${id}`, { method: 'DELETE' });
    loadLogs();
  } catch (e) {
    alert(e.message);
  }
}
