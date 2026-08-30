/**
 * Format Utilities
 * Date formatting, path reading, and meta localization helpers
 */

import { state } from '../store/index.js';

export function formatDate(v) {
  if (!v) return '—';
  const locale = state.lang === 'zh-CN' ? 'zh-CN' : state.lang === 'ar-TN' ? 'ar-TN' : 'en-GB';
  return new Date(v).toLocaleString(locale, { dateStyle: 'short', timeStyle: 'short' });
}

export function readPath(obj, path) {
  return String(path || '').split('.').reduce((v, key) => v == null ? undefined : v[key], obj);
}

export function localizedMeta(meta, prefix = 'label') {
  if (state.lang === 'ar-TN') return meta[prefix + 'Ar'] || meta[prefix + 'En'] || meta[prefix + 'Zh'];
  if (state.lang === 'en') return meta[prefix + 'En'] || meta[prefix + 'Zh'];
  return meta[prefix + 'Zh'];
}
