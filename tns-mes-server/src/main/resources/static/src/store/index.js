/**
 * TNS-MES Store
 * Centralized state management (similar to Vuex in D:\TNS)
 * Holds application state: token, user, language, view, and pagination data
 */

export const state = {
  token: sessionStorage.getItem('tns_token') || '',
  user: null,
  lang: localStorage.getItem('tns_lang') || 'zh-CN',
  view: 'overview',
  data: {},
  menuConfigs: {},
};

/** Initialize or get pagination state for a view */
export function ps(view, size = 20) {
  if (!state.data[view]) {
    state.data[view] = { items: [], page: 0, size, total: 0, totalPages: 0, sortKey: null, sortDir: 'asc' };
  }
  return state.data[view];
}

/** Set token and persist to session storage */
export function setToken(token) {
  state.token = token;
  if (token) sessionStorage.setItem('tns_token', token);
  else sessionStorage.removeItem('tns_token');
}

/** Set current user */
export function setUser(user) {
  state.user = user;
}

/** Get current user permissions */
export function getPermissions() {
  return state.user?.permissions || [];
}
