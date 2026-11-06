/**
 * Global UI Components
 * 提供全局 UI 组件：Toast、Modal、Drawer、Confirm、状态徽章、空状态等
 */

import { $, esc } from '../utils/dom.js';

// ---------- 状态/优先级映射 ----------

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

// ---------- 内部状态 ----------

let currentModal = null;
let currentDrawer = null;
let toastContainer = null;

function ensureToastContainer() {
  if (!toastContainer) {
    toastContainer = document.createElement('div');
    toastContainer.className = 'toast-container';
    document.body.appendChild(toastContainer);
  }
  return toastContainer;
}

// ---------- Toast ----------

/**
 * 右上角 Toast 通知
 * @param {string} message - 消息内容
 * @param {'success'|'error'|'warning'|'info'} [type='info'] - 类型
 * @param {number} [duration=3000] - 持续时间(ms)
 */
export function showToast(message, type = 'info', duration = 3000) {
  const container = ensureToastContainer();
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `
    <span class="toast-icon">${toastIcon(type)}</span>
    <span class="toast-message">${esc(message)}</span>
    <button class="toast-close">&times;</button>
  `;
  container.appendChild(toast);

  // 触发入场动画
  requestAnimationFrame(() => toast.classList.add('show'));

  const close = () => {
    toast.classList.remove('show');
    setTimeout(() => toast.remove(), 300);
  };

  toast.querySelector('.toast-close').onclick = close;
  const timer = setTimeout(close, duration);
  toast.addEventListener('mouseenter', () => clearTimeout(timer));

  return toast;
}

function toastIcon(type) {
  const icons = {
    success: '&#10003;',
    error: '&#10005;',
    warning: '&#9888;',
    info: '&#8505;',
  };
  return icons[type] || icons.info;
}

// ---------- Modal ----------

/**
 * 居中弹窗
 * @param {Object} opts
 * @param {string} opts.title - 标题
 * @param {string} opts.body - 内容 HTML
 * @param {string} [opts.footer] - 底部 HTML（可选，不传则使用默认的"取消/确定"按钮）
 * @param {Function} [opts.onClose] - 关闭回调
 * @returns {HTMLElement} modal 元素
 */
export function showModal({ title, body, footer, onClose }) {
  closeModal();

  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay';

  const footerHtml = footer !== undefined
    ? footer
    : `<button class="btn modal-cancel">取消</button>
       <button class="btn primary modal-confirm">确定</button>`;

  overlay.innerHTML = `
    <div class="modal">
      <div class="modal-header">
        <div class="modal-title">${esc(title)}</div>
        <button class="modal-close">&times;</button>
      </div>
      <div class="modal-body">${body || ''}</div>
      <div class="modal-footer">${footerHtml}</div>
    </div>
  `;

  document.body.appendChild(overlay);
  currentModal = overlay;

  const close = () => {
    if (currentModal === overlay) {
      overlay.remove();
      currentModal = null;
      if (typeof onClose === 'function') onClose();
    }
  };

  overlay.querySelector('.modal-close').onclick = close;
  overlay.onclick = e => { if (e.target === overlay) close(); };

  // 默认取消按钮
  const cancelBtn = overlay.querySelector('.modal-cancel');
  if (cancelBtn) cancelBtn.onclick = close;

  return overlay;
}

/** 关闭当前弹窗 */
export function closeModal() {
  if (currentModal) {
    currentModal.remove();
    currentModal = null;
  }
}

// ---------- Drawer ----------

/**
 * 右侧抽屉
 * @param {Object} opts
 * @param {string} opts.title - 标题
 * @param {string} opts.content - 内容 HTML
 * @param {string} [opts.width='480px'] - 抽屉宽度
 * @param {Function} [opts.onClose] - 关闭回调
 * @returns {HTMLElement} drawer 元素
 */
export function showDrawer({ title, content, width = '480px', onClose }) {
  closeDrawer();

  const overlay = document.createElement('div');
  overlay.className = 'drawer-overlay';

  overlay.innerHTML = `
    <div class="drawer" style="width:${esc(width)}">
      <div class="drawer-header">
        <div class="drawer-title">${esc(title)}</div>
        <button class="drawer-close">&times;</button>
      </div>
      <div class="drawer-body">${content || ''}</div>
    </div>
  `;

  document.body.appendChild(overlay);
  currentDrawer = overlay;

  const close = () => {
    if (currentDrawer === overlay) {
      overlay.remove();
      currentDrawer = null;
      if (typeof onClose === 'function') onClose();
    }
  };

  overlay.querySelector('.drawer-close').onclick = close;
  overlay.onclick = e => { if (e.target === overlay) close(); };

  // 触发入场动画
  requestAnimationFrame(() => overlay.classList.add('show'));

  return overlay;
}

/** 关闭当前抽屉 */
export function closeDrawer() {
  if (currentDrawer) {
    currentDrawer.remove();
    currentDrawer = null;
  }
}

// ---------- Confirm ----------

/**
 * 确认对话框
 * @param {Object} opts
 * @param {string} opts.title - 标题
 * @param {string} opts.message - 消息内容
 * @param {Function} [opts.onOk] - 确定回调
 * @param {Function} [opts.onCancel] - 取消回调
 * @param {string} [opts.okText='确定'] - 确定按钮文字
 * @param {string} [opts.cancelText='取消'] - 取消按钮文字
 */
export function confirm({ title, message, onOk, onCancel, okText = '确定', cancelText = '取消' }) {
  const body = `<div class="confirm-message">${esc(message)}</div>`;
  const footer = `
    <button class="btn confirm-cancel">${esc(cancelText)}</button>
    <button class="btn primary confirm-ok">${esc(okText)}</button>
  `;

  const modal = showModal({
    title,
    body,
    footer,
    onClose: () => { if (typeof onCancel === 'function') onCancel(); },
  });

  modal.querySelector('.confirm-cancel').onclick = () => {
    closeModal();
    if (typeof onCancel === 'function') onCancel();
  };

  modal.querySelector('.confirm-ok').onclick = async () => {
    try {
      if (typeof onOk === 'function') await onOk();
      closeModal();
    } catch (e) {
      showToast(e.message || '操作失败', 'error');
    }
  };

  return modal;
}

// ---------- 状态徽章 ----------

/**
 * 状态徽章 HTML
 * @param {string} status - 状态值
 * @returns {string} HTML 字符串
 */
export function statusBadge(status) {
  const label = statusMap[status] || status || '-';
  const cls = status ? `status-badge status-${status.toLowerCase()}` : 'status-badge status-default';
  return `<span class="${cls}">${label}</span>`;
}

/**
 * 优先级徽章 HTML
 * @param {string} priority - 优先级值
 * @returns {string} HTML 字符串
 */
export function priorityBadge(priority) {
  const label = priorityMap[priority] || priority || '-';
  const cls = priority ? `priority-badge priority-${priority.toLowerCase()}` : 'priority-badge priority-default';
  return `<span class="${cls}">${label}</span>`;
}

// ---------- 空状态 ----------

/**
 * 空状态占位
 * @param {string} [icon] - 图标（HTML 实体或字符）
 * @param {string} [text='暂无数据'] - 提示文字
 * @param {string} [action] - 操作按钮 HTML
 * @returns {string} HTML 字符串
 */
export function emptyState(icon, text = '暂无数据', action) {
  const iconHtml = icon
    ? `<div class="empty-icon">${icon}</div>`
    : `<div class="empty-icon">&#9776;</div>`;
  const actionHtml = action ? `<div class="empty-action">${action}</div>` : '';
  return `
    <div class="empty-state">
      ${iconHtml}
      <div class="empty-text">${esc(text)}</div>
      ${actionHtml}
    </div>
  `;
}

// ---------- 兼容旧 API（保留向后兼容） ----------

export function statusPill(status) {
  return statusBadge(status);
}

export function priorityPill(priority) {
  return priorityBadge(priority);
}

export function openModal(title, bodyHtml, onConfirm, confirmText) {
  const modal = showModal({
    title,
    body: bodyHtml,
    footer: `
      <button class="btn modal-cancel">取消</button>
      <button class="btn primary modal-confirm">${esc(confirmText || '确定')}</button>
    `,
  });
  const confirmBtn = modal.querySelector('.modal-confirm');
  if (confirmBtn && onConfirm) {
    confirmBtn.onclick = async () => {
      try {
        await onConfirm(modal);
        closeModal();
      } catch (e) {
        showToast(e.message || '操作失败', 'error');
      }
    };
  }
  return modal;
}

export function openDrawer(title, bodyHtml) {
  return showDrawer({ title, content: bodyHtml });
}

export function progressBar(value) {
  const p = Math.max(0, Math.min(100, value || 0));
  return `
    <div class="progress-bar"><div class="fill" style="width:${p}%"></div></div>
    <span style="font-size:12px;color:var(--text-muted);margin-left:8px">${p}%</span>
  `;
}

export function pagination(p, onPage) {
  const pages = [];
  const total = p.totalPages || 1;
  const current = p.page;
  for (let i = 0; i < total; i++) {
    if (i === 0 || i === total - 1 || Math.abs(i - current) <= 1) pages.push(i);
    else if (pages[pages.length - 1] !== '...') pages.push('...');
  }
  return `
    <div class="pagination">
      <span>共 ${p.total} 条</span>
      <button ${current === 0 ? 'disabled' : ''} data-page="${current - 1}">&laquo;</button>
      ${pages.map(pg => pg === '...'
        ? '<span>...</span>'
        : `<button class="${pg === current ? 'active' : ''}" data-page="${pg}">${pg + 1}</button>`
      ).join('')}
      <button ${current >= total - 1 ? 'disabled' : ''} data-page="${current + 1}">&raquo;</button>
    </div>
  `;
}

export function loadingState() {
  return `<div class="loading-state"><div class="spinner"></div><span>加载中...</span></div>`;
}
