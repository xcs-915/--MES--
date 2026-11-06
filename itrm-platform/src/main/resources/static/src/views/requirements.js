import { $, $$, esc, formatDate, formatDateTime } from '../utils/dom.js';
import { state } from '../store/index.js';
import { api } from '../api/request.js';
import { openModal, openDrawer, emptyState, loadingState, pagination } from './_ui.js';

// 支持 context 的查询助手
const $q = (sel, ctx) => (ctx || document).querySelector(sel);
const $qa = (sel, ctx) => [...(ctx || document).querySelectorAll(sel)];

// ==================== 常量配置 ====================

let STATUS_LIST = [
  { value: 'PENDING_RECEIVE', label: '待接收' },
  { value: 'PENDING_PLAN', label: '待排期' },
  { value: 'IN_DEVELOPMENT', label: '开发中' },
  { value: 'IN_TESTING', label: '测试中' },
  { value: 'PENDING_RELEASE', label: '待发布' },
  { value: 'COMPLETED', label: '已完成' },
];

const STATUS_MAP = {
  DRAFT: { label: '草稿', cls: 'badge-status-pending' },
  PENDING_RECEIVE: { label: '待接收', cls: 'badge-status-pending' },
  PENDING_PLAN: { label: '待排期', cls: 'badge-status-backlog' },
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
const TYPE_OPTIONS = ['新功能', '功能优化', 'BUG', '接口', '报表'];
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
            ${TYPE_OPTIONS.map(t => `<option value="${t}">${t}</option>`).join('')}
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
  loadRequirements(0);
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
        <th style="width:120px;text-align:right;padding-right:16px">操作</th>
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
          <button class="btn btn-ghost btn-sm" data-action="view-req" data-id="${id}">查看</button>
          ${canEdit(item.status) ? `<button class="btn btn-ghost btn-sm" data-action="edit-req" data-id="${id}" data-permission="requirement:edit">编辑</button>` : ''}
          <div class="dropdown" style="display:inline-block">
            <button class="btn btn-ghost btn-sm" data-action="more-req" data-id="${id}">
              <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="1"></circle><circle cx="12" cy="5" r="1"></circle><circle cx="12" cy="19" r="1"></circle></svg>
            </button>
          </div>
        </div>
      </td>
    </tr>
  `;
}

function renderTypeTag(type) {
  if (!type) return '—';
  const typeMap = {
    '新功能': 'tag-type-feature',
    '功能优化': 'tag-type-improvement',
    'BUG': 'tag-type-bug',
    '接口': 'tag-type-task',
    '报表': 'tag-type-task',
  };
  const cls = typeMap[type] || 'tag-type-task';
  return `<span class="tag ${cls}">${esc(type)}</span>`;
}

function canEdit(status) {
  return status === 'DRAFT' || status === 'PENDING_RECEIVE';
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
      case 'edit-req':
        openReqForm(id);
        break;
      case 'view-req':
        openReqDetail(id);
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
            ${TYPE_OPTIONS.map(t => `<option value="${t}">${t}</option>`).join('')}
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

    // 替换 drawer 标题为需求编号 + 标题
    const headerTitle = drawer.querySelector('.drawer-title');
    if (headerTitle) {
      headerTitle.innerHTML = `<span style="font-size:13px;color:var(--it-color-text-tertiary);font-weight:400">${esc(req.reqNo || req.id)}</span>`;
    }

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

      <!-- 操作按钮区 -->
      <div style="display:flex;gap:8px;margin-bottom:20px;flex-wrap:wrap" id="detail-actions">
        ${renderDetailActionButtons(req)}
      </div>

      <!-- Tabs -->
      <div class="tabs" id="detail-tabs">
        <div class="tab-item active" data-tab="basic">基本信息</div>
        <div class="tab-item" data-tab="tasks">任务分解 <span class="tab-count">${tasks.length}</span></div>
        <div class="tab-item" data-tab="approval">审批记录</div>
        <div class="tab-item" data-tab="change">变更记录</div>
        <div class="tab-item" data-tab="attachments">附件</div>
      </div>

      <!-- Tab 内容 -->
      <div class="tab-panels">
        <div class="tab-panel active" data-tab-panel="basic">
          ${renderBasicInfo(req)}
        </div>
        <div class="tab-panel" data-tab-panel="tasks" style="display:none">
          ${renderTasksTab(tasks, req)}
        </div>
        <div class="tab-panel" data-tab-panel="approval" style="display:none">
          ${renderApprovalTab(approvalRecords)}
        </div>
        <div class="tab-panel" data-tab-panel="change" style="display:none">
          ${renderChangeTab(changeLogs)}
        </div>
        <div class="tab-panel" data-tab-panel="attachments" style="display:none">
          ${renderAttachmentsTab(req)}
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

    bindDetailActions(req, drawer);
    bindTaskTabActions(req, drawer);
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
        <label class="form-label">审批模板</label>
        <select class="form-select" name="templateId">
          <option value="">默认模板</option>
          <option value="simple">简易审批</option>
          <option value="standard">标准审批</option>
          <option value="strict">严格审批</option>
        </select>
      </div>
      <div class="form-item">
        <label class="form-label">备注</label>
        <textarea class="form-textarea" name="remark" rows="2" placeholder="可选填写接收备注..."></textarea>
      </div>
    </form>
  `;
  openModal('接收需求', bodyHtml, async (overlay) => {
    const form = overlay.querySelector('form');
    const reviewOwner = form.reviewOwner.value.trim();
    if (!reviewOwner) {
      alert('请填写评审负责人');
      return Promise.reject(new Error('必填项未填写'));
    }
    await api(`/requirements/${id}/receive`, {
      method: 'POST',
      body: { reviewOwner, templateId: form.templateId.value || null, remark: form.remark.value.trim() },
    });
    drawer.remove();
    loadRequirements(currentPage);
  }, '确认接收');
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
  openModal('排期分配', bodyHtml, async (overlay) => {
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
    await api(`/requirements/${id}`, {
      method: 'PUT',
      body: { progress, progressRemark: form.progressRemark.value.trim() },
    });
    drawer.remove();
    loadRequirements(currentPage);
  }, '更新');
}

// ==================== 任务列表 Tab ====================

function renderTasksTab(tasks, req) {
  return `
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
      <div style="font-size:13px;font-weight:500;color:var(--it-color-text-primary)">任务列表</div>
      <button class="btn btn-primary btn-sm" data-action="add-task" data-permission="task:create">+ 新增任务</button>
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
    ` : `<div class="empty-state" style="padding:40px 20px"><div class="empty-state-title">暂无任务</div><div class="empty-state-desc">点击"新增任务"创建任务分解</div></div>`}
  `;
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
        <th style="width:70px">操作人</th>
        <th style="width:130px">时间</th>
      </tr></thead>
      <tbody>
        ${logs.map(log => `
          <tr>
            <td>${esc(log.fieldName || log.field || '—')}</td>
            <td style="color:var(--it-color-text-tertiary)">${esc(log.oldValue || log.old || '—')}</td>
            <td style="color:var(--it-color-text-primary)">${esc(log.newValue || log.new || '—')}</td>
            <td>${esc(log.operator || '—')}</td>
            <td style="color:var(--it-color-text-tertiary)">${formatDateTime(log.createdAt || log.time)}</td>
          </tr>
        `).join('')}
      </tbody>
    </table>
  `;
}

// ==================== 附件 Tab ====================

function renderAttachmentsTab(req) {
  const attachments = req.attachments || [];
  return `
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
      <div style="font-size:13px;font-weight:500;color:var(--it-color-text-primary)">附件列表</div>
      <button class="btn btn-primary btn-sm" data-action="upload-attachment">+ 上传附件</button>
    </div>
    ${attachments.length ? `
      <table class="data-table" style="font-size:12px">
        <thead><tr>
          <th>文件名</th>
          <th style="width:80px">大小</th>
          <th style="width:70px">上传人</th>
          <th style="width:130px">上传时间</th>
          <th style="width:80px;text-align:right">操作</th>
        </tr></thead>
        <tbody>
          ${attachments.map(att => `
            <tr>
              <td>${esc(att.fileName || att.name || '—')}</td>
              <td>${esc(att.fileSize || att.size || '—')}</td>
              <td>${esc(att.uploader || '—')}</td>
              <td style="color:var(--it-color-text-tertiary)">${formatDateTime(att.uploadTime || att.createdAt)}</td>
              <td style="text-align:right">
                <button class="btn btn-ghost btn-sm">下载</button>
              </td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    ` : `<div class="empty-state" style="padding:40px 20px"><div class="empty-state-title">暂无附件</div><div class="empty-state-desc">点击"上传附件"添加相关文件</div></div>`}
  `;
}
