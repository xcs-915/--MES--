/**
 * ESOP View (Electronic SOP)
 * List + review workflow (DRAFT→SUBMITTED→APPROVED→PUBLISHED)
 * + page-based visual editor + print/PDF export following the SOP template layout
 */

import { $, $$, esc, escVal, icon, renderIcons } from '../utils/dom.js';
import { state, ps } from '../store/index.js';
import { api } from '../api/request.js';
import { t } from '../i18n/index.js';
import { pageHead, btn, filterField, toolbar } from '../components/toolbar.js';
import { dataTable, paginationHTML } from '../components/table.js';
import { keyInfoCard, groupedDetailGrid, emptyState, statusPill } from '../components/feedback.js';
import { openDrawer, closeDrawer } from '../components/drawer.js';
import { toast } from '../utils/ui.js';
import { formatDate, readPath } from '../utils/format.js';

const DOC_TYPES = ['ASSEMBLY', 'WEAVING', 'GENERAL'];
const STATUSES = ['DRAFT', 'SUBMITTED', 'APPROVED', 'PUBLISHED', 'OBSOLETE'];

const TABLE_PRESETS = () => ([
  { key: 'quality', title: t('tableQuality'), columns: [t('seqItem'), t('inspRequirement'), t('inspMethod'), t('freqQty'), t('recordForm'), t('responsible'), t('reactionPlan')] },
  { key: 'materials', title: t('tableMaterials'), columns: [t('sequence'), t('materialCol'), t('materialCode'), t('applicableModel'), t('usageQty')] },
  { key: 'equipment', title: t('tableEquipment'), columns: [t('sequence'), t('equipmentName'), t('code'), t('quantity'), t('remark')] },
  { key: 'tooling', title: t('tableTooling'), columns: [t('sequence'), t('toolingName'), t('code'), t('quantity'), t('remark')] },
  { key: 'apparatus', title: t('tableApparatus'), columns: [t('sequence'), t('apparatusName'), t('specification'), t('quantity')] },
  { key: 'post', title: t('tablePost'), columns: [t('sequence'), t('postProcessing')] },
  { key: 'appendix', title: t('tableAppendix'), columns: [t('sequence'), t('appendix')] },
  { key: 'custom', title: t('tableCustom'), columns: [t('col') + '1', t('col') + '2', t('col') + '3'] },
]);

function docTypeLabel(v) { return t('docType.' + v) || v; }

/* ================================================================
   LIST
   ================================================================ */

export function renderEsops() {
  const fields = [
    filterField('esop-search', t('search'), 'text', null, t('esopDocumentNo') + '/' + t('docTitle')),
    filterField('esop-status', t('status'), 'select', [{ value: 'all', label: t('all') }, ...STATUSES.map(s => ({ value: s, label: t('status.' + s) }))]),
    filterField('esop-product', t('productCode'), 'text', null, t('productCode')),
  ];
  const actions = btn('esop-add', icon('plus') + t('newEsop'), 'primary', 'ESOP_WRITE');
  $('#page').innerHTML = pageHead(t('engineering'), t('esops'), t('esopSubtitle'), '')
    + `<div class="panel">${toolbar(fields, actions)}<div id="esop-table"></div></div>`;
  $('#esop-status')?.addEventListener('change', () => loadEsops(0));
  $('#esop-search')?.addEventListener('keydown', e => { if (e.key === 'Enter') loadEsops(0); });
  $('#esop-product')?.addEventListener('keydown', e => { if (e.key === 'Enter') loadEsops(0); });
  loadEsops();
}

export async function loadEsops(page) {
  const p = ps('esops');
  if (page !== undefined) p.page = page;
  const node = $('#esop-table');
  if (!node) return;
  const params = new URLSearchParams();
  params.set('page', p.page);
  params.set('size', p.size);
  params.set('keyword', $('#esop-search')?.value?.trim() || '');
  const statusVal = $('#esop-status')?.value || 'all';
  if (statusVal !== 'all') params.set('status', statusVal);
  const productVal = $('#esop-product')?.value?.trim() || '';
  if (productVal) params.set('productCode', productVal);
  try {
    const data = await api('/esops?' + params);
    const items = data.data.items || [];
    p.items = items;
    p.total = data.data.total || 0;
    p.totalPages = data.data.totalPages || Math.ceil(p.total / p.size) || 1;
    state.data.esops = p;

    let sorted = items;
    if (p.sortKey && p.sortKey !== 'actions') {
      const dir = p.sortDir === 'desc' ? -1 : 1;
      sorted = [...items].sort((a, b) => {
        const va = readPath(a, p.sortKey), vb = readPath(b, p.sortKey);
        if (va == null) return 1;
        if (vb == null) return -1;
        if (typeof va === 'number' && typeof vb === 'number') return (va - vb) * dir;
        return String(va).localeCompare(String(vb)) * dir;
      });
    }

    const headers = [
      { key: 'documentNo', label: t('esopDocumentNo'), sortable: true },
      { key: 'versionCode', label: t('versionCode'), sortable: true },
      { key: 'title', label: t('docTitle'), sortable: true },
      { key: 'productCode', label: t('productCode'), sortable: true },
      { key: 'productName', label: t('productName'), sortable: true },
      { key: 'documentType', label: t('type'), sortable: true },
      { key: 'status', label: t('status'), sortable: true },
      { key: 'effectiveDate', label: t('effectiveDate'), sortable: true },
      { key: 'preparedBy', label: t('preparedBy'), sortable: true },
      { key: 'updatedAt', label: t('changedAt'), sortable: true },
      { key: 'actions', label: t('actions'), sortable: false },
    ];

    const renderCell = (key, v) => {
      switch (key) {
        case 'documentNo': return `<span class="cell-title">${escVal(v.documentNo)}</span>`;
        case 'status': return statusPill(v.status);
        case 'documentType': return docTypeLabel(v.documentType);
        case 'effectiveDate': return v.effectiveDate ? String(v.effectiveDate).slice(0, 10) : '—';
        case 'updatedAt': return formatDate(v.updatedAt);
        case 'actions': return `<div class="table-actions">${rowActions(v)}</div>`;
        default: return escVal(readPath(v, key));
      }
    };

    const keys = headers.map(h => h.key);
    const rows = sorted.map(v => `<tr data-id="${esc(v.id)}">${keys.map(k => `<td class="${k === 'documentNo' ? 'code' : ''}">${renderCell(k, v)}</td>`).join('')}</tr>`);
    node.innerHTML = dataTable(headers, rows, p.sortKey, p.sortDir) + paginationHTML(p);
  } catch (e) {
    node.innerHTML = emptyState(e.message);
  }
  renderIcons();
}

function rowActions(v) {
  const acts = [btn('esop-detail', icon('eye'), 'ghost', 'ESOP_READ'), btn('esop-print', icon('printer'), 'ghost', 'ESOP_READ')];
  if (v.status === 'DRAFT') {
    acts.push(btn('esop-edit', icon('pencil'), 'ghost', 'ESOP_WRITE'));
    acts.push(btn('esop-submit', icon('send'), 'ghost', 'ESOP_WRITE'));
    acts.push(btn('esop-delete', icon('trash-2'), 'ghost', 'ESOP_WRITE'));
  }
  if (v.status === 'SUBMITTED') {
    acts.push(btn('esop-approve', icon('check'), 'ghost', 'ESOP_REVIEW'));
    acts.push(btn('esop-reject', icon('x'), 'ghost', 'ESOP_REVIEW'));
  }
  if (v.status === 'APPROVED') acts.push(btn('esop-publish', icon('upload-cloud'), 'ghost', 'ESOP_PUBLISH'));
  if (v.status === 'PUBLISHED' || v.status === 'OBSOLETE') acts.push(btn('esop-revise', icon('copy-plus'), 'ghost', 'ESOP_WRITE'));
  return acts.join('');
}



/* ================================================================
   DETAIL DRAWER + REVIEW LOG
   ================================================================ */

export async function openEsopDetail(item) {
  let doc = item;
  try { doc = (await api('/esops/' + item.id)).data; } catch {}
  let logs = [];
  try { logs = (await api('/esops/' + item.id + '/reviews')).data || []; } catch {}

  const workflow = groupedDetailGrid([
    { title: t('basicInfo'), fields: [
      [t('esopDocumentNo'), doc.documentNo], [t('versionCode'), doc.versionCode], [t('docTitle'), doc.title],
      [t('productCode'), doc.productCode], [t('productName'), doc.productName], [t('type'), docTypeLabel(doc.documentType)],
      [t('effectiveDate'), doc.effectiveDate ? String(doc.effectiveDate).slice(0, 10) : null],
      [t('changeSummary'), doc.changeSummary], [t('preparedBy'), doc.preparedBy], [t('changedAt'), formatDate(doc.updatedAt)],
    ]},
    { title: t('reviewWorkflow'), fields: [
      [t('submittedAt'), formatDate(doc.submittedAt)],
      [t('reviewedBy'), doc.reviewedBy], [t('reviewedAt'), formatDate(doc.reviewedAt)],
      [t('approvedBy'), doc.approvedBy], [t('approvedAt'), formatDate(doc.approvedAt)],
      [t('publishedBy'), doc.publishedBy], [t('publishedAt'), formatDate(doc.publishedAt)],
      [t('reviewComment'), doc.reviewComment],
    ]},
  ]);

  const logHtml = logs.length
    ? `<div class="esop-log-list">${logs.map(l => `<div class="esop-log-item"><span class="status ${l.action === 'APPROVE' ? 'success' : 'danger'}">${esc(t('esopAction.' + l.action) || l.action)}</span><span class="esop-log-meta">${esc(l.actor)} · ${formatDate(l.createdAt)}</span>${l.comment ? `<p>${esc(l.comment)}</p>` : ''}</div>`).join('')}</div>`
    : emptyState();

  const content = `<div class="drawer-body">
    ${keyInfoCard([
      { label: t('esopDocumentNo'), value: doc.documentNo, icon: 'file-text' },
      { label: t('versionCode'), value: doc.versionCode, icon: 'git-branch' },
      { label: t('status'), value: statusPill(doc.status), icon: 'badge-check' },
      { label: t('docTitle'), value: doc.title, icon: 'type' },
      { label: t('productCode'), value: doc.productCode, icon: 'package' },
      { label: t('type'), value: docTypeLabel(doc.documentType), icon: 'layers' },
    ])}
    <div class="esop-detail-actions">
      ${btn('esop-print', icon('printer') + t('printExport'), 'secondary', 'ESOP_READ')}
      ${doc.status === 'DRAFT' ? btn('esop-edit', icon('pencil') + t('edit'), 'secondary', 'ESOP_WRITE') + btn('esop-submit', icon('send') + t('submitReview'), 'primary', 'ESOP_WRITE') : ''}
      ${doc.status === 'SUBMITTED' ? btn('esop-approve', icon('check') + t('approve'), 'primary', 'ESOP_REVIEW') + btn('esop-reject', icon('x') + t('reject'), 'danger', 'ESOP_REVIEW') : ''}
      ${doc.status === 'APPROVED' ? btn('esop-publish', icon('upload-cloud') + t('publish'), 'primary', 'ESOP_PUBLISH') : ''}
      ${doc.status === 'PUBLISHED' || doc.status === 'OBSOLETE' ? btn('esop-revise', icon('copy-plus') + t('revise'), 'secondary', 'ESOP_WRITE') : ''}
      ${doc.status === 'DRAFT' ? btn('esop-delete', icon('trash-2') + t('delete'), 'ghost', 'ESOP_WRITE') : ''}
    </div>
    ${workflow}
    <div class="drawer-section-title"><h3>${esc(t('reviewLog'))}</h3></div>
    ${logHtml}
    <div class="drawer-section-title"><h3>${esc(t('esopPreview'))}</h3></div>
    ${sopPreviewHtml(doc)}
  </div>`;

  openDrawer(`${t('esops')} · ${doc.documentNo}`, `${doc.title} · ${doc.versionCode}`, content);
  // detail actions carry the doc id (drawer has no <tr>)
  $$('.esop-detail-actions [data-action]').forEach(n => n.dataset.id = doc.id);
  renderIcons();
}

/* ================================================================
   SOP PREVIEW (compact, in drawer)
   ================================================================ */

function sopPreviewHtml(doc) {
  const content = doc.content || {};
  const pages = content.pages || [];
  if (!pages.length) return emptyState();
  return `<div class="esop-preview">${pages.map((pg, i) => {
    if (pg.type === 'product') {
      return `<div class="esop-preview-page">
        <div class="esop-preview-head">
          <strong>${esc(sopDocTitle(doc))}</strong>
          <span class="esop-preview-type">${esc(t('productShowcase'))}</span>
          <span class="esop-preview-page-no">${i + 1}/${pages.length}</span>
        </div>
        ${pg.model || pg.projectNo || pg.customerPartNo ? `<p class="esop-preview-work">${esc([pg.model, pg.projectNo, pg.customerPartNo].filter(Boolean).join(' · '))}</p>` : ''}
        ${pg.description ? `<p class="esop-preview-work">${esc(pg.description)}</p>` : ''}
        ${(pg.images || []).length ? `<div class="esop-preview-steps">${pg.images.map(im => `<div class="esop-preview-step"><img src="${esc(im)}" alt=""></div>`).join('')}</div>` : ''}
        ${(pg.remarks || []).length ? `<ul class="esop-preview-remarks">${pg.remarks.map(r => `<li>${esc(r)}</li>`).join('')}</ul>` : ''}
      </div>`;
    }
    return `<div class="esop-preview-page">
      <div class="esop-preview-head">
        <strong>${esc(sopDocTitle(doc))}</strong>
        <span>${esc(pg.processNo ? t('operation') + ' ' + pg.processNo : '')} ${esc(pg.processName || pg.title || '')}</span>
        <span class="esop-preview-page-no">${i + 1}/${pages.length}</span>
      </div>
      ${pg.processFlow ? `<p class="esop-preview-work">${esc(pg.processFlow)}</p>` : ''}
      ${pg.workContent ? `<p class="esop-preview-work">${esc(pg.workContent)}</p>` : ''}
      ${(pg.tables || []).map(tb => `<div class="esop-preview-table"><h4>${esc(tb.title)}</h4>
        <div class="table-wrap"><table class="data-table compact"><thead><tr>${(tb.columns || []).map(c => `<th>${escVal(c)}</th>`).join('')}</tr></thead>
        <tbody>${(tb.rows || []).map(r => `<tr>${(tb.columns || []).map((c, ci) => `<td>${escVal((r || [])[ci])}</td>`).join('')}</tr>`).join('')}</tbody></table></div></div>`).join('')}
      ${(pg.steps || []).length ? `<div class="esop-preview-steps">${pg.steps.map(s => `
        <div class="esop-preview-step">${s.image ? `<img src="${esc(s.image)}" alt="">` : '<div class="esop-noimg"></div>'}<span>${escVal(s.title)}</span></div>`).join('')}</div>` : ''}
      ${(pg.images || []).length ? `<div class="esop-preview-steps">${pg.images.map(im => `<div class="esop-preview-step"><img src="${esc(im)}" alt=""></div>`).join('')}</div>` : ''}
      ${(pg.remarks || []).length ? `<ul class="esop-preview-remarks">${pg.remarks.map(r => `<li>${esc(r)}</li>`).join('')}</ul>` : ''}
    </div>`;
  }).join('')}</div>`;
}

/* ================================================================
   PRINT / PDF EXPORT (iframe print, template layout)
   ================================================================ */

function sopDocTitle(doc) {
  return doc.documentType === 'ASSEMBLY' ? t('sopTitleAssembly')
    : doc.documentType === 'WEAVING' ? t('sopTitleWeaving') : t('sopTitleGeneral');
}

export function printEsop(doc) {
  const content = doc.content || {};
  const history = content.changeHistory || [];
  const pages = content.pages || [];
  const today = new Date().toISOString().slice(0, 10);
  const cols = arr => (arr || []).map(c => `<th>${esc(String(c ?? ''))}</th>`).join('');

  /* print order: history sheet (page 1) -> product pages -> process pages */
  const productPages = pages.filter(pg => pg.type === 'product');
  const processPages = pages.filter(pg => pg.type !== 'product');
  const totalPages = 1 + productPages.length + processPages.length;
  const title = sopDocTitle(doc);
  const pageNos = {};
  let pageNo = 1;
  productPages.forEach(pg => { pageNos[pages.indexOf(pg)] = ++pageNo; });
  processPages.forEach(pg => { pageNos[pages.indexOf(pg)] = ++pageNo; });

  /* page header table, shared across all sheets */
  const headTable = (pg, pno) => {
    const isProduct = pg && pg.type === 'product';
    return `<table class="phead">
      <tr>
        <td class="logo" rowspan="2">TAIKING</td>
        <td class="ptitle" rowspan="2">${esc(title)}</td>
        <th>${esc(t('productName'))}</th><td>${escVal(doc.productName)}</td>
        <th>${esc(t('standardTime'))}(S)</th><td>${escVal(pg ? pg.standardTime : '')}</td>
        <th>${esc(t('productCode'))}</th><td>${escVal(doc.productCode)}</td>
        <th>${esc(t('esopDocumentNo'))}</th><td>${escVal(doc.documentNo)}</td>
      </tr>
      <tr>
        <th>${esc(isProduct ? t('productShowcase') : t('stationAttr'))}</th><td>${escVal(isProduct ? '' : (pg ? pg.stationAttr : ''))}</td>
        <th>${esc(t('processNo'))}</th><td>${escVal(pg ? pg.processNo : '')}</td>
        <th>${esc(t('processName'))}</th><td>${escVal(pg ? (pg.processName || pg.title) : '')}</td>
        <th>${esc(t('capacity'))}(PCS)</th><td>${escVal(pg ? pg.capacity : '')}</td>
        <th>${esc(t('pageNo'))}</th><td>${pno}/${totalPages}</td>
      </tr>
      <tr>
        <th class="sub">${esc(t('versionCode'))}</th><td class="sub">${escVal(doc.versionCode)}</td>
        <th class="sub">${esc(t('effectiveDate'))}</th><td class="sub">${escVal(doc.effectiveDate ? String(doc.effectiveDate).slice(0, 10) : '')}</td>
        <th class="sub">${esc(t('operators'))}</th><td class="sub">${escVal(pg ? pg.operators : '')}</td>
        <th class="sub">${esc(t('productModel'))}</th><td class="sub">${escVal(isProduct ? pg.model : '')}</td>
        <th class="sub">${esc(t('projectNo'))}</th><td class="sub">${escVal(isProduct ? pg.projectNo : '')}</td>
        <th class="sub">${esc(t('customerPartNo'))}</th><td class="sub">${escVal(isProduct ? pg.customerPartNo : '')}</td>
      </tr>
      ${((pg && pg.extraFields) || []).length ? `<tr>${(pg.extraFields || []).map(f => `<th class="sub">${esc(f.label)}</th><td class="sub">${escVal(f.value)}</td>`).join('')}</tr>` : ''}
    </table>`;
  };

  /* small change-record strip at the bottom of each sheet (page 2+) */
  const changeStrip = () => `<table class="change"><tr>${cols([t('sequence'), t('mark'), t('changeQty'), t('changeFileNo'), t('changer'), t('changeDate'), t('signDate')])}</tr>
    <tr><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td>${esc(today)}</td></tr></table>`;

  /* page 1: change history sheet */
  const historySheet = `<div class="sheet">
    ${headTable(null, 1)}
    <h3 class="sect">${esc(t('changeHistorySheet'))}</h3>
    <table class="grid big"><thead><tr>${cols([t('sequence'), t('mark'), t('changeQty'), t('changeFileNo'), t('changer'), t('changeDate'), t('remark')])}</tr></thead>
    <tbody>${history.map((h, i) => `<tr><td>${i + 1}</td><td>${esc(h.mark || '')}</td><td>${esc(h.qty || '')}</td><td>${esc(h.file || '')}</td><td>${esc(h.changer || '')}</td><td>${esc(h.date || '')}</td><td>${esc(h.remark || '')}</td></tr>`).join('')}
    ${Array.from({ length: Math.max(0, 16 - history.length) }, (_, i) => `<tr><td>${history.length + i + 1}</td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td></tr>`).join('')}</tbody></table>
    ${signRow(doc, today)}
  </div>`;

  /* page 2: product showcase */
  const productSheets = productPages.map(pg => `<div class="sheet">
    ${headTable(pg, pageNos[pages.indexOf(pg)])}
    ${pg.description ? `<p class="pdesc">${esc(pg.description)}</p>` : ''}
    <div class="showcase">${(pg.images || []).map(im => `<figure><img src="${esc(im)}"></figure>`).join('') || `<div class="noimg">${esc(t('noData'))}</div>`}</div>
    ${(pg.remarks || []).length ? `<ul class="notes">${pg.remarks.map(r => `<li>${esc(r)}</li>`).join('')}</ul>` : ''}
    ${signRow(doc, today)}
    ${changeStrip()}
  </div>`).join('');

  /* page 3+: process sheets, left tables + right step images */
  const processSheets = processPages.map(pg => `<div class="sheet">
    ${headTable(pg, pageNos[pages.indexOf(pg)])}
    ${pg.processFlow ? `<div class="flow">${esc(t('processFlow'))}: ${esc(pg.processFlow)}</div>` : ''}
    ${pg.workContent ? `<div class="flow">${esc(t('workContent'))}: ${esc(pg.workContent)}</div>` : ''}
    <div class="cols">
      <div class="col-left">
        ${(pg.tables || []).map(tb => `<h3 class="sect">${esc(tb.title)}</h3>
          <table class="grid"><thead><tr>${cols(tb.columns)}</tr></thead>
          <tbody>${(tb.rows || []).map(r => `<tr>${(tb.columns || []).map((c, ci) => `<td>${esc(String((r || [])[ci] ?? ''))}</td>`).join('')}</tr>`).join('') || `<tr>${(tb.columns || []).map(() => '<td> </td>').join('')}</tr>`}</tbody></table>`).join('')}
        ${(pg.remarks || []).length ? `<h3 class="sect">${esc(t('remarks'))}</h3><ul class="notes">${pg.remarks.map(r => `<li>${esc(r)}</li>`).join('')}</ul>` : ''}
        ${(pg.images || []).length ? `<h3 class="sect">${esc(t('images'))}</h3><div class="leftimgs">${pg.images.map(im => `<figure><img src="${esc(im)}"></figure>`).join('')}</div>` : ''}
      </div>
      <div class="col-right">
        ${(pg.steps || []).length ? pg.steps.map(s => `<div class="stepblk">
          <div class="steptitle">${escVal(s.title)}${s.desc ? `<span class="stepdesc">${escVal(s.desc)}</span>` : ''}</div>
          ${s.image ? `<img class="stepimg" src="${esc(s.image)}">` : '<div class="noimg small"></div>'}
        </div>`).join('') : `<div class="noimg">${esc(t('noData'))}</div>`}
      </div>
    </div>
    ${signRow(doc, today)}
    ${changeStrip()}
  </div>`).join('');

  const html = `<!doctype html><html><head><meta charset="utf-8"><title>${esc(doc.documentNo)} ${esc(doc.versionCode)}</title>
<style>
  @page { size: A3 landscape; margin: 8mm; }
  * { box-sizing: border-box; }
  body { font-family: "Microsoft YaHei", "Segoe UI", sans-serif; color: #111; margin: 0; font-size: 11px; }
  .sheet { page-break-after: always; padding: 2px 4px; }
  .sheet:last-child { page-break-after: auto; }
  table { border-collapse: collapse; width: 100%; margin-bottom: 6px; }
  th, td { border: 1px solid #333; padding: 3px 6px; text-align: left; vertical-align: middle; }
  .phead th { background: #f0ecf8; white-space: nowrap; }
  .phead td { white-space: nowrap; }
  .phead .logo { font-family: Arial, sans-serif; font-size: 26px; font-weight: 800; letter-spacing: 4px; text-align: center; width: 130px; border: 2px solid #111; }
  .phead .ptitle { font-size: 17px; font-weight: 700; text-align: center; letter-spacing: 3px; white-space: normal; }
  .phead tr.sub th, .phead tr.sub td { border-color: #666; font-size: 10px; }
  .sect { font-size: 12px; margin: 8px 0 3px; border-left: 4px solid #7C3AED; padding-left: 6px; }
  .flow { border: 1px solid #333; padding: 3px 8px; margin-bottom: 6px; background: #f7f5fd; font-weight: 600; }
  .grid th { background: #f0ecf8; text-align: center; white-space: nowrap; }
  .grid.big td { height: 20px; }
  .grid td { height: 16px; }
  .cols { display: flex; gap: 8px; align-items: flex-start; }
  .col-left { width: 42%; }
  .col-right { flex: 1; }
  .pdesc { border: 1px solid #333; padding: 6px 10px; line-height: 1.6; margin: 6px 0; }
  .showcase { display: flex; flex-wrap: wrap; gap: 10px; margin: 6px 0; }
  .showcase figure { margin: 0; width: 31%; text-align: center; }
  .showcase img { max-width: 100%; max-height: 240px; border: 1px solid #999; }
  .stepblk { margin-bottom: 8px; page-break-inside: avoid; }
  .steptitle { background: #333; color: #fff; font-weight: 700; padding: 2px 8px; font-size: 11px; }
  .stepdesc { font-weight: 400; margin-left: 10px; }
  .stepimg { width: 100%; max-height: 300px; object-fit: contain; border: 1px solid #999; }
  .leftimgs { display: flex; flex-wrap: wrap; gap: 6px; }
  .leftimgs figure { margin: 0; width: 48%; }
  .leftimgs img { max-width: 100%; max-height: 140px; border: 1px solid #999; }
  .noimg { border: 1px dashed #999; min-height: 180px; display: flex; align-items: center; justify-content: center; color: #999; }
  .noimg.small { min-height: 80px; }
  .notes { margin: 4px 0; padding-left: 18px; line-height: 1.7; }
  .sign { margin-top: 8px; }
  .sign table { table-layout: fixed; }
  .sign th { background: #f0ecf8; text-align: center; }
  .sign td { height: 28px; text-align: center; }
  .change th { background: #f7f5fd; text-align: center; font-size: 10px; }
  .change td { height: 20px; }
</style></head><body>${historySheet}${productSheets}${processSheets}
<div style="text-align:center;margin-top:10px"><button onclick="window.print()" style="padding:8px 24px;font-size:14px;cursor:pointer">${esc(t('printExport'))}</button></div>
</body></html>`;

  const frame = document.createElement('iframe');
  frame.style.cssText = 'position:fixed;right:0;bottom:0;width:1024px;height:768px;border:0;visibility:hidden;';
  document.body.appendChild(frame);
  const d = frame.contentDocument;
  d.open(); d.write(html); d.close();
  const doPrint = () => { try { frame.contentWindow.focus(); frame.contentWindow.print(); } catch {} setTimeout(() => frame.remove(), 120000); };
  if (d.readyState === 'complete') setTimeout(doPrint, 400); else frame.onload = () => setTimeout(doPrint, 400);
}

function signRow(doc, today) {
  const cells = [[t('preparedBy'), doc.preparedBy], [t('reviewedBy'), doc.reviewedBy], [t('rdSign'), ''], [t('productionSign'), ''], [t('qualitySign'), ''], [t('approvedBy'), doc.approvedBy], [t('signDate'), today]];
  return `<div class="sign"><table><tr>${cells.map(([l]) => `<th>${esc(l)}</th>`).join('')}</tr><tr>${cells.map(([, v]) => `<td>${esc(v || ' ')}</td>`).join('')}</tr></table></div>`;
}

/* ================================================================
   EDITOR
   ================================================================ */

let editor = null;

function defaultPage(type = 'process') {
  return { type, title: '', stationAttr: '', processNo: '', processName: '', standardTime: '', operators: '', capacity: '',
    productDrawingNo: '', partDrawingNo: '', processFlow: '', workContent: '',
    model: '', projectNo: '', customerPartNo: '', description: '',
    tables: [], steps: [], images: [], remarks: [], extraFields: [] };
}

function defaultContent() {
  return { changeHistory: [], pages: [defaultPage()] };
}

function normalizeContent(content) {
  const c = content && typeof content === 'object' ? content : {};
  const out = {
    changeHistory: Array.isArray(c.changeHistory) ? c.changeHistory.map(h => ({ mark: '', qty: '', file: '', changer: '', date: '', ...h })) : [],
    pages: (Array.isArray(c.pages) ? c.pages : []).map(p => ({ ...defaultPage(p.type === 'product' ? 'product' : 'process'), ...p, type: p.type === 'product' ? 'product' : 'process' })),
  };
  if (!out.pages.length) out.pages = [defaultPage()];
  return out;
}

export function openEsopEditor(existing) {
  editor = {
    mode: existing ? 'edit' : 'create',
    docId: existing?.id,
    meta: existing ? {
      documentNo: existing.documentNo || '', versionCode: existing.versionCode || '', title: existing.title || '',
      productCode: existing.productCode || '', productName: existing.productName || '',
      documentType: existing.documentType || 'ASSEMBLY', effectiveDate: existing.effectiveDate ? String(existing.effectiveDate).slice(0, 10) : '',
      changeSummary: existing.changeSummary || '', preparedBy: existing.preparedBy || state.user?.username || '',
    } : {
      documentNo: '', versionCode: 'A-0', title: '', productCode: '', productName: '',
      documentType: 'ASSEMBLY', effectiveDate: '', changeSummary: '', preparedBy: state.user?.username || '',
    },
    content: existing?.content ? normalizeContent(existing.content) : defaultContent(),
  };
  renderEditor();
}

/* ================================================================
   LIVE (WYSIWYG) EDITOR — type directly on template-styled sheets
   ================================================================ */

const li = (path, value, ph = '', type = 'text') => `<input class="li" type="${type}" data-path="${esc(path)}" value="${esc(value ?? '')}" placeholder="${esc(ph || '')}">`;
const liTa = (path, value, ph = '', rows = 3) => `<textarea class="li" rows="${rows}" data-path="${esc(path)}" placeholder="${esc(ph || '')}">${esc(value ?? '')}</textarea>`;

function liveHeadTable(pi, pg, pno, total) {
  const m = editor.meta;
  const isProduct = pg && pg.type === 'product';
  const mc = (key, type = 'text') => `<td>${li('meta.' + key, m[key], '', type)}</td>`;
  const pc = key => (pi == null)
    ? '<td> </td>'
    : ((isProduct && key === 'stationAttr') || (!isProduct && ['model', 'projectNo', 'customerPartNo'].includes(key)))
      ? '<td> </td>'
      : `<td>${li(`pages.${pi}.${key}`, pg[key])}</td>`;
  return `<table class="esop-lv-phead">
    <tr>
      <td class="logo" rowspan="2">TAIKING</td>
      <td class="ptitle" rowspan="2"><select class="li" data-path="meta.documentType">${DOC_TYPES.map(tp => `<option value="${tp}" ${m.documentType === tp ? 'selected' : ''}>${esc(docTypeLabel(tp))}</option>`).join('')}</select></td>
      <th>${esc(t('productName'))}</th>${mc('productName')}
      <th>${esc(t('standardTime'))}(S)</th>${pc('standardTime')}
      <th>${esc(t('productCode'))}</th>${mc('productCode')}
      <th>${esc(t('esopDocumentNo'))}</th>${mc('documentNo')}
    </tr>
    <tr>
      <th>${esc(isProduct ? t('productShowcase') : t('stationAttr'))}</th>${pc('stationAttr')}
      <th>${esc(t('processNo'))}</th>${pc('processNo')}
      <th>${esc(t('processName'))}</th>${pc('processName')}
      <th>${esc(t('capacity'))}(PCS)</th>${pc('capacity')}
      <th>${esc(t('pageNo'))}</th><td class="num">${pno}/${total}</td>
    </tr>
    <tr class="sub">
      <th>${esc(t('versionCode'))}</th>${mc('versionCode')}
      <th>${esc(t('effectiveDate'))}</th>${mc('effectiveDate', 'date')}
      <th>${esc(t('operators'))}</th>${pc('operators')}
      <th>${esc(t('productModel'))}</th>${pc('model')}
      <th>${esc(t('projectNo'))}</th>${pc('projectNo')}
      <th>${esc(t('customerPartNo'))}</th>${pc('customerPartNo')}
    </tr>
    ${pi != null ? `<tr class="sub">${(pg.extraFields || []).map((f, fi) => `
      <th>${li(`pages.${pi}.extraFields.${fi}.label`, f.label, t('fieldLabel'))}</th>
      <td>${li(`pages.${pi}.extraFields.${fi}.value`, f.value, t('fieldValue'))}</td>
      <td class="ops"><button class="icon-btn danger-icon" data-action="esop-remove-extra" data-pi="${pi}" data-fi="${fi}">${icon('trash-2')}</button></td>`).join('')}
      <td class="ops" colspan="2"><button class="icon-btn" data-action="esop-add-extra" data-pi="${pi}">${icon('plus')}</button></td></tr>` : ''}
  </table>`;
}

function liveSignRow() {
  const m = editor.meta;
  const today = new Date().toISOString().slice(0, 10);
  const cells = [[t('preparedBy'), li('meta.preparedBy', m.preparedBy)], [t('reviewedBy'), ' '], [t('rdSign'), ' '], [t('productionSign'), ' '], [t('qualitySign'), ' '], [t('approvedBy'), ' '], [t('signDate'), today]];
  return `<table class="esop-lv-sign"><tr>${cells.map(([l]) => `<th>${esc(l)}</th>`).join('')}</tr><tr>${cells.map(([, v]) => `<td>${v}</td>`).join('')}</tr></table>`;
}

function liveChangeStrip() {
  const today = new Date().toISOString().slice(0, 10);
  const cols = [t('sequence'), t('mark'), t('changeQty'), t('changeFileNo'), t('changer'), t('changeDate'), t('signDate')];
  return `<table class="esop-lv-change"><tr>${cols.map(c => `<th>${esc(c)}</th>`).join('')}</tr>
    <tr>${cols.map((c, i) => `<td>${i === cols.length - 1 ? today : ' '}</td>`).join('')}</tr></table>`;
}

function liveHistorySheet(total) {
  const c = editor.content;
  const rows = c.changeHistory || [];
  const head = [t('sequence'), t('mark'), t('changeQty'), t('changeFileNo'), t('changer'), t('changeDate'), t('remark'), ''];
  return `<div class="esop-live-sheet">
    <div class="esop-lv-pageops"><strong>${esc(t('changeHistorySheet'))}</strong><span class="num">1/${total}</span></div>
    ${liveHeadTable(null, null, 1, total)}
    <div class="esop-lv-sect">${esc(t('changeHistory'))}${btn('esop-add-history-row', icon('plus') + t('addRow'), 'ghost')}</div>
    <table class="esop-lv-grid">
      <thead><tr>${head.map(h => `<th>${esc(h)}</th>`).join('')}</tr></thead>
      <tbody>${rows.length ? rows.map((h, i) => `<tr>
        <td class="num">${i + 1}</td>
        <td>${li(`changeHistory.${i}.mark`, h.mark)}</td>
        <td>${li(`changeHistory.${i}.qty`, h.qty)}</td>
        <td>${li(`changeHistory.${i}.file`, h.file)}</td>
        <td>${li(`changeHistory.${i}.changer`, h.changer)}</td>
        <td>${li(`changeHistory.${i}.date`, h.date, '2026.09.09')}</td>
        <td>${li(`changeHistory.${i}.remark`, h.remark)}</td>
        <td class="ops"><button class="icon-btn danger-icon" data-action="esop-remove-history-row" data-i="${i}">${icon('trash-2')}</button></td>
      </tr>`).join('') : `<tr><td colspan="8" class="esop-lv-empty">${esc(t('noData'))}</td></tr>`}</tbody>
    </table>
    ${liveSignRow()}
  </div>`;
}

function liveTable(pi, tb, ti) {
  const cols = tb.columns || [];
  return `<div class="esop-lv-block">
    <div class="esop-lv-block-head">
      <input class="li title" data-path="pages.${pi}.tables.${ti}.title" value="${esc(tb.title || '')}">
      <input class="esop-table-columns" data-pi="${pi}" data-ti="${ti}" value="${esc(cols.join(','))}" placeholder="${esc(t('columnsPlaceholder'))}">
      ${btn('esop-add-row', icon('plus') + t('addRow'), 'ghost', '', `data-pi="${pi}" data-ti="${ti}"`)}
      <button class="icon-btn danger-icon" data-action="esop-remove-table" data-pi="${pi}" data-ti="${ti}">${icon('trash-2')}</button>
    </div>
    <table class="esop-lv-grid">
      <thead><tr>${cols.map(c => `<th>${escVal(c)}</th>`).join('')}<th class="ops"></th></tr></thead>
      <tbody>${(tb.rows || []).length ? tb.rows.map((r, ri) => `<tr>
        ${cols.map((c, ci) => `<td>${li(`pages.${pi}.tables.${ti}.rows.${ri}.${ci}`, (r || [])[ci])}</td>`).join('')}
        <td class="ops"><button class="icon-btn danger-icon" data-action="esop-remove-row" data-pi="${pi}" data-ti="${ti}" data-ri="${ri}">${icon('trash-2')}</button></td>
      </tr>`).join('') : `<tr><td colspan="${cols.length + 1}" class="esop-lv-empty">${esc(t('noData'))}</td></tr>`}</tbody>
    </table>
  </div>`;
}

function liveFigs(pi, pg) {
  return `<div class="esop-lv-figs">
    ${(pg.images || []).map((im, ii) => `<figure class="esop-lv-fig"><img src="${esc(im)}" alt=""><button class="icon-btn danger-icon" data-action="esop-remove-image" data-pi="${pi}" data-ii="${ii}">${icon('trash-2')}</button></figure>`).join('')}
    <label class="esop-lv-fig-add">${icon('image-plus')}<input type="file" accept="image/*" multiple class="esop-img-input" data-kind="images" data-pi="${pi}"></label>
  </div>`;
}

function liveRemarks(pi, pg) {
  return (pg.remarks || []).map((r, ri) => `<div class="esop-lv-remark">
    <span class="num">${ri + 1}.</span>${li(`pages.${pi}.remarks.${ri}`, r)}
    <button class="icon-btn danger-icon" data-action="esop-remove-remark" data-pi="${pi}" data-ri="${ri}">${icon('trash-2')}</button>
  </div>`).join('') + `<div class="esop-lv-addline">${btn('esop-add-remark', icon('plus') + t('addRemark'), 'ghost', '', `data-pi="${pi}"`)}</div>`;
}

function liveSteps(pi, pg) {
  return (pg.steps || []).map((s, si) => `<div class="esop-lv-step">
    <div class="esop-lv-step-head">
      <input class="li title" data-path="pages.${pi}.steps.${si}.title" value="${escVal(s.title)}" placeholder="${esc(t('stepTitle'))}">
      <input class="li" data-path="pages.${pi}.steps.${si}.desc" value="${escVal(s.desc)}" placeholder="${esc(t('stepDesc'))}">
      <label class="esop-lv-up">${icon('image-plus')}<input type="file" accept="image/*" class="esop-img-input" data-kind="step" data-pi="${pi}" data-si="${si}"></label>
      <button class="icon-btn danger-icon" data-action="esop-remove-step" data-pi="${pi}" data-si="${si}">${icon('trash-2')}</button>
    </div>
    ${s.image ? `<img class="esop-lv-step-img" src="${esc(s.image)}" alt="">` : ''}
  </div>`).join('') + `<div class="esop-lv-addline">${btn('esop-add-step', icon('plus') + t('addStep'), 'secondary', '', `data-pi="${pi}"`)}</div>`;
}

function liveProductPage(pi, pg, pno, total) {
  return `<div class="esop-live-sheet">
    <div class="esop-lv-pageops">
      <span class="esop-page-type product">${esc(t('productShowcase'))}</span>
      <span class="num">${pno}/${total}</span>
      ${btn('esop-remove-page', icon('trash-2') + t('removePage'), 'ghost', '', `data-pi="${pi}"`)}
    </div>
    ${liveHeadTable(pi, pg, pno, total)}
    <div class="esop-lv-work">${liTa(`pages.${pi}.description`, pg.description, t('description'))}</div>
    <div class="esop-lv-sect">${esc(t('productImages'))}</div>
    ${liveFigs(pi, pg)}
    <div class="esop-lv-sect">${esc(t('remarks'))}</div>
    ${liveRemarks(pi, pg)}
    ${liveSignRow()}
    ${liveChangeStrip()}
  </div>`;
}

function liveProcessPage(pi, pg, pno, total) {
  const presets = TABLE_PRESETS();
  return `<div class="esop-live-sheet">
    <div class="esop-lv-pageops">
      <span class="esop-page-type process">${esc(t('processWork'))}</span>
      <span class="num">${pno}/${total}</span>
      ${btn('esop-remove-page', icon('trash-2') + t('removePage'), 'ghost', '', `data-pi="${pi}"`)}
    </div>
    ${liveHeadTable(pi, pg, pno, total)}
    <div class="esop-lv-flow">
      <label><span>${esc(t('processFlow'))}</span>${li(`pages.${pi}.processFlow`, pg.processFlow, t('processFlowHint'))}</label>
      <label><span>${esc(t('workContent'))}</span>${li(`pages.${pi}.workContent`, pg.workContent)}</label>
    </div>
    <div class="esop-lv-cols">
      <div class="esop-lv-col-left">
        <div class="esop-lv-sect">${esc(t('tables'))}</div>
        ${(pg.tables || []).map((tb, ti) => liveTable(pi, tb, ti)).join('')}
        <div class="esop-lv-addline">
          <select class="esop-preset" data-pi="${pi}">${presets.map(p => `<option value="${p.key}">${esc(p.title)}</option>`).join('')}</select>
          ${btn('esop-add-table', icon('table') + t('addTable'), 'secondary', '', `data-pi="${pi}"`)}
        </div>
        <div class="esop-lv-sect">${esc(t('remarks'))}</div>
        ${liveRemarks(pi, pg)}
        <div class="esop-lv-sect">${esc(t('images'))}</div>
        ${liveFigs(pi, pg)}
      </div>
      <div class="esop-lv-col-right">
        <div class="esop-lv-sect">${esc(t('steps'))}</div>
        ${liveSteps(pi, pg)}
      </div>
    </div>
    ${liveSignRow()}
    ${liveChangeStrip()}
  </div>`;
}

function renderEditor() {
  const c = editor.content;
  const m = editor.meta;
  const pages = c.pages;
  const productPages = pages.filter(pg => pg.type === 'product');
  const processPages = pages.filter(pg => pg.type !== 'product');
  const total = 1 + productPages.length + processPages.length;
  const pageNos = {};
  let pno = 1;
  productPages.forEach(pg => { pageNos[pages.indexOf(pg)] = ++pno; });
  processPages.forEach(pg => { pageNos[pages.indexOf(pg)] = ++pno; });

  $('#page').innerHTML = `
  <div class="esop-editor esop-live">
    <div class="esop-editor-bar">
      ${btn('esop-editor-back', icon('arrow-left') + t('back'), 'secondary')}
      <input class="esop-live-title" data-path="meta.title" value="${esc(m.title)}" placeholder="${esc(t('docTitle'))}">
      <div class="esop-editor-bar-actions">
        ${btn('esop-add-product-page', icon('image') + t('addProductPage'), 'secondary')}
        ${btn('esop-add-page', icon('plus') + t('addProcessPage'), 'primary')}
        ${btn('esop-editor-preview', icon('eye') + t('esopPreview'), 'secondary')}
        ${btn('esop-editor-save', icon('save') + t('save'), 'primary')}
      </div>
    </div>
    <p class="esop-live-hint">${esc(t('liveEditHint'))}</p>
    ${liveHistorySheet(total)}
    ${pages.map((pg, pi) => pg.type === 'product'
      ? liveProductPage(pi, pg, pageNos[pi], total)
      : liveProcessPage(pi, pg, pageNos[pi], total)).join('')}
  </div>`;
  renderIcons();
}

/* --- content path get/set --- */
function getPath(obj, path) { return String(path).split('.').reduce((v, k) => v == null ? undefined : v[k], obj); }
function setPath(obj, path, value) {
  const keys = String(path).split('.');
  let cur = obj;
  for (let i = 0; i < keys.length - 1; i++) {
    const k = keys[i];
    if (cur[k] == null) cur[k] = /^\d+$/.test(keys[i + 1]) ? [] : {};
    cur = cur[k];
  }
  cur[keys[keys.length - 1]] = value;
}

/* --- image helpers --- */
function fileToDataUrl(file, maxW = 1400, quality = 0.85) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const img = new Image();
      img.onload = () => {
        const scale = Math.min(1, maxW / img.width);
        if (scale >= 1 && file.size < 300 * 1024) { resolve(reader.result); return; }
        const canvas = document.createElement('canvas');
        canvas.width = Math.max(1, Math.round(img.width * scale));
        canvas.height = Math.max(1, Math.round(img.height * scale));
        canvas.getContext('2d').drawImage(img, 0, 0, canvas.width, canvas.height);
        resolve(canvas.toDataURL('image/jpeg', quality));
      };
      img.onerror = () => resolve(reader.result);
      img.src = reader.result;
    };
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

/* ================================================================
   DELEGATED EVENTS (registered once at module load)
   ================================================================ */

document.addEventListener('input', e => {
  if (!editor) return;
  const el = e.target;
  const path = el.dataset?.path;
  if (!path || !el.closest('.esop-editor')) return;
  setPath({ meta: editor.meta, ...editor.content }, path, el.value);
});

document.addEventListener('change', async e => {
  if (!editor) return;
  const el = e.target;
  if (!el.closest('.esop-editor')) return;

  if (el.tagName === 'SELECT' && el.dataset?.path) {
    setPath({ meta: editor.meta, ...editor.content }, el.dataset.path, el.value);
    renderEditor();
    return;
  }

  if (el.classList.contains('esop-table-columns')) {
    const pg = editor.content.pages[+el.dataset.pi];
    const tb = pg.tables[+el.dataset.ti];
    const cols = el.value.split(/[,，]/).map(v => v.trim()).filter(Boolean);
    if (cols.length) {
      tb.columns = cols;
      tb.rows = (tb.rows || []).map(r => { const row = []; for (let i = 0; i < cols.length; i++) row.push((r || [])[i] ?? ''); return row; });
      renderEditor();
    }
    return;
  }

  if (el.classList.contains('esop-img-input')) {
    const files = [...(el.files || [])];
    if (!files.length) return;
    el.disabled = true;
    try {
      const urls = await Promise.all(files.map(f => fileToDataUrl(f)));
      const pg = editor.content.pages[+el.dataset.pi];
      if (el.dataset.kind === 'step') pg.steps[+el.dataset.si].image = urls[0];
      else pg.images = [...(pg.images || []), ...urls];
      renderEditor();
    } catch { toast(t('failedRequest'), true); }
    el.disabled = false;
  }
});

document.addEventListener('click', async e => {
  const node = e.target.closest('[data-action]');
  if (!node) return;
  const action = node.dataset.action;
  if (!action.startsWith('esop-')) return;

  /* ---- editor structural actions ---- */
  if (editor && node.closest('.esop-editor')) {
    const c = editor.content;
    const pi = +node.dataset.pi;
    switch (action) {
      case 'esop-add-history-row': c.changeHistory.push({ mark: '', qty: '', file: '', changer: '', date: '' }); renderEditor(); return;
      case 'esop-remove-history-row': c.changeHistory.splice(+node.dataset.i, 1); renderEditor(); return;
      case 'esop-add-page': c.pages.push(defaultPage('process')); renderEditor(); return;
      case 'esop-add-product-page': c.pages.push(defaultPage('product')); renderEditor(); return;
      case 'esop-remove-page': if (c.pages.length > 1) { c.pages.splice(pi, 1); renderEditor(); } else toast(t('lastPage'), true); return;
      case 'esop-add-table': {
        const presetKey = node.closest('.esop-lv-addline')?.querySelector('.esop-preset')?.value || 'custom';
        const preset = TABLE_PRESETS().find(p => p.key === presetKey) || TABLE_PRESETS().find(p => p.key === 'custom');
        c.pages[pi].tables.push({ title: preset.title, columns: [...preset.columns], rows: [] });
        renderEditor(); return;
      }
      case 'esop-remove-table': c.pages[pi].tables.splice(+node.dataset.ti, 1); renderEditor(); return;
      case 'esop-add-row': { const tb = c.pages[pi].tables[+node.dataset.ti]; tb.rows.push(tb.columns.map(() => '')); renderEditor(); return; }
      case 'esop-remove-row': c.pages[pi].tables[+node.dataset.ti].rows.splice(+node.dataset.ri, 1); renderEditor(); return;
      case 'esop-add-step': c.pages[pi].steps.push({ title: '', desc: '', image: '' }); renderEditor(); return;
      case 'esop-remove-step': c.pages[pi].steps.splice(+node.dataset.si, 1); renderEditor(); return;
      case 'esop-remove-image': c.pages[pi].images.splice(+node.dataset.ii, 1); renderEditor(); return;
      case 'esop-add-remark': c.pages[pi].remarks.push(''); renderEditor(); return;
      case 'esop-remove-remark': c.pages[pi].remarks.splice(+node.dataset.ri, 1); renderEditor(); return;
      case 'esop-add-extra': c.pages[pi].extraFields.push({ label: '', value: '' }); renderEditor(); return;
      case 'esop-remove-extra': c.pages[pi].extraFields.splice(+node.dataset.fi, 1); renderEditor(); return;
      case 'esop-editor-back': editor = null; renderEsops(); return;
      case 'esop-editor-preview': {
        const doc = { ...editor.meta, documentType: editor.meta.documentType, content: editor.content };
        printEsop(doc); return;
      }
      case 'esop-editor-save': {
        const m = editor.meta;
        if (!m.documentNo.trim() || !m.versionCode.trim() || !m.title.trim()) { toast(t('esopRequired'), true); return; }
        if (!editor.content.pages.length) { toast(t('esopRequired'), true); return; }
        const body = {
          documentNo: m.documentNo.trim(), versionCode: m.versionCode.trim(), title: m.title.trim(),
          productCode: m.productCode.trim(), productName: m.productName.trim(), documentType: m.documentType,
          effectiveDate: m.effectiveDate || null, changeSummary: m.changeSummary, preparedBy: m.preparedBy,
          content: editor.content,
        };
        node.disabled = true;
        try {
          if (editor.mode === 'create') await api('/esops', { method: 'POST', body });
          else await api('/esops/' + editor.docId, { method: 'PUT', body });
          toast(t('esopSaved')); editor = null; renderEsops();
        } catch (err) { toast(err.message, true); node.disabled = false; }
        return;
      }
    }
    return;
  }

  /* ---- list / drawer actions ---- */
  const docId = Number(node.dataset.id || node.closest('tr')?.dataset?.id);
  const loadDoc = async () => (await api('/esops/' + docId)).data;

  switch (action) {
    case 'esop-add': openEsopEditor(null); return;
    case 'esop-detail': { const item = (state.data.esops?.items || []).find(v => v.id === docId); if (item) await openEsopDetail(item); return; }
    case 'esop-edit': openEsopEditor(await loadDoc()); return;
    case 'esop-print': { const doc = await loadDoc(); printEsop(doc); return; }
    case 'esop-submit': {
      if (!confirm(t('confirmSubmit'))) return;
      node.disabled = true;
      try { await api(`/esops/${docId}/submit`, { method: 'POST' }); toast(t('esopSubmitted')); closeDrawer(); loadEsops(); }
      catch (err) { toast(err.message, true); node.disabled = false; }
      return;
    }
    case 'esop-approve': case 'esop-reject': { openReviewDrawer(docId, action === 'esop-approve' ? 'approve' : 'reject'); return; }
    case 'esop-review-confirm': {
      const mode = node.dataset.mode;
      const comment = $('#esop-review-comment')?.value?.trim() || null;
      if (mode === 'reject' && !comment) { toast(t('rejectRequired'), true); return; }
      node.disabled = true;
      try {
        await api(`/esops/${node.dataset.id}/${mode}`, { method: 'POST', body: { comment } });
        toast(mode === 'approve' ? t('esopApproved') : t('esopRejected'));
        closeDrawer(); loadEsops();
      } catch (err) { toast(err.message, true); node.disabled = false; }
      return;
    }
    case 'esop-publish': {
      if (!confirm(t('confirmPublish'))) return;
      node.disabled = true;
      try { await api(`/esops/${docId}/publish`, { method: 'POST' }); toast(t('esopPublished')); closeDrawer(); loadEsops(); }
      catch (err) { toast(err.message, true); node.disabled = false; }
      return;
    }
    case 'esop-revise': { openReviseDrawer(docId); return; }
    case 'esop-revise-confirm': {
      const versionCode = $('#esop-revise-version')?.value?.trim();
      const changeSummary = $('#esop-revise-summary')?.value?.trim();
      if (!versionCode) { toast(t('pleaseEnter') + t('versionCode'), true); return; }
      node.disabled = true;
      try {
        await api(`/esops/${node.dataset.id}/revise`, { method: 'POST', body: { versionCode, changeSummary } });
        toast(t('esopRevised')); closeDrawer(); loadEsops();
      } catch (err) { toast(err.message, true); node.disabled = false; }
      return;
    }
    case 'esop-delete': {
      if (!confirm(t('confirmDelete'))) return;
      try { await api(`/esops/${docId}`, { method: 'DELETE' }); toast(t('saved')); closeDrawer(); loadEsops(); }
      catch (err) { toast(err.message, true); }
      return;
    }
  }
});

function openReviewDrawer(docId, mode) {
  const approve = mode === 'approve';
  openDrawer(approve ? t('approve') : t('reject'), t('reviewCommentHint'),
    `<div class="drawer-body">
      <label class="esop-review-label"><span>${esc(t('reviewComment'))}</span><textarea id="esop-review-comment" rows="5" placeholder="${esc(approve ? t('optional') : t('rejectRequired'))}"></textarea></label>
      <div class="esop-detail-actions">
        ${btn('esop-review-confirm', icon('check') + (approve ? t('approve') : t('reject')), approve ? 'primary' : 'danger', '', `data-mode="${mode}" data-id="${docId}"`)}
        ${btn('close-drawer', t('cancel'), 'secondary')}
      </div>
    </div>`);
  renderIcons();
}

function openReviseDrawer(docId) {
  openDrawer(t('revise'), t('reviseHint'),
    `<div class="drawer-body">
      <div class="form-grid">
        <label><span>${esc(t('versionCode'))}</span><input id="esop-revise-version" placeholder="B-1"></label>
        <label><span>${esc(t('changeSummary'))}</span><input id="esop-revise-summary"></label>
      </div>
      <div class="esop-detail-actions">
        ${btn('esop-revise-confirm', icon('copy-plus') + t('revise'), 'primary', '', `data-id="${docId}"`)}
        ${btn('close-drawer', t('cancel'), 'secondary')}
      </div>
    </div>`);
  renderIcons();
}
