/**
 * TNS-MES Configuration
 * Centralized configuration for API base URL and constants
 * Similar to D:\TNS window._CONFIG pattern
 */

export const config = {
  apiBaseUrl: (window.TNS_MES_CONFIG?.apiBaseUrl || '/tns-mes') + '/api/v1',
  contextPath: '/tns-mes',
  appName: 'TNS MES',
  defaultPageSize: 20,
  toastDuration: 3200,
};

export function apiBase() {
  return config.apiBaseUrl;
}
