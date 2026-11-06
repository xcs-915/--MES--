/** Simple reactive store */

const initialState = {
  view: 'dashboard',
  user: null,
  token: null,
  currentReqId: null,
  requirements: { items: [], total: 0, page: 0, size: 20 },
  tasks: { items: [], total: 0, page: 0, size: 20 },
};

export const state = new Proxy(JSON.parse(JSON.stringify(initialState)), {
  get(target, key) { return target[key]; },
  set(target, key, value) {
    target[key] = value;
    return true;
  }
});

export function setToken(token) {
  state.token = token;
  if (token) localStorage.setItem('itrm_token', token);
  else localStorage.removeItem('itrm_token');
}

export function getToken() {
  return state.token || localStorage.getItem('itrm_token');
}

export function setUser(user) {
  state.user = user;
  if (user) localStorage.setItem('itrm_user', JSON.stringify(user));
  else localStorage.removeItem('itrm_user');
}

export function getUser() {
  if (state.user) return state.user;
  try {
    const u = localStorage.getItem('itrm_user');
    if (u) { state.user = JSON.parse(u); return state.user; }
  } catch {}
  return null;
}

export function getPermissions() {
  const user = getUser();
  return Array.isArray(user?.permissions) ? user.permissions : [];
}

export function hasPermission(code) {
  if (!code) return true;
  return getPermissions().includes(code);
}

/** 设置当前需求 ID */
export function setCurrentReqId(id) {
  state.currentReqId = id;
  if (id !== null && id !== undefined) {
    sessionStorage.setItem('itrm_current_req_id', String(id));
  } else {
    sessionStorage.removeItem('itrm_current_req_id');
  }
}

/** 获取当前需求 ID */
export function getCurrentReqId() {
  if (state.currentReqId !== null && state.currentReqId !== undefined) {
    return state.currentReqId;
  }
  const cached = sessionStorage.getItem('itrm_current_req_id');
  if (cached !== null) {
    state.currentReqId = isNaN(Number(cached)) ? cached : Number(cached);
    return state.currentReqId;
  }
  return null;
}

/** 清空状态（登出时使用） */
export function resetState() {
  Object.assign(state, JSON.parse(JSON.stringify(initialState)));
  localStorage.removeItem('itrm_token');
  localStorage.removeItem('itrm_user');
  sessionStorage.removeItem('itrm_current_req_id');
}
