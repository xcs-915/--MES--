/** API request module */
import { state, getToken } from '../store/index.js';

const baseUrl = '/itrm/api';

// ---------- 错误事件总线 ----------

const errorListeners = new Set();

/** 注册全局错误监听器 */
export function onApiError(listener) {
  errorListeners.add(listener);
  return () => errorListeners.delete(listener);
}

function emitError(err, info = {}) {
  errorListeners.forEach(fn => {
    try { fn(err, info); } catch {}
  });
}

// ---------- HTTP 状态码友好消息 ----------

const statusMessages = {
  400: '请求参数错误',
  401: '登录已过期，请重新登录',
  403: '没有操作权限',
  404: '请求的资源不存在',
  405: '请求方法不允许',
  408: '请求超时',
  409: '数据冲突',
  413: '请求内容过大',
  422: '数据校验失败',
  429: '请求过于频繁，请稍后再试',
  500: '服务器内部错误',
  502: '网关错误',
  503: '服务不可用',
  504: '网关超时',
};

// ---------- 核心请求函数 ----------

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
  const silent = options.silent === true;

  // AbortController 用于超时
  const controller = new AbortController();
  const timeout = options.timeout || 30000;
  const timeoutId = setTimeout(() => controller.abort(), timeout);

  try {
    const res = await fetch(fullUrl, {
      ...options,
      method,
      headers,
      body: bodyData,
      signal: controller.signal,
    });

    clearTimeout(timeoutId);

    // 401 未授权：清除本地登录信息并刷新
    if (res.status === 401) {
      localStorage.removeItem('itrm_token');
      localStorage.removeItem('itrm_user');
      sessionStorage.removeItem('itrm_current_req_id');
      if (!silent) {
        emitError(new Error('登录已过期，请重新登录'), { status: 401, path });
      }
      // 延迟刷新，让错误提示有时间展示
      setTimeout(() => window.location.reload(), 300);
      throw new Error('登录已过期');
    }

    let payload = null;
    try { payload = await res.json(); } catch {}

    // 业务错误码
    if (!res.ok || (payload && payload.code !== undefined && payload.code !== 0)) {
      const message = payload?.message || statusMessages[res.status] || '请求失败';
      const err = new Error(message);
      err.status = res.status;
      err.code = payload?.code;
      err.payload = payload;
      if (!silent) {
        emitError(err, { status: res.status, path, method });
      }
      throw err;
    }

    return payload;
  } catch (err) {
    // 超时
    if (err.name === 'AbortError') {
      const timeoutErr = new Error('请求超时，请检查网络连接');
      timeoutErr.status = 408;
      if (!silent) {
        emitError(timeoutErr, { status: 408, path, method });
      }
      throw timeoutErr;
    }

    // 网络错误
    if (err.message === 'Failed to fetch' || err.type === 'failed') {
      const netErr = new Error('网络连接失败，请检查网络');
      netErr.status = 0;
      if (!silent) {
        emitError(netErr, { status: 0, path, method });
      }
      throw netErr;
    }

    // 其他已处理的错误（上面 throw 的）
    if (err.status) throw err;

    // 未知错误
    if (!silent) {
      emitError(err, { path, method });
    }
    throw err;
  }
}

// ---------- 便捷方法 ----------

api.get = (path, params, options = {}) => {
  const query = params ? '?' + new URLSearchParams(params).toString() : '';
  return api(path + query, { ...options, method: 'GET' });
};

api.post = (path, body, options = {}) => {
  return api(path, { ...options, method: 'POST', body });
};

api.put = (path, body, options = {}) => {
  return api(path, { ...options, method: 'PUT', body });
};

api.del = (path, options = {}) => {
  return api(path, { ...options, method: 'DELETE' });
};
