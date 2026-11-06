/** API request module */
import { state, getToken } from '../store/index.js';

const baseUrl = '/itrm/api';

export async function api(path, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  };
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  const fullUrl = `${baseUrl}${path}`;
  const method = options.method || 'GET';
  const bodyData = options.body === undefined ? undefined : JSON.stringify(options.body);

  try {
    const res = await fetch(fullUrl, {
      ...options,
      method,
      headers,
      body: bodyData,
    });

    if (res.status === 401) {
      localStorage.removeItem('itrm_token');
      localStorage.removeItem('itrm_user');
      window.location.reload();
      throw new Error('登录已过期');
    }

    let payload = null;
    try { payload = await res.json(); } catch {}

    if (!res.ok || (payload && payload.code !== undefined && payload.code !== 0)) {
      throw new Error(payload?.message || '请求失败');
    }

    return payload;
  } catch (err) {
    if (err.message === 'Failed to fetch') {
      throw new Error('网络连接失败');
    }
    throw err;
  }
}
