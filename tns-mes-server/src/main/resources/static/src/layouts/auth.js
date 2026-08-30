/**
 * Auth Layout
 * Login, workspace show/hide, logout logic
 */

import { $ } from '../utils/dom.js';
import { state, setToken, setUser } from '../store/index.js';
import { api } from '../api/request.js';
import { t } from '../i18n/index.js';
import { renderIcons } from '../utils/dom.js';

export async function login() {
  const payload = await api('/auth/login', {
    method: 'POST',
    body: { username: $('#username').value, password: $('#password').value },
  });
  setToken(payload.data.accessToken);
  setUser(payload.data);
  showWorkspace(payload.data);
}

export async function restoreSession() {
  const payload = await api('/auth/me');
  setUser(payload.data);
  showWorkspace(payload.data);
  return payload.data;
}

export function showWorkspace(userData) {
  $('#login-view').classList.add('hidden');
  $('#workspace-view').classList.remove('hidden');
  $('#user-name').textContent = userData.displayName || userData.username;
  $('#user-role').textContent = (userData.permissions || []).includes('USER_ADMIN') ? 'MES Administrator' : 'MES Operator';
  $('#avatar').textContent = (userData.displayName || userData.username || 'U').slice(0, 1).toUpperCase();
  renderIcons();
}

export function logout(clear = true) {
  if (clear) {
    setToken('');
  }
  $('#login-view').classList.remove('hidden');
  $('#workspace-view').classList.add('hidden');
}
