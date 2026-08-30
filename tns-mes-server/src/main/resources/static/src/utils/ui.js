/**
 * UI Utilities
 * Toast notifications and rendering helpers
 */

import { $ } from './dom.js';
import { config } from '../config/index.js';

export function toast(message, error = false) {
  const node = $('#toast');
  if (!node) return;
  node.textContent = message;
  node.className = `toast show${error ? ' error' : ''}`;
  clearTimeout(window.__toast);
  window.__toast = setTimeout(() => { node.className = 'toast'; }, config.toastDuration);
}
