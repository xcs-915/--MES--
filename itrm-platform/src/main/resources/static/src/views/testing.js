import { $, $$, esc, formatDate, formatDateTime } from '../utils/dom.js';
import { state } from '../store/index.js';
import { api } from '../api/request.js';
import { statusPill, openModal, openDrawer, emptyState, loadingState } from './_ui.js';

const statusOptions = [
  { value: 'OPEN', label: '待处理' },
  { value: 'IN_PROGRESS', label: '处理中' },
  { value: 'RESOLVED', label: '已解决' },
  { value: 'CLOSED', label: '已关闭' },
];

const severityOptions = [
  { value: 'CRITICAL', label: '致命' },
  { value: 'HIGH', label: '严重' },
  { value: 'MEDIUM', label: '一般' },
  { value: 'LOW', label: '轻微' },
];

const typeOptions = [
  { value: 'testRecord', label: '测试记录' },
  { value: 'bug', label: '缺陷' },
];

let currentTab = 'testRecord';
let currentFilters = { status: '', severity: '', tester: '', handler: '' };
let allRecords = [];
let allRequirements = [];

export async function renderTesting() {
  const page = $('#page');
  page.innerHTML = `
    <div class="page-container">
      <div class="page-header">
        <h2>测试验收</h2>
        <button class="btn primary" data-action="new-test" data-permission="test:create">+ 新建记录</button>
      </div>

      <div class="tabs">
        <button class="tab-btn active" data-tab="testRecord">测试记录</button>
        <button class="tab-btn" data-tab="bug">缺陷管理</button>
      </div>

      <div class="filter-bar">
        <select id="f-status">
          <option value="">全部状态</option>
          ${statusOptions.map(s => `<option value="${s.value}">${s.label}</option>`).join('')}
        </select>
        <select id="f-severity">
          <option value="">全部严重程度</option>
          ${severityOptions.map(s => `<option value="${s.value}">${s.label}</option>`).join('')}
        </select>
        <input type="text" id="f-tester" placeholder="测试人员" />
        <input type="text" id="f-handler" placeholder="处理人" />
        <button class="btn" data-action="search-test">搜索</button>
        <button class="btn" data-action="reset-test">重置</button>
      </div>

      <div class="table-wrap" id="test-table">
        ${emptyState('请从需求详情添加测试记录')}
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

    if (action === 'search-test') {
      currentFilters = {
        status: $('#f-status').value,
        severity: $('#f-severity').value,
        tester: $('#f-tester').value.trim(),
        handler: $('#f-handler').value.trim(),
      };
      loadRecords();
    } else if (action === 'reset-test') {
      currentFilters = { status: '', severity: '', tester: '', handler: '' };
      $('#f-status').value = '';
      $('#f-severity').value = '';
      $('#f-tester').value = '';
      $('#f-handler').value = '';
      loadRecords();
    } else if (action === 'new-test') {
      openTestForm();
    } else if (action === 'view-test') {
      openTestDetail(id);
    } else if (action === 'resolve-test') {
      if (confirm('确定标记为已解决？')) resolveRecord(id);
    } else if (action === 'close-test') {
      if (confirm('确定关闭这条记录？')) closeRecord(id);
    } else if (action === 'delete-test') {
      if (confirm('确定删除这条记录？')) deleteRecord(id);
    }

    // Tab 切换
    const tabBtn = e.target.closest('.tab-btn');
    if (tabBtn) {
      $$('.tab-btn').forEach(b => b.classList.remove('active'));
      tabBtn.classList.add('active');
      currentTab = tabBtn.dataset.tab;
      loadRecords();
    }
  });
}

async function loadRecords() {
  const table = $('#test-table');

  if (!allRequirements.length) {
    try {
      const res = await api('/requirements?size=20');
      allRequirements = res?.data?.content || res?.data || [];
    } catch (e) {
      table.innerHTML = emptyState('请从需求详情添加测试记录');
      return;
    }
  }

  table.innerHTML = loadingState();

  try {
    // 从需求列表聚合测试记录
    const recordPromises = allRequirements.map(req =>
      api(`/test-records/by-requirement/${req.id}`)
        .then(res => (res?.data || res || []).map(r => ({ ...r, _reqTitle: req.title, _reqNo: req.reqNo || req.id })))
        .catch(() => [])
    );
    const results = await Promise.all(recordPromises);
    allRecords = results.flat();

    // 按类型筛选（tab）
    let filtered = allRecords.filter(r => {
      if (currentTab === 'bug') return r.type === 'bug' || r.type === 'BUG';
      return r.type === 'testRecord' || r.type === 'TEST' || !r.type || r.type === 'test';
    });

    // 筛选条件
    if (currentFilters.status) {
      filtered = filtered.filter(r => r.status === currentFilters.status);
    }
    if (currentFilters.severity) {
      filtered = filtered.filter(r => r.severity === currentFilters.severity);
    }
    if (currentFilters.tester) {
      const kw = currentFilters.tester.toLowerCase();
      filtered = filtered.filter(r => (r.tester || '').toLowerCase().includes(kw));
    }
    if (currentFilters.handler) {
      const kw = currentFilters.handler.toLowerCase();
      filtered = filtered.filter(r => (r.handler || '').toLowerCase().includes(kw));
    }

    if (!filtered.length) {
      table.innerHTML = emptyState('暂无数据');
      return;
    }

    table.innerHTML = `<table class="data-table">
      <thead><tr>
        <th>编号</th>
        <th>标题</th>
        <th>类型</th>
        <th>严重程度</th>
        <th>状态</th>
        <th>所属需求</th>
        <th>测试人员</th>
        <th>处理人</th>
        <th>创建时间</th>
        <th>操作</th>
      </tr></thead>
      <tbody>
        ${filtered.map(item => `
          <tr data-id="${esc(item.id)}">
            <td>${esc(item.recordNo || item.id)}</td>
            <td class="ellipsis" title="${esc(item.title)}">${esc(item.title)}</td>
            <td>${item.type === 'bug' || item.type === 'BUG' ? '缺陷' : '测试记录'}</td>
            <td><span class="severity-${(item.severity || '').toLowerCase()} status-pill">${item.severity || '-'}</span></td>
            <td>${statusPill(item.status)}</td>
            <td class="ellipsis" title="${esc(item._reqTitle || '')}">${esc(item._reqTitle || '-')}</td>
            <td>${esc(item.tester || '-')}</td>
            <td>${esc(item.handler || '-')}</td>
            <td>${formatDate(item.createdAt)}</td>
            <td class="actions">
              <button class="btn-link" data-action="view-test" data-id="${esc(item.id)}">详情</button>
              <button class="btn-link" data-action="resolve-test" data-id="${esc(item.id)}" data-permission="test:transition">解决</button>
              <button class="btn-link" data-action="close-test" data-id="${esc(item.id)}" data-permission="test:transition">关闭</button>
              <button class="btn-link danger" data-action="delete-test" data-id="${esc(item.id)}" data-permission="test:delete">删除</button>
            </td>
          </tr>
        `).join('')}
      </tbody>
    </table>`;
  } catch (e) {
    table.innerHTML = emptyState('请从需求详情添加测试记录');
  }
}

function openTestForm(id) {
  const isEdit = !!id;
  const title = isEdit ? '编辑测试记录' : '新建测试记录';

  const reqOptions = allRequirements.length
    ? allRequirements.map(r => `<option value="${esc(r.id)}">${esc(r.reqNo || r.id)} - ${esc(r.title)}</option>`).join('')
    : `<option value="">暂无需求</option>`;

  const bodyHtml = `
    <form class="form">
      <div class="form-row">
        <label>所属需求 <span class="required">*</span></label>
        <select name="requirementId" required>
          <option value="">请选择</option>
          ${reqOptions}
        </select>
      </div>
      <div class="form-row">
        <label>标题 <span class="required">*</span></label>
        <input type="text" name="title" required />
      </div>
      <div class="form-row">
        <label>类型</label>
        <select name="type">
          <option value="testRecord">测试记录</option>
          <option value="bug">缺陷</option>
        </select>
      </div>
      <div class="form-row">
        <label>严重程度</label>
        <select name="severity">
          ${severityOptions.map(s => `<option value="${s.value}">${s.label}</option>`).join('')}
        </select>
      </div>
      <div class="form-row">
        <label>描述</label>
        <textarea name="description" rows="3"></textarea>
      </div>
      <div class="form-row">
        <label>预期结果</label>
        <textarea name="expectedResult" rows="2"></textarea>
      </div>
      <div class="form-row">
        <label>实际结果</label>
        <textarea name="actualResult" rows="2"></textarea>
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
      type: form.type.value,
      severity: form.severity.value,
      description: form.description.value.trim(),
      expectedResult: form.expectedResult.value.trim(),
      actualResult: form.actualResult.value.trim(),
    };

    if (isEdit) {
      await api(`/test-records/${id}`, { method: 'PUT', body });
    } else {
      await api('/test-records', { method: 'POST', body });
    }
    loadRecords();
  }, '保存');

  if (isEdit) {
    api(`/test-records/${id}`).then(res => {
      const d = res?.data || res;
      const form = modal.querySelector('form');
      if (d) {
        form.requirementId.value = d.requirementId || '';
        form.title.value = d.title || '';
        form.type.value = d.type || 'testRecord';
        form.severity.value = d.severity || 'MEDIUM';
        form.description.value = d.description || '';
        form.expectedResult.value = d.expectedResult || '';
        form.actualResult.value = d.actualResult || '';
      }
    }).catch(e => alert(e.message));
  }
}

async function openTestDetail(id) {
  const drawer = openDrawer('测试记录详情', loadingState());
  const body = drawer.querySelector('.drawer-body');

  try {
    const record = allRecords.find(r => r.id === id);
    if (!record) {
      body.innerHTML = emptyState('暂无数据');
      return;
    }

    body.innerHTML = `
      <div class="detail-section">
        <div class="detail-title">${esc(record.title)}</div>
        <div class="detail-meta">
          <span>${statusPill(record.status)}</span>
          <span class="severity-${(record.severity || '').toLowerCase()} status-pill">${record.severity || '-'}</span>
          <span>类型: ${record.type === 'bug' || record.type === 'BUG' ? '缺陷' : '测试记录'}</span>
        </div>
      </div>

      <div class="detail-section">
        <div class="detail-grid">
          <div><span class="label">所属需求:</span> ${esc(record._reqTitle || '-')}</div>
          <div><span class="label">测试人员:</span> ${esc(record.tester || '-')}</div>
          <div><span class="label">处理人:</span> ${esc(record.handler || '-')}</div>
          <div><span class="label">创建时间:</span> ${formatDateTime(record.createdAt)}</div>
        </div>
      </div>

      <div class="detail-section">
        <div class="section-title">描述</div>
        <div class="detail-content">${esc(record.description) || '-'}</div>
      </div>

      <div class="detail-section">
        <div class="section-title">预期结果</div>
        <div class="detail-content">${esc(record.expectedResult) || '-'}</div>
      </div>

      <div class="detail-section">
        <div class="section-title">实际结果</div>
        <div class="detail-content">${esc(record.actualResult) || '-'}</div>
      </div>

      <div class="detail-actions">
        <button class="btn" data-action="resolve-test" data-id="${esc(record.id)}" data-permission="test:transition">标记解决</button>
        <button class="btn" data-action="close-test" data-id="${esc(record.id)}" data-permission="test:transition">关闭</button>
        <button class="btn danger" data-action="delete-test" data-id="${esc(record.id)}" data-permission="test:delete">删除</button>
      </div>
    `;

    body.querySelector('[data-action="resolve-test"]').onclick = () => {
      if (confirm('确定标记为已解决？')) { resolveRecord(id); drawer.remove(); }
    };
    body.querySelector('[data-action="close-test"]').onclick = () => {
      if (confirm('确定关闭这条记录？')) { closeRecord(id); drawer.remove(); }
    };
    body.querySelector('[data-action="delete-test"]').onclick = () => {
      if (confirm('确定删除这条记录？')) { deleteRecord(id); drawer.remove(); }
    };
  } catch (e) {
    body.innerHTML = emptyState(e.message || '加载失败');
  }
}

async function resolveRecord(id) {
  try {
    await api(`/test-records/${id}/resolve`, { method: 'POST' });
    loadRecords();
  } catch (e) {
    alert(e.message);
  }
}

async function closeRecord(id) {
  try {
    await api(`/test-records/${id}/close`, { method: 'POST' });
    loadRecords();
  } catch (e) {
    alert(e.message);
  }
}

async function deleteRecord(id) {
  try {
    await api(`/test-records/${id}`, { method: 'DELETE' });
    loadRecords();
  } catch (e) {
    alert(e.message);
  }
}
