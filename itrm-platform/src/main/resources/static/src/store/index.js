/** Simple reactive store */

const initialState = {
  view: 'dashboard',
  user: null,
  token: null,
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
