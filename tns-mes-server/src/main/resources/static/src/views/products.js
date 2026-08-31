/**
 * Products View
 * SAP product master data (read-only) with menu-config-driven field rendering
 */

import { $, $$, esc, escVal, icon, renderIcons } from '../utils/dom.js';
import { state, ps } from '../store/index.js';
import { api, getMenuConfig } from '../api/request.js';
import { t } from '../i18n/index.js';
import { pageHead, btn, filterField, toolbar } from '../components/toolbar.js';
import { dataTable, paginationHTML } from '../components/table.js';
import { detailGrid, groupedDetailGrid, keyInfoCard, emptyState, statusPill, progressBar, sectionTitle } from '../components/feedback.js';
import { openDrawer, closeDrawer } from '../components/drawer.js';
import { toast } from '../utils/ui.js';
import { formatDate, readPath, localizedMeta } from '../utils/format.js';

export function renderProducts() {
  const fields = [
    filterField('product-search', t('search'), 'text', null, t('code') + '/' + t('name')),
    filterField('product-status', t('status'), 'select', [
      { value: 'all', label: t('all') },
      { value: 'ACTIVE', label: t('enabled') },
      { value: 'INACTIVE', label: t('disabled') }
    ]),
    filterField('product-type', t('type'), 'select', [
      { value: 'all', label: t('all') },
      { value: 'FINISHED', label: 'FINISHED' },
      { value: 'COMPONENT', label: 'COMPONENT' }
    ])
  ];
  const actions = btn('sync-products', icon('refresh-cw') + t('syncProducts'), 'primary', 'INTEGRATION_WRITE');
  $('#page').innerHTML = pageHead(t('engineering'), t('products'), t('productSubtitle'), '')
    + `<div class="panel">${toolbar(fields, actions)}<div id="product-table"></div></div>`;
  $('#product-status')?.addEventListener('change', () => loadProducts(0));
  $('#product-type')?.addEventListener('change', () => loadProducts(0));
  $('#product-search')?.addEventListener('keydown', e => { if (e.key === 'Enter') loadProducts(0); });
  loadProducts();
}

export async function loadProducts(page) {
  const p = ps('products');
  if (page !== undefined) p.page = page;
  const node = $('#product-table');
  if (!node) return;
  const params = new URLSearchParams();
  params.set('page', p.page);
  params.set('size', p.size);
  params.set('keyword', $('#product-search')?.value || '');
  const statusVal = $('#product-status')?.value || 'all';
  if (statusVal !== 'all') params.set('status', statusVal);
  const typeVal = $('#product-type')?.value || 'all';
  if (typeVal !== 'all') params.set('productType', typeVal);
  try {
    const data = await api('/products?' + params);
    let items = data.data.items || [];
    p.items = items;
    p.total = data.data.total || 0;
    p.totalPages = data.data.totalPages || Math.ceil(p.total / p.size) || 1;
    state.data.products = p;

    const defaultHeaders = [
      { key: 'code', label: t('code'), sortable: true },
      { key: 'name', label: t('name'), sortable: true },
      { key: 'productType', label: t('type'), sortable: true },
      { key: 'specification', label: t('specification'), sortable: true },
      { key: 'productModel', label: t('productModel'), sortable: true },
      { key: 'customerPartNumber', label: t('customerPartNumber'), sortable: true },
      { key: 'yy1ColorNumber', label: t('colorNumber'), sortable: true },
      { key: 'yy1Material', label: t('material'), sortable: true },
      { key: 'minPackagingQty', label: t('minPackagingQty'), sortable: true },
      { key: 'unit', label: t('unit'), sortable: true },
      { key: 'source', label: t('source'), sortable: false },
      { key: 'sapLastSyncAt', label: t('lastSync'), sortable: true },
      { key: 'actions', label: t('actions'), sortable: false }
    ];

    let headers = defaultHeaders;
    let configured = false;
    try {
      const config = await getMenuConfig('products');
      const cfgFields = config?.fields;
      if (cfgFields && cfgFields.length) {
        const visibleList = cfgFields.filter(f => f.listVisible);
        if (visibleList.length) {
          headers = visibleList.map(f => ({
            key: f.fieldCode || f.fieldPath,
            label: localizedMeta(f),
            sortable: f.sortable !== false
          }));
          headers.push({ key: 'actions', label: t('actions'), sortable: false });
          configured = true;
        }
      }
    } catch {}

    // client-side sort
    let sorted = items;
    if (p.sortKey && p.sortKey !== 'actions' && p.sortKey !== 'source') {
      const dir = p.sortDir === 'desc' ? -1 : 1;
      sorted = [...items].sort((a, b) => {
        const va = readPath(a, p.sortKey), vb = readPath(b, p.sortKey);
        if (va == null) return 1;
        if (vb == null) return -1;
        if (typeof va === 'number' && typeof vb === 'number') return (va - vb) * dir;
        return String(va).localeCompare(String(vb)) * dir;
      });
    }

    const renderCell = (key, v) => {
      switch (key) {
        case 'code':
          return `<span class="cell-title">${escVal(v.code)}</span>`;
        case 'name':
          return `<span class="cell-title">${escVal(localizedMeta(v, 'name'))}</span><span class="cell-sub">${escVal(v.nameZh || v.nameEn || v.nameAr)}</span>`;
        case 'source':
          return statusPill(v.source || 'SAP');
        case 'sapLastSyncAt':
          return formatDate(v.sapLastSyncAt);
        case 'actions':
          return `<div class="table-actions">${btn('product-detail', icon('eye'), 'ghost', 'ENGINEERING_READ')} ${btn('product-sync', icon('refresh-cw'), 'ghost', 'INTEGRATION_WRITE')}</div>`;
        default:
          return escVal(readPath(v, key));
      }
    };

    const keys = headers.map(h => h.key);
    const rows = sorted.map(v => {
      const cells = keys.map(k => `<td class="${k === 'code' ? 'code' : ''}">${renderCell(k, v)}</td>`).join('');
      return `<tr data-id="${esc(v.id)}">${cells}</tr>`;
    });

    node.innerHTML = dataTable(headers, rows, p.sortKey, p.sortDir) + paginationHTML(p);
  } catch (e) {
    node.innerHTML = emptyState(e.message);
  }
  renderIcons();
}

export async function openProductDetail(product) {
  let value = product;
  try { value = (await api('/products/' + product.id)).data; } catch {}

  const renderDetailValue = (key, v) => {
    switch (key) {
      case 'source': return statusPill(v.source || 'SAP');
      case 'sapLastSyncAt': case 'lastSync': return formatDate(v.sapLastSyncAt);
      case 'createdAt': return formatDate(v.sapCreatedAt || v.createdAt);
      case 'changedAt': return formatDate(v.sapChangedAt || v.updatedAt);
      case 'batchManaged': case 'traceable': case 'markedForDeletion':
        return v[key] ? t('enabled') : t('disabled');
      case 'grossWeight':
        return v.grossWeight != null ? `${v.grossWeight} ${v.weightUnit || ''}`.trim() : null;
      case 'netWeight':
        return v.netWeight != null ? `${v.netWeight} ${v.weightUnit || ''}`.trim() : null;
      case 'name': return localizedMeta(v, 'name');
      case 'type': return v.productType;
      default: return readPath(v, key);
    }
  };

  const sections = [
    {
      title: t('basicInfo'),
      fields: [
        [t('code'), renderDetailValue('code', value)],
        [t('name'), renderDetailValue('name', value)],
        [t('source'), renderDetailValue('source', value)],
        [t('type'), renderDetailValue('type', value)],
        [t('unit'), renderDetailValue('unit', value)],
        [t('specification'), renderDetailValue('specification', value)],
        [t('productModel'), renderDetailValue('productModel', value)],
        [t('customerPartNumber'), renderDetailValue('customerPartNumber', value)],
        [t('drawingNumber'), renderDetailValue('drawingNumber', value)],
        [t('brand'), renderDetailValue('brand', value)],
        [t('color'), renderDetailValue('color', value)],
        [t('productGroup'), renderDetailValue('productGroup', value)],
        [t('productOldId'), renderDetailValue('productOldId', value)],
        [t('productHierarchy'), renderDetailValue('productHierarchy', value)],
        [t('divisionCode'), renderDetailValue('divisionCode', value)]
      ]
    },
    {
      title: t('weightDimensions'),
      fields: [
        [t('grossWeight'), renderDetailValue('grossWeight', value)],
        [t('netWeight'), renderDetailValue('netWeight', value)],
        [t('countryOfOrigin'), renderDetailValue('countryOfOrigin', value)],
        [t('batchManaged'), renderDetailValue('batchManaged', value)],
        [t('traceable'), renderDetailValue('traceable', value)],
        [t('markedForDeletion'), renderDetailValue('markedForDeletion', value)]
      ]
    },
    {
      title: t('sapSystemInfo'),
      fields: [
        [t('crossPlantStatus'), renderDetailValue('crossPlantStatus', value)],
        [t('createdByUser'), renderDetailValue('createdByUser', value)],
        [t('lastChangedByUser'), renderDetailValue('lastChangedByUser', value)],
        [t('anpCode'), renderDetailValue('anpCode', value)],
        [t('industrySector'), renderDetailValue('industrySector', value)],
        [t('authorizationGroup'), renderDetailValue('authorizationGroup', value)],
        [t('materialRevisionLevel'), renderDetailValue('materialRevisionLevel', value)],
        [t('serialNumberProfile'), renderDetailValue('serialNumberProfile', value)],
        [t('manufacturerNumber'), renderDetailValue('manufacturerNumber', value)],
        [t('manufacturerPartNumber'), renderDetailValue('manufacturerPartNumber', value)]
      ]
    },
    {
      title: t('customFields'),
      fields: [
        [t('colorNumber'), renderDetailValue('yy1ColorNumber', value)],
        [t('fifoProsign'), renderDetailValue('yy1FifoProsign', value)],
        [t('moistureLevel'), renderDetailValue('yy1MoistureLevel', value)],
        [t('moistureSensitive'), renderDetailValue('yy1MoistureSensitive', value)],
        [t('shapeAndSize'), renderDetailValue('yy1ShapeAndSize', value)],
        [t('material'), renderDetailValue('yy1Material', value)],
        [t('brandM'), renderDetailValue('yy1BrandM', value)],
        [t('designer'), renderDetailValue('yy1Designer', value)],
        [t('cavity'), renderDetailValue('yy1Cavity', value)],
        [t('colorRegion'), renderDetailValue('yy1ColorRegion', value)],
        [t('plmPackageNumber'), renderDetailValue('yy1PlmPackageNumber', value)],
        [t('productTypeCustom'), renderDetailValue('yy1ProductTypeCustom', value)],
        [t('processTreatment'), renderDetailValue('yy1ProcessTreatment', value)],
        [t('descriptionOtp'), renderDetailValue('yy1DescriptionOtp', value)],
        [t('encapsulation'), renderDetailValue('yy1Encapsulation', value)],
        [t('project'), renderDetailValue('yy1Project', value)],
        [t('exteriorColor'), renderDetailValue('yy1ExteriorColor', value)]
      ]
    },
    {
      title: t('syncInfo'),
      fields: [
        [t('createdAt'), renderDetailValue('createdAt', value)],
        [t('changedAt'), renderDetailValue('changedAt', value)],
        [t('lastSync'), renderDetailValue('lastSync', value)]
      ]
    }
  ];

  openDrawer(`${t('products')} · ${value.code}`, t('productSubtitle'), `<div class="drawer-body">${keyInfoCard([
    { label: t('code'), value: value.code, icon: 'package' },
    { label: t('name'), value: localizedMeta(value, 'name'), icon: 'tag' },
    { label: t('type'), value: value.productType, icon: 'layers' },
    { label: t('specification'), value: value.specification, icon: 'ruler' },
    { label: t('customerPartNumber'), value: value.customerPartNumber, icon: 'hash' },
    { label: t('source'), value: statusPill(value.source || 'SAP'), icon: 'cloud' }
  ])}${groupedDetailGrid(sections)}</div>`);
}
