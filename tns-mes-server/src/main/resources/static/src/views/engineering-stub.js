/**
 * Engineering Stub View
 * Placeholder for BOM, routes, quality views not yet implemented
 */

import { $ } from '../utils/dom.js';
import { state } from '../store/index.js';
import { t } from '../i18n/index.js';
import { pageHead } from '../components/toolbar.js';

export function renderEngineeringStub() {
  const title = t(state.view);
  $('#page').innerHTML = pageHead(t('engineering'), title, t('engineering'));
}
