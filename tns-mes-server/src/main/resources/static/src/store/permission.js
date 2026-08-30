/**
 * Permission State
 * Handles permission-based visibility for UI elements
 */

import { $$ } from '../utils/dom.js';
import { getPermissions } from './index.js';

export function applyPermissions() {
  const permissions = getPermissions();
  $$('[data-permission]').forEach(node => {
    const required = node.dataset.permission;
    const has = !required || permissions.includes(required) || permissions.includes('USER_ADMIN');
    node.classList.toggle('hidden', !has);
  });
}
