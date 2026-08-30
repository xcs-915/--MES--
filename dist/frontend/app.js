(() => {
  'use strict';

  /* ================================================================
     UTILITIES
     ================================================================ */
  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];
  const esc = v => String(v ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const escVal = v => (v === null || v === undefined || v === '') ? '—' : esc(v);
  const icon = name => `<i data-lucide="${name}"></i>`;
  const formatDate = v => v ? new Date(v).toLocaleString(state.lang === 'zh-CN' ? 'zh-CN' : state.lang === 'ar-TN' ? 'ar-TN' : 'en-GB', {dateStyle:'short', timeStyle:'short'}) : '—';

  /* ================================================================
     STATE
     ================================================================ */
  const state = {
    token: sessionStorage.getItem('tns_token') || '',
    user: null,
    lang: localStorage.getItem('tns_lang') || 'zh-CN',
    view: 'overview',
    data: {} // view-specific pagination state stored as state.data[view]
  };

  // Initialize pagination state for a view
  function ps(view, size = 20) {
    if (!state.data[view]) state.data[view] = { items: [], page: 0, size, total: 0, totalPages: 0, sortKey: null, sortDir: 'asc' };
    return state.data[view];
  }

  /* ================================================================
     i18n
     ================================================================ */
  const i18n = {
    'zh-CN': {
      platform:'集团制造运营平台', loginTitle:'统一主数据、工程准备与生产执行', loginSubtitle:'面向突尼斯工厂的标准化制造执行工作台',
      signIn:'登录工作台', signInHint:'使用 MES 账号继续', username:'用户名', password:'密码', currentSite:'当前站点',
      overview:'运营总览', foundation:'基础与组织', masterData:'基础资料', iam:'用户与权限',
      engineering:'产品工程', products:'产品主数据', routes:'工艺路线', quality:'检验规则',
      production:'生产执行', workOrders:'生产工单', integrationCenter:'集成中心', manualSync:'接口同步', scheduledJobs:'定时任务', apiLogs:'接口调用日志',
      logout:'退出登录', search:'输入编码、名称或关键字', searchMenu:'搜索菜单',
      query:'查询', refresh:'刷新', reset:'重置', details:'详情', actions:'操作', status:'状态', source:'来源', lastSync:'最近同步',
      noData:'暂无数据', all:'全部', enabled:'启用', disabled:'停用', run:'立即执行', save:'保存', cancel:'取消',
      add:'新增', edit:'编辑', delete:'删除', confirmDelete:'确认删除？',
      // Product fields
      product:'产品', code:'编码', name:'名称', type:'类型', unit:'单位', specification:'规格', productGroup:'产品组',
      productModel:'型号', customerPartNumber:'客户零件号', minPackagingQty:'最小包装量', brand:'品牌', color:'颜色',
      drawingNumber:'图号', productOldId:'旧物料号', grossWeight:'毛重', netWeight:'净重', weightUnit:'重量单位',
      countryOfOrigin:'原产国', manufacturerNumber:'制造商编号', manufacturerPartNumber:'制造商零件号',
      materialRevisionLevel:'物料版本', serialNumberProfile:'序列号参数', productHierarchy:'产品层级',
      divisionCode:'部门', markedForDeletion:'已标记删除', batchManaged:'批次管理', traceable:'可追溯',
      description:'描述', parent:'上级',
      // Work order fields
      orderNo:'工单号', orderType:'工单类型', quantity:'计划数量', completed:'已完成', progress:'进度',
      plannedStart:'计划开始', plannedEnd:'计划结束', productionPlant:'生产工厂', storageLocation:'库存地点',
      mrpController:'MRP控制员', productionVersion:'生产版本', companyCode:'公司代码', profitCenter:'利润中心',
      scheduledStart:'排程开始', scheduledEnd:'排程结束', components:'组件明细', operations:'工序明细',
      sequence:'序号', material:'物料', requiredQuantity:'需求数量', withdrawn:'已领料', available:'可用量',
      reservation:'预留号', requirementDate:'需求日期', goodsMovement:'移动类型', workCenter:'工作中心',
      operation:'工序', plannedYield:'计划产出', confirmedYield:'确认产出',
      // Job fields
      jobCode:'任务编码', cron:'Cron表达式', endpoint:'调用接口', system:'系统', lastRun:'最近运行', nextRun:'下次运行',
      history:'运行历史', trigger:'触发方式', permission:'权限', role:'角色', users:'用户', roles:'角色',
      // IAM
      menuManagement:'菜单管理', dataDictionary:'数据字典', usersManagement:'用户管理', rolesManagement:'角色管理',
      usernameExists:'用户名已存在', passwordHint:'至少8位密码', dictType:'字典类型', dictCode:'字典编码',
      dictValue:'字典值', label:'显示名称', parentMenu:'上级菜单', path:'路由路径', iconName:'图标',
      permissionCode:'权限编码', sortOrder:'排序', language:'语言', email:'邮箱', displayName:'显示名称',
      roleCodes:'角色编码', addUser:'新增用户', addRole:'新增角色', httpMethod:'HTTP方法',
      // Sync
      syncResult:'同步结果', received:'接收', created:'新增', updated:'更新', failed:'失败',
      syncProducts:'同步 SAP 产品', syncOrders:'同步 SAP 工单', syncBatches:'同步 SAP 批次', syncHint:'同步使用服务器端 SAP 地址和凭据。',
      singleSync:'按单号同步', productCode:'产品编码', workOrderNo:'工单号', pleaseEnter:'请输入', sync:'同步',
      integrationSubtitle:'统一管理 SAP 主数据同步与接口运行状态。', jobSubtitle:'查看计划、接口、运行结果和失败信息。', apiLogsSubtitle:'记录所有 SAP 接口调用的请求、响应和耗时。',
      productSubtitle:'SAP 产品主数据只读视图，字段来自 API_PRODUCT_SRV。', orderSubtitle:'SAP 生产工单主从视图，组件和工序在详情下方。', batchSubtitle:'SAP 批次主数据管理，支持批次状态、有效期、检验信息等。',
      batchNo:'批次号', batch:'批次', batches:'批次管理', batchStatus:'批次状态', plant:'工厂', availabilityDate:'可用日期', expirationDate:'到期日期', shelfLifeExpirationDate:'货架寿命', manufactureDate:'生产日期', supplierBatch:'供应商批次', vendor:'供应商', quantity:'数量', unit:'单位', restrictedUse:'限制使用', inspectionLot:'检验批', inspectionStatus:'检验状态', batchClass:'批次等级',
      masterSubtitle:'维护组织、仓库、工作中心、岗位等基础资料。', accessSubtitle:'按页面和按钮动作配置角色权限。',
      // Messages
      permissionSaved:'权限已保存', sapDisabled:'SAP接口未启用，请检查服务器配置',
      invalidLogin:'用户名或密码错误', saved:'保存成功', started:'任务已启动', failedRequest:'请求失败',
      // Pagination
      total:'共', items:'条', perPage:'条/页', page:'页'
    },
    en: {
      platform:'Group Manufacturing Operations Platform', loginTitle:'One workspace for master data, engineering and execution', loginSubtitle:'Standardized execution workbench for Tunis Plant',
      signIn:'Sign in', signInHint:'Use your MES account to continue', username:'Username', password:'Password', currentSite:'Current site',
      overview:'Operations overview', foundation:'Foundation', masterData:'Master data', iam:'Users & access',
      engineering:'Product engineering', products:'Products', routes:'Process routes', quality:'Quality rules',
      production:'Production execution', workOrders:'Production orders', integrationCenter:'Integration', manualSync:'Interface sync', scheduledJobs:'Scheduled jobs', apiLogs:'API call logs',
      logout:'Sign out', search:'Search code, name or keyword', searchMenu:'Search menu',
      query:'Search', refresh:'Refresh', reset:'Reset', details:'Details', actions:'Actions', status:'Status', source:'Source', lastSync:'Last sync',
      noData:'No data', all:'All', enabled:'Enabled', disabled:'Disabled', run:'Run now', save:'Save', cancel:'Cancel',
      add:'Add', edit:'Edit', delete:'Delete', confirmDelete:'Confirm delete?',
      product:'Product', code:'Code', name:'Name', type:'Type', unit:'Unit', specification:'Specification', productGroup:'Product group',
      productModel:'Model', customerPartNumber:'Customer part no.', minPackagingQty:'Min pack qty', brand:'Brand', color:'Color',
      drawingNumber:'Drawing no.', productOldId:'Legacy material', grossWeight:'Gross weight', netWeight:'Net weight', weightUnit:'Weight unit',
      countryOfOrigin:'Country of origin', manufacturerNumber:'Manufacturer no.', manufacturerPartNumber:'Manufacturer part no.',
      materialRevisionLevel:'Material revision', serialNumberProfile:'Serial profile', productHierarchy:'Hierarchy',
      divisionCode:'Division', markedForDeletion:'Marked for deletion', batchManaged:'Batch managed', traceable:'Traceable',
      description:'Description', parent:'Parent',
      orderNo:'Order no.', orderType:'Order type', quantity:'Planned qty', completed:'Completed', progress:'Progress',
      plannedStart:'Planned start', plannedEnd:'Planned end', productionPlant:'Production plant', storageLocation:'Storage location',
      mrpController:'MRP controller', productionVersion:'Production version', companyCode:'Company code', profitCenter:'Profit center',
      scheduledStart:'Scheduled start', scheduledEnd:'Scheduled end', components:'Components', operations:'Operations',
      sequence:'Seq.', material:'Material', requiredQuantity:'Required qty', withdrawn:'Withdrawn', available:'Available',
      reservation:'Reservation', requirementDate:'Requirement date', goodsMovement:'Movement type', workCenter:'Work center',
      operation:'Operation', plannedYield:'Planned yield', confirmedYield:'Confirmed yield',
      jobCode:'Job code', cron:'Cron expression', endpoint:'Endpoint', system:'System', lastRun:'Last run', nextRun:'Next run',
      history:'Run history', trigger:'Trigger', permission:'Permission', role:'Role', users:'Users', roles:'Roles',
      menuManagement:'Menu management', dataDictionary:'Data dictionary', usersManagement:'User management', rolesManagement:'Role management',
      usernameExists:'Username already exists', passwordHint:'At least 8 characters', dictType:'Dictionary type', dictCode:'Dictionary code',
      dictValue:'Dictionary value', label:'Label', parentMenu:'Parent menu', path:'Route path', iconName:'Icon',
      permissionCode:'Permission code', sortOrder:'Sort order', language:'Language', email:'Email', displayName:'Display name',
      roleCodes:'Role codes', addUser:'Add user', addRole:'Add role', httpMethod:'HTTP method',
      syncResult:'Sync result', received:'Received', created:'Created', updated:'Updated', failed:'Failed',
      syncProducts:'Sync SAP products', syncOrders:'Sync SAP work orders', syncBatches:'Sync SAP batches', syncHint:'Uses SAP URL and credentials configured on the server.',
      singleSync:'Sync by number', productCode:'Product code', workOrderNo:'Work order no.', pleaseEnter:'Please enter', sync:'Sync',
      integrationSubtitle:'Manage SAP master-data synchronization and interface status.', jobSubtitle:'Inspect schedules, endpoints, run results and failures.', apiLogsSubtitle:'Record of all SAP API calls with request, response and duration.',
      productSubtitle:'Read-only SAP product master view from API_PRODUCT_SRV.', orderSubtitle:'SAP production order master-detail view with components and operations.', batchSubtitle:'SAP batch master data management with status, shelf life and inspection info.',
      batchNo:'Batch No.', batch:'Batch', batches:'Batch management', batchStatus:'Batch status', plant:'Plant', availabilityDate:'Availability date', expirationDate:'Expiration date', shelfLifeExpirationDate:'Shelf life', manufactureDate:'Manufacture date', supplierBatch:'Supplier batch', vendor:'Vendor', quantity:'Quantity', unit:'Unit', restrictedUse:'Restricted use', inspectionLot:'Inspection lot', inspectionStatus:'Inspection status', batchClass:'Batch class',
      masterSubtitle:'Maintain organizations, warehouses, work centers and roles.', accessSubtitle:'Configure page access and button actions by role.',
      permissionSaved:'Permissions saved', sapDisabled:'SAP integration is disabled. Check server configuration.',
      invalidLogin:'Invalid username or password', saved:'Saved', started:'Job started', failedRequest:'Request failed',
      total:'Total', items:'items', perPage:'/ page', page:'Page'
    },
    'ar-TN': {
      platform:'منصة عمليات التصنيع للمجموعة', loginTitle:'مساحة موحدة للبيانات والهندسة والتنفيذ', loginSubtitle:'منصة تنفيذ موحدة لمصنع تونس',
      signIn:'تسجيل الدخول', signInHint:'استخدم حساب MES للمتابعة', username:'اسم المستخدم', password:'كلمة المرور', currentSite:'الموقع الحالي',
      overview:'نظرة عامة', foundation:'الأساس والتنظيم', masterData:'البيانات الأساسية', iam:'المستخدمون والصلاحيات',
      engineering:'هندسة المنتجات', products:'المنتجات', routes:'مسارات العمليات', quality:'قواعد الجودة',
      production:'التنفيذ', workOrders:'أوامر الإنتاج', integrationCenter:'التكامل', manualSync:'مزامنة الواجهات', scheduledJobs:'المهام المجدولة', apiLogs:'سجلات استدعاء الواجهات',
      logout:'تسجيل الخروج', search:'ابحث بالكود أو الاسم', searchMenu:'بحث في القائمة',
      query:'بحث', refresh:'تحديث', reset:'إعادة ضبط', details:'التفاصيل', actions:'الإجراءات', status:'الحالة', source:'المصدر', lastSync:'آخر مزامنة',
      noData:'لا توجد بيانات', all:'الكل', enabled:'مفعل', disabled:'متوقف', run:'تشغيل الآن', save:'حفظ', cancel:'إلغاء',
      add:'إضافة', edit:'تحرير', delete:'حذف', confirmDelete:'تأكيد الحذف؟',
      product:'المنتج', code:'الكود', name:'الاسم', type:'النوع', unit:'الوحدة', specification:'المواصفة', productGroup:'مجموعة المنتج',
      productModel:'الموديل', customerPartNumber:'رقم قطعة العميل', minPackagingQty:'حد أدنى للتعبئة', brand:'العلامة', color:'اللون',
      drawingNumber:'رقم الرسم', productOldId:'رمز المادة القديم', grossWeight:'الوزن الإجمالي', netWeight:'الوزن الصافي', weightUnit:'وحدة الوزن',
      countryOfOrigin:'بلد المنشأ', manufacturerNumber:'رقم المصنع', manufacturerPartNumber:'رقم قطعة المصنع',
      materialRevisionLevel:'مراجعة المادة', serialNumberProfile:'ملف الرقم التسلسلي', productHierarchy:'التسلسل الهرمي',
      divisionCode:'القسم', markedForDeletion:'محذوف', batchManaged:'إدارة الدفعات', traceable:'قابل للتتبع',
      description:'الوصف', parent:'الأصل',
      orderNo:'رقم الأمر', orderType:'نوع الأمر', quantity:'الكمية المخططة', completed:'المكتمل', progress:'التقدم',
      plannedStart:'بداية الخطة', plannedEnd:'نهاية الخطة', productionPlant:'مصنع الإنتاج', storageLocation:'موقع التخزين',
      mrpController:'متحكم MRP', productionVersion:'نسخة الإنتاج', companyCode:'رمز الشركة', profitCenter:'مركز الربح',
      scheduledStart:'بداية الجدولة', scheduledEnd:'نهاية الجدولة', components:'المكونات', operations:'العمليات',
      sequence:'التسلسل', material:'المادة', requiredQuantity:'الكمية المطلوبة', withdrawn:'المسحوب', available:'المتاح',
      reservation:'الحجز', requirementDate:'تاريخ الاحتياج', goodsMovement:'نوع الحركة', workCenter:'مركز العمل',
      operation:'العملية', plannedYield:'الإنتاج المخطط', confirmedYield:'الإنتاج المؤكد',
      jobCode:'رمز المهمة', cron:'تعبير Cron', endpoint:'الواجهة', system:'النظام', lastRun:'آخر تشغيل', nextRun:'التشغيل التالي',
      history:'سجل التشغيل', trigger:'المشغل', permission:'الصلاحية', role:'الدور', users:'المستخدمون', roles:'الأدوار',
      menuManagement:'إدارة القوائم', dataDictionary:'قاموس البيانات', usersManagement:'إدارة المستخدمين', rolesManagement:'إدارة الأدوار',
      usernameExists:'اسم المستخدم موجود', passwordHint:'8 أحرف على الأقل', dictType:'نوع القاموس', dictCode:'رمز القاموس',
      dictValue:'قيمة القاموس', label:'التسمية', parentMenu:'القائمة الرئيسية', path:'مسار الصفحة', iconName:'الأيقونة',
      permissionCode:'رمز الصلاحية', sortOrder:'الترتيب', language:'اللغة', email:'البريد الإلكتروني', displayName:'الاسم المعروض',
      roleCodes:'رموز الأدوار', addUser:'إضافة مستخدم', addRole:'إضافة دور', httpMethod:'طريقة HTTP',
      syncResult:'نتيجة المزامنة', received:'المستلم', created:'الجديد', updated:'المحدث', failed:'الفشل',
      syncProducts:'مزامنة منتجات SAP', syncOrders:'مزامنة أوامر SAP', syncHint:'يستخدم عنوان SAP وبيانات الاعتماد المهيأة على الخادم.',
      integrationSubtitle:'إدارة مزامنة SAP وحالة الواجهات.', jobSubtitle:'عرض الخطط والواجهات ونتائج التشغيل والأخطاء.',
      productSubtitle:'عرض منتجات SAP للقراءة فقط من API_PRODUCT_SRV.', orderSubtitle:'عرض أمر الإنتاج مع المكونات والعمليات.',
      masterSubtitle:'إدارة المؤسسات والمخازن ومراكز العمل.', accessSubtitle:'تكوين صلاحيات الصفحات والأزرار حسب الدور.',
      permissionSaved:'تم حفظ الصلاحيات', sapDisabled:'تكامل SAP غير مفعل. تحقق من إعدادات الخادم.',
      invalidLogin:'اسم المستخدم أو كلمة المرور غير صحيح', saved:'تم الحفظ', started:'بدأت المهمة', failedRequest:'فشل الطلب',
      total:'المجموع', items:'عناصر', perPage:'/ صفحة', page:'صفحة'
    }
  };
  const t = key => i18n[state.lang]?.[key] || i18n['zh-CN'][key] || key;
  const setLanguage = lang => {
    state.lang = lang;
    localStorage.setItem('tns_lang', lang);
    document.documentElement.lang = lang;
    document.documentElement.dir = lang === 'ar-TN' ? 'rtl' : 'ltr';
    $$('[data-i18n]').forEach(n => n.textContent = t(n.dataset.i18n));
    $$('[data-i18n-placeholder]').forEach(n => n.placeholder = t(n.dataset.i18nPlaceholder));
    $$('[data-lang]').forEach(n => n.classList.toggle('active', n.dataset.lang === lang));
    if (state.token) renderView(state.view);
    renderIcons();
  };

  /* ================================================================
     API
     ================================================================ */
  function apiBase() {
    const cfg = window.TNS_MES_CONFIG;
    if (cfg && cfg.apiBaseUrl) return cfg.apiBaseUrl + '/api/v1';
    return '/tns-mes/api/v1';
  }

  async function api(path, options = {}) {
    const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
    if (state.token) headers.Authorization = `Bearer ${state.token}`;
    const res = await fetch(`${apiBase()}${path}`, { ...options, headers, body: options.body === undefined ? undefined : JSON.stringify(options.body) });
    let payload = null;
    try { payload = await res.json(); } catch {}
    if (res.status === 401) { logout(false); throw new Error(t('invalidLogin')); }
    if (!res.ok || (payload && payload.code && payload.code !== 0)) throw new Error(payload?.message || t('failedRequest'));
    return payload;
  }

  /* ================================================================
     TEMPLATE COMPONENTS
     ================================================================ */
  function pageHead(section, title, subtitle, actions = '') {
    return `<div class="page-head"><div><h1>${esc(title)}</h1>${subtitle ? `<p class="page-subtitle">${esc(subtitle)}</p>` : ''}</div><div class="page-actions">${actions}</div></div>`;
  }
  function btn(action, label, kind = 'secondary', permission = '') {
    return `<button class="btn ${kind}" data-action="${action}"${permission ? ` data-permission="${permission}"` : ''}>${label}</button>`;
  }
  function filterField(id, label, type, options, placeholder) {
    if (type === 'select') {
      return `<label><span>${esc(label)}</span><select id="${id}">${options.map(o => typeof o === 'string' ? `<option value="${esc(o)}">${esc(o)}</option>` : `<option value="${esc(o.value)}">${esc(o.label)}</option>`).join('')}</select></label>`;
    }
    return `<label><span>${esc(label)}</span><input id="${id}" type="${type || 'text'}" placeholder="${esc(placeholder || '')}"></label>`;
  }
  function toolbar(fields, extraActions) {
    const actions = extraActions || (btn('query', icon('search') + t('query'), 'primary') + btn('reset', icon('rotate-ccw') + t('reset')) + btn('refresh-data', icon('refresh-cw') + t('refresh')));
    return `<div class="toolbar">${fields.join('')}<div class="toolbar-actions">${actions}</div></div>`;
  }
  function dataTable(headers, rows, sortKey, sortDir) {
    const thead = headers.map((h, i) => {
      const key = typeof h === 'object' ? h.key : null;
      const label = typeof h === 'object' ? h.label : h;
      const sortable = typeof h === 'object' && h.sortable;
      if (!sortable) return `<th>${esc(label)}</th>`;
      const active = sortKey === key;
      const dir = active && sortDir === 'desc' ? 'asc' : 'desc';
      return `<th class="sortable ${active ? 'sort-' + sortDir : ''}" data-sort="${key}" data-next-dir="${dir}">${esc(label)}${active ? icon(sortDir === 'asc' ? 'arrow-up' : 'arrow-down') : icon('arrow-up-down')}</th>`;
    }).join('');
    return `<div class="table-wrap"><table class="data-table"><thead><tr>${thead}</tr></thead><tbody>${rows.length ? rows.join('') : `<tr><td colspan="${headers.length}" class="empty">${t('noData')}</td></tr>`}</tbody></table></div>`;
  }
  function paginationHTML(ps) {
    if (!ps || ps.totalPages <= 1) return '';
    const cur = ps.page, total = ps.totalPages;
    const pages = [];
    for (let i = 0; i < total; i++) {
      if (i === 0 || i === total - 1 || Math.abs(i - cur) <= 1) pages.push(i);
      else if (pages[pages.length - 1] !== -1) pages.push(-1);
    }
    const start = ps.total === 0 ? 0 : cur * ps.size + 1;
    const end = Math.min((cur + 1) * ps.size, ps.total);
    return `<div class="pagination"><span class="total">${t('total')} <strong>${ps.total}</strong> ${t('items')} · ${start}-${end}</span><button data-page="${Math.max(0, cur - 1)}" ${cur === 0 ? 'disabled' : ''}>${icon('chevron-left')}</button>${pages.map(p => p === -1 ? '<span class="ellipsis">…</span>' : `<button data-page="${p}" class="${p === cur ? 'active' : ''}">${p + 1}</button>`).join('')}<button data-page="${Math.min(total - 1, cur + 1)}" ${cur >= total - 1 ? 'disabled' : ''}>${icon('chevron-right')}</button><select data-page-size><option value="10">10 ${t('perPage')}</option><option value="20" ${ps.size === 20 ? 'selected' : ''}>20 ${t('perPage')}</option><option value="50" ${ps.size === 50 ? 'selected' : ''}>50 ${t('perPage')}</option><option value="100">100 ${t('perPage')}</option></select></div>`;
  }
  function statCard(label, value, iconName) {
    return `<div class="stat-card"><small>${esc(label)}</small><strong>${esc(value)}</strong><span class="stat-icon">${icon(iconName)}</span></div>`;
  }
  function detailGrid(fields) {
    return `<div class="detail-grid">${fields.map(([label, v]) => `<div class="detail-item"><small>${esc(label)}</small><strong>${typeof v === 'string' && v.startsWith('<') ? v : escVal(v)}</strong></div>`).join('')}</div>`;
  }
  function sectionTitle(title, count) {
    return `<div class="section-title"><h2>${esc(title)}</h2><span>${count}</span></div>`;
  }
  function emptyState(msg) { return `<div class="empty">${esc(msg || t('noData'))}</div>`; }
  function progressBar(value, total) {
    const pct = total ? Math.round(Number(value || 0) / Number(total) * 100) : 0;
    return `<span class="progress-bar"><span class="bar"><span class="fill" style="width:${pct}%"></span></span><span class="pct">${pct}%</span></span>`;
  }
  function statusPill(value) {
    const cls = ['ACTIVE','RELEASED','IN_PROGRESS','COMPLETED','SUCCESS','IDLE','ENABLED'].includes(String(value)) ? 'success'
      : ['DRAFT','RUNNING','PARTIAL'].includes(String(value)) ? 'warn'
      : ['FAILED','CANCELLED','DISABLED','INACTIVE'].includes(String(value)) ? 'danger' : '';
    return `<span class="status ${cls}">${esc(value || '—')}</span>`;
  }
  function renderIcons() { window.lucide?.createIcons({ attrs: { 'stroke-width': 1.8 } }); }
  function toast(message, error = false) {
    const node = $('#toast');
    node.textContent = message;
    node.className = `toast show${error ? ' error' : ''}`;
    clearTimeout(window.__toast);
    window.__toast = setTimeout(() => node.className = 'toast', 3200);
  }

  /* ================================================================
     PERMISSIONS
     ================================================================ */
  function applyPermissions() {
    const permissions = state.user?.permissions || [];
    $$('[data-permission]').forEach(node => {
      const required = node.dataset.permission;
      const has = !required || permissions.includes(required) || permissions.includes('USER_ADMIN');
      node.classList.toggle('hidden', !has);
    });
  }

  /* ================================================================
     VIEW ROUTER
     ================================================================ */
  const viewTitles = {
    overview: 'overview', master: 'masterData', iam: 'iam', menus: 'menuManagement', dictionaries: 'dataDictionary',
    products: 'products', boms: 'engineering', routes: 'routes', quality: 'quality', batches: 'batches',
    orders: 'workOrders', integration: 'manualSync', jobs: 'scheduledJobs', apiLogs: 'apiLogs'
  };
  function renderView(view) {
    state.view = view;
    $$('.nav-link').forEach(n => n.classList.toggle('active', n.dataset.view === view));
    // Auto-expand parent nav group of active view
    const activeLink = document.querySelector(`[data-view="${view}"]`);
    if (activeLink) {
      const parentGroup = activeLink.closest('.nav-group');
      if (parentGroup) {
        $$('.nav-group').forEach(g => { if (g !== parentGroup) g.classList.add('collapsed'); });
        parentGroup.classList.remove('collapsed');
      }
    }
    $('#current-title').textContent = t(viewTitles[view] || view);
    const renderer = ({
      overview: renderOverview, master: renderMaster, iam: renderIam, menus: renderMenus, dictionaries: renderDictionaries,
      products: renderProducts, boms: renderEngineeringStub, routes: renderEngineeringStub, quality: renderEngineeringStub, batches: renderBatches,
      orders: renderOrders, integration: renderIntegration, jobs: renderJobs, apiLogs: renderApiLogs
    })[view] || renderOverview;
    renderer();
    setTimeout(() => { applyPermissions(); renderIcons(); }, 0);
  }

  /* ================================================================
     OVERVIEW
     ================================================================ */
  async function renderOverview() {
    const node = $('#page');
    node.innerHTML = pageHead(t('overview'), t('overview'), '');
    try {
      const [products, orders] = await Promise.all([api('/products?size=1'), api('/work-orders?size=1')]);
      node.innerHTML += `<div class="stat-grid">${statCard(t('products'), products.data.total || 0, 'package-search')}${statCard(t('workOrders'), orders.data.total || 0, 'list-checks')}${statCard(t('integrationCenter'), 2, 'plug-zap')}${statCard(t('status'), '<span class="status success">ONLINE</span>', 'activity')}</div><div class="panel"><div class="panel-body">${detailGrid([[t('products'), t('productSubtitle')], [t('workOrders'), t('orderSubtitle')]])}</div></div>`;
    } catch (e) { node.innerHTML += emptyState(e.message); }
    renderIcons();
  }

  /* ================================================================
     MASTER DATA
     ================================================================ */
  function renderMaster() {
    const fields = [
      filterField('master-search', t('search'), 'text', null, t('code') + '/' + t('name')),
      filterField('master-type', t('type'), 'select', [
        {value:'FACTORY', label:'Factory'},
        {value:'WORKSHOP', label:'Workshop'},
        {value:'DEPARTMENT', label:'Department'},
        {value:'WAREHOUSE', label:'Warehouse'},
        {value:'WORK_CENTER', label:'Work center'},
        {value:'PRODUCTION_LINE', label:'Production line'},
        {value:'WORKSTATION', label:'Workstation'},
        {value:'PERSON', label:'Person'},
        {value:'POSITION', label:'Position'},
        {value:'CUSTOMER', label:'Customer'},
        {value:'SUPPLIER', label:'Supplier'},
        {value:'MANUFACTURER', label:'Manufacturer'}
      ])
    ];
    $('#page').innerHTML = pageHead(t('foundation'), t('masterData'), t('masterSubtitle'), btn('master-add', icon('plus') + t('add'), 'primary', 'BASIC_DATA_WRITE')) + `<div class="panel">${toolbar(fields)}<div id="master-table"></div></div>`;
    $('#master-type').addEventListener('change', loadMaster);
    loadMaster();
  }
  async function loadMaster() {
    const type = $('#master-type')?.value || 'FACTORY', node = $('#master-table');
    if (!node) return;
    try {
      const data = await api(`/master-data/${type}?size=100&keyword=${encodeURIComponent($('#master-search')?.value || '')}`);
      const items = data.data.items || [];
      state.data.master = items;
      node.innerHTML = items.length ? dataTable([t('code'), t('name'), t('status'), t('actions')], items.map(v => `<tr><td class="code">${escVal(v.code)}</td><td><span class="cell-title">${escVal(v.nameZh)}</span><span class="cell-sub">${escVal(v.nameEn || v.nameAr)}</span></td><td>${statusPill(v.status)}</td><td class="table-actions">${btn('master-detail', icon('eye'), 'ghost')} ${btn('master-edit', icon('pencil'), 'ghost', 'BASIC_DATA_WRITE')} ${btn('master-delete', icon('trash-2'), 'ghost', 'BASIC_DATA_WRITE')}</td></tr>`)) : emptyState();
    } catch (e) { node.innerHTML = emptyState(e.message); }
    renderIcons();
  }

  // Master data helpers
  function getMasterParentTypes(type) {
    switch (type) {
      case 'FACTORY': return ['ENTERPRISE'];
      case 'WORKSHOP': return ['FACTORY'];
      case 'DEPARTMENT': return ['ENTERPRISE', 'FACTORY', 'WORKSHOP'];
      case 'WAREHOUSE': return ['FACTORY'];
      case 'WORK_CENTER': return ['FACTORY', 'WORKSHOP'];
      case 'PRODUCTION_LINE': return ['FACTORY', 'WORKSHOP'];
      case 'WORKSTATION': return ['PRODUCTION_LINE', 'WORK_CENTER'];
      case 'PERSON': return ['DEPARTMENT'];
      case 'POSITION': return ['DEPARTMENT'];
      default: return [];
    }
  }
  async function loadMasterParents(type) {
    const parentTypes = getMasterParentTypes(type);
    if (!parentTypes.length) return [];
    const results = await Promise.all(parentTypes.map(pt => api(`/master-data/${pt}?size=200`).catch(() => ({ data: { items: [] } }))));
    return results.flatMap(r => r.data?.items || []);
  }

  // Master data detail drawer
  async function openMasterDetail(item) {
    const type = $('#master-type')?.value || 'FACTORY';
    let value = item;
    try { value = (await api(`/master-data/${type}/${item.id}`)).data; } catch {}
    const fields = [
      [t('code'), value.code],
      [t('name'), value.nameZh],
      ['English', value.nameEn],
      ['العربية', value.nameAr],
      [t('status'), statusPill(value.status)],
      [t('sortOrder'), value.sortOrder],
      [t('description'), value.description]
    ];
    openDrawer(`${t('masterData')} · ${value.code}`, t('masterSubtitle'),
      `<div class="drawer-body">${detailGrid(fields)}</div>`);
  }

  // Master data create/edit drawer
  async function openMasterCreate() {
    const type = $('#master-type')?.value || 'FACTORY';
    const parents = await loadMasterParents(type);
    const parentOptions = parents.length
      ? `<option value="">—</option>` + parents.map(p => `<option value="${p.id}">${esc(p.code)} - ${esc(p.nameZh)}</option>`).join('')
      : '';
    const parentField = getMasterParentTypes(type).length
      ? `<label><span>${t('parent')}</span><select id="md-parent">${parentOptions}</select></label>`
      : '';
    openDrawer(t('add') + ' · ' + type, t('masterSubtitle'),
      `<div class="drawer-body"><div class="form-grid">
        <label><span>${t('code')}</span><input id="md-code" required></label>
        <label><span>${t('name')}</span><input id="md-name-zh" required></label>
        <label><span>English</span><input id="md-name-en"></label>
        <label><span>العربية</span><input id="md-name-ar"></label>
        ${parentField}
        <label><span>${t('status')}</span><select id="md-status"><option value="ACTIVE">${t('enabled')}</option><option value="INACTIVE">${t('disabled')}</option></select></label>
        <label><span>${t('sortOrder')}</span><input id="md-sort-order" type="number" value="0"></label>
        <label class="full"><span>${t('description')}</span><textarea id="md-description" rows="3"></textarea></label>
      </div></div>
      <div class="form-footer"><button class="btn secondary" data-action="close-drawer">${t('cancel')}</button><button class="btn primary" data-action="save-master" data-mode="create">${t('save')}</button></div>`);
  }
  async function openMasterEdit(item) {
    const type = $('#master-type')?.value || 'FACTORY';
    const parents = await loadMasterParents(type);
    const parentOptions = parents.length
      ? `<option value="">—</option>` + parents.map(p => `<option value="${p.id}" ${p.id === item.parentId ? 'selected' : ''}>${esc(p.code)} - ${esc(p.nameZh)}</option>`).join('')
      : '';
    const parentField = getMasterParentTypes(type).length
      ? `<label><span>${t('parent')}</span><select id="md-parent">${parentOptions}</select></label>`
      : '';
    openDrawer(t('edit') + ' · ' + item.code, t('masterSubtitle'),
      `<div class="drawer-body"><div class="form-grid">
        <label><span>${t('code')}</span><input id="md-code" value="${esc(item.code)}" required></label>
        <label><span>${t('name')}</span><input id="md-name-zh" value="${esc(item.nameZh)}" required></label>
        <label><span>English</span><input id="md-name-en" value="${esc(item.nameEn || '')}"></label>
        <label><span>العربية</span><input id="md-name-ar" value="${esc(item.nameAr || '')}"></label>
        ${parentField}
        <label><span>${t('status')}</span><select id="md-status"><option value="ACTIVE" ${item.status === 'ACTIVE' ? 'selected' : ''}>${t('enabled')}</option><option value="INACTIVE" ${item.status === 'INACTIVE' ? 'selected' : ''}>${t('disabled')}</option></select></label>
        <label><span>${t('sortOrder')}</span><input id="md-sort-order" type="number" value="${item.sortOrder ?? 0}"></label>
        <label class="full"><span>${t('description')}</span><textarea id="md-description" rows="3">${esc(item.description || '')}</textarea></label>
      </div></div>
      <div class="form-footer"><button class="btn secondary" data-action="close-drawer">${t('cancel')}</button><button class="btn primary" data-action="save-master" data-mode="edit" data-id="${item.id}">${t('save')}</button></div>`);
  }

  /* ================================================================
     PRODUCTS
     ================================================================ */
  function renderProducts() {
    const fields = [
      filterField('product-search', t('search'), 'text', null, t('code') + '/' + t('name')),
      filterField('product-status', t('status'), 'select', [{value:'', label:t('all')}, {value:'ACTIVE', label:t('enabled')}, {value:'INACTIVE', label:t('disabled')}]),
      filterField('product-type', t('type'), 'select', [{value:'', label:t('all')}, {value:'FINISHED', label:'FINISHED'}, {value:'COMPONENT', label:'COMPONENT'}])
    ];
    const singleSync = `<div class="panel"><div class="toolbar"><div class="muted">${esc(t('singleSync'))}</div><div class="toolbar-actions"><label><span>${esc(t('productCode'))}</span><input id="single-product" type="text" placeholder="TG123456" style="width:160px"></label>${btn('sync-single-product', icon('refresh-cw') + t('sync'), 'secondary', 'INTEGRATION_WRITE')}</div></div></div>`;
    const actions = btn('sync-products', icon('refresh-cw') + t('syncProducts'), 'primary', 'INTEGRATION_WRITE');
    $('#page').innerHTML = pageHead(t('engineering'), t('products'), t('productSubtitle'), '') + `<div class="panel">${toolbar(fields, actions)}<div id="product-table"></div></div>` + singleSync;
    loadProducts();
  }
  async function loadProducts(page) {
    const p = ps('products'), node = $('#product-table');
    if (page !== undefined) p.page = page;
    if (!node) return;
    try {
      const params = new URLSearchParams({ page: String(p.page), size: String(p.size) });
      const keyword = $('#product-search')?.value;
      if (keyword) params.set('keyword', keyword);
      if ($('#product-status')?.value) params.set('status', $('#product-status').value);
      if ($('#product-type')?.value) params.set('productType', $('#product-type').value);
      const data = await api('/products?' + params);
      p.items = data.data.items || [];
      p.total = data.data.total || 0;
      p.totalPages = data.data.totalPages || 0;
      const headers = [
        { key:'code', label:t('code'), sortable:true },
        { key:'name', label:t('name'), sortable:true },
        { key:'productType', label:t('type'), sortable:true },
        { key:'specification', label:t('specification'), sortable:true },
        { key:'productModel', label:t('productModel'), sortable:true },
        { key:'customerPartNumber', label:t('customerPartNumber'), sortable:true },
        { key:'minPackagingQty', label:t('minPackagingQty'), sortable:true },
        { key:'unit', label:t('unit'), sortable:true },
        { key:'source', label:t('source'), sortable:false },
        { key:'sapLastSyncAt', label:t('lastSync'), sortable:true },
        { key:'actions', label:t('actions'), sortable:false }
      ];
      // Client-side sort
      if (p.sortKey && p.items.length) {
        const key = p.sortKey;
        const dir = p.sortDir === 'desc' ? -1 : 1;
        p.items.sort((a, b) => {
          let va = a[key], vb = b[key];
          if (key === 'name') { va = a.nameZh; vb = b.nameZh; }
          if (va == null) va = '';
          if (vb == null) vb = '';
          if (typeof va === 'number' && typeof vb === 'number') return (va - vb) * dir;
          return String(va).localeCompare(String(vb), 'zh-CN') * dir;
        });
      }
      node.innerHTML = dataTable(
        headers,
        p.items.map(v => `<tr><td class="code">${escVal(v.code)}</td><td><span class="cell-title">${escVal(v.nameZh)}</span><span class="cell-sub">${escVal(v.nameEn || v.nameAr)}</span></td><td>${escVal(v.productType)}</td><td>${escVal(v.specification)}</td><td>${escVal(v.productModel)}</td><td>${escVal(v.customerPartNumber)}</td><td>${escVal(v.minPackagingQty)}</td><td>${escVal(v.unit)}</td><td>${statusPill(v.source || 'SAP')}</td><td>${formatDate(v.sapLastSyncAt)}</td><td class="table-actions">${btn('product-detail', icon('eye'), 'ghost', 'ENGINEERING_READ')} ${btn('product-sync', icon('refresh-cw'), 'ghost', 'INTEGRATION_WRITE')}</td></tr>`),
        p.sortKey, p.sortDir
      ) + paginationHTML(p);
    } catch (e) { node.innerHTML = emptyState(e.message); }
    renderIcons();
  }

  /* ================================================================
     WORK ORDERS
     ================================================================ */
  function renderOrders() {
    const fields = [
      filterField('order-search', t('search'), 'text', null, t('orderNo') + '/' + t('product')),
      filterField('order-status', t('status'), 'select', [{value:'', label:t('all')}, {value:'DRAFT', label:'DRAFT'}, {value:'RELEASED', label:'RELEASED'}, {value:'IN_PROGRESS', label:'IN_PROGRESS'}, {value:'COMPLETED', label:'COMPLETED'}, {value:'CANCELLED', label:'CANCELLED'}]),
      filterField('order-plant', t('productionPlant'), 'text', null, '')
    ];
    const singleSync = `<div class="panel"><div class="toolbar"><div class="muted">${esc(t('singleSync'))}</div><div class="toolbar-actions"><label><span>${esc(t('workOrderNo'))}</span><input id="single-order" type="text" placeholder="1000000" style="width:160px"></label>${btn('sync-single-order', icon('refresh-cw') + t('sync'), 'secondary', 'INTEGRATION_WRITE')}</div></div></div>`;
    const actions = btn('sync-orders', icon('refresh-cw') + t('syncOrders'), 'primary', 'INTEGRATION_WRITE');
    $('#page').innerHTML = pageHead(t('production'), t('workOrders'), t('orderSubtitle'), '') + `<div class="panel">${toolbar(fields, actions)}<div id="order-table"></div></div>` + singleSync;
    loadOrders();
  }
  async function loadOrders(page) {
    const p = ps('orders'), node = $('#order-table');
    if (page !== undefined) p.page = page;
    if (!node) return;
    try {
      const params = new URLSearchParams({ page: String(p.page), size: String(p.size) });
      [['keyword', 'order-search'], ['status', 'order-status'], ['plant', 'order-plant']].forEach(([key, id]) => { const v = $('#' + id)?.value; if (v) params.set(key, v); });
      const data = await api('/work-orders?' + params);
      p.items = data.data.items || [];
      p.total = data.data.total || 0;
      p.totalPages = data.data.totalPages || 0;
      const headers = [
        { key:'orderNo', label:t('orderNo'), sortable:true },
        { key:'product', label:t('product'), sortable:true },
        { key:'orderType', label:t('orderType'), sortable:true },
        { key:'productionPlant', label:t('productionPlant'), sortable:true },
        { key:'quantity', label:t('quantity'), sortable:true },
        { key:'completedQuantity', label:t('completed'), sortable:true },
        { key:'progress', label:t('progress'), sortable:false },
        { key:'plannedStart', label:t('plannedStart'), sortable:true },
        { key:'status', label:t('status'), sortable:true },
        { key:'actions', label:t('actions'), sortable:false }
      ];
      // Client-side sort
      if (p.sortKey && p.items.length) {
        const key = p.sortKey;
        const dir = p.sortDir === 'desc' ? -1 : 1;
        p.items.sort((a, b) => {
          let va = a[key], vb = b[key];
          if (key === 'product') { va = a.productCode; vb = b.productCode; }
          if (key === 'productionPlant') { va = a.productionPlant || a.plant; vb = b.productionPlant || b.plant; }
          if (va == null) va = '';
          if (vb == null) vb = '';
          if (typeof va === 'number' && typeof vb === 'number') return (va - vb) * dir;
          return String(va).localeCompare(String(vb), 'zh-CN') * dir;
        });
      }
      node.innerHTML = dataTable(
        headers,
        p.items.map(v => `<tr><td class="code">${escVal(v.orderNo)}</td><td><span class="cell-title">${escVal(v.productCode)}</span><span class="cell-sub">${escVal(v.productNameZh || v.productNameEn || v.productNameAr)}</span></td><td>${escVal(v.orderType)}</td><td>${escVal(v.productionPlant || v.plant)}</td><td>${escVal(v.quantity)} ${escVal(v.productionUnit)}</td><td>${escVal(v.completedQuantity)}</td><td>${progressBar(v.completedQuantity, v.quantity)}</td><td>${formatDate(v.plannedStart)}</td><td>${statusPill(v.status)}</td><td class="table-actions">${btn('order-detail', icon('eye'), 'ghost', 'WORK_ORDER_READ')} ${btn('order-sync', icon('refresh-cw'), 'ghost', 'INTEGRATION_WRITE')}</td></tr>`),
        p.sortKey, p.sortDir
      ) + paginationHTML(p);
    } catch (e) { node.innerHTML = emptyState(e.message); }
    renderIcons();
  }

  /* ================================================================
     BATCHES
     ================================================================ */
  function renderBatches() {
    const fields = [
      filterField('batch-search', t('search'), 'text', null, t('batchNo') + '/' + t('product')),
      filterField('batch-status', t('batchStatus'), 'select', [{value:'', label:t('all')}, {value:'RELEASED', label:'RELEASED'}, {value:'RESTRICTED', label:'RESTRICTED'}, {value:'UNREST', label:'UNREST'}]),
      filterField('batch-plant', t('plant'), 'text', null, '')
    ];
    const singleSync = `<div class="panel"><div class="toolbar"><div class="muted">${esc(t('singleSync'))}</div><div class="toolbar-actions"><label><span>${esc(t('batchNo'))}</span><input id="single-batch" type="text" placeholder="B000123" style="width:160px"></label>${btn('sync-single-batch', icon('refresh-cw') + t('sync'), 'secondary', 'INTEGRATION_WRITE')}</div></div></div>`;
    const actions = btn('sync-batches', icon('refresh-cw') + t('syncBatches'), 'primary', 'INTEGRATION_WRITE');
    $('#page').innerHTML = pageHead(t('quality'), t('batches'), t('batchSubtitle'), '') + `<div class="panel">${toolbar(fields, actions)}<div id="batch-table"></div></div>` + singleSync;
    loadBatches();
  }
  async function loadBatches(page) {
    const p = ps('batches'), node = $('#batch-table');
    if (page !== undefined) p.page = page;
    if (!node) return;
    try {
      const params = new URLSearchParams({ page: String(p.page), size: String(p.size) });
      const keyword = $('#batch-search')?.value;
      if (keyword) params.set('keyword', keyword);
      if ($('#batch-status')?.value) params.set('batchStatus', $('#batch-status').value);
      if ($('#batch-plant')?.value) params.set('plant', $('#batch-plant').value);
      const data = await api('/quality/batches?' + params);
      p.items = data.data.items || [];
      p.total = data.data.total || 0;
      p.totalPages = data.data.totalPages || 0;
      const headers = [
        { key:'batchNo', label:t('batchNo'), sortable:true },
        { key:'productCode', label:t('product'), sortable:true },
        { key:'plant', label:t('plant'), sortable:true },
        { key:'batchStatus', label:t('batchStatus'), sortable:true },
        { key:'quantity', label:t('quantity'), sortable:true },
        { key:'manufactureDate', label:t('manufactureDate'), sortable:true },
        { key:'expirationDate', label:t('expirationDate'), sortable:true },
        { key:'shelfLifeExpirationDate', label:t('shelfLifeExpirationDate'), sortable:true },
        { key:'supplierBatch', label:t('supplierBatch'), sortable:true },
        { key:'inspectionLot', label:t('inspectionLot'), sortable:true },
        { key:'lastSync', label:t('lastSync'), sortable:false }
      ];
      // Client-side sort
      if (p.sortKey && p.items.length) {
        const key = p.sortKey;
        const dir = p.sortDir === 'desc' ? -1 : 1;
        p.items.sort((a, b) => {
          let va = a[key], vb = b[key];
          if (va == null) va = '';
          if (vb == null) vb = '';
          if (typeof va === 'number' && typeof vb === 'number') return (va - vb) * dir;
          return String(va).localeCompare(String(vb), 'zh-CN') * dir;
        });
      }
      node.innerHTML = dataTable(
        headers,
        p.items.map(v => `<tr><td class="code">${escVal(v.batchNo)}</td><td><span class="cell-title">${escVal(v.productCode)}</span><span class="cell-sub">${escVal(v.productName || '')}</span></td><td>${escVal(v.plant)}</td><td>${statusPill(v.batchStatus)}</td><td>${escVal(v.quantity)} ${escVal(v.unit)}</td><td>${formatDate(v.manufactureDate)}</td><td>${formatDate(v.expirationDate)}</td><td>${formatDate(v.shelfLifeExpirationDate)}</td><td>${escVal(v.supplierBatch)}</td><td>${escVal(v.inspectionLot)}</td><td>${formatDate(v.sapLastSyncAt)}</td></tr>`),
        p.sortKey, p.sortDir
      ) + paginationHTML(p);
    } catch (e) { node.innerHTML = emptyState(e.message); }
    renderIcons();
  }

  /* ================================================================
     INTEGRATION
     ================================================================ */
  async function renderIntegration() {
    const singleSyncHTML = `<div class="panel">
      <div class="toolbar">
        <div class="muted">${esc(t('singleSync'))}</div>
        <div class="toolbar-actions">
          <label><span>${esc(t('productCode'))}</span><input id="single-product" type="text" placeholder="TG123456"></label>
          ${btn('sync-single-product', icon('package-check') + t('sync'), 'secondary', 'INTEGRATION_WRITE')}
          <label><span>${esc(t('workOrderNo'))}</span><input id="single-order" type="text" placeholder="1000000"></label>
          ${btn('sync-single-order', icon('clipboard-sync') + t('sync'), 'secondary', 'INTEGRATION_WRITE')}
          <label><span>${esc(t('batchNo'))}</span><input id="single-batch" type="text" placeholder="B000123"></label>
          ${btn('sync-single-batch', icon('boxes') + t('sync'), 'secondary', 'INTEGRATION_WRITE')}
        </div>
      </div>
    </div>`;
    $('#page').innerHTML = pageHead(t('integrationCenter'), t('manualSync'), t('integrationSubtitle')) +
      `<div class="panel"><div class="toolbar"><div id="sync-hint" class="muted">${esc(t('syncHint'))}</div><div class="toolbar-actions">${btn('sync-products', icon('package-check') + t('syncProducts'), 'primary', 'INTEGRATION_WRITE')} ${btn('sync-orders', icon('clipboard-sync') + t('syncOrders'), 'secondary', 'INTEGRATION_WRITE')} ${btn('sync-batches', icon('boxes') + t('syncBatches'), 'secondary', 'INTEGRATION_WRITE')} ${btn('refresh-data', icon('refresh-cw') + t('refresh'))}</div></div><div id="sync-result" class="empty">${t('noData')}</div></div>` + singleSyncHTML;
    try {
      const health = await api('/integrations/sap/request', { method: 'POST', body: { path: '/sap/opu/odata/sap/API_PRODUCT_SRV/A_Product', method: 'GET', query: { '$top': 1 } } });
      $('#sync-hint').textContent = `${t('syncHint')} · HTTP ${health.data.status}`;
    } catch { $('#sync-hint').textContent = t('sapDisabled'); }
    renderIcons();
  }
  async function runSync(kind, trigger) {
    const original = trigger.innerHTML;
    trigger.disabled = true;
    trigger.innerHTML = icon('loader-circle') + t('run') + '…';
    try {
      const endpoint = kind === 'products' ? 'products' : kind === 'batches' ? 'batches' : 'work-orders';
      const data = await api(`/integrations/sap/${endpoint}/sync`, { method: 'POST', body: { minutes: 15 } });
      const v = data.data || {};
      const resultNode = $('#sync-result');
      if (resultNode) resultNode.innerHTML = `<div class="section-title"><h2>${t('syncResult')}</h2><span>${formatDate(new Date())} · 15 min</span></div>${detailGrid([[t('received'), v.received || 0], [t('created'), v.created || 0], [t('updated'), v.updated || 0], [t('failed'), v.failed || 0]])}${v.errors?.length ? `<pre style="white-space:pre-wrap;background:#f5f8fc;padding:12px;border-radius:6px;font-size:12px;margin-top:12px">${esc(v.errors.join('\n'))}</pre>` : ''}`;
      toast(`${t('saved')} · ${v.received || 0} ${t('received')}`);
      if (state.view === 'products') await loadProducts();
      if (state.view === 'orders') await loadOrders();
      if (state.view === 'batches') await loadBatches();
    } catch (e) { toast(e.message, true); }
    finally { trigger.disabled = false; trigger.innerHTML = original; renderIcons(); }
  }
  async function runSingleSync(kind, value, trigger) {
    const original = trigger.innerHTML;
    trigger.disabled = true;
    trigger.innerHTML = icon('loader-circle');
    try {
      const endpoint = kind === 'products' ? 'products' : kind === 'batches' ? 'batches' : 'work-orders';
      const result = await api(`/integrations/sap/${endpoint}/${encodeURIComponent(value)}/sync`, { method: 'POST', body: {} });
      const v = result.data || {};
      const resultNode = $('#sync-result');
      if (resultNode) resultNode.innerHTML = `<div class="section-title"><h2>${t('syncResult')}</h2><span>${formatDate(new Date())} · ${esc(value)}</span></div>${detailGrid([[t('received'), v.received || 0], [t('created'), v.created || 0], [t('updated'), v.updated || 0], [t('failed'), v.failed || 0]])}${v.errors?.length ? `<pre style="white-space:pre-wrap;background:#f5f8fc;padding:12px;border-radius:6px;font-size:12px;margin-top:12px">${esc(v.errors.join('\n'))}</pre>` : ''}`;
      toast(`${t('saved')} · ${v.received || 0} ${t('received')}`);
      if (kind === 'products' && state.view === 'products') await loadProducts();
      if (kind === 'orders' && state.view === 'orders') await loadOrders();
      if (kind === 'batches' && state.view === 'batches') await loadBatches();
    } catch (e) { toast(e.message, true); }
    finally { trigger.disabled = false; trigger.innerHTML = original; renderIcons(); }
  }

  /* ================================================================
     API LOGS
     ================================================================ */
  function renderApiLogs() {
    const fields = [
      filterField('log-search', t('search'), 'text', null, t('endpoint')),
      filterField('log-system', t('system'), 'select', [
        {value:'', label: t('all')},
        {value:'SAP', label:'SAP'}
      ])
    ];
    $('#page').innerHTML = pageHead(t('integrationCenter'), t('apiLogs'), t('apiLogsSubtitle')) + `<div class="panel">${toolbar(fields)}<div id="log-table"></div></div>`;
    loadApiLogs();
  }
  async function loadApiLogs(page) {
    const p = ps('apiLogs'), node = $('#log-table');
    if (page !== undefined) p.page = page;
    if (!node) return;
    try {
      const params = new URLSearchParams({ page: String(p.page), size: String(p.size) });
      const ep = $('#log-search')?.value;
      const sys = $('#log-system')?.value;
      if (ep) params.set('endpoint', ep);
      if (sys) params.set('system', sys);
      const data = await api('/integrations/logs?' + params);
      p.items = data.data.items || [];
      p.total = data.data.total || 0;
      p.totalPages = data.data.totalPages || 0;
      node.innerHTML = dataTable(
        ['#', 'System', t('httpMethod'), t('endpoint'), t('status'), '耗时(ms)', t('lastSync'), t('actions')],
        p.items.map((v, i) => `<tr><td>${p.page * p.size + i + 1}</td><td>${statusPill(v.systemCode)}</td><td><span class="status ${v.success ? 'success' : 'danger'}">${escVal(v.httpMethod)}</span></td><td class="code" style="max-width:300px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${esc(v.endpoint || '')}">${escVal(v.endpoint)}</td><td>${statusPill(v.responseStatus)}</td><td>${escVal(v.durationMs)}</td><td>${formatDate(v.createdAt)}</td><td class="table-actions">${btn('log-detail', icon('eye'), 'ghost')}</td></tr>`)
      ) + paginationHTML(p);
    } catch (e) { node.innerHTML = emptyState(e.message); }
    renderIcons();
  }
  function openLogDetail(log) {
    openDrawer(`API Log · ${log.id}`, log.endpoint || '',
      `<div class="drawer-body">${detailGrid([
        [t('httpMethod'), log.httpMethod],
        ['System', log.systemCode],
        [t('status'), statusPill(log.responseStatus)],
        ['耗时(ms)', log.durationMs + ' ms'],
        [t('lastSync'), formatDate(log.createdAt)],
        [t('details'), log.success ? '<span class="status success">SUCCESS</span>' : '<span class="status danger">FAILED</span>']
      ])}${log.errorMessage ? `<div class="section-title"><h2>Error</h2></div><pre style="white-space:pre-wrap;background:#FEF2F2;padding:12px;border-radius:8px;font-size:12px;color:#DC2626;max-height:200px;overflow:auto">${esc(log.errorMessage)}</pre>` : ''}
      <div class="section-title"><h2>Request Params</h2></div>
      <pre style="white-space:pre-wrap;background:#F8FAFC;padding:12px;border-radius:8px;font-size:12px;max-height:200px;overflow:auto">${esc(log.requestParams || '{}')}</pre>
      ${log.requestBody ? `<div class="section-title"><h2>Request Body</h2></div><pre style="white-space:pre-wrap;background:#F8FAFC;padding:12px;border-radius:8px;font-size:12px;max-height:200px;overflow:auto">${esc(log.requestBody)}</pre>` : ''}
      <div class="section-title"><h2>Response</h2></div>
      <pre style="white-space:pre-wrap;background:#F0FDF4;padding:12px;border-radius:8px;font-size:12px;max-height:300px;overflow:auto">${esc(log.responseBody || '—')}</pre>
      </div>`);
  }

  /* ================================================================
     SYNC JOBS
     ================================================================ */
  function renderJobs() {
    const fields = [
      filterField('job-search', t('search'), 'text', null, t('jobCode') + '/' + t('name')),
      filterField('job-status', t('status'), 'select', [
        {value:'', label: t('all')},
        {value:'ENABLED', label: t('enabled')},
        {value:'DISABLED', label: t('disabled')},
        {value:'SUCCESS', label:'SUCCESS'},
        {value:'FAILED', label:'FAILED'}
      ])
    ];
    $('#page').innerHTML = pageHead(t('integrationCenter'), t('scheduledJobs'), t('jobSubtitle')) + `<div class="panel">${toolbar(fields)}<div id="job-table"></div></div>`;
    loadJobs();
  }
  async function loadJobs() {
    const node = $('#job-table');
    if (!node) return;
    try {
      const params = new URLSearchParams();
      const kw = $('#job-search')?.value, st = $('#job-status')?.value;
      if (kw) params.set('keyword', kw);
      if (st) params.set('status', st);
      const data = await api('/integrations/sync-jobs?' + params);
      const items = data.data || [];
      state.data.jobs = items;
      node.innerHTML = items.length ? dataTable([t('jobCode'), t('name'), t('system'), t('cron'), t('endpoint'), t('status'), t('lastRun'), t('nextRun'), t('actions')], items.map(v => `<tr><td class="code">${escVal(v.code)}</td><td><span class="cell-title">${escVal(v.nameZh)}</span><span class="cell-sub">${escVal(v.nameEn || v.nameAr)}</span></td><td>${escVal(v.systemCode)}</td><td class="code">${escVal(v.cronExpression)}</td><td class="code">${escVal(v.endpoint)}</td><td>${statusPill(v.status)}</td><td>${formatDate(v.lastRunAt)}</td><td>${formatDate(v.nextRunAt)}</td><td class="table-actions">${btn('job-detail', icon('eye'), 'ghost')} ${btn('job-run', icon('play'), 'ghost', 'INTEGRATION_WRITE')} ${btn('job-toggle', icon(v.enabled ? 'pause' : 'play'), 'ghost', 'INTEGRATION_WRITE')}</td></tr>`)) : emptyState();
    } catch (e) { node.innerHTML = emptyState(e.message); }
    renderIcons();
  }

  /* ================================================================
     IAM (Roles & Users)
     ================================================================ */
  function renderIam() {
    $('#page').innerHTML = pageHead(t('foundation'), t('iam'), t('accessSubtitle')) + `<div class="tabs"><button class="tab active" data-tab="roles">${t('rolesManagement')}</button><button class="tab" data-tab="users">${t('usersManagement')}</button></div><div class="panel"><div class="toolbar"><div class="muted">${t('accessSubtitle')}</div><div class="toolbar-actions">${btn('add-role', icon('plus') + t('addRole'), 'primary', 'USER_ADMIN')} ${btn('add-user', icon('user-plus') + t('addUser'), 'secondary', 'USER_ADMIN')} ${btn('refresh-data', icon('refresh-cw') + t('refresh'))}</div></div><div id="iam-table"></div></div>`;
    loadRoles();
  }
  async function loadRoles() {
    const node = $('#iam-table');
    try {
      const data = await api('/iam/roles?size=200');
      state.data.roles = data.data.items || [];
      node.innerHTML = dataTable([t('code'), t('name'), t('permission'), t('status'), t('actions')], state.data.roles.map(v => `<tr><td class="code">${escVal(v.code)}</td><td>${escVal(v.nameZh)}<span class="cell-sub">${escVal(v.nameEn || v.nameAr)}</span></td><td>${(v.permissions || []).length}</td><td>${statusPill(v.status)}</td><td class="table-actions">${btn('role-detail', icon('shield-check'), 'ghost', 'USER_ADMIN')} ${btn('role-delete', icon('trash-2'), 'ghost', 'USER_ADMIN')}</td></tr>`));
    } catch (e) { node.innerHTML = emptyState(e.message); }
    renderIcons();
  }
  async function loadUsers() {
    const node = $('#iam-table');
    try {
      const data = await api('/iam/users?size=200');
      state.data.users = data.data.items || [];
      node.innerHTML = dataTable([t('username'), t('name'), t('email'), t('language'), t('role'), t('status'), t('actions')], state.data.users.map(v => `<tr><td class="code">${escVal(v.username)}</td><td>${escVal(v.displayName)}</td><td>${escVal(v.email)}</td><td>${escVal(v.languageCode)}</td><td>${escVal((v.roles || []).join(', '))}</td><td>${statusPill(v.status)}</td><td class="table-actions">${btn('user-toggle', icon(v.status === 'ACTIVE' ? 'user-round-x' : 'user-round-check'), 'ghost', 'USER_ADMIN')} ${btn('user-delete', icon('trash-2'), 'ghost', 'USER_ADMIN')}</td></tr>`));
    } catch (e) { node.innerHTML = emptyState(e.message); }
    renderIcons();
  }

  /* ================================================================
     MENUS
     ================================================================ */
  function renderMenus() {
    $('#page').innerHTML = pageHead(t('foundation'), t('menuManagement'), t('accessSubtitle'), btn('menu-add', icon('plus') + t('add'), 'primary', 'USER_ADMIN')) + `<div class="panel"><div class="toolbar"><label class="grow"><input id="menu-search" placeholder="${esc(t('search'))}"></label><div class="toolbar-actions">${btn('refresh-data', icon('refresh-cw') + t('refresh'))}</div></div><div id="menu-table"></div></div>`;
    loadMenus();
  }
  async function loadMenus() {
    const node = $('#menu-table');
    try {
      state.data.menus = (await api('/system/menus')).data || [];
      // Build hierarchy display
      const buildMenuRows = (items, parentCode, depth) => {
        const children = items.filter(v => (v.parentCode || null) === parentCode);
        return children.map(v => {
          const indent = depth > 0 ? '│&nbsp;'.repeat(depth) + '├&nbsp;' : '';
          const hasChildren = items.some(c => c.parentCode === v.code);
          const toggleBtn = v.status === 'ACTIVE' ? btn('menu-toggle', icon('circle-check'), 'ghost', 'USER_ADMIN') : btn('menu-toggle', icon('circle-x'), 'ghost', 'USER_ADMIN');
          return `<tr><td class="code">${escVal(v.code)}</td><td>${indent}${escVal(v.nameZh)}<span class="cell-sub">${escVal(v.nameEn || v.nameAr)}</span></td><td class="code">${escVal(v.parentCode || '—')}</td><td class="code">${escVal(v.path || '—')}</td><td class="code">${escVal(v.permissionCode || '—')}</td><td>${escVal(v.sortOrder ?? 0)}</td><td>${statusPill(v.status)}</td><td class="table-actions">${btn('menu-edit', icon('pencil'), 'ghost', 'USER_ADMIN')} ${toggleBtn} ${btn('menu-delete', icon('trash-2'), 'ghost', 'USER_ADMIN')}</td></tr>` + buildMenuRows(items, v.code, depth + 1);
        }).join('');
      };
      const rows = buildMenuRows(state.data.menus, null, 0);
      node.innerHTML = rows ? dataTable([t('code'), t('name'), t('parentMenu'), t('path'), t('permissionCode'), t('sortOrder'), t('status'), t('actions')], rows) : emptyState();
    } catch (e) { node.innerHTML = emptyState(e.message); }
    renderIcons();
  }

  /* ================================================================
     DICTIONARIES
     ================================================================ */
  function renderDictionaries() {
    $('#page').innerHTML = pageHead(t('foundation'), t('dataDictionary'), t('accessSubtitle'), btn('dictionary-add', icon('plus') + t('add'), 'primary', 'USER_ADMIN')) + `<div class="panel"><div class="toolbar"><label class="grow"><input id="dictionary-search" placeholder="${esc(t('search'))}"></label><div class="toolbar-actions">${btn('refresh-data', icon('refresh-cw') + t('refresh'))}</div></div><div id="dictionary-table"></div></div>`;
    loadDictionaries();
  }
  async function loadDictionaries() {
    const node = $('#dictionary-table');
    try {
      state.data.dictionaries = (await api('/system/dictionaries')).data || [];
      node.innerHTML = state.data.dictionaries.length ? dataTable([t('dictType'), t('dictCode'), t('label'), t('dictValue'), t('sortOrder'), t('status'), t('actions')], state.data.dictionaries.map(v => `<tr><td>${escVal(v.dictType)}</td><td class="code">${escVal(v.dictCode)}</td><td>${escVal(v.labelZh)}<span class="cell-sub">${escVal(v.labelEn || v.labelAr)}</span></td><td>${escVal(v.dictValue)}</td><td>${escVal(v.sortOrder)}</td><td>${statusPill(v.status)}</td><td>${btn('dictionary-delete', icon('trash-2'), 'ghost', 'USER_ADMIN')}</td></tr>`)) : emptyState();
    } catch (e) { node.innerHTML = emptyState(e.message); }
    renderIcons();
  }

  function renderEngineeringStub() {
    const title = t(state.view);
    $('#page').innerHTML = pageHead(t('engineering'), title, t('engineering'));
  }

  /* ================================================================
     DRAWERS
     ================================================================ */
  function openDrawer(title, subtitle, content) {
    $('#overlay-root').innerHTML = `<div class="drawer-backdrop" data-action="close-drawer"><aside class="drawer" data-stop-close><div class="drawer-header"><div><h2>${esc(title)}</h2><p>${esc(subtitle || '')}</p></div><button class="icon-btn" data-action="close-drawer">${icon('x')}</button></div>${content}</aside></div>`;
    renderIcons();
  }
  function closeDrawer() { $('#overlay-root').innerHTML = ''; }

  // Product Detail - shows all fields
  async function openProductDetail(product) {
    let value = product;
    try { value = (await api('/products/' + product.id)).data; } catch {}
    const fields = [
      [t('code'), value.code], [t('name'), value.nameZh], [t('source'), value.source], [t('type'), value.productType],
      [t('unit'), value.unit], [t('specification'), value.specification], [t('productModel'), value.productModel],
      [t('customerPartNumber'), value.customerPartNumber], [t('minPackagingQty'), value.minPackagingQty],
      [t('drawingNumber'), value.drawingNumber], [t('brand'), value.brand], [t('color'), value.color],
      [t('productGroup'), value.productGroup], [t('productOldId'), value.productOldId],
      [t('productHierarchy'), value.productHierarchy], [t('divisionCode'), value.divisionCode],
      [t('manufacturerNumber'), value.manufacturerNumber], [t('manufacturerPartNumber'), value.manufacturerPartNumber],
      [t('materialRevisionLevel'), value.materialRevisionLevel], [t('serialNumberProfile'), value.serialNumberProfile],
      [t('grossWeight'), value.grossWeight && `${value.grossWeight} ${value.weightUnit || ''}`],
      [t('netWeight'), value.netWeight && `${value.netWeight} ${value.weightUnit || ''}`],
      [t('countryOfOrigin'), value.countryOfOrigin],
      [t('batchManaged'), value.batchManaged ? t('enabled') : t('disabled')],
      [t('traceable'), value.traceable ? t('enabled') : t('disabled')],
      [t('markedForDeletion'), value.markedForDeletion ? t('enabled') : t('disabled')],
      [t('createdAt'), formatDate(value.sapCreatedAt || value.createdAt)],
      [t('changedAt'), formatDate(value.sapChangedAt || value.updatedAt)],
      [t('lastSync'), formatDate(value.sapLastSyncAt)]
    ];
    openDrawer(`${t('products')} · ${value.code}`, t('productSubtitle'), `<div class="drawer-body">${detailGrid(fields)}</div>`);
  }

  // Work Order Detail - shows all fields + components + operations
  async function openOrderDetail(order) {
    try { order = (await api('/work-orders/' + order.id)).data; } catch {}
    const summary = [
      [t('orderNo'), order.orderNo], [t('product'), `${order.productCode || ''} ${order.productNameZh || ''}`],
      [t('orderType'), order.orderType], [t('status'), statusPill(order.status)],
      [t('quantity'), `${order.quantity || '—'} ${order.productionUnit || ''}`], [t('completed'), order.completedQuantity],
      [t('progress'), progressBar(order.completedQuantity, order.quantity)],
      [t('productionPlant'), order.productionPlant || order.plant], [t('storageLocation'), order.storageLocation],
      [t('mrpController'), order.mrpController], [t('productionVersion'), order.productionVersion],
      [t('companyCode'), order.companyCode], [t('profitCenter'), order.profitCenter],
      [t('plannedStart'), formatDate(order.plannedStart)], [t('plannedEnd'), formatDate(order.plannedEnd)],
      [t('scheduledStart'), formatDate(order.scheduledStart)], [t('scheduledEnd'), formatDate(order.scheduledEnd)],
      [t('source'), order.source], [t('lastSync'), formatDate(order.sapLastSyncAt)]
    ];
    const components = order.components || [], operations = order.operations || [];
    const compTable = components.length ? dataTable([t('sequence'), t('material'), t('requiredQuantity'), t('withdrawn'), t('available'), t('unit'), t('reservation'), t('requirementDate'), t('goodsMovement')], components.map(v => `<tr><td>${escVal(v.sequenceNo)}</td><td><span class="cell-title">${escVal(v.productCode)}</span><span class="cell-sub">${escVal(v.productName || v.itemDescription)}</span></td><td>${escVal(v.requiredQuantity)}</td><td>${escVal(v.withdrawnQuantity)}</td><td>${escVal(v.availableQuantity)}</td><td>${escVal(v.unit)}</td><td>${escVal(v.reservationNo)} / ${escVal(v.reservationItem)}</td><td>${escVal(v.requirementDate)}</td><td>${escVal(v.goodsMovementType)}</td></tr>`)) : `<div class="empty">${t('noData')}</div>`;
    const opTable = operations.length ? dataTable([t('sequence'), t('operation'), t('workCenter'), t('plant'), t('plannedYield'), t('confirmedYield'), t('status')], operations.map(v => `<tr><td>${escVal(v.sequenceNo)}</td><td><span class="cell-title">${escVal(v.operationCode)}</span><span class="cell-sub">${escVal(v.operationNameZh || v.operationNameEn || v.operationNameAr)}</span></td><td>${escVal(v.workCenterCode)}</td><td>${escVal(v.plant)}</td><td>${escVal(v.plannedYieldQuantity || v.plannedQuantity)}</td><td>${escVal(v.confirmedYieldQuantity || v.completedQuantity)}</td><td>${statusPill(v.status)}</td></tr>`)) : `<div class="empty">${t('noData')}</div>`;
    openDrawer(`${t('workOrders')} · ${order.orderNo}`, t('orderSubtitle'),
      `<div class="drawer-body">${detailGrid(summary)}${sectionTitle(t('components'), components.length)}<div class="subtable">${compTable}</div>${sectionTitle(t('operations'), operations.length)}<div class="subtable">${opTable}</div></div>`);
  }

  // Job Detail
  async function openJobDetail(job) {
    try { job = (await api('/integrations/sync-jobs/' + job.id)).data; } catch {}
    const historyRows = (job.runs || []).map(v => `<tr><td>${escVal(v.triggerType)}</td><td>${statusPill(v.status)}</td><td class="code">${escVal(v.endpoint)}</td><td>${formatDate(v.startedAt)}</td><td>${escVal(v.received)}</td><td>${escVal(v.created)}</td><td>${escVal(v.updated)}</td><td>${escVal(v.failed)}</td></tr>`);
    openDrawer(`${t('scheduledJobs')} · ${job.code}`, t('jobSubtitle'),
      `<div class="drawer-body"><div class="job-detail"><div class="job-meta"><dl><dt>${t('jobCode')}</dt><dd>${escVal(job.code)}</dd><dt>${t('system')}</dt><dd>${escVal(job.systemCode)}</dd><dt>${t('cron')}</dt><dd>${escVal(job.cronExpression)}</dd><dt>${t('status')}</dt><dd>${statusPill(job.status)}</dd><dt>${t('lastRun')}</dt><dd>${formatDate(job.lastRunAt)}</dd><dt>${t('nextRun')}</dt><dd>${formatDate(job.nextRunAt)}</dd></dl></div><div class="job-meta"><dl><dt>${t('endpoint')}</dt><dd>${escVal(job.endpoint)}</dd><dt>HTTP</dt><dd>${escVal(job.httpMethod)}</dd><dt>${t('description')}</dt><dd>${escVal(job.description)}</dd></dl></div></div>${sectionTitle(t('history'), (job.runs || []).length)}<div class="subtable">${dataTable([t('trigger'), t('status'), t('endpoint'), t('lastRun'), t('received'), t('created'), t('updated'), t('failed')], historyRows)}</div></div>`);
  }

  // Role Detail with permission checkboxes (tree layout for scalability)
  async function openRoleDetail(role) {
    const permissions = (await api('/iam/permissions')).data || [];
    const groups = permissions.reduce((map, p) => { (map[p.groupCode] = map[p.groupCode] || []).push(p); return map; }, {});
    const rolePerms = new Set(role.permissions || []);
    const groupNames = { MASTER_DATA: t('foundation'), ENGINEERING: t('engineering'), PRODUCTION: t('production'), INTEGRATION: t('integrationCenter'), QUALITY: t('quality'), SECURITY: t('iam'), SYSTEM: t('system'), OVERVIEW: t('overview') };
    const groupHTML = Object.entries(groups).map(([group, list]) => {
      const allChecked = list.every(p => rolePerms.has(p.code));
      const someChecked = list.some(p => rolePerms.has(p.code));
      return `<div class="perm-group" data-group="${esc(group)}">
        <div class="perm-group-header" style="display:flex;align-items:center;gap:8px;padding:8px 12px;background:var(--bg-table-header);border-radius:var(--r-sm);cursor:pointer;margin-bottom:4px;">
          <input type="checkbox" class="perm-group-all" ${allChecked ? 'checked' : ''} style="accent-color:var(--primary)">
          <i data-lucide="chevron-down" style="width:14px;height:14px"></i>
          <strong>${esc(groupNames[group] || group)}</strong>
          <span class="muted" style="margin-left:auto;font-size:12px">${list.filter(p=>rolePerms.has(p.code)).length}/${list.length}</span>
        </div>
        <div class="perm-group-body" style="padding:4px 0 8px 28px;display:flex;flex-wrap:wrap;gap:4px;">
          ${list.map(p => `<label style="display:inline-flex;align-items:center;gap:4px;padding:4px 8px;background:var(--bg-page);border-radius:var(--r-sm);cursor:pointer;font-size:12px;"><input type="checkbox" value="${esc(p.code)}" class="perm-item" ${rolePerms.has(p.code) ? 'checked' : ''} style="accent-color:var(--primary)"><span>${esc(p.nameZh)}</span><span class="cell-sub" style="font-size:10px;color:var(--text-tertiary)">${esc(p.permissionType)}</span></label>`).join('')}
        </div>
      </div>`;
    }).join('');
    openDrawer(`${t('role')} · ${role.code}`, t('accessSubtitle'),
      `<div class="drawer-body"><div class="form-grid"><label><span>${t('name')}</span><input id="role-name-zh" value="${esc(role.nameZh)}"></label><label><span>English</span><input id="role-name-en" value="${esc(role.nameEn)}"></label><label><span>العربية</span><input id="role-name-ar" value="${esc(role.nameAr)}"></label></div>${sectionTitle(t('permission'), (role.permissions || []).length)}<div class="permission-groups">${groupHTML}</div></div><div class="form-footer"><button class="btn secondary" data-action="close-drawer">${t('cancel')}</button><button class="btn primary" data-action="save-role" data-id="${role.id}">${t('save')}</button></div>`);
    // Add group toggle and select-all behavior
    $$('.perm-group-header').forEach(header => {
      header.addEventListener('click', e => {
        if (e.target.tagName !== 'INPUT') {
          const body = header.nextElementSibling;
          body.style.display = body.style.display === 'none' ? 'flex' : 'none';
          const chevron = header.querySelector('[data-lucide]');
          if (chevron) { chevron.style.transform = body.style.display === 'none' ? 'rotate(-90deg)' : ''; }
        }
      });
      const groupAll = header.querySelector('.perm-group-all');
      if (groupAll) {
        groupAll.addEventListener('change', e => {
          const body = header.nextElementSibling;
          body.querySelectorAll('.perm-item').forEach(item => { item.checked = e.target.checked; });
        });
      }
    });
    renderIcons();
  }

  // Create Drawers
  function openRoleCreate() {
    openDrawer(t('addRole'), t('accessSubtitle'),
      `<div class="drawer-body"><div class="form-grid"><label><span>${t('code')}</span><input id="new-role-code" required></label><label><span>${t('name')}</span><input id="new-role-name-zh" required></label><label><span>English</span><input id="new-role-name-en"></label><label><span>العربية</span><input id="new-role-name-ar"></label><label><span>${t('status')}</span><select id="new-role-status"><option value="ACTIVE">${t('enabled')}</option><option value="INACTIVE">${t('disabled')}</option></select></label></div></div><div class="form-footer"><button class="btn secondary" data-action="close-drawer">${t('cancel')}</button><button class="btn primary" data-action="create-role">${t('save')}</button></div>`);
  }
  function openUserCreate() {
    openDrawer(t('addUser'), t('accessSubtitle'),
      `<div class="drawer-body"><div class="form-grid"><label><span>${t('username')}</span><input id="new-user-username" required></label><label><span>${t('password')}</span><input id="new-user-password" type="password" minlength="8" placeholder="${esc(t('passwordHint'))}" required></label><label><span>${t('displayName')}</span><input id="new-user-display-name" required></label><label><span>${t('email')}</span><input id="new-user-email" type="email"></label><label><span>${t('language')}</span><select id="new-user-language"><option value="zh-CN">中文</option><option value="en">English</option><option value="ar-TN">العربية</option></select></label><label><span>${t('status')}</span><select id="new-user-status"><option value="ACTIVE">${t('enabled')}</option><option value="INACTIVE">${t('disabled')}</option></select></label><label><span>${t('roleCodes')}</span><input id="new-user-roles" placeholder="MES_ADMIN, MES_OPERATOR"></label></div></div><div class="form-footer"><button class="btn secondary" data-action="close-drawer">${t('cancel')}</button><button class="btn primary" data-action="create-user">${t('save')}</button></div>`);
  }
  function openMenuCreate() {
    openDrawer(t('menuManagement'), t('accessSubtitle'),
      `<div class="drawer-body"><div class="form-grid"><label><span>${t('code')}</span><input id="new-menu-code" required></label><label><span>${t('name')}</span><input id="new-menu-name-zh" required></label><label><span>English</span><input id="new-menu-name-en"></label><label><span>العربية</span><input id="new-menu-name-ar"></label><label><span>${t('parentMenu')}</span><input id="new-menu-parent" placeholder="${esc(t('parentMenuPlaceholder') || '')}"></label><label><span>${t('path')}</span><input id="new-menu-path"></label><label><span>${t('iconName')}</span><input id="new-menu-icon" placeholder="layout-dashboard"></label><label><span>${t('permissionCode')}</span><input id="new-menu-permission"></label><label><span>${t('sortOrder')}</span><input id="new-menu-sort" type="number" value="0"></label><label><span>${t('status')}</span><select id="new-menu-status"><option value="ACTIVE">${t('enabled')}</option><option value="INACTIVE">${t('disabled')}</option></select></label></div></div><div class="form-footer"><button class="btn secondary" data-action="close-drawer">${t('cancel')}</button><button class="btn primary" data-action="create-menu">${t('save')}</button></div>`);
  }
  function openMenuEdit(menu) {
    openDrawer(`${t('edit')} · ${menu.code}`, t('accessSubtitle'),
      `<div class="drawer-body"><div class="form-grid"><label><span>${t('code')}</span><input id="edit-menu-code" value="${esc(menu.code)}" disabled></label><label><span>${t('name')}</span><input id="edit-menu-name-zh" value="${esc(menu.nameZh)}" required></label><label><span>English</span><input id="edit-menu-name-en" value="${esc(menu.nameEn || '')}"></label><label><span>العربية</span><input id="edit-menu-name-ar" value="${esc(menu.nameAr || '')}"></label><label><span>${t('parentMenu')}</span><input id="edit-menu-parent" value="${esc(menu.parentCode || '')}"></label><label><span>${t('path')}</span><input id="edit-menu-path" value="${esc(menu.path || '')}"></label><label><span>${t('iconName')}</span><input id="edit-menu-icon" value="${esc(menu.icon || '')}"></label><label><span>${t('permissionCode')}</span><input id="edit-menu-permission" value="${esc(menu.permissionCode || '')}"></label><label><span>${t('sortOrder')}</span><input id="edit-menu-sort" type="number" value="${menu.sortOrder ?? 0}"></label><label><span>${t('status')}</span><select id="edit-menu-status"><option value="ACTIVE" ${menu.status === 'ACTIVE' ? 'selected' : ''}>${t('enabled')}</option><option value="INACTIVE" ${menu.status === 'INACTIVE' ? 'selected' : ''}>${t('disabled')}</option></select></label></div></div><div class="form-footer"><button class="btn secondary" data-action="close-drawer">${t('cancel')}</button><button class="btn primary" data-action="save-menu" data-id="${menu.id}">${t('save')}</button></div>`);
  }
  function openDictionaryCreate() {
    openDrawer(t('dataDictionary'), t('accessSubtitle'),
      `<div class="drawer-body"><div class="form-grid"><label><span>${t('dictType')}</span><input id="new-dict-type" required></label><label><span>${t('dictCode')}</span><input id="new-dict-code" required></label><label><span>${t('label')}</span><input id="new-dict-label-zh" required></label><label><span>English</span><input id="new-dict-label-en"></label><label><span>العربية</span><input id="new-dict-label-ar"></label><label><span>${t('dictValue')}</span><input id="new-dict-value" required></label></div></div><div class="form-footer"><button class="btn secondary" data-action="close-drawer">${t('cancel')}</button><button class="btn primary" data-action="create-dictionary">${t('save')}</button></div>`);
  }

  /* ================================================================
     AUTH
     ================================================================ */
  async function login() {
    const payload = await api('/auth/login', { method: 'POST', body: { username: $('#username').value, password: $('#password').value } });
    state.token = payload.data.accessToken;
    state.user = payload.data;
    sessionStorage.setItem('tns_token', state.token);
    showWorkspace(payload.data);
    renderView('overview');
  }
  function showWorkspace(userData) {
    $('#login-view').classList.add('hidden');
    $('#workspace-view').classList.remove('hidden');
    $('#user-name').textContent = userData.displayName || userData.username;
    $('#user-role').textContent = (userData.permissions || []).includes('USER_ADMIN') ? 'MES Administrator' : 'MES Operator';
    $('#avatar').textContent = (userData.displayName || userData.username || 'U').slice(0, 1).toUpperCase();
  }
  function logout(clear = true) {
    if (clear) { state.token = ''; sessionStorage.removeItem('tns_token'); }
    $('#login-view').classList.remove('hidden');
    $('#workspace-view').classList.add('hidden');
  }

  /* ================================================================
     EVENT HANDLERS (consolidated single click handler)
     ================================================================ */
  document.addEventListener('click', async event => {
    // Nav group toggle (accordion: collapse others when expanding)
    const groupToggle = event.target.closest('.nav-group-toggle');
    if (groupToggle) {
      const group = groupToggle.closest('.nav-group');
      const wasCollapsed = group.classList.contains('collapsed');
      // Collapse all groups first
      $$('.nav-group').forEach(g => g.classList.add('collapsed'));
      // If the clicked group was collapsed, expand it
      if (wasCollapsed) group.classList.remove('collapsed');
      return;
    }

    // Tab switch
    const tab = event.target.closest('[data-tab]');
    if (tab) {
      $$('.tab', tab.closest('.tabs')).forEach(x => x.classList.remove('active'));
      tab.classList.add('active');
      const tabName = tab.dataset.tab;
      if (state.view === 'iam') { tabName === 'users' ? loadUsers() : loadRoles(); }
      return;
    }

    // Nav view switch
    const viewLink = event.target.closest('[data-view]');
    if (viewLink) { renderView(viewLink.dataset.view); if (innerWidth < 800) $('#sidebar').classList.remove('mobile-open'); return; }

    // Pagination page button
    const pageBtn = event.target.closest('[data-page]');
    if (pageBtn && pageBtn.dataset.page !== undefined) {
      const newPage = parseInt(pageBtn.dataset.page);
      const p = state.data[state.view];
      if (p && newPage >= 0 && newPage < p.totalPages) {
        const loader = ({ products: loadProducts, orders: loadOrders, batches: loadBatches, apiLogs: loadApiLogs })[state.view];
        if (loader) await loader(newPage);
      }
      return;
    }

    // Table header sort
    const sortTh = event.target.closest('th[data-sort]');
    if (sortTh) {
      const key = sortTh.dataset.sort;
      const dir = sortTh.dataset.nextDir || 'asc';
      const p = state.data[state.view];
      if (p) {
        p.sortKey = key;
        p.sortDir = dir;
        const loader = ({ products: loadProducts, orders: loadOrders, batches: loadBatches })[state.view];
        if (loader) await loader();
      }
      return;
    }

    // Action buttons
    const actionNode = event.target.closest('[data-action]');
    if (!actionNode) return;

    // Drawer click handling - close if clicking backdrop or close button
    const action = actionNode.dataset.action;
    if (event.target.closest('[data-stop-close]') && action === 'close-drawer' && !event.target.closest('.drawer-header')) return;
    if (action === 'close-drawer') { closeDrawer(); return; }

    // Query / Reset / Refresh
    if (action === 'query') { const p = state.data[state.view]; if (p) p.page = 0; const loader = ({ master: loadMaster, products: loadProducts, orders: loadOrders, batches: loadBatches, jobs: loadJobs, apiLogs: loadApiLogs })[state.view]; if (loader) await loader(); return; }
    if (action === 'reset') { $$('input, select', '#page .toolbar').forEach(el => { if (el.id !== 'master-type') el.value = ''; }); const p = state.data[state.view]; if (p) { p.page = 0; p.sortKey = null; p.sortDir = 'asc'; } const loader = ({ master: loadMaster, products: loadProducts, orders: loadOrders, batches: loadBatches, jobs: loadJobs, apiLogs: loadApiLogs })[state.view]; if (loader) await loader(); return; }
    if (action === 'refresh-data') {
      const refreshLoaders = { products: loadProducts, orders: loadOrders, batches: loadBatches, jobs: loadJobs, master: loadMaster, menus: loadMenus, dictionaries: loadDictionaries, apiLogs: loadApiLogs };
      if (state.view === 'iam') { const usersTab = $('.tab[data-tab="users"]')?.classList.contains('active'); (usersTab ? loadUsers : loadRoles)(); return; }
      const loader = refreshLoaders[state.view]; if (loader) { await loader(); return; }
    }

    // Logout
    if (action === 'logout') { logout(true); return; }

    // Sync actions
    if (action === 'sync-products') { await runSync('products', actionNode); return; }
    if (action === 'sync-orders') { await runSync('orders', actionNode); return; }
    if (action === 'sync-batches') { await runSync('batches', actionNode); return; }

    // Single item sync actions
    if (action === 'sync-single-product') { const val = $('#single-product')?.value?.trim(); if (val) await runSingleSync('products', val, actionNode); else toast(t('pleaseEnter') + t('productCode'), true); return; }
    if (action === 'sync-single-order') { const val = $('#single-order')?.value?.trim(); if (val) await runSingleSync('orders', val, actionNode); else toast(t('pleaseEnter') + t('workOrderNo'), true); return; }
    if (action === 'sync-single-batch') { const val = $('#single-batch')?.value?.trim(); if (val) await runSingleSync('batches', val, actionNode); else toast(t('pleaseEnter') + t('batchNo'), true); return; }

    // Product row actions
    if (action === 'product-detail') { const row = actionNode.closest('tr'); const item = state.data.products?.items?.find(v => v.code === row.querySelector('.code').textContent.trim()); if (item) await openProductDetail(item); return; }
    if (action === 'product-sync') { const row = actionNode.closest('tr'); await runSingleSync('products', row.querySelector('.code').textContent.trim(), actionNode); return; }

    // Order row actions
    if (action === 'order-detail') { const row = actionNode.closest('tr'); const item = state.data.orders?.items?.find(v => v.orderNo === row.querySelector('.code').textContent.trim()); if (item) await openOrderDetail(item); return; }
    if (action === 'order-sync') { const row = actionNode.closest('tr'); await runSingleSync('work-orders', row.querySelector('.code').textContent.trim(), actionNode); return; }

    // Job row actions
    if (action === 'job-detail') { const row = actionNode.closest('tr'); const item = (state.data.jobs || []).find(v => v.code === row.querySelector('.code').textContent.trim()); if (item) await openJobDetail(item); return; }
    if (action === 'job-run') { const code = actionNode.closest('tr').querySelector('.code').textContent.trim(); const job = (state.data.jobs || []).find(v => v.code === code); if (job) { actionNode.disabled = true; await api('/integrations/sync-jobs/' + encodeURIComponent(job.id) + '/run', { method: 'POST' }).then(() => toast(t('started'))).catch(e => toast(e.message, true)).finally(() => { actionNode.disabled = false; loadJobs(); }); } return; }
    if (action === 'job-toggle') { const code = actionNode.closest('tr').querySelector('.code').textContent.trim(); const job = (state.data.jobs || []).find(v => v.code === code); if (job) { await api('/integrations/sync-jobs/' + job.id + '/enabled?value=' + (!job.enabled), { method: 'PUT' }); toast(t('saved')); loadJobs(); } return; }

    // API Log actions
    if (action === 'log-detail') { const idx = parseInt(actionNode.closest('tr').querySelector('td').textContent.trim()) - 1; const p = state.data.apiLogs; if (p && p.items && p.items[idx]) openLogDetail(p.items[idx]); return; }

    // Master data actions
    if (action === 'master-add') { await openMasterCreate(); return; }
    if (action === 'master-detail') { const row = actionNode.closest('tr'); const item = (state.data.master || []).find(v => v.code === row.querySelector('.code').textContent.trim()); if (item) await openMasterDetail(item); return; }
    if (action === 'master-edit') { const row = actionNode.closest('tr'); const item = (state.data.master || []).find(v => v.code === row.querySelector('.code').textContent.trim()); if (item) await openMasterEdit(item); return; }
    if (action === 'master-delete') { const type = $('#master-type')?.value || 'FACTORY'; const id = (state.data.master || []).find(v => v.code === actionNode.closest('tr').querySelector('.code').textContent.trim())?.id; if (id && confirm(t('confirmDelete'))) { await api(`/master-data/${type}/${id}`, { method: 'DELETE' }).then(() => { toast(t('saved')); loadMaster(); }); } return; }
    if (action === 'save-master') {
      const type = $('#master-type')?.value || 'FACTORY';
      const mode = actionNode.dataset.mode;
      const parentVal = $('#md-parent')?.value;
      const body = {
        code: $('#md-code').value,
        nameZh: $('#md-name-zh').value,
        nameEn: $('#md-name-en').value || '',
        nameAr: $('#md-name-ar').value || '',
        parentId: parentVal && parentVal !== '' ? Number(parentVal) : null,
        description: $('#md-description').value || '',
        status: $('#md-status').value,
        sortOrder: Number($('#md-sort-order').value || 0)
      };
      if (mode === 'create') {
        await api(`/master-data/${type}`, { method: 'POST', body });
      } else {
        await api(`/master-data/${type}/${actionNode.dataset.id}`, { method: 'PUT', body });
      }
      closeDrawer(); toast(t('saved')); loadMaster(); return;
    }

    // Role actions
    if (action === 'role-detail') { const code = actionNode.closest('tr').querySelector('.code').textContent.trim(); const role = (state.data.roles || []).find(v => v.code === code); if (role) await openRoleDetail(role); return; }
    if (action === 'save-role') { const role = (state.data.roles || []).find(v => String(v.id) === actionNode.dataset.id); const permissionCodes = $$('.perm-item:checked').map(input => input.value); await api('/iam/roles/' + role.id, { method: 'PUT', body: { nameZh: $('#role-name-zh').value, nameEn: $('#role-name-en').value, nameAr: $('#role-name-ar').value, permissionCodes } }); closeDrawer(); toast(t('permissionSaved')); loadRoles(); return; }
    if (action === 'role-delete') { const id = (state.data.roles || []).find(v => v.code === actionNode.closest('tr').querySelector('.code').textContent.trim())?.id; if (id && confirm(t('confirmDelete'))) { await api('/iam/roles/' + id, { method: 'DELETE' }).then(() => { toast(t('saved')); loadRoles(); }); } return; }

    // User actions
    if (action === 'user-toggle') { const row = actionNode.closest('tr'); const u = (state.data.users || []).find(v => v.username === row.querySelector('.code').textContent.trim()); if (u) { await api('/iam/users/' + u.id + '/status?value=' + (u.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'), { method: 'PUT' }).then(() => { toast(t('saved')); loadUsers(); }); } return; }
    if (action === 'user-delete') { const id = (state.data.users || []).find(v => v.username === actionNode.closest('tr').querySelector('.code').textContent.trim())?.id; if (id && confirm(t('confirmDelete'))) { await api('/iam/users/' + id, { method: 'DELETE' }).then(() => { toast(t('saved')); loadUsers(); }); } return; }

    // Menu actions
    if (action === 'menu-edit') { const code = actionNode.closest('tr').querySelector('.code').textContent.trim(); const menu = (state.data.menus || []).find(v => v.code === code); if (menu) openMenuEdit(menu); return; }
    if (action === 'menu-toggle') { const code = actionNode.closest('tr').querySelector('.code').textContent.trim(); const menu = (state.data.menus || []).find(v => v.code === code); if (menu) { await api('/system/menus/' + menu.id, { method: 'PUT', body: { code: menu.code, nameZh: menu.nameZh, nameEn: menu.nameEn, nameAr: menu.nameAr, parentCode: menu.parentCode, path: menu.path, icon: menu.icon, permissionCode: menu.permissionCode, sortOrder: menu.sortOrder, status: menu.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' } }).then(() => { toast(t('saved')); loadMenus(); }); } return; }
    if (action === 'menu-delete') { const id = (state.data.menus || []).find(v => v.code === actionNode.closest('tr').querySelector('.code').textContent.trim())?.id; if (id && confirm(t('confirmDelete'))) { await api('/system/menus/' + id, { method: 'DELETE' }).then(() => { toast(t('saved')); loadMenus(); }); } return; }

    // Dictionary actions
    if (action === 'dictionary-delete') { const id = (state.data.dictionaries || []).find(v => v.dictCode === actionNode.closest('tr').querySelector('.code').textContent.trim())?.id; if (id && confirm(t('confirmDelete'))) { await api('/system/dictionaries/' + id, { method: 'DELETE' }).then(() => { toast(t('saved')); loadDictionaries(); }); } return; }

    // Create drawers
    if (action === 'add-role') { openRoleCreate(); return; }
    if (action === 'add-user') { openUserCreate(); return; }
    if (action === 'menu-add') { openMenuCreate(); return; }
    if (action === 'dictionary-add') { openDictionaryCreate(); return; }

    // Create submissions
    if (action === 'create-role') { await api('/iam/roles', { method: 'POST', body: { code: $('#new-role-code').value, nameZh: $('#new-role-name-zh').value, nameEn: $('#new-role-name-en').value, nameAr: $('#new-role-name-ar').value, status: $('#new-role-status').value, permissionCodes: [] } }); closeDrawer(); toast(t('saved')); loadRoles(); return; }
    if (action === 'create-user') { const roles = $('#new-user-roles').value.split(',').map(v => v.trim()).filter(Boolean); await api('/iam/users', { method: 'POST', body: { username: $('#new-user-username').value, password: $('#new-user-password').value, displayName: $('#new-user-display-name').value, email: $('#new-user-email').value, languageCode: $('#new-user-language').value, status: $('#new-user-status').value, roleCodes: roles } }); closeDrawer(); toast(t('saved')); loadUsers(); return; }
    if (action === 'create-menu') { await api('/system/menus', { method: 'POST', body: { code: $('#new-menu-code').value, nameZh: $('#new-menu-name-zh').value, nameEn: $('#new-menu-name-en').value, nameAr: $('#new-menu-name-ar').value, parentCode: $('#new-menu-parent').value || null, path: $('#new-menu-path').value, icon: $('#new-menu-icon').value, permissionCode: $('#new-menu-permission').value, sortOrder: parseInt($('#new-menu-sort').value) || 0, status: $('#new-menu-status').value } }); closeDrawer(); toast(t('saved')); loadMenus(); return; }
    if (action === 'save-menu') { const id = actionNode.dataset.id; await api('/system/menus/' + id, { method: 'PUT', body: { code: $('#edit-menu-code').value, nameZh: $('#edit-menu-name-zh').value, nameEn: $('#edit-menu-name-en').value, nameAr: $('#edit-menu-name-ar').value, parentCode: $('#edit-menu-parent').value || null, path: $('#edit-menu-path').value, icon: $('#edit-menu-icon').value, permissionCode: $('#edit-menu-permission').value, sortOrder: parseInt($('#edit-menu-sort').value) || 0, status: $('#edit-menu-status').value } }); closeDrawer(); toast(t('saved')); loadMenus(); return; }
    if (action === 'create-dictionary') { await api('/system/dictionaries', { method: 'POST', body: { dictType: $('#new-dict-type').value, dictCode: $('#new-dict-code').value, labelZh: $('#new-dict-label-zh').value, labelEn: $('#new-dict-label-en').value, labelAr: $('#new-dict-label-ar').value, dictValue: $('#new-dict-value').value } }); closeDrawer(); toast(t('saved')); loadDictionaries(); return; }
  });

  /* ================================================================
     INIT
     ================================================================ */
  // Login form
  $('#login-form').addEventListener('submit', event => {
    event.preventDefault();
    const button = event.target.querySelector('button[type="submit"]');
    button.disabled = true;
    $('#login-error').textContent = '';
    login().catch(e => $('#login-error').textContent = e.message).finally(() => button.disabled = false);
  });

  // Sidebar
  $('#sidebar-collapse').addEventListener('click', () => {
    $('#sidebar').classList.toggle('collapsed');
    $('#workspace-view').classList.toggle('sidebar-collapsed');
  });
  $('#mobile-menu').addEventListener('click', () => $('#sidebar').classList.toggle('mobile-open'));
  $('#global-refresh').addEventListener('click', () => renderView(state.view));

  // Language switcher
  $$('[data-lang]').forEach(node => node.addEventListener('click', () => setLanguage(node.dataset.lang)));

  // Nav search
  $('#nav-search').addEventListener('input', event => {
    const term = event.target.value.toLowerCase().trim();
    $$('.nav-link, .nav-group').forEach(node => {
      const match = !term || node.textContent.toLowerCase().includes(term);
      node.classList.toggle('hidden', !match);
    });
  });

  // Page size selector (delegated change event for dynamically created selects)
  document.addEventListener('change', event => {
    const sizeSelect = event.target.closest('[data-page-size]');
    if (!sizeSelect) return;
    const p = state.data[state.view];
    if (p) {
      p.size = parseInt(sizeSelect.value);
      p.page = 0;
      const loader = ({ products: loadProducts, orders: loadOrders, apiLogs: loadApiLogs })[state.view];
      if (loader) loader(0);
    }
  });

  // Search inputs - trigger query on Enter key
  document.addEventListener('keydown', event => {
    if (event.key !== 'Enter') return;
    const input = event.target.closest('#page input[id$="-search"]');
    if (!input) return;
    const loader = ({ master: loadMaster, products: loadProducts, orders: loadOrders, batches: loadBatches, jobs: loadJobs, apiLogs: loadApiLogs, iam: () => { const usersTab = $('.tab[data-tab="users"]')?.classList.contains('active'); (usersTab ? loadUsers : loadRoles)(); }, menus: loadMenus, dictionaries: loadDictionaries })[state.view];
    if (loader) { const p = state.data[state.view]; if (p) p.page = 0; loader(); }
  });

  // Initialize
  setLanguage(state.lang);
  if (state.token) {
    api('/auth/me').then(payload => {
      state.user = payload.data;
      showWorkspace(payload.data);
      renderView('overview');
    }).catch(() => logout(false));
  }
})();
