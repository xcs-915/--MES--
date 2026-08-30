/**
 * i18n Manager
 * Handles language switching and translation lookup
 * Similar to vue-i18n in D:\TNS but lightweight
 */

import { state } from '../store/index.js';
import { $$ } from '../utils/dom.js';
import zhCN from './zh-CN.js';
import en from './en.js';
import arTN from './ar-TN.js';

const translations = {
  'zh-CN': zhCN,
  'en': en,
  'ar-TN': arTN,
};

export function t(key) {
  return translations[state.lang]?.[key] || translations['zh-CN'][key] || key;
}

export function setLanguage(lang) {
  state.lang = lang;
  localStorage.setItem('tns_lang', lang);
  document.documentElement.lang = lang;
  document.documentElement.dir = lang === 'ar-TN' ? 'rtl' : 'ltr';
  $$('[data-i18n]').forEach(n => n.textContent = t(n.dataset.i18n));
  $$('[data-i18n-placeholder]').forEach(n => n.placeholder = t(n.dataset.i18nPlaceholder));
  $$('[data-lang]').forEach(n => n.classList.toggle('active', n.dataset.lang === lang));
}
