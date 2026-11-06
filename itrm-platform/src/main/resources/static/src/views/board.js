import { $, esc, formatDate } from '../utils/dom.js';
import { api } from '../api/request.js';
import { openDrawer, loadingState } from './_ui.js';

// ==================== 配置 ====================

const boardColumns = [
  { key: 'PENDING_RECEIVE', label: '待接收',  dot: 'pending',    color: '#94a3b8' },
  { key: 'PENDING_DOCUMENT',label: '待完善',  dot: 'backlog',    color: '#8b5cf6' },
  { key: 'PENDING_APPROVAL',label: '待审批',  dot: 'backlog',    color: '#a855f7' },
  { key: 'APPROVED',        label: '待排期',  dot: 'backlog',    color: '#6366f1' },
  { key: 'IN_DEVELOPMENT',  label: '开发中',  dot: 'developing', color: '#8b5cf6' },
  { key: 'IN_TESTING',      label: '测试中',  dot: 'testing',    color: '#f97316' },
  { key: 'PENDING_RELEASE', label: '待发布',  dot: 'ready',      color: '#06b6d4' },
  { key: 'COMPLETED',       label: '已完成',  dot: 'done',       color: '#22c55e' },
];

const priorityMap = {
  URGENT: { label: '紧急', cls: 'tag-priority-urgent' },
  HIGH:   { label: '高',   cls: 'tag-priority-high' },
  MEDIUM: { label: '中',   cls: 'tag-priority-medium' },
  LOW:    { label: '低',   cls: 'tag-priority-low' },
};

// 状态流转映射（简易版拖拽：点击按钮移动到下一状态）
const statusFlow = {};

// ==================== 渲染入口 ====================

export async function renderBoard() {
  const page = $('#page');
  page.innerHTML = `
    <div class="page-container">
      <div class="page-header">
        <h2>项目看板</h2>
      </div>

      <div class="kanban-board" id="kanban-board">
        ${boardColumns.map(col => `
          <div class="kanban-column" data-status="${col.key}">
            <div class="kanban-column-header">
              <div class="kanban-column-title">
                <span class="kanban-column-dot ${col.dot}"></span>
                <span>${col.label}</span>
                <span class="kanban-column-count" data-count="${col.key}">0</span>
              </div>
              <div class="kanban-column-add" data-action="col-more" data-status="${col.key}" title="更多操作">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="5" r="1.5"/><circle cx="12" cy="12" r="1.5"/><circle cx="12" cy="19" r="1.5"/>
                </svg>
              </div>
            </div>
            <div class="kanban-card-list" data-body="${col.key}">
              ${loadingState()}
            </div>
            <div style="flex-shrink:0;padding:8px 12px 12px;border-top:1px solid var(--it-color-border-light)">
              <button class="btn" data-action="add-req" data-status="${col.key}" data-permission="requirement:create" style="width:100%;justify-content:center;gap:6px;background:transparent;border:1px dashed var(--it-color-border);color:var(--it-color-text-tertiary);font-size:12px;padding:6px 12px;transition:all var(--it-transition-fast)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                添加需求
              </button>
            </div>
          </div>
        `).join('')}
      </div>
    </div>
  `;

  bindBoardEvents();
  loadBoardData();
}

// ==================== 事件绑定 ====================

function bindBoardEvents() {
  const board = $('#kanban-board');
  if (!board) return;

  board.addEventListener('click', e => {
    // 卡片点击 → 打开详情
    const card = e.target.closest('.kanban-card');
    if (card && !e.target.closest('[data-action]')) {
      const id = card.dataset.id;
      if (id) openRequirementDetail(id);
      return;
    }

    // 按钮动作
    const btn = e.target.closest('[data-action]');
    if (!btn) return;
    const action = btn.dataset.action;

    if (action === 'add-req') {
      const status = btn.dataset.status;
      alert(`在「${boardColumns.find(c => c.key === status)?.label}」列添加需求\n（功能开发中）`);
    } else if (action === 'col-more') {
      const status = btn.dataset.status;
      alert(`更多操作：${boardColumns.find(c => c.key === status)?.label}\n（功能开发中）`);
    } else if (action === 'move-next') {
      const id = btn.dataset.id;
      moveToNextStatus(id, btn);
    } else if (action === 'move-prev') {
      const id = btn.dataset.id;
      moveToPrevStatus(id, btn);
    } else if (action === 'open-requirement') {
      sessionStorage.setItem('itrm_current_req_id', btn.dataset.id);
      document.querySelector('[data-view="requirements"]')?.click();
    }
  });

  // 卡片 hover 时显示操作按钮
  board.addEventListener('mouseover', e => {
    const card = e.target.closest('.kanban-card');
    if (card) {
      const actions = card.querySelector('.card-actions');
      if (actions) actions.style.opacity = '1';
    }
  });

  board.addEventListener('mouseout', e => {
    const card = e.target.closest('.kanban-card');
    if (card && !card.contains(e.relatedTarget)) {
      const actions = card.querySelector('.card-actions');
      if (actions) actions.style.opacity = '0';
    }
  });
}

// ==================== 数据加载 ====================

async function loadBoardData() {
  // 重置每列为加载状态
  boardColumns.forEach(col => {
    const body = $(`[data-body="${col.key}"]`);
    if (body) body.innerHTML = loadingState();
  });

  try {
    // 获取所有需求（不分页，获取全部用于看板展示）
    const res = await api('/requirements?size=200');
    const requirements = res?.data?.items || res?.data?.content || [];

    // 按状态分组
    const grouped = {};
    boardColumns.forEach(col => { grouped[col.key] = []; });
    requirements.forEach(req => {
      const status = req.status || 'PENDING_RECEIVE';
      if (grouped[status]) {
        grouped[status].push(req);
      }
    });

    // 渲染各列
    boardColumns.forEach(col => {
      renderColumn(col.key, grouped[col.key] || []);
    });
  } catch (e) {
    boardColumns.forEach(col => {
      const body = $(`[data-body="${col.key}"]`);
      if (body) {
        body.innerHTML = renderEmptyState('加载失败');
      }
    });
  }
}

function renderColumn(statusKey, items) {
  const body = $(`[data-body="${statusKey}"]`);
  const count = $(`[data-count="${statusKey}"]`);

  if (count) count.textContent = items.length;

  if (!body) return;

  if (!items.length) {
    body.innerHTML = renderEmptyState('暂无需求');
    return;
  }

  body.innerHTML = items.map(req => renderCard(req, statusKey)).join('');
}

// ==================== 卡片渲染 ====================

function renderCard(req, columnStatus) {
  const reqNo = req.reqNo || req.id || '';
  const title = req.title || '无标题';
  const priority = req.urgency || req.priority || 'MEDIUM';
  const priorityInfo = priorityMap[priority] || priorityMap.MEDIUM;
  const belongSystem = req.belongSystem || '';
  const reqType = req.category || req.reqType || '';
  const assignee = req.devOwner || req.reviewOwner || '-';
  const progress = req.progress || 0;
  const dueDate = req.plannedEndDate || req.devPlannedEnd || '';

  return `
    <div class="kanban-card" data-id="${esc(req.id)}">
      <div class="kanban-card-header">
        <span class="kanban-card-id">${esc(reqNo)}</span>
        <span class="tag ${priorityInfo.cls}" style="padding:1px 6px;font-size:11px;line-height:1.6">${priorityInfo.label}</span>
      </div>

      <div class="kanban-card-title" title="${esc(title)}">${esc(title)}</div>

      <div class="kanban-card-tags" style="margin-bottom:10px">
        ${belongSystem ? `<span class="tag tag-type-task" style="font-size:11px;padding:1px 6px;line-height:1.6;background:#f1f5f9;color:#475569;border-color:#e2e8f0">${esc(belongSystem)}</span>` : ''}
        ${reqType ? `<span class="tag ${typeTagClass(reqType)}" style="font-size:11px;padding:1px 6px;line-height:1.6">${esc(reqType)}</span>` : ''}
      </div>

      <div style="margin-bottom:8px">
        <div style="display:flex;align-items:center;gap:6px;margin-bottom:6px">
          <div class="kanban-card-avatar">${getInitial(assignee)}</div>
          <span style="font-size:12px;color:var(--it-color-text-secondary);font-weight:500">${esc(assignee)}</span>
          <span style="flex:1"></span>
          ${dueDate ? `
            <span style="font-size:11px;color:var(--it-color-text-quaternary);display:inline-flex;align-items:center;gap:3px">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                <line x1="16" y1="2" x2="16" y2="6"/>
                <line x1="8" y1="2" x2="8" y2="6"/>
                <line x1="3" y1="10" x2="21" y2="10"/>
              </svg>
              ${formatDate(dueDate)}
            </span>
          ` : ''}
        </div>
        <div style="display:flex;align-items:center;gap:8px">
          <div style="flex:1;height:4px;background:var(--it-color-bg-hover);border-radius:2px;overflow:hidden">
            <div style="height:100%;width:${progress}%;background:${progressColor(progress)};border-radius:2px;transition:width .3s ease"></div>
          </div>
          <span style="font-size:11px;color:var(--it-color-text-quaternary);min-width:30px;text-align:right">${progress}%</span>
        </div>
      </div>

      <div class="card-actions" style="display:flex;gap:4px;padding-top:8px;border-top:1px solid var(--it-color-border-light);opacity:0;transition:opacity .15s ease">
        <button class="btn" data-action="open-requirement" data-id="${esc(req.id)}" style="flex:1;font-size:11px;padding:4px 8px;background:var(--it-color-primary-bg);border:1px solid var(--it-color-primary-lighter);color:var(--it-color-primary)">打开需求子页 →</button>
      </div>
    </div>
  `;
}

// ==================== 状态流转 ====================

function getPrevStatus(statusKey) {
  const idx = boardColumns.findIndex(c => c.key === statusKey);
  if (idx > 0) return boardColumns[idx - 1].key;
  return null;
}

async function moveToNextStatus(id, btn) {
  const card = btn.closest('.kanban-card');
  const col = btn.closest('.kanban-column');
  if (!col) return;
  const currentStatus = col.dataset.status;
  const flow = statusFlow[currentStatus];
  if (!flow?.next) return;

  const targetStatus = flow.next;
  const targetLabel = boardColumns.find(c => c.key === targetStatus)?.label || '';

  if (!confirm(`确定要将需求移到「${targetLabel}」吗？`)) return;

  // 乐观 UI：先淡出再请求
  if (card) card.style.opacity = '0.4';

  try {
    // 根据目标状态调用对应的 API
    const apiMap = {
      PENDING_PLAN:    '/requirements/{id}/receive',
      IN_DEVELOPMENT:  '/requirements/{id}/plan',
      IN_TESTING:      '/requirements/{id}/start-testing',
      PENDING_RELEASE: '/requirements/{id}/finish-testing',
      COMPLETED:       '/requirements/{id}/release-success',
    };
    const apiPath = apiMap[targetStatus];

    if (apiPath) {
      await api(apiPath.replace('{id}', id), { method: 'POST' });
    }

    // 重新加载数据以确保状态正确
    loadBoardData();
  } catch (e) {
    if (card) card.style.opacity = '1';
    alert('操作失败：' + e.message + '\n请打开详情页进行完整操作');
  }
}

async function moveToPrevStatus(id, btn) {
  const col = btn.closest('.kanban-column');
  if (!col) return;
  const currentStatus = col.dataset.status;
  const prevStatus = getPrevStatus(currentStatus);
  if (!prevStatus) return;

  if (!confirm(`确定要将需求移回到「${boardColumns.find(c => c.key === prevStatus)?.label}」吗？`)) return;

  try {
    // 回退操作：直接重新加载
    loadBoardData();
  } catch (e) {
    alert('操作失败：' + e.message);
  }
}

// ==================== 详情抽屉 ====================

async function openRequirementDetail(id) {
  const drawer = openDrawer('需求详情', loadingState());
  const body = drawer.querySelector('.drawer-body');

  try {
    const res = await api(`/requirements/${id}`);
    const req = res?.data || res || {};

    body.innerHTML = renderDetailContent(req);
    bindDetailActions(req, drawer);
  } catch (e) {
    body.innerHTML = `<div class="empty-state"><div class="empty-icon">&#9776;</div><div class="empty-text">加载失败：${esc(e.message)}</div></div>`;
  }
}

function renderDetailContent(req) {
  const priority = req.urgency || req.priority || 'MEDIUM';
  const priorityInfo = priorityMap[priority] || priorityMap.MEDIUM;

  return `
    <div class="detail-section">
      <div class="detail-title">${esc(req.title || '无标题')}</div>
      <div class="detail-meta">
        <span class="status-pill status-${(req.status || '').toLowerCase()}">${statusLabel(req.status)}</span>
        <span class="tag ${priorityInfo.cls}">${priorityInfo.label}</span>
        <span style="color:var(--it-color-text-quaternary);font-size:12px">编号: ${esc(req.reqNo || req.id)}</span>
      </div>
    </div>

    <div class="detail-section">
      <div class="detail-grid">
        <div><span class="label">所属系统:</span> ${esc(req.belongSystem) || '-'}</div>
        <div><span class="label">需求类型:</span> ${esc(req.category || req.reqType) || '-'}</div>
        <div><span class="label">来源部门:</span> ${esc(req.sourceDept) || '-'}</div>
        <div><span class="label">提出人:</span> ${esc(req.proposer) || '-'}</div>
        <div><span class="label">开发负责人:</span> ${esc(req.devOwner) || '-'}</div>
        <div><span class="label">评审负责人:</span> ${esc(req.reviewOwner) || '-'}</div>
        <div><span class="label">计划开始:</span> ${formatDate(req.plannedStartDate)}</div>
        <div><span class="label">计划完成:</span> ${formatDate(req.plannedEndDate || req.devPlannedEnd)}</div>
        <div><span class="label">预估工时:</span> ${esc(req.estimatedHours || '-')} 小时</div>
        <div><span class="label">进度:</span>
          <div style="display:inline-flex;align-items:center;vertical-align:middle;gap:8px;min-width:120px">
            <div style="flex:1;height:6px;background:var(--it-color-bg-hover);border-radius:3px;overflow:hidden">
              <div style="height:100%;width:${req.progress || 0}%;background:var(--it-color-primary);border-radius:3px"></div>
            </div>
            <span style="font-size:12px;color:var(--it-color-text-tertiary)">${req.progress || 0}%</span>
          </div>
        </div>
        <div><span class="label">创建时间:</span> ${formatDate(req.createdAt)}</div>
        <div><span class="label">更新时间:</span> ${formatDate(req.updatedAt)}</div>
      </div>
    </div>

    ${req.currentProblem ? `
      <div class="detail-section">
        <div class="section-title">当前问题</div>
        <div class="detail-content">${esc(req.currentProblem)}</div>
      </div>
    ` : ''}

    ${req.expectedEffect ? `
      <div class="detail-section">
        <div class="section-title">预期效果</div>
        <div class="detail-content">${esc(req.expectedEffect)}</div>
      </div>
    ` : ''}

    ${req.acceptanceCriteria ? `
      <div class="detail-section">
        <div class="section-title">验收标准</div>
        <div class="detail-content">${esc(req.acceptanceCriteria)}</div>
      </div>
    ` : ''}

    ${req.remark ? `
      <div class="detail-section">
        <div class="section-title">备注</div>
        <div class="detail-content">${esc(req.remark)}</div>
      </div>
    ` : ''}
  `;
}

function bindDetailActions(req, drawer) {
  // 抽屉内的操作按钮（如有需要可扩展）
}

// ==================== 辅助函数 ====================

function statusLabel(status) {
  const map = {
    PENDING_RECEIVE: '待接收',
    PENDING_PLAN: '待排期',
    IN_DEVELOPMENT: '开发中',
    IN_TESTING: '测试中',
    PENDING_RELEASE: '待发布',
    COMPLETED: '已完成',
    REJECTED: '已驳回',
    DRAFT: '草稿',
  };
  return map[status] || status || '-';
}

function typeTagClass(type) {
  if (!type) return 'tag-type-task';
  const t = String(type).toLowerCase();
  if (t.includes('bug') || t.includes('缺陷') || t.includes('问题')) return 'tag-type-bug';
  if (t.includes('feature') || t.includes('功能') || t.includes('新需求')) return 'tag-type-feature';
  if (t.includes('improve') || t.includes('优化') || t.includes('改进')) return 'tag-type-improvement';
  return 'tag-type-task';
}

function progressColor(value) {
  const v = value || 0;
  if (v >= 100) return '#22c55e';
  if (v >= 70) return '#3b82f6';
  if (v >= 30) return '#f59e0b';
  return '#ef4444';
}

function getInitial(name) {
  if (!name || name === '-') return '?';
  const n = String(name).trim();
  if (!n) return '?';
  // 取第一个字符（支持中英文）
  return n.charAt(0).toUpperCase();
}

function renderEmptyState(text) {
  return `
    <div style="display:flex;flex-direction:column;align-items:center;justify-content:center;padding:32px 16px;text-align:center;flex:1">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="color:var(--it-color-text-quaternary);opacity:0.5;margin-bottom:12px">
        <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
        <line x1="9" y1="9" x2="15" y2="15"/>
        <line x1="15" y1="9" x2="9" y2="15"/>
      </svg>
      <div style="font-size:12px;color:var(--it-color-text-tertiary)">${esc(text)}</div>
    </div>
  `;
}
