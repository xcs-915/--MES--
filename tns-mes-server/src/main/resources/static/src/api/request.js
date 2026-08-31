/**
 * API Request Module
 * Base HTTP client with interceptors (similar to D:\TNS utils/request.js)
 * Handles authentication, error handling, and JSON serialization
 */

import { apiBase } from '../config/index.js';
import { state, setToken } from '../store/index.js';
import { t } from '../i18n/index.js';

/**
 * Core API request function
 * @param {string} path - API path (without base URL)
 * @param {object} options - fetch options (method, body, headers)
 * @returns {Promise<object>} - Response JSON payload
 */
export async function api(path, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  };
  if (state.token) headers.Authorization = `Bearer ${state.token}`;

  const fullUrl = `${apiBase()}${path}`;
  const method = options.method || 'GET';
  const bodyData = options.body === undefined ? undefined : JSON.stringify(options.body);

  // Print complete request info to browser console
  console.groupCollapsed(`%c[API] ${method} ${path}`, 'color:#7C3AED;font-weight:bold');
  console.log('Full URL:', fullUrl);
  console.log('Method:', method);
  console.log('Headers:', { ...headers, Authorization: headers.Authorization ? 'Bearer ***' : '(none)' });
  if (options.body !== undefined) {
    console.log('Request Body:', options.body);
    console.log('Request Body (JSON):', bodyData);
  }
  console.time(`[API] ${path}`);

  const res = await fetch(fullUrl, {
    ...options,
    method,
    headers,
    body: bodyData,
  });

  console.timeEnd(`[API] ${path}`);

  let payload = null;
  try { payload = await res.json(); } catch {}

  console.log('Response Status:', res.status, res.statusText);
  console.log('Response Data:', payload);
  console.groupEnd();

  if (res.status === 401) {
    handleUnauthorized();
    throw new Error(t('invalidLogin'));
  }
  if (!res.ok || (payload && payload.code && payload.code !== 0)) {
    console.error(`[API ERROR] ${method} ${path}`, { status: res.status, payload });
    throw new Error(payload?.message || t('failedRequest'));
  }
  return payload;
}

/** Handle 401 by clearing token and redirecting to login */
function handleUnauthorized() {
  setToken('');
  const loginView = document.getElementById('login-view');
  const workspaceView = document.getElementById('workspace-view');
  if (loginView) loginView.classList.remove('hidden');
  if (workspaceView) workspaceView.classList.add('hidden');
}

/** Get menu configuration (field/button metadata) */
export async function getMenuConfig(code) {
  if (state.menuConfigs[code]) return state.menuConfigs[code];
  const menus = state.data.menus || (await api('/system/menus').catch(() => ({ data: [] }))).data || [];
  const menu = menus.find(m => m.code === code);
  if (!menu) return null;
  const config = (await api('/system/menus/' + menu.id + '/config').catch(() => ({ data: null }))).data;
  if (config) state.menuConfigs[code] = config;
  return config;
}
