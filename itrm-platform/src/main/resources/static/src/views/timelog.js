import { $, $$, esc, formatDate, formatDateTime } from '../utils/dom.js';
import { state, getUser } from '../store/index.js';
import { api } from '../api/request.js';
import { openModal, emptyState, loadingState } from './_ui.js';

const workTypeOptions = [
  { value: 'dev', label: '开发' },
  { value: 'test', label: '测试' },
  { value: 'review', label: '评审' },
  { value: 'doc', label: '文档' },
  { value: 'meeting', label: '会议' },
  { value: 'other', label: '其他' },
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
        <h2>工时管理</h2>
        <button class="btn primary" data-action="new-log">+ 登记工时</button>
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
  loadRequirementsForSelect();
}

async function loadRequirementsForSelect() {
  try {
    const res = await api('/requirements?size=20');
    allRequirements = res?.data?.content || res?.data || [];
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
        const res = await api('/requirements?size=20');
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
              <td>${esc(item.taskId || '-')}</td>
              <td>${esc(item.hours || 0)}</td>
              <td class="ellipsis" title="${esc(item.description || '')}">${esc(item.description || '-')}</td>
              <td class="actions">
                <button class="btn-link danger" data-action="delete-log" data-id="${esc(item.id)}">删除</button>
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
  const found = workTypeOptions.find(w => w.value === type);
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
  const reqOptions = allRequirements.length
    ? allRequirements.map(r => `<option value="${esc(r.id)}">${esc(r.reqNo || r.id)} - ${esc(r.title)}</option>`).join('')
    : `<option value="">暂无需求</option>`;

  const bodyHtml = `
    <form class="form">
      <div class="form-row">
        <label>所属需求 <span class="required">*</span></label>
        <select name="requirementId" id="log-req" required>
          <option value="">请选择</option>
          ${reqOptions}
        </select>
      </div>
      <div class="form-row">
        <label>关联任务 (可选)</label>
        <select name="taskId" id="log-task">
          <option value="">无</option>
        </select>
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
    const reqId = form.requirementId.value;
    if (!workerVal || !workDateVal || !hoursVal || !reqId) { alert('请填写必填项'); return; }

    const body = {
      requirementId: reqId,
      taskId: form.taskId.value || null,
      worker: workerVal,
      workDate: workDateVal,
      hours: hoursVal,
      workType: form.workType.value,
      description: form.description.value.trim(),
    };

    await api('/time-logs', { method: 'POST', body });
    loadLogs();
  }, '保存');

  // 需求切换时加载任务列表
  const reqSel = modal.querySelector('#log-req');
  const taskSel = modal.querySelector('#log-task');
  reqSel.onchange = async () => {
    const reqId = reqSel.value;
    taskSel.innerHTML = `<option value="">无</option>`;
    if (!reqId) return;
    try {
      const res = await api(`/tasks/by-requirement/${reqId}`);
      const tasks = res?.data || res || [];
      taskSel.innerHTML = `<option value="">无</option>` +
        tasks.map(t => `<option value="${esc(t.id)}">${esc(t.taskNo || t.id)} - ${esc(t.title)}</option>`).join('');
    } catch (e) { /* ignore */ }
  };
}

async function deleteLog(id) {
  try {
    await api(`/time-logs/${id}`, { method: 'DELETE' });
    loadLogs();
  } catch (e) {
    alert(e.message);
  }
}
