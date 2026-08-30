/**
 * Overview View
 * Dashboard with key statistics
 */

import { $ } from '../utils/dom.js';
import { renderIcons } from '../utils/dom.js';
import { api } from '../api/request.js';
import { t } from '../i18n/index.js';
import { pageHead } from '../components/toolbar.js';
import { statCard, detailGrid, emptyState } from '../components/feedback.js';

export async function renderOverview() {
  const node = $('#page');
  node.innerHTML = pageHead(t('overview'), t('overview'), '');
  try {
    const [products, orders] = await Promise.all([
      api('/products?size=1'),
      api('/work-orders?size=1'),
    ]);
    node.innerHTML += `<div class="stat-grid">${statCard(t('products'), products.data.total || 0, 'package-search')}${statCard(t('workOrders'), orders.data.total || 0, 'list-checks')}${statCard(t('integrationCenter'), 2, 'plug-zap')}${statCard(t('status'), '<span class="status success">ONLINE</span>', 'activity')}</div><div class="panel"><div class="panel-body">${detailGrid([[t('products'), t('productSubtitle')], [t('workOrders'), t('orderSubtitle')]])}</div></div>`;
  } catch (e) {
    node.innerHTML += emptyState(e.message);
  }
  renderIcons();
}
