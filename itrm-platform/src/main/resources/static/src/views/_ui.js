import { $, esc } from '../utils/dom.js';

const statusMap = {
  DRAFT: '草稿',
  PENDING_RECEIVE: '待接收',
  PENDING_PLAN: '待排期',
  IN_DEVELOPMENT: '开发中',
  IN_TESTING: '测试中',
  PENDING_RELEASE: '待发布',
  COMPLETED: '已完成',
  REJECTED: '已驳回',
};

const priorityMap = {
  URGENT: '紧急',
  HIGH: '高',
  MEDIUM: '中',
  LOW: '低',
};

export function statusPill(status) {
  const label = statusMap[status] || status || '-';
  const cls = status ? `status-${status.toLowerCase()}` : 'status-default';
  return `<span class="status-pill ${cls}">${label}</span>`;
}

export function priorityPill(priority) {
  const label = priorityMap[priority] || priority || '-';
  const cls = priority ? `priority-${priority.toLowerCase()}` : 'priority-default';
  return `<span class="status-pill ${cls}">${label}</span>`;
}

export function progressBar(value) {
  const p = Math.max(0, Math.min(100, value || 0));
  return `<div class="progress-bar"><div class="fill" style="width:${p}%"></div></div>
    <span style="font-size:12px;color:var(--text-muted);margin-left:8px">${p}%</span>`;
}

export function pagination(p, onPage) {
  const pages = [];
  const total = p.totalPages || 1;
  const current = p.page;
  for (let i = 0; i < total; i++) {
    if (i === 0 || i === total - 1 || Math.abs(i - current) <= 1) pages.push(i);
    else if (pages[pages.length-1] !== '...') pages.push('...');
  }
  return `<div class="pagination">
    <span>共 ${p.total} 条</span>
    <button ${current === 0 ? 'disabled' : ''} data-page="${current - 1}">&laquo;</button>
    ${pages.map(pg => pg === '...' ? '<span>...</span>' :
      `<button class="${pg === current ? 'active' : ''}" data-page="${pg}">${pg + 1}</button>`).join('')}
    <button ${current >= total - 1 ? 'disabled' : ''} data-page="${current + 1}">&raquo;</button>
  </div>`;
}

export function openModal(title, bodyHtml, onConfirm, confirmText) {
  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay';
  overlay.innerHTML = `<div class="modal">
    <div class="modal-header"><div class="modal-title">${esc(title)}</div>
      <button class="modal-close">&times;</button></div>
    <div class="modal-body">${bodyHtml}</div>
    <div class="modal-footer">
      <button class="btn modal-cancel">取消</button>
      <button class="btn primary modal-confirm">${confirmText || '确定'}</button>
    </div></div>`;
  document.body.appendChild(overlay);
  const close = () => overlay.remove();
  overlay.querySelector('.modal-close').onclick = close;
  overlay.querySelector('.modal-cancel').onclick = close;
  overlay.querySelector('.modal-confirm').onclick = async () => {
    try { await onConfirm(overlay); close(); } catch(e) { alert(e.message); }
  };
  overlay.onclick = e => { if (e.target === overlay) close(); };
  return overlay;
}

export function openDrawer(title, bodyHtml) {
  const overlay = document.createElement('div');
  overlay.className = 'drawer-overlay';
  overlay.innerHTML = `<div class="drawer">
    <div class="drawer-header"><div class="drawer-title">${esc(title)}</div>
      <button class="drawer-close">&times;</button></div>
    <div class="drawer-body">${bodyHtml}</div>
  </div>`;
  document.body.appendChild(overlay);
  const close = () => overlay.remove();
  overlay.querySelector('.drawer-close').onclick = close;
  overlay.onclick = e => { if (e.target === overlay) close(); };
  return overlay;
}

export function emptyState(text) {
  return `<div class="empty-state"><div class="empty-icon">&#9776;</div><div class="empty-text">${esc(text || '暂无数据')}</div></div>`;
}

export function loadingState() {
  return `<div class="loading-state"><div class="spinner"></div><span>加载中...</span></div>`;
}
