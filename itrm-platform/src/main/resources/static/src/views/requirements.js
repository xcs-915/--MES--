import { $, $$, esc, formatDate, formatDateTime } from '../utils/dom.js';
import { state } from '../store/index.js';
import { api } from '../api/request.js';
import { statusPill, priorityPill, progressBar, pagination, openModal, openDrawer, emptyState, loadingState } from './_ui.js';

const statusOptions = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'PENDING_RECEIVE', label: '待接收' },
  { value: 'PENDING_PLAN', label: '待排期' },
  { value: 'IN_DEVELOPMENT', label: '开发中' },
  { value: 'IN_TESTING', label: '测试中' },
  { value: 'PENDING_RELEASE', label: '待发布' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'REJECTED', label: '已驳回' },
];

const urgencyOptions = [
  { value: 'URGENT', label: '紧急' },
  { value: 'HIGH', label: '高' },
  { value: 'MEDIUM', label: '中' },
  { value: 'LOW', label: '低' },
];

const systemOptions = ['MES', 'WMS', 'SAP', 'OA', 'SCADA', '其他'];

const typeOptions = ['新功能', '功能优化', 'BUG', '接口', '报表', '其他'];

let currentPage = 0;
let currentFilters = { keyword: '', status: '', urgency: '', category: '', belongSystem: '', sourceDept: '' };

export async function renderRequirements() {
  const page = $('#page');
  page.innerHTML = `
    <div class="page-container">
      <div class="page-header">
        <h2>需求池</h2>
        <button class="btn primary" data-action="new-req">+ 新建需求</button>
      </div>

      <div class="filter-bar">
        <input type="text" id="f-keyword" placeholder="搜索需求标题、编号..." />
        <select id="f-status">
          <option value="">全部状态</option>
          ${statusOptions.map(s => `<option value="${s.value}">${s.label}</option>`).join('')}
        </select>
        <select id="f-system">
          <option value="">全部所属系统</option>
          ${systemOptions.map(s => `<option value="${s}">${s}</option>`).join('')}
        </select>
        <select id="f-type">
          <option value="">全部需求类型</option>
          ${typeOptions.map(t => `<option value="${t}">${t}</option>`).join('')}
        </select>
        <select id="f-urgency">
          <option value="">全部紧急程度</option>
          ${urgencyOptions.map(u => `<option value="${u.value}">${u.label}</option>`).join('')}
        </select>
        <input type="text" id="f-sourceDept" placeholder="来源部门" />
        <button class="btn" data-action="search-req">搜索</button>
        <button class="btn" data-action="reset-req">重置</button>
      </div>

      <div class="table-wrap" id="req-table">
        ${loadingState()}
      </div>

      <div class="pagination-wrap" id="req-pagination"></div>
    </div>
  `;

  bindEvents();
  loadRequirements(0);
}

export async function loadRequirements(pageNum) {
  currentPage = pageNum ?? 0;
  const table = $('#req-table');
  const pagWrap = $('#req-pagination');
  table.innerHTML = loadingState();

  const params = new URLSearchParams({
    page: currentPage,
    size: 20,
    ...Object.fromEntries(Object.entries(currentFilters).filter(([, v]) => v)),
  });

  try {
    const res = await api(`/requirements?${params.toString()}`);
    const data = res?.data || res || {};
    const items = data.content || data.items || [];
    const total = data.totalElements ?? data.total ?? 0;
    const totalPages = data.totalPages ?? Math.ceil(total / 20);

    if (!items.length) {
      table.innerHTML = emptyState('暂无需求数据');
      pagWrap.innerHTML = '';
      return;
    }

    table.innerHTML = `<table class="data-table">
      <thead><tr>
        <th>需求编号</th>
        <th>需求标题</th>
        <th>所属系统</th>
        <th>需求类型</th>
        <th>紧急程度</th>
        <th>当前状态</th>
        <th>来源部门</th>
        <th>提出人</th>
        <th>开发负责人</th>
        <th>进度</th>
        <th>创建时间</th>
        <th>操作</th>
      </tr></thead>
      <tbody>
        ${items.map(item => `
          <tr data-id="${esc(item.id)}">
            <td>${esc(item.reqNo || item.id)}</td>
            <td class="ellipsis" title="${esc(item.title)}">${esc(item.title)}</td>
            <td>${esc(item.belongSystem || '-')}</td>
            <td>${esc(item.category || item.reqType || '-')}</td>
            <td>${priorityPill(item.urgency || item.priority)}</td>
            <td>${statusPill(item.status)}</td>
            <td>${esc(item.sourceDept || '-')}</td>
            <td>${esc(item.proposer || '-')}</td>
            <td>${esc(item.devOwner || '-')}</td>
            <td><div style="display:flex;align-items:center;min-width:80px">${progressBar(item.progress || 0)}</div></td>
            <td>${formatDate(item.createdAt)}</td>
            <td class="actions">
              <button class="btn-link" data-action="view-req" data-id="${esc(item.id)}">详情</button>
              ${item.status === 'DRAFT' ? `<button class="btn-link" data-action="edit-req" data-id="${esc(item.id)}">编辑</button>` : ''}
              ${item.status === 'DRAFT' ? `<button class="btn-link danger" data-action="delete-req" data-id="${esc(item.id)}">删除</button>` : ''}
            </td>
          </tr>
        `).join('')}
      </tbody>
    </table>`;

    pagWrap.innerHTML = pagination({ page: currentPage, total, totalPages }, 'req');
    bindPagination();
  } catch (e) {
    table.innerHTML = emptyState(e.message || '加载失败');
    pagWrap.innerHTML = '';
  }
}

function bindEvents() {
  const container = $('.page-container');

  container.addEventListener('click', e => {
    const btn = e.target.closest('[data-action]');
    if (!btn) return;
    const action = btn.dataset.action;
    const id = btn.dataset.id;

    if (action === 'search-req') {
      currentFilters = {
        keyword: $('#f-keyword').value.trim(),
        status: $('#f-status').value,
        urgency: $('#f-urgency').value,
        category: $('#f-type').value,
        belongSystem: $('#f-system').value,
        sourceDept: $('#f-sourceDept').value.trim(),
      };
      loadRequirements(0);
    } else if (action === 'reset-req') {
      currentFilters = { keyword: '', status: '', urgency: '', category: '', belongSystem: '', sourceDept: '' };
      $('#f-keyword').value = '';
      $('#f-status').value = '';
      $('#f-urgency').value = '';
      $('#f-type').value = '';
      $('#f-system').value = '';
      $('#f-sourceDept').value = '';
      loadRequirements(0);
    } else if (action === 'new-req') {
      openReqForm();
    } else if (action === 'edit-req') {
      openReqForm(id);
    } else if (action === 'delete-req') {
      if (confirm('确定要删除这条需求吗？')) deleteReq(id);
    } else if (action === 'view-req') {
      openReqDetail(id);
    }
  });
}

function bindPagination() {
  $$('#req-pagination button[data-page]').forEach(btn => {
    btn.onclick = () => {
      if (btn.disabled) return;
      loadRequirements(parseInt(btn.dataset.page));
    };
  });
}

function openReqForm(id) {
  const isEdit = !!id;
  const title = isEdit ? '编辑需求' : '新建需求';

  const bodyHtml = `
    <form class="form">
      <div class="form-row">
        <label>需求标题 <span class="required">*</span></label>
        <input type="text" name="title" required />
      </div>
      <div class="form-row">
        <label>所属系统 <span class="required">*</span></label>
        <select name="belongSystem" required>
          <option value="">请选择</option>
          ${systemOptions.map(s => `<option value="${s}">${s}</option>`).join('')}
        </select>
      </div>
      <div class="form-row">
        <label>需求类型 <span class="required">*</span></label>
        <select name="category" required>
          <option value="">请选择</option>
          ${typeOptions.map(t => `<option value="${t}">${t}</option>`).join('')}
        </select>
      </div>
      <div class="form-row">
        <label>紧急程度</label>
        <select name="urgency">
          <option value="">请选择</option>
          ${urgencyOptions.map(u => `<option value="${u.value}">${u.label}</option>`).join('')}
        </select>
      </div>
      <div class="form-row">
        <label>来源部门</label>
        <input type="text" name="sourceDept" />
      </div>
      <div class="form-row">
        <label>提出人</label>
        <input type="text" name="proposer" />
      </div>
      <div class="form-row">
        <label>当前问题</label>
        <textarea name="currentProblem" rows="3" placeholder="请描述当前遇到的问题..."></textarea>
      </div>
      <div class="form-row">
        <label>预期效果</label>
        <textarea name="expectedEffect" rows="3" placeholder="请描述期望达到的效果..."></textarea>
      </div>
      <div class="form-row">
        <label>验收标准</label>
        <textarea name="acceptanceCriteria" rows="3" placeholder="请描述验收标准..."></textarea>
      </div>
      <div class="form-row">
        <label>备注</label>
        <textarea name="remark" rows="2"></textarea>
      </div>
    </form>
  `;

  const modal = openModal(title, bodyHtml, async (overlay) => {
    const form = overlay.querySelector('form');
    const titleVal = form.title.value.trim();
    const systemVal = form.belongSystem.value;
    const categoryVal = form.category.value;
    if (!titleVal || !systemVal || !categoryVal) { alert('请填写必填项'); return; }

    const body = {
      title: titleVal,
      belongSystem: systemVal,
      category: categoryVal,
      urgency: form.urgency.value || 'MEDIUM',
      sourceDept: form.sourceDept.value.trim(),
      proposer: form.proposer.value.trim(),
      currentProblem: form.currentProblem.value.trim(),
      expectedEffect: form.expectedEffect.value.trim(),
      acceptanceCriteria: form.acceptanceCriteria.value.trim(),
      remark: form.remark.value.trim(),
    };

    if (isEdit) {
      await api(`/requirements/${id}`, { method: 'PUT', body });
    } else {
      await api('/requirements', { method: 'POST', body });
    }
    loadRequirements(currentPage);
  }, '保存');

  // 编辑时加载数据
  if (isEdit) {
    api(`/requirements/${id}`).then(res => {
      const d = res?.data || res;
      const form = modal.querySelector('form');
      if (d) {
        form.title.value = d.title || '';
        form.belongSystem.value = d.belongSystem || '';
        form.category.value = d.category || d.reqType || '';
        form.urgency.value = d.urgency || d.priority || 'MEDIUM';
        form.sourceDept.value = d.sourceDept || '';
        form.proposer.value = d.proposer || '';
        form.currentProblem.value = d.currentProblem || '';
        form.expectedEffect.value = d.expectedEffect || '';
        form.acceptanceCriteria.value = d.acceptanceCriteria || '';
        form.remark.value = d.remark || '';
      }
    }).catch(e => alert(e.message));
  }
}

async function deleteReq(id) {
  try {
    await api(`/requirements/${id}`, { method: 'DELETE' });
    loadRequirements(currentPage);
  } catch (e) {
    alert(e.message);
  }
}

// ========== 详情抽屉 ==========

let currentDetailId = null;
let currentDrawer = null;

async function openReqDetail(id) {
  currentDetailId = id;
  const drawer = openDrawer('需求详情', loadingState());
  currentDrawer = drawer;
  const body = drawer.querySelector('.drawer-body');

  try {
    const [reqRes, approvalRes, changeRes, tasksRes] = await Promise.all([
      api(`/requirements/${id}`),
      api(`/requirements/${id}/approval-records`).catch(() => ({ data: [] })),
      api(`/requirements/${id}/change-logs`).catch(() => ({ data: [] })),
      api(`/tasks/by-requirement/${id}`).catch(() => ({ data: [] })),
    ]);

    const req = reqRes?.data || reqRes || {};
    const approvalRecords = approvalRes?.data || approvalRes || [];
    const changeLogs = changeRes?.data || changeRes || [];
    const tasks = tasksRes?.data || tasksRes || [];

    body.innerHTML = `
      <div class="detail-tabs">
        <button class="tab-btn active" data-tab="basic">基本信息</button>
        <button class="tab-btn" data-tab="tasks">任务列表 (${tasks.length})</button>
        <button class="tab-btn" data-tab="approval">审批记录</button>
        <button class="tab-btn" data-tab="change">变更记录</button>
        <button class="tab-btn" data-tab="attachments">附件</button>
      </div>

      <div class="tab-content active" data-tab-content="basic">
        ${renderBasicInfo(req)}
      </div>
      <div class="tab-content" data-tab-content="tasks">
        ${renderTasksTab(tasks, req)}
      </div>
      <div class="tab-content" data-tab-content="approval">
        ${renderApprovalTab(approvalRecords)}
      </div>
      <div class="tab-content" data-tab-content="change">
        ${renderChangeTab(changeLogs)}
      </div>
      <div class="tab-content" data-tab-content="attachments">
        ${renderAttachmentsTab(req)}
      </div>
    `;

    // Tab 切换
    $$('.tab-btn', body).forEach(btn => {
      btn.onclick = () => {
        $$('.tab-btn', body).forEach(b => b.classList.remove('active'));
        $$('.tab-content', body).forEach(c => c.classList.remove('active'));
        btn.classList.add('active');
        body.querySelector(`[data-tab-content="${btn.dataset.tab}"]`).classList.add('active');
      };
    });

    // 绑定操作按钮
    bindDetailActions(req, drawer);
    // 绑定任务按钮
    bindTaskTabActions(req, drawer);
  } catch (e) {
    body.innerHTML = emptyState(e.message || '加载失败');
  }
}

function renderBasicInfo(req) {
  const actionButtons = renderActionButtons(req.status, req.id);

  return `
    <div class="detail-section">
      <div class="detail-title">${esc(req.title)}</div>
      <div class="detail-meta">
        <span>${statusPill(req.status)}</span>
        <span>${priorityPill(req.urgency || req.priority)}</span>
        <span style="color:var(--text-muted)">编号: ${esc(req.reqNo || req.id)}</span>
      </div>
    </div>

    <div class="detail-actions">
      ${actionButtons}
    </div>

    <div class="detail-section">
      <div class="detail-grid">
        <div><span class="label">所属系统:</span> ${esc(req.belongSystem) || '-'}</div>
        <div><span class="label">需求类型:</span> ${esc(req.category || req.reqType) || '-'}</div>
        <div><span class="label">来源部门:</span> ${esc(req.sourceDept) || '-'}</div>
        <div><span class="label">提出人:</span> ${esc(req.proposer) || '-'}</div>
        <div><span class="label">评审负责人:</span> ${esc(req.reviewOwner) || '-'}</div>
        <div><span class="label">开发负责人:</span> ${esc(req.devOwner) || '-'}</div>
        <div><span class="label">计划开始:</span> ${formatDate(req.plannedStartDate)}</div>
        <div><span class="label">计划完成:</span> ${formatDate(req.plannedEndDate || req.devPlannedEnd)}</div>
        <div><span class="label">预估工时:</span> ${esc(req.estimatedHours || '-')} 小时</div>
        <div><span class="label">进度:</span> <div style="display:inline-flex;align-items:center;vertical-align:middle">${progressBar(req.progress || 0)}</div></div>
        <div><span class="label">创建时间:</span> ${formatDateTime(req.createdAt)}</div>
        <div><span class="label">更新时间:</span> ${formatDateTime(req.updatedAt)}</div>
      </div>
    </div>

    <div class="detail-section">
      <div class="section-title">当前问题</div>
      <div class="detail-content">${esc(req.currentProblem) || '-'}</div>
    </div>

    <div class="detail-section">
      <div class="section-title">预期效果</div>
      <div class="detail-content">${esc(req.expectedEffect) || '-'}</div>
    </div>

    <div class="detail-section">
      <div class="section-title">验收标准</div>
      <div class="detail-content">${esc(req.acceptanceCriteria) || '-'}</div>
    </div>

    <div class="detail-section">
      <div class="section-title">备注</div>
      <div class="detail-content">${esc(req.remark) || '-'}</div>
    </div>
  `;
}

function renderActionButtons(status, id) {
  const btnMap = {
    DRAFT: `
      <button class="btn primary" data-action="submit-req" data-id="${esc(id)}">提交需求</button>
      <button class="btn" data-action="edit-req" data-id="${esc(id)}">编辑</button>
      <button class="btn danger" data-action="delete-req" data-id="${esc(id)}">删除</button>
    `,
    PENDING_RECEIVE: `
      <button class="btn primary" data-action="receive-req" data-id="${esc(id)}">接收需求</button>
      <button class="btn danger" data-action="reject-req" data-id="${esc(id)}">驳回</button>
    `,
    PENDING_PLAN: `
      <button class="btn primary" data-action="plan-req" data-id="${esc(id)}">排期分配</button>
    `,
    IN_DEVELOPMENT: `
      <button class="btn primary" data-action="start-testing-req" data-id="${esc(id)}">转测试</button>
      <button class="btn" data-action="update-progress-req" data-id="${esc(id)}">更新进度</button>
    `,
    IN_TESTING: `
      <button class="btn primary" data-action="finish-testing-req" data-id="${esc(id)}">测试完成</button>
    `,
    PENDING_RELEASE: `
      <button class="btn primary" data-action="release-success-req" data-id="${esc(id)}">发布成功</button>
      <button class="btn danger" data-action="release-fail-req" data-id="${esc(id)}">发布失败</button>
    `,
  };
  return btnMap[status] || '';
}

function bindDetailActions(req, drawer) {
  const id = req.id;
  const body = drawer.querySelector('.drawer-body');

  const submitBtn = body.querySelector('[data-action="submit-req"]');
  if (submitBtn) submitBtn.onclick = () => {
    if (confirm('确定要提交这条需求吗？')) {
      api(`/requirements/${id}/submit`, { method: 'POST' })
        .then(() => { drawer.remove(); loadRequirements(currentPage); })
        .catch(e => alert(e.message));
    }
  };

  const editBtn = body.querySelector('[data-action="edit-req"]');
  if (editBtn) editBtn.onclick = () => { drawer.remove(); openReqForm(id); };

  const deleteBtn = body.querySelector('[data-action="delete-req"]');
  if (deleteBtn) deleteBtn.onclick = () => {
    if (confirm('确定要删除这条需求吗？')) {
      api(`/requirements/${id}`, { method: 'DELETE' })
        .then(() => { drawer.remove(); loadRequirements(currentPage); })
        .catch(e => alert(e.message));
    }
  };

  const receiveBtn = body.querySelector('[data-action="receive-req"]');
  if (receiveBtn) receiveBtn.onclick = () => openReceiveDialog(id, drawer);

  const rejectBtn = body.querySelector('[data-action="reject-req"]');
  if (rejectBtn) rejectBtn.onclick = () => openRejectDialog(id, drawer);

  const planBtn = body.querySelector('[data-action="plan-req"]');
  if (planBtn) planBtn.onclick = () => openPlanDialog(id, drawer);

  const startTestingBtn = body.querySelector('[data-action="start-testing-req"]');
  if (startTestingBtn) startTestingBtn.onclick = () => openStartTestingDialog(id, drawer);

  const updateProgressBtn = body.querySelector('[data-action="update-progress-req"]');
  if (updateProgressBtn) updateProgressBtn.onclick = () => openUpdateProgressDialog(id, drawer);

  const finishTestingBtn = body.querySelector('[data-action="finish-testing-req"]');
  if (finishTestingBtn) finishTestingBtn.onclick = () => {
    if (confirm('确定测试已完成，进入待发布状态？')) {
      api(`/requirements/${id}/finish-testing`, { method: 'POST' })
        .then(() => { drawer.remove(); loadRequirements(currentPage); })
        .catch(e => alert(e.message));
    }
  };

  const releaseSuccessBtn = body.querySelector('[data-action="release-success-req"]');
  if (releaseSuccessBtn) releaseSuccessBtn.onclick = () => {
    if (confirm('确定发布成功，需求已完成？')) {
      api(`/requirements/${id}/release`, { method: 'POST', body: { success: true } })
        .then(() => { drawer.remove(); loadRequirements(currentPage); })
        .catch(e => alert(e.message));
    }
  };

  const releaseFailBtn = body.querySelector('[data-action="release-fail-req"]');
  if (releaseFailBtn) releaseFailBtn.onclick = () => {
    if (confirm('确定发布失败，回退到开发中？')) {
      api(`/requirements/${id}/release`, { method: 'POST', body: { success: false } })
        .then(() => { drawer.remove(); loadRequirements(currentPage); })
        .catch(e => alert(e.message));
    }
  };
}

function openReceiveDialog(id, drawer) {
  const bodyHtml = `
    <form class="form">
      <div class="form-row">
        <label>评审负责人 <span class="required">*</span></label>
        <input type="text" name="reviewOwner" required />
      </div>
      <div class="form-row">
        <label>审批模板</label>
        <select name="templateId">
          <option value="">默认模板</option>
        </select>
      </div>
    </form>
  `;
  openModal('接收需求', bodyHtml, async (overlay) => {
    const form = overlay.querySelector('form');
    const reviewOwner = form.reviewOwner.value.trim();
    if (!reviewOwner) { alert('请填写评审负责人'); return; }
    await api(`/requirements/${id}/receive`, {
      method: 'POST',
      body: { reviewOwner, templateId: form.templateId.value || null },
    });
    drawer.remove();
    loadRequirements(currentPage);
  }, '确认接收');
}

function openRejectDialog(id, drawer) {
  const bodyHtml = `
    <form class="form">
      <div class="form-row">
        <label>驳回原因 <span class="required">*</span></label>
        <textarea name="comment" rows="4" required placeholder="请填写驳回原因..."></textarea>
      </div>
    </form>
  `;
  openModal('驳回需求', bodyHtml, async (overlay) => {
    const form = overlay.querySelector('form');
    const comment = form.comment.value.trim();
    if (!comment) { alert('请填写驳回原因'); return; }
    await api(`/requirements/${id}/reject`, { method: 'POST', body: { comment } });
    drawer.remove();
    loadRequirements(currentPage);
  }, '确认驳回');
}

function openPlanDialog(id, drawer) {
  const bodyHtml = `
    <form class="form">
      <div class="form-row">
        <label>开发负责人 <span class="required">*</span></label>
        <input type="text" name="devOwner" required />
      </div>
      <div class="form-row">
        <label>计划开始日期</label>
        <input type="date" name="plannedStartDate" />
      </div>
      <div class="form-row">
        <label>计划结束日期</label>
        <input type="date" name="plannedEndDate" />
      </div>
      <div class="form-row">
        <label>开发计划完成日期</label>
        <input type="date" name="devPlannedEnd" />
      </div>
      <div class="form-row">
        <label>预估工时 (小时)</label>
        <input type="number" name="estimatedHours" min="0" step="0.5" />
      </div>
    </form>
  `;
  openModal('排期分配', bodyHtml, async (overlay) => {
    const form = overlay.querySelector('form');
    const devOwner = form.devOwner.value.trim();
    if (!devOwner) { alert('请填写开发负责人'); return; }
    await api(`/requirements/${id}/plan`, {
      method: 'POST',
      body: {
        devOwner,
        plannedStartDate: form.plannedStartDate.value || null,
        plannedEndDate: form.plannedEndDate.value || null,
        devPlannedEnd: form.devPlannedEnd.value || null,
        estimatedHours: parseFloat(form.estimatedHours.value) || 0,
      },
    });
    drawer.remove();
    loadRequirements(currentPage);
  }, '确认排期');
}

function openStartTestingDialog(id, drawer) {
  const bodyHtml = `
    <form class="form">
      <div class="form-row">
        <label>测试人员 <span class="required">*</span></label>
        <input type="text" name="tester" required />
      </div>
    </form>
  `;
  openModal('转测试', bodyHtml, async (overlay) => {
    const form = overlay.querySelector('form');
    const tester = form.tester.value.trim();
    if (!tester) { alert('请填写测试人员'); return; }
    await api(`/requirements/${id}/start-testing`, { method: 'POST', body: { tester } });
    drawer.remove();
    loadRequirements(currentPage);
  }, '确认转测');
}

function openUpdateProgressDialog(id, drawer) {
  const bodyHtml = `
    <form class="form">
      <div class="form-row">
        <label>当前进度 (%)</label>
        <input type="number" name="progress" min="0" max="100" value="0" />
      </div>
    </form>
  `;
  openModal('更新进度', bodyHtml, async (overlay) => {
    const form = overlay.querySelector('form');
    const progress = parseInt(form.progress.value) || 0;
    await api(`/requirements/${id}`, { method: 'PUT', body: { progress } });
    drawer.remove();
    loadRequirements(currentPage);
  }, '更新');
}

// ========== 任务列表 Tab ==========

function renderTasksTab(tasks, req) {
  return `
    <div class="detail-section">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
        <div class="section-title" style="margin:0">任务列表</div>
        <button class="btn primary small" data-action="add-task">+ 新增任务</button>
      </div>
      ${tasks.length ? `
        <table class="data-table small">
          <thead><tr>
            <th>任务编号</th><th>标题</th>
            <th>状态</th><th>负责人</th>
            <th>进度</th><th>截止日期</th>
            <th>操作</th>
          </tr></thead>
          <tbody>
            ${tasks.map(tk => `
              <tr data-id="${esc(tk.id)}">
                <td>${esc(tk.taskNo || tk.id)}</td>
                <td>${esc(tk.title)}</td>
                <td>${statusPill(tk.status)}</td>
                <td>${esc(tk.assignee || '-')}</td>
                <td>${progressBar(tk.progress || 0)}</td>
                <td>${formatDate(tk.dueDate)}</td>
                <td class="actions">
                  <button class="btn-link" data-action="view-task" data-id="${esc(tk.id)}">查看</button>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      ` : `<div class="empty-state small">暂无任务</div>`}
    </div>
  `;
}

function bindTaskTabActions(req, drawer) {
  const body = drawer.querySelector('.drawer-body');
  const addBtn = body.querySelector('[data-action="add-task"]');
  if (addBtn) addBtn.onclick = () => openAddTaskDialog(req.id, drawer);
}

function openAddTaskDialog(reqId, drawer) {
  const bodyHtml = `
    <form class="form">
      <div class="form-row">
        <label>任务标题 <span class="required">*</span></label>
        <input type="text" name="title" required />
      </div>
      <div class="form-row">
        <label>负责人</label>
        <input type="text" name="assignee" />
      </div>
      <div class="form-row">
        <label>紧急程度</label>
        <select name="priority">
          ${urgencyOptions.map(u => `<option value="${u.value}" ${u.value === 'MEDIUM' ? 'selected' : ''}>${u.label}</option>`).join('')}
        </select>
      </div>
      <div class="form-row">
        <label>截止日期</label>
        <input type="date" name="dueDate" />
      </div>
      <div class="form-row">
        <label>预估工时 (小时)</label>
        <input type="number" name="estimatedHours" min="0" step="0.5" />
      </div>
      <div class="form-row">
        <label>任务描述</label>
        <textarea name="description" rows="3"></textarea>
      </div>
    </form>
  `;
  openModal('新增任务', bodyHtml, async (overlay) => {
    const form = overlay.querySelector('form');
    const title = form.title.value.trim();
    if (!title) { alert('请填写任务标题'); return; }
    await api('/tasks', {
      method: 'POST',
      body: {
        requirementId: reqId,
        title,
        assignee: form.assignee.value.trim(),
        priority: form.priority.value,
        dueDate: form.dueDate.value || null,
        estimatedHours: parseFloat(form.estimatedHours.value) || 0,
        description: form.description.value.trim(),
      },
    });
    // 刷新详情
    drawer.remove();
    openReqDetail(reqId);
    loadRequirements(currentPage);
  }, '创建');
}

// ========== 审批记录 Tab ==========

function renderApprovalTab(records) {
  if (!records || !records.length) {
    return `<div class="detail-section"><div class="empty-state small">暂无审批记录</div></div>`;
  }
  return `
    <div class="detail-section">
      <div class="timeline">
        ${records.map(log => `
          <div class="timeline-item">
            <div class="timeline-dot"></div>
            <div class="timeline-content">
              <div class="timeline-time">${formatDateTime(log.createdAt || log.time)}</div>
              <div class="timeline-text">
                <strong>${esc(log.nodeName || log.action || '审批节点')}</strong>
                ${log.approver ? ` - ${esc(log.approver)}` : ''}
                ${log.action ? ` (${esc(log.action)})` : ''}
              </div>
              ${log.comment ? `<div class="timeline-comment">${esc(log.comment)}</div>` : ''}
            </div>
          </div>
        `).join('')}
      </div>
    </div>
  `;
}

// ========== 变更记录 Tab ==========

function renderChangeTab(logs) {
  if (!logs || !logs.length) {
    return `<div class="detail-section"><div class="empty-state small">暂无变更记录</div></div>`;
  }
  return `
    <div class="detail-section">
      <table class="data-table small">
        <thead><tr>
          <th>字段名</th>
          <th>旧值</th>
          <th>新值</th>
          <th>变更原因</th>
          <th>操作人</th>
          <th>时间</th>
        </tr></thead>
        <tbody>
          ${logs.map(log => `
            <tr>
              <td>${esc(log.fieldName || log.field || '-')}</td>
              <td>${esc(log.oldValue || log.old || '-')}</td>
              <td>${esc(log.newValue || log.new || '-')}</td>
              <td>${esc(log.reason || '-')}</td>
              <td>${esc(log.operator || '-')}</td>
              <td>${formatDateTime(log.createdAt || log.time)}</td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    </div>
  `;
}

// ========== 附件 Tab ==========

function renderAttachmentsTab(req) {
  const attachments = req.attachments || [];
  return `
    <div class="detail-section">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
        <div class="section-title" style="margin:0">附件列表</div>
        <button class="btn primary small" data-action="upload-attachment">+ 上传附件</button>
      </div>
      ${attachments.length ? `
        <table class="data-table small">
          <thead><tr>
            <th>文件名</th>
            <th>大小</th>
            <th>上传人</th>
            <th>上传时间</th>
            <th>操作</th>
          </tr></thead>
          <tbody>
            ${attachments.map(att => `
              <tr>
                <td>${esc(att.fileName || att.name || '-')}</td>
                <td>${esc(att.fileSize || att.size || '-')}</td>
                <td>${esc(att.uploader || '-')}</td>
                <td>${formatDateTime(att.uploadTime || att.createdAt)}</td>
                <td class="actions">
                  <button class="btn-link">下载</button>
                  <button class="btn-link danger">删除</button>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      ` : `<div class="empty-state small">暂无附件</div>`}
    </div>
  `;
}
