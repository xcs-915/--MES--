import { $, $$, esc, formatDate, formatDateTime } from '../utils/dom.js';
import { state } from '../store/index.js';
import { api, apiBlob } from '../api/request.js';
import { openModal, openDrawer, emptyState, loadingState, pagination } from './_ui.js';

// 支持 context 的查询助手
const $q = (sel, ctx) => (ctx || document).querySelector(sel);
const $qa = (sel, ctx) => [...(ctx || document).querySelectorAll(sel)];

// ==================== 常量配置 ====================

let STATUS_LIST = [
  { value: 'PENDING_RECEIVE', label: '待接收' },
  { value: 'PENDING_DOCUMENT', label: '待完善文档' },
  { value: 'PENDING_APPROVAL', label: '待审批' },
  { value: 'APPROVED', label: '已审批待排期' },
  { value: 'IN_DEVELOPMENT', label: '开发中' },
  { value: 'IN_TESTING', label: '测试中' },
  { value: 'PENDING_RELEASE', label: '待发布' },
  { value: 'COMPLETED', label: '已完成' },
];

const STATUS_MAP = {
  DRAFT: { label: '草稿', cls: 'badge-status-pending' },
  PENDING_RECEIVE: { label: '待接收', cls: 'badge-status-pending' },
  PENDING_DOCUMENT: { label: '待完善文档', cls: 'badge-status-backlog' },
  PENDING_APPROVAL: { label: '待审批', cls: 'badge-status-backlog' },
  APPROVED: { label: '已审批待排期', cls: 'badge-status-ready' },
  IN_DEVELOPMENT: { label: '开发中', cls: 'badge-status-developing' },
  IN_TESTING: { label: '测试中', cls: 'badge-status-testing' },
  PENDING_RELEASE: { label: '待发布', cls: 'badge-status-ready' },
  COMPLETED: { label: '已完成', cls: 'badge-status-done' },
  REJECTED: { label: '已驳回', cls: 'badge-status-rejected' },
};

const PRIORITY_MAP = {
  URGENT: { label: '紧急', cls: 'tag-priority-urgent' },
  HIGH: { label: '高', cls: 'tag-priority-high' },
  MEDIUM: { label: '中', cls: 'tag-priority-medium' },
  LOW: { label: '低', cls: 'tag-priority-low' },
};

const SYSTEM_OPTIONS = ['MES', 'WMS', 'SAP', 'OA', 'SCADA'];
const TYPE_OPTIONS = [
  { value: 'NEW_FUNC', label: '新功能' },
  { value: 'FUNC_OPT', label: '功能优化' },
  { value: 'BUG', label: 'BUG' },
  { value: 'INTERFACE', label: '接口' },
  { value: 'REPORT', label: '报表' },
  { value: 'OTHER', label: '其他' },
];
const DEPT_OPTIONS = ['生产部', '质量部', '设备部', '仓储部', 'IT部', '采购部', '销售部', '财务部', '人事部'];
const PAGE_SIZE = 20;

// ==================== 本地 UI 渲染函数（使用 app.css 类名） ====================

function statusBadge(status) {
  const m = STATUS_MAP[status];
  if (!m) return '<span class="badge badge-status-pending"><span class="badge-dot"></span>—</span>';
  return `<span class="badge ${m.cls}"><span class="badge-dot"></span>${m.label}</span>`;
}

function priorityTag(priority) {
  const m = PRIORITY_MAP[priority];
  if (!m) return '<span class="tag tag-priority-medium">—</span>';
  return `<span class="tag ${m.cls}">${m.label}</span>`;
}

function progressRender(value) {
  const p = Math.max(0, Math.min(100, value || 0));
  let barCls = '';
  if (p >= 100) barCls = 'success';
  else if (p < 30) barCls = 'warning';
  return `<div style="display:flex;align-items:center;min-width:110px;gap:8px">
    <div class="progress" style="flex:1"><div class="progress-bar ${barCls}" style="width:${p}%"></div></div>
    <span style="font-size:12px;color:var(--it-color-text-tertiary);min-width:32px">${p}%</span>
  </div>`;
}

// ==================== 状态管理 ====================

let currentPage = 0;
let currentView = 'list';
let currentFilters = {
  keyword: '',
  statusList: [],
  belongSystem: '',
  category: '',
  urgency: '',
  sourceDept: '',
};
let totalCount = 0;
let currentItems = [];
let selectedIds = new Set();
let currentDetailId = null;
let currentDrawer = null;

// ==================== 页面主渲染 ====================

export async function renderRequirements() {
  try {
    const statusRes = await api('/workflow-statuses', { silent: true });
    const configured = (statusRes?.data || []).filter(s => s.status === 'ACTIVE');
    if (configured.length) {
      STATUS_LIST = configured.map(s => ({ value: s.code, label: s.name }));
      configured.forEach(s => { if (!STATUS_MAP[s.code]) STATUS_MAP[s.code] = { label: s.name, cls: 'badge-status-backlog' }; });
    }
  } catch { /* use built-in status catalogue */ }
  const dashboardStatus = sessionStorage.getItem('itrm_dashboard_status');
  if (dashboardStatus) {
    currentFilters.statusList = [dashboardStatus];
    sessionStorage.removeItem('itrm_dashboard_status');
  }
  const page = $('#page');
  page.innerHTML = `
    <div class="page-container">
      <!-- 页面头部 -->
      <div class="page-header">
        <div>
          <div class="page-title">需求池</div>
          <div class="page-subtitle">统一管理各部门提出的 IT 需求，跟踪从提出到落地的全生命周期</div>
        </div>
        <div class="page-actions">
          <div class="view-toggle" id="view-toggle">
            <button class="view-toggle-btn active" data-view="list" title="列表视图">
              <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="8" y1="6" x2="21" y2="6"></line><line x1="8" y1="12" x2="21" y2="12"></line><line x1="8" y1="18" x2="21" y2="18"></line><line x1="3" y1="6" x2="3.01" y2="6"></line><line x1="3" y1="12" x2="3.01" y2="12"></line><line x1="3" y1="18" x2="3.01" y2="18"></line></svg>
            </button>
            <button class="view-toggle-btn" data-view="kanban" title="看板视图">
              <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="18"></rect><rect x="14" y="3" width="7" height="12"></rect></svg>
            </button>
          </div>
          <button class="btn btn-primary" data-action="new-req" data-permission="requirement:create">
            <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
            新建需求
          </button>
        </div>
      </div>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <div class="filter-row">
          <div class="search-bar">
            <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
            <input type="text" id="f-keyword" placeholder="搜索需求标题、编号..." />
          </div>
          <select class="form-select" id="f-system" style="width:140px">
            <option value="">全部系统</option>
            ${SYSTEM_OPTIONS.map(s => `<option value="${s}">${s}</option>`).join('')}
          </select>
          <select class="form-select" id="f-type" style="width:140px">
            <option value="">全部类型</option>
            ${TYPE_OPTIONS.map(t => `<option value="${t.value}">${t.label}</option>`).join('')}
          </select>
          <select class="form-select" id="f-urgency" style="width:120px">
            <option value="">全部紧急度</option>
            ${Object.entries(PRIORITY_MAP).map(([v, m]) => `<option value="${v}">${m.label}</option>`).join('')}
          </select>
          <select class="form-select" id="f-dept" style="width:140px">
            <option value="">全部部门</option>
            ${DEPT_OPTIONS.map(d => `<option value="${d}">${d}</option>`).join('')}
          </select>
          <div class="filter-actions">
            <button class="btn btn-secondary" data-action="reset-filter">重置</button>
          </div>
        </div>
        <div class="filter-row">
          <span class="filter-label">状态</span>
          <div class="tag-filter" id="status-filter">
            <span class="tag-filter-item ${currentFilters.statusList.length ? '' : 'active'}" data-status="">全部</span>
            ${STATUS_LIST.map(s => `<span class="tag-filter-item ${currentFilters.statusList.includes(s.value) ? 'active' : ''}" data-status="${s.value}">${s.label}</span>`).join('')}
          </div>
        </div>
      </div>

      <!-- 工具栏 -->
      <div class="page-toolbar">
        <div class="toolbar-left">
          <span style="font-size:13px;color:var(--it-color-text-secondary)">共 <strong id="total-count" style="color:var(--it-color-text-primary)">0</strong> 条</span>
          <span id="selected-info" style="font-size:12px;color:var(--it-color-text-tertiary);display:none">已选 <strong id="selected-count">0</strong> 项</span>
        </div>
        <div class="toolbar-right">
          <button class="btn btn-secondary btn-sm" data-action="batch-export" disabled>
            <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
            导出
          </button>
          <button class="btn btn-secondary btn-sm" data-action="batch-assign" data-permission="requirement:plan" disabled>批量指派</button>
          <button class="btn btn-secondary btn-sm" data-action="refresh">
            <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 4 23 10 17 10"></polyline><polyline points="1 20 1 14 7 14"></polyline><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path></svg>
          </button>
        </div>
      </div>

      <!-- 数据表格 -->
      <div class="table-wrapper" id="req-table">
        ${loadingState()}
      </div>

      <!-- 分页 -->
      <div style="display:flex;justify-content:flex-end;margin-top:16px">
        <div id="req-pagination"></div>
      </div>
    </div>
  `;

  bindPageEvents();
  await loadRequirements(0);
  const directId = sessionStorage.getItem('itrm_current_req_id');
  if (directId) {
    sessionStorage.removeItem('itrm_current_req_id');
    openReqDetail(directId);
  }
}

// ==================== 数据加载 ====================

async function loadRequirements(pageNum) {
  currentPage = pageNum ?? 0;
  const tableWrap = $('#req-table');
  const pagWrap = $('#req-pagination');
  tableWrap.innerHTML = loadingState();
  selectedIds.clear();
  updateSelectedInfo();

  const params = new URLSearchParams({
    page: currentPage,
    size: PAGE_SIZE,
  });

  if (currentFilters.keyword) params.set('keyword', currentFilters.keyword);
  if (currentFilters.belongSystem) params.set('belongSystem', currentFilters.belongSystem);
  if (currentFilters.category) params.set('category', currentFilters.category);
  if (currentFilters.urgency) params.set('urgency', currentFilters.urgency);
  if (currentFilters.sourceDept) params.set('sourceDept', currentFilters.sourceDept);
  if (currentFilters.statusList.length) {
    currentFilters.statusList.forEach(s => params.append('status', s));
  }

  try {
    const res = await api(`/requirements?${params.toString()}`);
    const data = res?.data || res || {};
    const items = data.content || data.items || [];
    const total = data.totalElements ?? data.total ?? 0;
    const totalPages = data.totalPages ?? Math.ceil(total / PAGE_SIZE);

    currentItems = items;
    totalCount = total;

    $('#total-count').textContent = total;

    if (!items.length) {
      tableWrap.innerHTML = emptyState('暂无需求数据');
      pagWrap.innerHTML = '';
      return;
    }

    // 根据视图渲染
    if (currentView === 'list') {
      tableWrap.innerHTML = renderTable(items);
    } else {
      tableWrap.innerHTML = renderKanban(items);
    }

    pagWrap.innerHTML = pagination({ page: currentPage, total, totalPages }, 'req');
    bindPagination();
    bindTableEvents();
  } catch (e) {
    tableWrap.innerHTML = emptyState(e.message || '加载失败');
    pagWrap.innerHTML = '';
  }
}

// ==================== 表格渲染 ====================

function renderTable(items) {
  return `<table class="data-table">
    <thead>
      <tr>
        <th style="width:40px;padding-left:16px">
          <input type="checkbox" id="check-all" style="cursor:pointer" />
        </th>
        <th style="width:120px">需求编号</th>
        <th>需求标题</th>
        <th style="width:90px">所属系统</th>
        <th style="width:90px">需求类型</th>
        <th style="width:80px">紧急程度</th>
        <th style="width:90px">状态</th>
        <th style="width:90px">来源部门</th>
        <th style="width:80px">提出人</th>
        <th style="width:90px">评审负责人</th>
        <th style="width:90px">开发负责人</th>
        <th style="width:140px">进度</th>
        <th style="width:110px">创建时间</th>
        <th style="width:280px;text-align:right;padding-right:16px">流程操作</th>
      </tr>
    </thead>
    <tbody>
      ${items.map(item => renderTableRow(item)).join('')}
    </tbody>
  </table>`;
}

function renderTableRow(item) {
  const id = esc(item.id);
  const idStr = String(item.id);
  const reqNo = esc(item.reqNo || item.id);
  const checked = selectedIds.has(idStr) ? 'checked' : '';

  return `
    <tr data-id="${id}" class="${checked ? 'row-selected' : ''}">
      <td style="padding-left:16px">
        <input type="checkbox" class="row-check" data-id="${id}" ${checked} style="cursor:pointer" />
      </td>
      <td><span class="table-id">${reqNo}</span></td>
      <td>
        <div class="table-link" data-action="view-req" data-id="${id}" title="${esc(item.title)}">
          ${esc(item.title)}
        </div>
      </td>
      <td>${esc(item.belongSystem || '—')}</td>
      <td>${renderTypeTag(item.category || item.reqType)}</td>
      <td>${priorityTag(item.urgency || item.priority)}</td>
      <td>${statusBadge(item.status)}</td>
      <td>${esc(item.sourceDept || '—')}</td>
      <td>${esc(item.proposer || '—')}</td>
      <td>${esc(item.reviewOwner || '—')}</td>
      <td>${esc(item.devOwner || '—')}</td>
      <td>${progressRender(item.progress || 0)}</td>
      <td style="color:var(--it-color-text-tertiary);font-size:12px">${formatDate(item.createdAt)}</td>
      <td style="text-align:right;padding-right:16px">
        <div style="display:flex;align-items:center;justify-content:flex-end;gap:4px">
          <button class="btn btn-ghost btn-sm" data-action="view-req" data-id="${id}">详情</button>
          ${renderListActions(item)}
        </div>
      </td>
    </tr>
  `;
}

function renderTypeTag(type) {
  if (!type) return '—';
  const typeMap = {
    NEW_FUNC: ['新功能', 'tag-type-feature'],
    FUNC_OPT: ['功能优化', 'tag-type-improvement'],
    BUG: ['BUG', 'tag-type-bug'],
    INTERFACE: ['接口', 'tag-type-task'],
    REPORT: ['报表', 'tag-type-task'],
    OTHER: ['其他', 'tag-type-task'],
    '新功能': ['新功能', 'tag-type-feature'],
    '功能优化': ['功能优化', 'tag-type-improvement'],
  };
  const [label, cls] = typeMap[type] || [type, 'tag-type-task'];
  return `<span class="tag ${cls}">${esc(label)}</span>`;
}

function canEdit(status) {
  return false;
}

function actionButton(action, id, label, primary = false, permission = '') {
  return `<button class="btn ${primary ? 'btn-primary' : 'btn-secondary'} btn-sm" data-action="${action}" data-id="${esc(id)}" ${permission ? `data-permission="${permission}"` : ''}>${label}</button>`;
}

function renderListActions(item) {
  const id = item.id;
  switch (item.status) {
    case 'DRAFT': return actionButton('submit-req', id, '提交', true, 'requirement:submit') + actionButton('change-req', id, '变更', false, 'requirement:change');
    case 'PENDING_RECEIVE': return actionButton('receive-req', id, '接收', true, 'requirement:receive') + actionButton('reject-req', id, '驳回', false, 'requirement:review');
    case 'PENDING_DOCUMENT': return actionButton('document-req', id, '完善文档', true, 'requirement:review') + actionButton('submit-approval-req', id, '提交审批', false, 'requirement:review') + actionButton('change-req', id, '变更', false, 'requirement:change');
    case 'PENDING_APPROVAL': return actionButton('approve-req', id, '审批', true, 'requirement:review');
    case 'APPROVED': return actionButton('plan-req', id, '排期分配', true, 'requirement:plan') + actionButton('change-req', id, '变更', false, 'requirement:change');
    case 'IN_DEVELOPMENT': return actionButton('start-testing-req', id, '转测试', true, 'test:transition') + actionButton('change-req', id, '变更', false, 'requirement:change');
    case 'IN_TESTING': return actionButton('finish-testing-req', id, '测试通过', true, 'test:transition') + actionButton('change-req', id, '变更', false, 'requirement:change');
    case 'PENDING_RELEASE': return actionButton('release-success-req', id, '确认发布', true, 'requirement:release') + actionButton('change-req', id, '变更', false, 'requirement:change');
    case 'REJECTED': return actionButton('change-req', id, '变更后重提', true, 'requirement:change');
    default: return '';
  }
}

// ==================== 看板视图（简化版） ====================

function renderKanban(items) {
  const columns = STATUS_LIST;
  return `<div style="display:flex;gap:16px;overflow-x:auto;padding:16px">
    ${columns.map(col => {
      const colItems = items.filter(it => it.status === col.value);
      return `
        <div style="flex:0 0 280px;min-width:280px;background:var(--it-color-bg-page);border-radius:var(--it-radius-lg);border:1px solid var(--it-color-border-light);display:flex;flex-direction:column">
          <div style="padding:12px 16px;border-bottom:1px solid var(--it-color-border-light);display:flex;align-items:center;justify-content:space-between">
            <div style="display:flex;align-items:center;gap:8px">
              ${statusBadge(col.value)}
              <span style="font-size:12px;color:var(--it-color-text-tertiary)">${colItems.length}</span>
            </div>
          </div>
          <div style="padding:12px;display:flex;flex-direction:column;gap:10px;flex:1;min-height:400px">
            ${colItems.length ? colItems.map(item => `
              <div data-id="${esc(item.id)}" style="background:#fff;border:1px solid var(--it-color-border-light);border-radius:var(--it-radius-md);padding:12px;cursor:pointer;transition:all var(--it-transition-fast)" class="kanban-card" data-action="view-req">
                <div style="font-size:12px;color:var(--it-color-text-tertiary);margin-bottom:6px">${esc(item.reqNo || item.id)}</div>
                <div style="font-size:13px;font-weight:500;color:var(--it-color-text-primary);margin-bottom:8px;line-height:1.5;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden">${esc(item.title)}</div>
                <div style="display:flex;align-items:center;justify-content:space-between">
                  ${priorityTag(item.urgency || item.priority)}
                  <span style="font-size:12px;color:var(--it-color-text-tertiary)">${esc(item.devOwner || '—')}</span>
                </div>
              </div>
            `).join('') : `<div style="text-align:center;padding:40px 20px;color:var(--it-color-text-quaternary);font-size:12px">暂无需求</div>`}
          </div>
        </div>
      `;
    }).join('')}
  </div>`;
}

// ==================== 事件绑定 ====================

function bindPageEvents() {
  const container = $('.page-container');

  // 搜索框回车搜索
  const kwInput = $('#f-keyword');
  if (kwInput) {
    kwInput.addEventListener('keydown', e => {
      if (e.key === 'Enter') applyFilters();
    });
  }

  // 下拉框变更立即搜索
  ['f-system', 'f-type', 'f-urgency', 'f-dept'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.addEventListener('change', applyFilters);
  });

  // 状态标签筛选（支持多选 + 全部）
  const statusFilter = $('#status-filter');
  if (statusFilter) {
    statusFilter.addEventListener('click', e => {
      const item = e.target.closest('.tag-filter-item');
      if (!item) return;
      const status = item.dataset.status;

      if (status === '') {
        // 点击"全部"
        $qa('.tag-filter-item', statusFilter).forEach(i => i.classList.remove('active'));
        item.classList.add('active');
        currentFilters.statusList = [];
      } else {
        // 取消"全部"选中
        const allItem = $q('[data-status=""]', statusFilter);
        if (allItem) allItem.classList.remove('active');

        if (item.classList.contains('active')) {
          item.classList.remove('active');
          currentFilters.statusList = currentFilters.statusList.filter(s => s !== status);
        } else {
          item.classList.add('active');
          currentFilters.statusList.push(status);
        }

        // 如果都没选中，默认选"全部"
        if (currentFilters.statusList.length === 0) {
          if (allItem) allItem.classList.add('active');
        }
      }
      loadRequirements(0);
    });
  }

  // 按钮操作
  container.addEventListener('click', e => {
    const btn = e.target.closest('[data-action]');
    if (!btn) return;
    const action = btn.dataset.action;
    const id = btn.dataset.id;

    switch (action) {
      case 'new-req':
        openReqForm();
        break;
      case 'view-req':
        openReqDetail(id);
        break;
      case 'submit-req':
      case 'receive-req':
      case 'reject-req':
      case 'document-req':
      case 'submit-approval-req':
      case 'approve-req':
      case 'change-req':
      case 'plan-req':
      case 'start-testing-req':
      case 'finish-testing-req':
      case 'release-success-req':
        handleListAction(action, id);
        break;
      case 'more-req':
        e.stopPropagation();
        toggleMoreMenu(btn, id);
        break;
      case 'reset-filter':
        resetFilters();
        break;
      case 'refresh':
        loadRequirements(currentPage);
        break;
      case 'batch-export':
        alert('批量导出功能开发中');
        break;
      case 'batch-assign':
        alert('批量指派功能开发中');
        break;
    }
  });

  // 视图切换
  const viewToggle = $('#view-toggle');
  if (viewToggle) {
    viewToggle.addEventListener('click', e => {
      const btn = e.target.closest('.view-toggle-btn');
      if (!btn) return;
      const view = btn.dataset.view;
      if (view === currentView) return;
      currentView = view;
      $qa('.view-toggle-btn', viewToggle).forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      loadRequirements(currentPage);
    });
  }
}

function handleListAction(action, id) {
  const listContext = { remove() {} };
  const refresh = () => loadRequirements(currentPage);
  if (action === 'submit-req') {
    if (confirm('确定提交需求到 IT 待接收队列？')) api(`/requirements/${id}/submit`, { method: 'POST' }).then(refresh).catch(e => alert(e.message));
  } else if (action === 'receive-req') openReceiveDialog(id, listContext);
  else if (action === 'reject-req') openRejectDialog(id, listContext);
  else if (action === 'document-req') openDocumentDialog(id, listContext);
  else if (action === 'submit-approval-req') {
    if (confirm('确定需求文档已完善，提交给上级审批？')) api(`/requirements/${id}/submit-approval`, { method: 'POST' }).then(refresh).catch(e => alert(e.message));
  } else if (action === 'approve-req') openApprovalDialog(id, listContext);
  else if (action === 'change-req') openChangeDialog(id, listContext);
  else if (action === 'plan-req') openPlanDialog(id, listContext);
  else if (action === 'start-testing-req') openStartTestingDialog(id, listContext);
  else if (action === 'finish-testing-req') {
    if (confirm('确定测试验收通过，进入待发布？')) api(`/requirements/${id}/finish-testing`, { method: 'POST' }).then(refresh).catch(e => alert(e.message));
  } else if (action === 'release-success-req') {
    if (confirm('确定已发布上线，完成该需求？')) api(`/requirements/${id}/release`, { method: 'POST', body: { success: true } }).then(refresh).catch(e => alert(e.message));
  }
}

function bindTableEvents() {
  const tableWrap = $('#req-table');

  // 全选
  const checkAll = $('#check-all');
  if (checkAll) {
    checkAll.addEventListener('change', () => {
      const checks = $qa('.row-check', tableWrap);
      if (checkAll.checked) {
        checks.forEach(c => {
          c.checked = true;
          selectedIds.add(c.dataset.id);
          c.closest('tr').classList.add('row-selected');
        });
      } else {
        checks.forEach(c => {
          c.checked = false;
          selectedIds.delete(c.dataset.id);
          c.closest('tr').classList.remove('row-selected');
        });
      }
      updateSelectedInfo();
    });
  }

  // 行勾选
  $qa('.row-check', tableWrap).forEach(cb => {
    cb.addEventListener('change', () => {
      const id = cb.dataset.id;
      const tr = cb.closest('tr');
      if (cb.checked) {
        selectedIds.add(id);
        tr.classList.add('row-selected');
      } else {
        selectedIds.delete(id);
        tr.classList.remove('row-selected');
      }
      updateSelectedInfo();
      // 更新全选状态
      const allChecks = $qa('.row-check', tableWrap);
      if (checkAll) checkAll.checked = allChecks.length > 0 && allChecks.every(c => c.checked);
    });
  });

  // 点击行查看详情（除了勾选框和操作按钮）
  $$('#req-table tbody tr').forEach(tr => {
    tr.addEventListener('click', e => {
      if (e.target.closest('.row-check') || e.target.closest('button') || e.target.closest('.dropdown')) return;
      const id = tr.dataset.id;
      if (id) openReqDetail(id);
    });
  });

  // 看板卡片点击
  $qa('.kanban-card', tableWrap).forEach(card => {
    card.addEventListener('click', () => {
      const id = card.dataset.id;
      if (id) openReqDetail(id);
    });
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

function updateSelectedInfo() {
  const info = $('#selected-info');
  const count = $('#selected-count');
  const batchBtns = $$('[data-action^="batch-"]');
  if (!info || !count) return;

  const n = selectedIds.size;
  if (n > 0) {
    info.style.display = 'inline';
    count.textContent = n;
    batchBtns.forEach(b => b.disabled = false);
  } else {
    info.style.display = 'none';
    batchBtns.forEach(b => b.disabled = true);
  }
}

function applyFilters() {
  currentFilters.keyword = $('#f-keyword').value.trim();
  currentFilters.belongSystem = $('#f-system').value;
  currentFilters.category = $('#f-type').value;
  currentFilters.urgency = $('#f-urgency').value;
  currentFilters.sourceDept = $('#f-dept').value;
  loadRequirements(0);
}

function resetFilters() {
  currentFilters = {
    keyword: '',
    statusList: [],
    belongSystem: '',
    category: '',
    urgency: '',
    sourceDept: '',
  };
  $('#f-keyword').value = '';
  $('#f-system').value = '';
  $('#f-type').value = '';
  $('#f-urgency').value = '';
  $('#f-dept').value = '';
  // 重置状态标签
  const statusFilter = $('#status-filter');
  if (statusFilter) {
    $qa('.tag-filter-item', statusFilter).forEach(i => i.classList.remove('active'));
    const allItem = $q('[data-status=""]', statusFilter);
    if (allItem) allItem.classList.add('active');
  }
  loadRequirements(0);
}

function toggleMoreMenu(btn, id) {
  // 关闭已有菜单
  document.querySelectorAll('.more-menu').forEach(m => m.remove());

  const menu = document.createElement('div');
  menu.className = 'dropdown-menu more-menu';
  menu.style.cssText = 'position:fixed;z-index:1000';
  menu.innerHTML = `
    <div class="dropdown-item" data-action="view-req" data-id="${id}">查看详情</div>
    ${canEdit(currentItems.find(i => String(i.id) === String(id))?.status) ? `<div class="dropdown-item" data-action="edit-req" data-id="${id}" data-permission="requirement:edit">编辑</div>` : ''}
    <div class="dropdown-item" data-action="copy-req" data-id="${id}">复制需求</div>
    <div class="dropdown-divider"></div>
    <div class="dropdown-item danger" data-action="delete-req" data-id="${id}" data-permission="requirement:delete">删除</div>
  `;

  const rect = btn.getBoundingClientRect();
  document.body.appendChild(menu);
  menu.style.top = `${rect.bottom + 4}px`;
  menu.style.left = `${rect.right - menu.offsetWidth}px`;

  menu.addEventListener('click', e => {
    const item = e.target.closest('.dropdown-item');
    if (!item) return;
    const action = item.dataset.action;
    const itemId = item.dataset.id;
    menu.remove();

    switch (action) {
      case 'view-req':
        openReqDetail(itemId);
        break;
      case 'edit-req':
        openReqForm(itemId);
        break;
      case 'copy-req':
        copyReq(itemId);
        break;
      case 'delete-req':
        if (confirm('确定要删除这条需求吗？')) deleteReq(itemId);
        break;
    }
  });

  // 点击外部关闭
  setTimeout(() => {
    const closeHandler = (e) => {
      if (!menu.contains(e.target)) {
        menu.remove();
        document.removeEventListener('click', closeHandler);
      }
    };
    document.addEventListener('click', closeHandler);
  }, 0);
}

// ==================== 新建/编辑需求表单 ====================

function openReqForm(id) {
  const isEdit = !!id;
  const title = isEdit ? '编辑需求' : '新建需求';

  const bodyHtml = `
    <form>
      <div class="form-item">
        <label class="form-label required">需求标题</label>
        <input type="text" class="form-input" name="title" placeholder="请输入需求标题" required />
      </div>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">
        <div class="form-item">
          <label class="form-label required">所属系统</label>
          <select class="form-select" name="belongSystem" required>
            <option value="">请选择</option>
            ${SYSTEM_OPTIONS.map(s => `<option value="${s}">${s}</option>`).join('')}
          </select>
        </div>
        <div class="form-item">
          <label class="form-label required">需求类型</label>
          <select class="form-select" name="category" required>
            <option value="">请选择</option>
            ${TYPE_OPTIONS.map(t => `<option value="${t.value}">${t.label}</option>`).join('')}
          </select>
        </div>
      </div>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">
        <div class="form-item">
          <label class="form-label">紧急程度</label>
          <select class="form-select" name="urgency">
            ${Object.entries(PRIORITY_MAP).map(([v, m]) => `<option value="${v}" ${v === 'MEDIUM' ? 'selected' : ''}>${m.label}</option>`).join('')}
          </select>
        </div>
        <div class="form-item">
          <label class="form-label">来源部门</label>
          <select class="form-select" name="sourceDept">
            <option value="">请选择</option>
            ${DEPT_OPTIONS.map(d => `<option value="${d}">${d}</option>`).join('')}
          </select>
        </div>
      </div>
      <div class="form-item">
        <label class="form-label">提出人</label>
        <input type="text" class="form-input" name="proposer" placeholder="请输入提出人姓名" />
      </div>
      <div class="form-item">
        <label class="form-label">当前问题</label>
        <textarea class="form-textarea" name="currentProblem" rows="3" placeholder="请描述当前遇到的问题或现状..."></textarea>
      </div>
      <div class="form-item">
        <label class="form-label">预期效果</label>
        <textarea class="form-textarea" name="expectedEffect" rows="3" placeholder="请描述期望达到的效果或目标..."></textarea>
      </div>
      <div class="form-item">
        <label class="form-label">验收标准</label>
        <textarea class="form-textarea" name="acceptanceCriteria" rows="3" placeholder="请描述验收标准或成功条件..."></textarea>
      </div>
      <div class="form-item">
        <label class="form-label">备注</label>
        <textarea class="form-textarea" name="remark" rows="2" placeholder="其他补充说明..."></textarea>
      </div>
      <div class="form-item">
        <label class="form-label">需求截图与附件</label>
        <label class="attachment-picker">
          <input type="file" name="files" multiple accept="image/*,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.zip,.rar" />
          <span class="attachment-picker-icon">↑</span>
          <span><strong>选择图片或附件</strong><small>支持多选，单个文件最大 20MB</small></span>
        </label>
        <div class="attachment-selection" data-file-selection></div>
      </div>
    </form>
  `;

  const modal = openModal(title, bodyHtml, async (overlay) => {
    const form = overlay.querySelector('form');
    const titleVal = form.title.value.trim();
    const systemVal = form.belongSystem.value;
    const categoryVal = form.category.value;
    if (!titleVal || !systemVal || !categoryVal) {
      alert('请填写必填项');
      return Promise.reject(new Error('必填项未填写'));
    }

    const body = {
      title: titleVal,
      belongSystem: systemVal,
      category: categoryVal,
      urgency: form.urgency.value || 'MEDIUM',
      sourceDept: form.sourceDept.value,
      proposer: form.proposer.value.trim(),
      currentProblem: form.currentProblem.value.trim(),
      expectedEffect: form.expectedEffect.value.trim(),
      acceptanceCriteria: form.acceptanceCriteria.value.trim(),
      remark: form.remark.value.trim(),
    };

    const saved = isEdit
      ? await api(`/requirements/${id}`, { method: 'PUT', body })
      : await api('/requirements', { method: 'POST', body });
    const requirementId = id || saved?.data?.id;
    const files = [...(form.files.files || [])];
    if (requirementId && files.length) await uploadRequirementFiles(requirementId, files);
    loadRequirements(currentPage);
  }, '保存');

  const fileInput = modal.querySelector('input[name="files"]');
  fileInput?.addEventListener('change', () => renderSelectedFiles(modal, [...fileInput.files]));

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

function copyReq(id) {
  api(`/requirements/${id}`).then(res => {
    const d = res?.data || res;
    if (!d) return;
    openReqForm();
    // 延迟填充表单
    setTimeout(() => {
      const modal = document.querySelector('.modal-overlay:last-of-type');
      if (!modal) return;
      const form = modal.querySelector('form');
      if (!form) return;
      form.title.value = (d.title || '') + ' - 副本';
      form.belongSystem.value = d.belongSystem || '';
      form.category.value = d.category || d.reqType || '';
      form.urgency.value = d.urgency || d.priority || 'MEDIUM';
      form.sourceDept.value = d.sourceDept || '';
      form.proposer.value = d.proposer || '';
      form.currentProblem.value = d.currentProblem || '';
      form.expectedEffect.value = d.expectedEffect || '';
      form.acceptanceCriteria.value = d.acceptanceCriteria || '';
      form.remark.value = d.remark || '';
    }, 100);
  }).catch(e => alert(e.message));
}

// ==================== 详情抽屉 ====================

async function openReqDetail(id) {
  currentDetailId = id;
  const page = $('#page');
  page.innerHTML = `<div class="page-container requirement-detail-page">
    <div class="detail-page-nav"><button class="btn btn-ghost" data-back-requirements>← 返回需求池</button><span>需求详情</span></div>
    <div class="requirement-detail-body">${loadingState()}</div>
  </div>`;
  const drawer = page.querySelector('.requirement-detail-page');
  currentDrawer = drawer;
  const body = drawer.querySelector('.requirement-detail-body');
  drawer.querySelector('[data-back-requirements]').onclick = () => renderRequirements();

  try {
    const [reqRes, approvalRes, changeRes, tasksRes, attachmentsRes, testsRes] = await Promise.all([
      api(`/requirements/${id}`),
      api(`/requirements/${id}/approval-records`).catch(() => ({ data: [] })),
      api(`/requirements/${id}/change-logs`).catch(() => ({ data: [] })),
      api(`/tasks/by-requirement/${id}`).catch(() => ({ data: [] })),
      api(`/requirements/${id}/attachments`).catch(() => ({ data: [] })),
      api(`/test-records/by-requirement/${id}`).catch(() => ({ data: [] })),
    ]);

    const req = reqRes?.data || reqRes || {};
    const approvalRecords = approvalRes?.data || approvalRes || [];
    const changeLogs = changeRes?.data || changeRes || [];
    const tasks = tasksRes?.data || tasksRes || [];
    const attachments = attachmentsRes?.data || attachmentsRes || [];
    const tests = testsRes?.data || testsRes || [];

    // 替换 drawer 标题为需求编号 + 标题
    body.innerHTML = `
      <!-- 头部信息 -->
      <div style="margin-bottom:20px">
        <div style="display:flex;align-items:center;gap:10px;margin-bottom:10px">
          ${statusBadge(req.status)}
          ${priorityTag(req.urgency || req.priority)}
        </div>
        <div style="font-size:18px;font-weight:700;color:var(--it-color-text-primary);line-height:1.5;margin-bottom:8px">${esc(req.title)}</div>
        <div style="font-size:12px;color:var(--it-color-text-tertiary);display:flex;gap:16px;flex-wrap:wrap">
          <span>编号：${esc(req.reqNo || req.id)}</span>
          <span>创建：${formatDateTime(req.createdAt)}</span>
          <span>更新：${formatDateTime(req.updatedAt)}</span>
        </div>
      </div>

      <!-- Tabs -->
      <div class="tabs" id="detail-tabs">
        <div class="tab-item active" data-tab="basic">需求概览</div>
        <div class="tab-item" data-tab="document">需求文档</div>
        <div class="tab-item" data-tab="tasks">开发任务 <span class="tab-count">${tasks.length}</span></div>
        <div class="tab-item" data-tab="testing">测试与缺陷 <span class="tab-count">${tests.length}</span></div>
        <div class="tab-item" data-tab="approval">审批记录</div>
        <div class="tab-item" data-tab="change">变更记录</div>
        <div class="tab-item" data-tab="attachments">附件</div>
      </div>

      <!-- Tab 内容 -->
      <div class="tab-panels">
        <div class="tab-panel active" data-tab-panel="basic">
          ${renderBasicInfo(req)}
        </div>
        <div class="tab-panel" data-tab-panel="document" style="display:none">
          ${renderRequirementDocument(req)}
        </div>
        <div class="tab-panel" data-tab-panel="tasks" style="display:none">
          ${renderTasksTab(tasks, req)}
        </div>
        <div class="tab-panel" data-tab-panel="testing" style="display:none">
          ${renderTestingTab(tests, tasks, req)}
        </div>
        <div class="tab-panel" data-tab-panel="approval" style="display:none">
          ${renderApprovalTab(approvalRecords)}
        </div>
        <div class="tab-panel" data-tab-panel="change" style="display:none">
          ${renderChangeTab(changeLogs)}
        </div>
        <div class="tab-panel" data-tab-panel="attachments" style="display:none">
          ${renderAttachmentsTab(attachments, req.id)}
        </div>
      </div>
    `;

    // Tab 切换
    const tabs = $q('#detail-tabs', body);
    if (tabs) {
      tabs.addEventListener('click', e => {
        const item = e.target.closest('.tab-item');
        if (!item) return;
        const tab = item.dataset.tab;
        $qa('.tab-item', tabs).forEach(t => t.classList.remove('active'));
        item.classList.add('active');
        $qa('.tab-panel', body).forEach(p => {
          p.style.display = p.dataset.tabPanel === tab ? 'block' : 'none';
        });
      });
    }

    bindTaskTabActions(req, drawer);
    bindTestingTabActions(req, tasks, drawer);
    bindAttachmentActions(req, drawer);
    loadAttachmentPreviews(req.id, drawer);
  } catch (e) {
    body.innerHTML = emptyState(e.message || '加载失败');
  }
}

// ==================== 详情 - 基本信息 ====================

function renderBasicInfo(req) {
  return `
    <!-- 业务描述区 -->
    <div class="detail-section" style="margin-bottom:16px">
      <div class="detail-section-header">
        <div class="detail-section-title">业务描述</div>
      </div>
      <div class="detail-section-body">
        <div style="margin-bottom:20px">
          <div style="font-size:12px;color:var(--it-color-text-tertiary);margin-bottom:6px;font-weight:500">当前问题</div>
          <div style="font-size:13px;color:var(--it-color-text-secondary);line-height:1.7;background:var(--it-color-bg-page);padding:12px;border-radius:var(--it-radius-md)">${esc(req.currentProblem) || '—'}</div>
        </div>
        <div style="margin-bottom:20px">
          <div style="font-size:12px;color:var(--it-color-text-tertiary);margin-bottom:6px;font-weight:500">预期效果</div>
          <div style="font-size:13px;color:var(--it-color-text-secondary);line-height:1.7;background:var(--it-color-bg-page);padding:12px;border-radius:var(--it-radius-md)">${esc(req.expectedEffect) || '—'}</div>
        </div>
        <div>
          <div style="font-size:12px;color:var(--it-color-text-tertiary);margin-bottom:6px;font-weight:500">验收标准</div>
          <div style="font-size:13px;color:var(--it-color-text-secondary);line-height:1.7;background:var(--it-color-bg-page);padding:12px;border-radius:var(--it-radius-md)">${esc(req.acceptanceCriteria) || '—'}</div>
        </div>
      </div>
    </div>

    <!-- 基本字段 -->
    <div class="detail-section">
      <div class="detail-section-header">
        <div class="detail-section-title">基本信息</div>
      </div>
      <div class="detail-section-body">
        <div class="detail-field-grid">
          <div class="detail-field">
            <div class="detail-field-label">所属系统</div>
            <div class="detail-field-value">${esc(req.belongSystem) || '—'}</div>
          </div>
          <div class="detail-field">
            <div class="detail-field-label">需求类型</div>
            <div class="detail-field-value">${renderTypeTag(req.category || req.reqType)}</div>
          </div>
          <div class="detail-field">
            <div class="detail-field-label">来源部门</div>
            <div class="detail-field-value">${esc(req.sourceDept) || '—'}</div>
          </div>
          <div class="detail-field">
            <div class="detail-field-label">提出人</div>
            <div class="detail-field-value">${esc(req.proposer) || '—'}</div>
          </div>
          <div class="detail-field">
            <div class="detail-field-label">评审负责人</div>
            <div class="detail-field-value">${esc(req.reviewOwner) || '—'}</div>
          </div>
          <div class="detail-field">
            <div class="detail-field-label">开发负责人</div>
            <div class="detail-field-value">${esc(req.devOwner) || '—'}</div>
          </div>
          <div class="detail-field">
            <div class="detail-field-label">计划开始</div>
            <div class="detail-field-value">${formatDate(req.plannedStartDate)}</div>
          </div>
          <div class="detail-field">
            <div class="detail-field-label">计划完成</div>
            <div class="detail-field-value">${formatDate(req.plannedEndDate || req.devPlannedEnd)}</div>
          </div>
          <div class="detail-field">
            <div class="detail-field-label">预估工时</div>
            <div class="detail-field-value">${esc(req.estimatedHours || '—')} 小时</div>
          </div>
          <div class="detail-field">
            <div class="detail-field-label">当前进度</div>
            <div class="detail-field-value">${progressRender(req.progress || 0)}</div>
          </div>
        </div>
      </div>
    </div>

    ${req.remark ? `
    <div class="detail-section" style="margin-top:16px">
      <div class="detail-section-header">
        <div class="detail-section-title">备注</div>
      </div>
      <div class="detail-section-body">
        <div style="font-size:13px;color:var(--it-color-text-secondary);line-height:1.7">${esc(req.remark)}</div>
      </div>
    </div>
    ` : ''}
  `;
}

function renderRequirementDocument(req) {
  const sections = [
    ['需求说明', req.description], ['当前问题', req.currentProblem], ['预期效果', req.expectedEffect],
    ['验收标准', req.acceptanceCriteria], ['可行性分析', req.feasibility],
  ];
  return `<div class="document-sheet">
    <div class="document-sheet-header"><div><span>${esc(req.reqNo || req.id)}</span><h3>${esc(req.title)}</h3></div><div class="document-meta">评审负责人：${esc(req.reviewOwner || '未指定')} · 预计完成：${formatDate(req.plannedEndDate)}</div></div>
    ${sections.map(([title, value], index) => `<section><h4>${index + 1}. ${title}</h4><div>${value ? esc(value) : '<span class="document-empty">待完善</span>'}</div></section>`).join('')}
  </div>`;
}

// ==================== 详情 - 操作按钮 ====================

function renderDetailActionButtons(req) {
  const status = req.status;
  const id = req.id;
  const buttons = [];

  switch (status) {
    case 'DRAFT':
      buttons.push(`<button class="btn btn-primary btn-sm" data-action="submit-req" data-permission="requirement:submit">提交需求</button>`);
      buttons.push(`<button class="btn btn-secondary btn-sm" data-action="edit-req" data-permission="requirement:edit">编辑</button>`);
      buttons.push(`<button class="btn btn-ghost btn-sm" data-action="delete-req" data-permission="requirement:delete">删除</button>`);
      break;
    case 'PENDING_RECEIVE':
      buttons.push(`<button class="btn btn-primary btn-sm" data-action="receive-req" data-permission="requirement:receive">接收需求</button>`);
      buttons.push(`<button class="btn btn-danger btn-sm" data-action="reject-req" data-permission="requirement:review">驳回</button>`);
      break;
    case 'PENDING_PLAN':
      buttons.push(`<button class="btn btn-primary btn-sm" data-action="plan-req" data-permission="requirement:plan">排期分配</button>`);
      break;
    case 'IN_DEVELOPMENT':
      buttons.push(`<button class="btn btn-primary btn-sm" data-action="start-testing-req" data-permission="test:transition">转测试</button>`);
      buttons.push(`<button class="btn btn-secondary btn-sm" data-action="update-progress-req" data-permission="task:transition">更新进度</button>`);
      break;
    case 'IN_TESTING':
      buttons.push(`<button class="btn btn-primary btn-sm" data-action="finish-testing-req" data-permission="test:transition">测试完成</button>`);
      buttons.push(`<button class="btn btn-secondary btn-sm" data-action="test-fail-req" data-permission="test:transition">测试不通过</button>`);
      break;
    case 'PENDING_RELEASE':
      buttons.push(`<button class="btn btn-primary btn-sm" data-action="release-success-req" data-permission="requirement:release">发布成功</button>`);
      buttons.push(`<button class="btn btn-danger btn-sm" data-action="release-fail-req" data-permission="requirement:release">发布失败</button>`);
      break;
    case 'COMPLETED':
      buttons.push(`<button class="btn btn-secondary btn-sm" data-action="reopen-req">重新打开</button>`);
      break;
    case 'REJECTED':
      buttons.push(`<button class="btn btn-secondary btn-sm" data-action="edit-req">修改后重新提交</button>`);
      break;
  }

  return buttons.join('');
}

function bindDetailActions(req, drawer) {
  const id = req.id;
  const actionsWrap = $q('#detail-actions', drawer.querySelector('.drawer-body'));
  if (!actionsWrap) return;

  const actionMap = {
    'submit-req': () => {
      if (confirm('确定要提交这条需求吗？提交后状态变为"待接收"。')) {
        api(`/requirements/${id}/submit`, { method: 'POST' })
          .then(() => { drawer.remove(); loadRequirements(currentPage); })
          .catch(e => alert(e.message));
      }
    },
    'edit-req': () => { drawer.remove(); openReqForm(id); },
    'delete-req': () => {
      if (confirm('确定要删除这条需求吗？')) {
        api(`/requirements/${id}`, { method: 'DELETE' })
          .then(() => { drawer.remove(); loadRequirements(currentPage); })
          .catch(e => alert(e.message));
      }
    },
    'receive-req': () => openReceiveDialog(id, drawer),
    'reject-req': () => openRejectDialog(id, drawer),
    'plan-req': () => openPlanDialog(id, drawer),
    'start-testing-req': () => openStartTestingDialog(id, drawer),
    'update-progress-req': () => openUpdateProgressDialog(id, drawer),
    'finish-testing-req': () => {
      if (confirm('确定测试已完成，进入待发布状态？')) {
        api(`/requirements/${id}/finish-testing`, { method: 'POST' })
          .then(() => { drawer.remove(); loadRequirements(currentPage); })
          .catch(e => alert(e.message));
      }
    },
    'test-fail-req': () => openTestFailDialog(id, drawer),
    'release-success-req': () => {
      if (confirm('确定发布成功，需求已完成？')) {
        api(`/requirements/${id}/release`, { method: 'POST', body: { success: true } })
          .then(() => { drawer.remove(); loadRequirements(currentPage); })
          .catch(e => alert(e.message));
      }
    },
    'release-fail-req': () => {
      if (confirm('确定发布失败，回退到开发中？')) {
        api(`/requirements/${id}/release`, { method: 'POST', body: { success: false } })
          .then(() => { drawer.remove(); loadRequirements(currentPage); })
          .catch(e => alert(e.message));
      }
    },
    'reopen-req': () => {
      if (confirm('确定要重新打开此需求？')) {
        api(`/requirements/${id}/reopen`, { method: 'POST' })
          .then(() => { drawer.remove(); loadRequirements(currentPage); })
          .catch(e => alert(e.message));
      }
    },
  };

  actionsWrap.addEventListener('click', e => {
    const btn = e.target.closest('[data-action]');
    if (!btn) return;
    const action = btn.dataset.action;
    if (actionMap[action]) actionMap[action]();
  });
}

// ==================== 状态流转对话框 ====================

function openReceiveDialog(id, drawer) {
  const bodyHtml = `
    <form>
      <div class="form-item">
        <label class="form-label required">评审负责人</label>
        <input type="text" class="form-input" name="reviewOwner" placeholder="请输入评审负责人姓名" required />
      </div>
      <div class="form-item">
        <label class="form-label required">审批流</label>
        <select class="form-select" name="templateId" required>
          <option value="">加载审批流...</option>
        </select>
      </div>
      <div class="form-item">
        <label class="form-label required">预计完成时间</label>
        <input type="date" class="form-input" name="plannedEndDate" required />
        <div class="form-hint">接收时先做初步交付评估，后续排期可进一步细化</div>
      </div>
    </form>
  `;
  const modal = openModal('接收需求', bodyHtml, async (overlay) => {
    const form = overlay.querySelector('form');
    const reviewOwner = form.reviewOwner.value.trim();
    if (!reviewOwner || !form.templateId.value || !form.plannedEndDate.value) {
      alert('请填写评审负责人、审批流和预计完成时间');
      return Promise.reject(new Error('必填项未填写'));
    }
    await api(`/requirements/${id}/receive`, {
      method: 'POST',
      body: { reviewOwner, templateId: form.templateId.value, plannedEndDate: form.plannedEndDate.value },
    });
    drawer.remove();
    loadRequirements(currentPage);
  }, '确认接收');
  modal.querySelector('input[name="plannedEndDate"]').min = new Date().toISOString().slice(0, 10);
  api('/approval-templates').then(result => {
    const templates = (result?.data || []).filter(item => item.status === 'ACTIVE');
    const select = modal.querySelector('select[name="templateId"]');
    select.innerHTML = `<option value="">请选择</option>${templates.map(item => `<option value="${esc(item.id)}">${esc(item.name)}</option>`).join('')}`;
  }).catch(() => { modal.querySelector('select[name="templateId"]').innerHTML = `<option value="">审批流加载失败</option>`; });
}

function openRejectDialog(id, drawer) {
  const bodyHtml = `
    <form>
      <div class="form-item">
        <label class="form-label required">驳回原因</label>
        <textarea class="form-textarea" name="comment" rows="4" required placeholder="请填写驳回原因..."></textarea>
      </div>
    </form>
  `;
  openModal('驳回需求', bodyHtml, async (overlay) => {
    const form = overlay.querySelector('form');
    const comment = form.comment.value.trim();
    if (!comment) {
      alert('请填写驳回原因');
      return Promise.reject(new Error('必填项未填写'));
    }
    await api(`/requirements/${id}/reject`, { method: 'POST', body: { comment } });
    drawer.remove();
    loadRequirements(currentPage);
  }, '确认驳回');
}

function openDocumentDialog(id, drawer) {
  const bodyHtml = `<form>
    <div class="form-item"><label class="form-label required">当前问题</label><textarea class="form-textarea" name="currentProblem" rows="3" required></textarea></div>
    <div class="form-item"><label class="form-label required">预期效果</label><textarea class="form-textarea" name="expectedEffect" rows="3" required></textarea></div>
    <div class="form-item"><label class="form-label required">验收标准</label><textarea class="form-textarea" name="acceptanceCriteria" rows="3" required></textarea></div>
    <div class="form-item"><label class="form-label">可行性分析</label><textarea class="form-textarea" name="feasibility" rows="3"></textarea></div>
    <div class="form-item"><label class="form-label required">本次完善说明</label><input class="form-input" name="reason" required placeholder="例：补充业务边界和验收口径" /></div>
  </form>`;
  const modal = openModal('完善需求文档', bodyHtml, async overlay => {
    const form = overlay.querySelector('form');
    if (!form.currentProblem.value.trim() || !form.expectedEffect.value.trim() || !form.acceptanceCriteria.value.trim() || !form.reason.value.trim()) {
      alert('请完整填写当前问题、预期效果、验收标准和完善说明'); return Promise.reject(new Error('文档不完整'));
    }
    await api(`/requirements/${id}/document`, { method: 'PUT', body: {
      currentProblem: form.currentProblem.value.trim(), expectedEffect: form.expectedEffect.value.trim(),
      acceptanceCriteria: form.acceptanceCriteria.value.trim(), feasibility: form.feasibility.value.trim(), reason: form.reason.value.trim(),
    }});
    drawer.remove(); loadRequirements(currentPage);
  }, '保存文档');
  api(`/requirements/${id}`).then(result => {
    const data = result?.data || {};
    const form = modal.querySelector('form');
    form.currentProblem.value = data.currentProblem || '';
    form.expectedEffect.value = data.expectedEffect || '';
    form.acceptanceCriteria.value = data.acceptanceCriteria || '';
    form.feasibility.value = data.feasibility || '';
  });
}

function openChangeDialog(id, drawer) {
  const bodyHtml = `<form>
    <div class="change-notice">需求创建后不直接编辑。每次变更都会记录变更前后内容、原因和操作人；已进入审批或研发的需求变更后需重新审批。</div>
    <div class="form-item"><label class="form-label required">需求标题</label><input class="form-input" name="title" required /></div>
    <div class="form-grid">
      <div class="form-item"><label class="form-label">所属系统</label><select class="form-select" name="belongSystem">${SYSTEM_OPTIONS.map(s => `<option value="${s}">${s}</option>`).join('')}</select></div>
      <div class="form-item"><label class="form-label">需求类型</label><select class="form-select" name="category">${TYPE_OPTIONS.map(t => `<option value="${t.value}">${t.label}</option>`).join('')}</select></div>
      <div class="form-item"><label class="form-label">紧急程度</label><select class="form-select" name="urgency">${Object.entries(PRIORITY_MAP).map(([v,m]) => `<option value="${v}">${m.label}</option>`).join('')}</select></div>
      <div class="form-item"><label class="form-label">预计完成</label><input type="date" class="form-input" name="plannedEndDate" /></div>
    </div>
    <div class="form-item"><label class="form-label">当前问题</label><textarea class="form-textarea" name="currentProblem" rows="2"></textarea></div>
    <div class="form-item"><label class="form-label">预期效果</label><textarea class="form-textarea" name="expectedEffect" rows="2"></textarea></div>
    <div class="form-item"><label class="form-label">验收标准</label><textarea class="form-textarea" name="acceptanceCriteria" rows="2"></textarea></div>
    <div class="form-item"><label class="form-label required">变更原因</label><textarea class="form-textarea" name="reason" rows="3" required placeholder="说明为什么变更、影响范围以及与谁确认"></textarea></div>
  </form>`;
  const modal = openModal('提交需求变更', bodyHtml, async overlay => {
    const form = overlay.querySelector('form');
    if (!form.title.value.trim() || !form.reason.value.trim()) { alert('请填写需求标题和变更原因'); return Promise.reject(new Error('必填项未填写')); }
    await api(`/requirements/${id}/changes`, { method: 'POST', body: {
      title: form.title.value.trim(), belongSystem: form.belongSystem.value, category: form.category.value,
      urgency: form.urgency.value, plannedEndDate: form.plannedEndDate.value || null,
      currentProblem: form.currentProblem.value.trim(), expectedEffect: form.expectedEffect.value.trim(),
      acceptanceCriteria: form.acceptanceCriteria.value.trim(), reason: form.reason.value.trim(),
    }});
    drawer.remove(); loadRequirements(currentPage);
  }, '提交变更');
  api(`/requirements/${id}`).then(result => {
    const data = result?.data || {}; const form = modal.querySelector('form');
    ['title','belongSystem','category','urgency','plannedEndDate','currentProblem','expectedEffect','acceptanceCriteria'].forEach(key => { if (form[key]) form[key].value = data[key] || ''; });
  });
}

function openApprovalDialog(id, drawer) {
  const bodyHtml = `<form><div class="form-item"><label class="form-label required">审批结果</label><select class="form-select" name="action"><option value="APPROVE">通过</option><option value="REJECT">驳回</option></select></div><div class="form-item"><label class="form-label">审批意见</label><textarea class="form-textarea" name="comment" rows="4" placeholder="请填写审批意见"></textarea></div></form>`;
  openModal('需求审批', bodyHtml, async overlay => {
    const form = overlay.querySelector('form');
    if (form.action.value === 'REJECT' && !form.comment.value.trim()) { alert('驳回时必须填写原因'); return Promise.reject(new Error('请填写驳回原因')); }
    await api(`/requirements/${id}/approve`, { method: 'POST', body: { action: form.action.value, comment: form.comment.value.trim() } });
    drawer.remove(); loadRequirements(currentPage);
  }, '确认审批');
}

function openPlanDialog(id, drawer) {
  const bodyHtml = `
    <form>
      <div class="form-item">
        <label class="form-label required">开发负责人</label>
        <input type="text" class="form-input" name="devOwner" placeholder="请输入开发负责人姓名" required />
      </div>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">
        <div class="form-item">
          <label class="form-label">计划开始日期</label>
          <input type="date" class="form-input" name="plannedStartDate" />
        </div>
        <div class="form-item">
          <label class="form-label">计划结束日期</label>
          <input type="date" class="form-input" name="plannedEndDate" />
        </div>
      </div>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">
        <div class="form-item">
          <label class="form-label">开发计划完成</label>
          <input type="date" class="form-input" name="devPlannedEnd" />
        </div>
        <div class="form-item">
          <label class="form-label">预估工时 (小时)</label>
          <input type="number" class="form-input" name="estimatedHours" min="0" step="0.5" />
        </div>
      </div>
    </form>
  `;
  const modal = openModal('排期分配', bodyHtml, async (overlay) => {
    const form = overlay.querySelector('form');
    const devOwner = form.devOwner.value.trim();
    if (!devOwner) {
      alert('请填写开发负责人');
      return Promise.reject(new Error('必填项未填写'));
    }
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
  api(`/requirements/${id}`).then(result => {
    const data = result?.data || {}; const form = modal.querySelector('form');
    form.plannedStartDate.value = data.plannedStartDate || new Date().toISOString().slice(0, 10);
    form.plannedEndDate.value = data.plannedEndDate || '';
    form.devPlannedEnd.value = data.devPlannedEnd || data.plannedEndDate || '';
    form.estimatedHours.value = data.estimatedHours || '';
  });
}

function openStartTestingDialog(id, drawer) {
  const bodyHtml = `
    <form>
      <div class="form-item">
        <label class="form-label required">测试人员</label>
        <input type="text" class="form-input" name="tester" placeholder="请输入测试人员姓名" required />
      </div>
      <div class="form-item">
        <label class="form-label">测试说明</label>
        <textarea class="form-textarea" name="testRemark" rows="2" placeholder="可选填写测试说明..."></textarea>
      </div>
    </form>
  `;
  openModal('转测试', bodyHtml, async (overlay) => {
    const form = overlay.querySelector('form');
    const tester = form.tester.value.trim();
    if (!tester) {
      alert('请填写测试人员');
      return Promise.reject(new Error('必填项未填写'));
    }
    await api(`/requirements/${id}/start-testing`, {
      method: 'POST',
      body: { tester, testRemark: form.testRemark.value.trim() },
    });
    drawer.remove();
    loadRequirements(currentPage);
  }, '确认转测');
}

function openTestFailDialog(id, drawer) {
  const bodyHtml = `
    <form>
      <div class="form-item">
        <label class="form-label required">不通过原因</label>
        <textarea class="form-textarea" name="comment" rows="4" required placeholder="请填写测试不通过的原因..."></textarea>
      </div>
    </form>
  `;
  openModal('测试不通过', bodyHtml, async (overlay) => {
    const form = overlay.querySelector('form');
    const comment = form.comment.value.trim();
    if (!comment) {
      alert('请填写不通过原因');
      return Promise.reject(new Error('必填项未填写'));
    }
    await api(`/requirements/${id}/test-fail`, { method: 'POST', body: { comment } });
    drawer.remove();
    loadRequirements(currentPage);
  }, '确认');
}

function openUpdateProgressDialog(id, drawer) {
  const bodyHtml = `
    <form>
      <div class="form-item">
        <label class="form-label">当前进度 (%)</label>
        <input type="number" class="form-input" name="progress" min="0" max="100" value="0" />
      </div>
      <div class="form-item">
        <label class="form-label">进度说明</label>
        <textarea class="form-textarea" name="progressRemark" rows="2" placeholder="可选填写进度说明..."></textarea>
      </div>
    </form>
  `;
  openModal('更新进度', bodyHtml, async (overlay) => {
    const form = overlay.querySelector('form');
    const progress = parseInt(form.progress.value) || 0;
    await api(`/requirements/${id}/progress`, {
      method: 'PUT',
      body: { progress },
    });
    drawer.remove();
    loadRequirements(currentPage);
  }, '更新');
}

// ==================== 任务列表 Tab ====================

function renderTasksTab(tasks, req) {
  const canSplit = ['IN_DEVELOPMENT', 'IN_TESTING', 'PENDING_RELEASE'].includes(req.status);
  return `
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
      <div><div style="font-size:13px;font-weight:500;color:var(--it-color-text-primary)">开发任务</div><div style="font-size:12px;color:var(--it-color-text-tertiary);margin-top:3px">任务从已接收的需求中拆分，工时将汇总到任务和需求</div></div>
      ${canSplit ? `<button class="btn btn-primary btn-sm" data-action="add-task" data-permission="task:create">+ 拆分任务</button>` : ''}
    </div>
    ${tasks.length ? `
      <table class="data-table" style="font-size:12px">
        <thead><tr>
          <th>任务标题</th>
          <th style="width:80px">状态</th>
          <th style="width:70px">负责人</th>
          <th style="width:100px">进度</th>
          <th style="width:90px">截止日期</th>
        </tr></thead>
        <tbody>
          ${tasks.map(tk => `
            <tr>
              <td style="font-size:12px">${esc(tk.title)}</td>
              <td>${statusBadge(tk.status)}</td>
              <td style="font-size:12px">${esc(tk.assignee || '—')}</td>
              <td>${progressRender(tk.progress || 0)}</td>
              <td style="font-size:12px;color:var(--it-color-text-tertiary)">${formatDate(tk.dueDate)}</td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    ` : `<div class="empty-state" style="padding:40px 20px"><div class="empty-state-title">暂无开发任务</div><div class="empty-state-desc">${canSplit ? '点击“拆分任务”创建开发任务' : '需求接收并进入排期后可拆分开发任务'}</div></div>`}
  `;
}

function renderTestingTab(records, tasks, req) {
  const statusLabels = { OPEN: '待处理', IN_PROGRESS: '处理中', RESOLVED: '待验收', CLOSED: '已关闭', REOPENED: '重新打开' };
  const severityLabels = { LOW: '低', MEDIUM: '中', HIGH: '高', CRITICAL: '严重' };
  return `<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px"><div><div style="font-size:13px;font-weight:600">测试记录与缺陷</div><div style="font-size:12px;color:var(--it-color-text-tertiary);margin-top:3px">每条记录绑定当前需求，可选关联开发任务</div></div><button class="btn btn-primary btn-sm" data-action="add-test-record" data-permission="test:create">+ 记录测试/缺陷</button></div>
  ${records.length ? `<table class="data-table"><thead><tr><th>类型</th><th>标题</th><th>严重程度</th><th>状态</th><th>测试人</th><th>处理人</th><th>记录时间</th></tr></thead><tbody>${records.map(record => `<tr class="clickable-row" data-action="view-test-record" data-id="${esc(record.id)}"><td><span class="tag">${record.type === 'BUG' ? '缺陷' : '测试'}</span></td><td><strong>${esc(record.title)}</strong></td><td>${severityLabels[record.severity] || esc(record.severity)}</td><td>${statusLabels[record.status] || esc(record.status)}</td><td>${esc(record.tester || '—')}</td><td>${esc(record.handler || '—')}</td><td>${formatDateTime(record.createdAt)}</td></tr>`).join('')}</tbody></table>` : `<div class="empty-state"><div class="empty-state-title">暂无测试或缺陷记录</div><div class="empty-state-desc">点击上方按钮记录测试结果</div></div>`}`;
}

function bindTestingTabActions(req, tasks, container) {
  const body = container.querySelector('.requirement-detail-body');
  body.querySelector('[data-action="add-test-record"]')?.addEventListener('click', () => openAddTestRecordDialog(req, tasks, container));
  body.querySelectorAll('[data-action="view-test-record"]').forEach(row => row.onclick = async () => {
    try {
      const result = await api(`/test-records/${row.dataset.id}`); const record = result?.data || {};
      openModal('测试/缺陷记录', `<div class="test-record-detail"><h3>${esc(record.title)}</h3><div class="detail-field-grid"><div><small>类型</small><strong>${record.type === 'BUG' ? '缺陷' : '测试'}</strong></div><div><small>状态</small><strong>${esc(record.status)}</strong></div><div><small>测试人</small><strong>${esc(record.tester || '—')}</strong></div><div><small>处理人</small><strong>${esc(record.handler || '—')}</strong></div></div><section><h4>描述</h4><p>${esc(record.description || '—')}</p></section><section><h4>预期结果</h4><p>${esc(record.expectedResult || '—')}</p></section><section><h4>实际结果</h4><p>${esc(record.actualResult || '—')}</p></section></div>`, async () => {}, '关闭');
    } catch (e) { alert(e.message); }
  });
}

function openAddTestRecordDialog(req, tasks, container) {
  const bodyHtml = `<form><div class="form-grid"><div class="form-item"><label class="form-label required">记录类型</label><select class="form-select" name="type"><option value="TEST">测试记录</option><option value="BUG">缺陷</option></select></div><div class="form-item"><label class="form-label">严重程度</label><select class="form-select" name="severity"><option value="LOW">低</option><option value="MEDIUM">中</option><option value="HIGH">高</option><option value="CRITICAL">严重</option></select></div></div><div class="form-item"><label class="form-label required">标题</label><input class="form-input" name="title" required /></div><div class="form-item"><label class="form-label">关联任务</label><select class="form-select" name="taskId"><option value="">不关联任务</option>${tasks.map(task => `<option value="${esc(task.id)}">${esc(task.taskNo)} - ${esc(task.title)}</option>`).join('')}</select></div><div class="form-grid"><div class="form-item"><label class="form-label">测试人</label><input class="form-input" name="tester" /></div><div class="form-item"><label class="form-label">处理人</label><input class="form-input" name="handler" /></div></div><div class="form-item"><label class="form-label">描述</label><textarea class="form-textarea" name="description" rows="3"></textarea></div><div class="form-item"><label class="form-label">预期结果</label><textarea class="form-textarea" name="expectedResult" rows="2"></textarea></div><div class="form-item"><label class="form-label">实际结果</label><textarea class="form-textarea" name="actualResult" rows="2"></textarea></div></form>`;
  openModal('记录测试/缺陷', bodyHtml, async overlay => {
    const form = overlay.querySelector('form'); if (!form.title.value.trim()) { alert('请填写标题'); return Promise.reject(new Error('必填项未填写')); }
    await api('/test-records', { method: 'POST', body: { requirementId: req.id, taskId: form.taskId.value || null, title: form.title.value.trim(), type: form.type.value, severity: form.severity.value, tester: form.tester.value.trim(), handler: form.handler.value.trim(), description: form.description.value.trim(), expectedResult: form.expectedResult.value.trim(), actualResult: form.actualResult.value.trim() }});
    container.remove(); openReqDetail(req.id);
  }, '保存记录');
}

function bindTaskTabActions(req, drawer) {
  const body = drawer.querySelector('.drawer-body');
  const addBtn = body.querySelector('[data-action="add-task"]');
  if (addBtn) addBtn.onclick = () => openAddTaskDialog(req.id, drawer);
}

function openAddTaskDialog(reqId, drawer) {
  const bodyHtml = `
    <form>
      <div class="form-item">
        <label class="form-label required">任务标题</label>
        <input type="text" class="form-input" name="title" required placeholder="请输入任务标题" />
      </div>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">
        <div class="form-item">
          <label class="form-label">负责人</label>
          <input type="text" class="form-input" name="assignee" placeholder="请输入负责人" />
        </div>
        <div class="form-item">
          <label class="form-label">紧急程度</label>
          <select class="form-select" name="priority">
            ${Object.entries(PRIORITY_MAP).map(([v, m]) => `<option value="${v}" ${v === 'MEDIUM' ? 'selected' : ''}>${m.label}</option>`).join('')}
          </select>
        </div>
      </div>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">
        <div class="form-item">
          <label class="form-label">截止日期</label>
          <input type="date" class="form-input" name="dueDate" />
        </div>
        <div class="form-item">
          <label class="form-label">预估工时 (小时)</label>
          <input type="number" class="form-input" name="estimatedHours" min="0" step="0.5" />
        </div>
      </div>
      <div class="form-item">
        <label class="form-label">任务描述</label>
        <textarea class="form-textarea" name="description" rows="3" placeholder="请输入任务描述..."></textarea>
      </div>
    </form>
  `;
  openModal('新增任务', bodyHtml, async (overlay) => {
    const form = overlay.querySelector('form');
    const title = form.title.value.trim();
    if (!title) {
      alert('请填写任务标题');
      return Promise.reject(new Error('必填项未填写'));
    }
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
    drawer.remove();
    openReqDetail(reqId);
    loadRequirements(currentPage);
  }, '创建');
}

// ==================== 审批记录 Tab ====================

function renderApprovalTab(records) {
  if (!records || !records.length) {
    return `<div class="empty-state" style="padding:40px 20px"><div class="empty-state-title">暂无审批记录</div><div class="empty-state-desc">需求接收后将启动审批流程</div></div>`;
  }
  return `
    <div class="timeline">
      ${records.map(log => `
        <div class="timeline-item">
          <div class="timeline-dot"></div>
          <div style="margin-left:8px">
            <div style="font-size:12px;color:var(--it-color-text-tertiary);margin-bottom:4px">${formatDateTime(log.createdAt || log.time)}</div>
            <div style="font-size:13px;color:var(--it-color-text-primary);font-weight:500">
              ${esc(log.nodeName || log.action || '审批节点')}
              ${log.approver ? `<span style="color:var(--it-color-text-secondary);font-weight:400"> - ${esc(log.approver)}</span>` : ''}
            </div>
            ${log.comment ? `<div style="font-size:12px;color:var(--it-color-text-secondary);margin-top:4px;background:var(--it-color-bg-page);padding:8px 10px;border-radius:var(--it-radius-sm)">${esc(log.comment)}</div>` : ''}
          </div>
        </div>
      `).join('')}
    </div>
  `;
}

// ==================== 变更记录 Tab ====================

function renderChangeTab(logs) {
  if (!logs || !logs.length) {
    return `<div class="empty-state" style="padding:40px 20px"><div class="empty-state-title">暂无变更记录</div><div class="empty-state-desc">需求字段变更将记录在此处</div></div>`;
  }
  return `
    <table class="data-table" style="font-size:12px">
      <thead><tr>
        <th>字段</th>
        <th>变更前</th>
        <th>变更后</th>
        <th>变更原因</th>
        <th style="width:70px">操作人</th>
        <th style="width:130px">时间</th>
      </tr></thead>
      <tbody>
        ${logs.map(log => `
          <tr>
            <td>${esc(changeFieldLabel(log.fieldName || log.field))}</td>
            <td style="color:var(--it-color-text-tertiary)">${esc(changeDisplayValue(log.oldValue ?? log.old))}</td>
            <td style="color:var(--it-color-text-primary)">${esc(changeDisplayValue(log.newValue ?? log.new))}</td>
            <td>${esc(log.changeReason || '—')}</td>
            <td>${esc(log.changedBy || log.operator || '—')}</td>
            <td style="color:var(--it-color-text-tertiary)">${formatDateTime(log.createdAt || log.time)}</td>
          </tr>
        `).join('')}
      </tbody>
    </table>
  `;
}

function changeFieldLabel(field) {
  const labels = { status: '需求状态', approvalNode: '审批节点', title: '需求标题', category: '需求类型', urgency: '紧急程度', belongSystem: '所属系统', sourceDept: '来源部门', currentProblem: '当前问题', expectedEffect: '预期效果', acceptanceCriteria: '验收标准' };
  return labels[field] || field || '—';
}

function changeDisplayValue(value) {
  if (value === null || value === undefined || value === 'null' || value === '') return '—';
  return STATUS_MAP[value]?.label || TYPE_OPTIONS.find(item => item.value === value)?.label || PRIORITY_MAP[value]?.label || value;
}

// ==================== 附件 Tab ====================

function renderAttachmentsTab(attachments, requirementId) {
  return `
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
      <div><div style="font-size:13px;font-weight:500;color:var(--it-color-text-primary)">图片与附件</div><div style="font-size:12px;color:var(--it-color-text-tertiary);margin-top:3px">需求截图可在此预览，其他文件可下载</div></div>
      <button class="btn btn-primary btn-sm" data-action="upload-attachment" data-permission="requirement:edit">+ 上传</button>
      <input type="file" data-attachment-input hidden multiple accept="image/*,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.zip,.rar" />
    </div>
    ${attachments.length ? `
      <div class="attachment-grid">
        ${attachments.map(att => `<article class="attachment-card">
          <div class="attachment-preview ${att.image ? 'is-image' : ''}">
            ${att.image ? `<img alt="${esc(att.fileName)}" data-attachment-preview="${esc(att.id)}" />` : `<span>${fileExtension(att.fileName)}</span>`}
          </div>
          <div class="attachment-meta"><strong title="${esc(att.fileName)}">${esc(att.fileName)}</strong><small>${formatFileSize(att.fileSize)} · ${esc(att.uploadedBy || '—')}</small><small>${formatDateTime(att.uploadedAt)}</small></div>
          <div class="attachment-actions">
            <button class="btn btn-ghost btn-sm" data-action="download-attachment" data-id="${esc(att.id)}" data-name="${esc(att.fileName)}">下载</button>
            <button class="btn btn-ghost btn-sm" data-action="delete-attachment" data-id="${esc(att.id)}" data-permission="requirement:edit">删除</button>
          </div>
        </article>`).join('')}
      </div>
    ` : `<div class="empty-state" style="padding:40px 20px"><div class="empty-state-title">暂无附件</div><div class="empty-state-desc">点击"上传附件"添加相关文件</div></div>`}
  `;
}

function renderSelectedFiles(modal, files) {
  const wrap = modal.querySelector('[data-file-selection]');
  if (!wrap) return;
  wrap.innerHTML = files.map(file => `
    <div class="selected-file">
      <span class="selected-file-icon">${file.type?.startsWith('image/') ? '图' : fileExtension(file.name)}</span>
      <span><strong>${esc(file.name)}</strong><small>${formatFileSize(file.size)}</small></span>
    </div>
  `).join('');
}

async function uploadRequirementFiles(requirementId, files) {
  for (const file of files) {
    if (file.size > 20 * 1024 * 1024) throw new Error(`${file.name} 超过 20MB 限制`);
    const data = new FormData();
    data.append('file', file);
    await api(`/requirements/${requirementId}/attachments`, { method: 'POST', body: data, timeout: 60000 });
  }
}

function bindAttachmentActions(req, drawer) {
  const body = drawer.querySelector('.drawer-body');
  const input = body.querySelector('[data-attachment-input]');
  const uploadButton = body.querySelector('[data-action="upload-attachment"]');
  if (uploadButton && input) {
    uploadButton.onclick = () => input.click();
    input.onchange = async () => {
      try {
        const files = [...input.files];
        if (!files.length) return;
        uploadButton.disabled = true;
        uploadButton.textContent = '上传中...';
        await uploadRequirementFiles(req.id, files);
        drawer.remove();
        openReqDetail(req.id);
      } catch (e) {
        alert(e.message || '上传失败');
        uploadButton.disabled = false;
        uploadButton.textContent = '+ 上传';
      }
    };
  }

  body.querySelectorAll('[data-action="download-attachment"]').forEach(button => {
    button.onclick = async () => {
      try {
        const blob = await apiBlob(`/requirements/${req.id}/attachments/${button.dataset.id}/content?download=true`);
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = button.dataset.name || '附件';
        link.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
      } catch (e) { alert(e.message || '下载失败'); }
    };
  });

  body.querySelectorAll('[data-action="delete-attachment"]').forEach(button => {
    button.onclick = async () => {
      if (!confirm('确定删除该附件？')) return;
      try {
        await api(`/requirements/${req.id}/attachments/${button.dataset.id}`, { method: 'DELETE' });
        drawer.remove();
        openReqDetail(req.id);
      } catch (e) { alert(e.message || '删除失败'); }
    };
  });
}

async function loadAttachmentPreviews(requirementId, drawer) {
  const images = [...drawer.querySelectorAll('[data-attachment-preview]')];
  await Promise.all(images.map(async image => {
    try {
      const blob = await apiBlob(`/requirements/${requirementId}/attachments/${image.dataset.attachmentPreview}/content`);
      const url = URL.createObjectURL(blob);
      image.src = url;
      image.onload = () => URL.revokeObjectURL(url);
    } catch { image.closest('.attachment-preview')?.classList.add('preview-error'); }
  }));
}

function formatFileSize(size) {
  const bytes = Number(size || 0);
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

function fileExtension(name) {
  const ext = String(name || '').split('.').pop();
  return ext && ext !== name ? ext.slice(0, 4).toUpperCase() : 'FILE';
}
