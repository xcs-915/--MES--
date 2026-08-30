/**
 * View Router
 * Maps view names to renderers, handles navigation state
 * Similar to Vue Router in D:\TNS but lightweight
 */

import { $, $$ } from '../utils/dom.js';
import { state } from '../store/index.js';
import { t } from '../i18n/index.js';
import { renderIcons } from '../utils/dom.js';
import { applyPermissions } from '../store/permission.js';

// View imports
import { renderOverview } from '../views/overview.js';
import { renderMaster } from '../views/master.js';
import { renderProducts, loadProducts } from '../views/products.js';
import { renderOrders, loadOrders } from '../views/orders.js';
import { renderBatches, loadBatches } from '../views/batches.js';
import { renderIntegration } from '../views/integration.js';
import { renderApiLogs, loadApiLogs } from '../views/api-logs.js';
import { renderJobs, loadJobs } from '../views/jobs.js';
import { renderIam, loadRoles, loadUsers } from '../views/iam.js';
import { renderMenus, loadMenus } from '../views/menus.js';
import { renderDictionaries, loadDictionaries } from '../views/dictionaries.js';
import { renderInterfaces, loadInterfaces } from '../views/interfaces.js';
import { renderEngineeringStub } from '../views/engineering-stub.js';

const viewTitles = {
  overview: 'overview', master: 'masterData', iam: 'iam', menus: 'menuManagement', dictionaries: 'dataDictionary',
  products: 'products', boms: 'engineering', routes: 'routes', quality: 'quality', batches: 'batches',
  orders: 'workOrders', integration: 'manualSync', jobs: 'scheduledJobs', apiLogs: 'apiLogs',
  interfaces: 'interfaceManagement'
};

const viewRenderers = {
  overview: renderOverview,
  master: renderMaster,
  iam: renderIam,
  menus: renderMenus,
  dictionaries: renderDictionaries,
  products: renderProducts,
  boms: renderEngineeringStub,
  routes: renderEngineeringStub,
  quality: renderEngineeringStub,
  batches: renderBatches,
  orders: renderOrders,
  integration: renderIntegration,
  jobs: renderJobs,
  apiLogs: renderApiLogs,
  interfaces: renderInterfaces,
};

/** Reload data for the current view */
export async function reloadCurrentView() {
  const loaders = {
    products: loadProducts,
    orders: loadOrders,
    batches: loadBatches,
    master: renderMaster, // master triggers its own load
    jobs: loadJobs,
    menus: loadMenus,
    dictionaries: loadDictionaries,
    interfaces: loadInterfaces,
    apiLogs: loadApiLogs,
  };

  if (state.view === 'iam') {
    const usersTab = $('.tab[data-tab="users"]')?.classList.contains('active');
    (usersTab ? loadUsers : loadRoles)();
    return;
  }

  const loader = loaders[state.view];
  if (loader) await loader();
}

/** Switch to a view and render it */
export function renderView(view) {
  state.view = view;
  $$('.nav-link').forEach(n => n.classList.toggle('active', n.dataset.view === view));

  // Auto-expand parent nav group of active view, collapse all others (accordion)
  const activeLink = document.querySelector(`[data-view="${view}"]`);
  const parentGroup = activeLink ? activeLink.closest('.nav-group') : null;
  $$('.nav-group').forEach(g => g.classList.add('collapsed'));
  if (parentGroup) {
    parentGroup.classList.remove('collapsed');
  } else {
    // Active view is a top-level item (e.g. overview). Expand the first
    // group so users can see that sub-menus exist and where to find things
    // like menu management, data dictionary, etc.
    const firstGroup = document.querySelector('.nav-group');
    if (firstGroup) firstGroup.classList.remove('collapsed');
  }

  $('#current-title').textContent = t(viewTitles[view] || view);
  const renderer = viewRenderers[view] || renderOverview;
  renderer();
  setTimeout(() => { applyPermissions(); renderIcons(); }, 0);
}
