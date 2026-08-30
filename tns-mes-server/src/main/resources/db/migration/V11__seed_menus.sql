-- Seed menu data for sys_menu table.
-- SQL Server compatible: uses INSERT ... SELECT ... WHERE NOT EXISTS idempotent pattern.
-- Top-level menus have parent_code = NULL; group menus have path = NULL.

-- 1. overview - 运营总览
INSERT INTO sys_menu (code, name_zh, name_en, parent_code, path, icon, permission_code, sort_order, status, created_at, updated_at, created_by, updated_by, version)
SELECT N'overview', N'运营总览', N'Operations Overview', NULL, N'/overview', N'layout-dashboard', N'PAGE_OVERVIEW', 10, N'ACTIVE', GETDATE(), GETDATE(), N'system', N'system', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = N'overview');

-- 2. foundation - 基础与组织 (group)
INSERT INTO sys_menu (code, name_zh, name_en, parent_code, path, icon, permission_code, sort_order, status, created_at, updated_at, created_by, updated_by, version)
SELECT N'foundation', N'基础与组织', N'Foundation & Organization', NULL, NULL, N'database', NULL, 20, N'ACTIVE', GETDATE(), GETDATE(), N'system', N'system', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = N'foundation');

-- 2.1 master - 基础资料
INSERT INTO sys_menu (code, name_zh, name_en, parent_code, path, icon, permission_code, sort_order, status, created_at, updated_at, created_by, updated_by, version)
SELECT N'master', N'基础资料', N'Master Data', N'foundation', N'/master', N'building-2', N'PAGE_MASTER_DATA', 21, N'ACTIVE', GETDATE(), GETDATE(), N'system', N'system', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = N'master');

-- 2.2 iam - 用户与权限
INSERT INTO sys_menu (code, name_zh, name_en, parent_code, path, icon, permission_code, sort_order, status, created_at, updated_at, created_by, updated_by, version)
SELECT N'iam', N'用户与权限', N'Users & Permissions', N'foundation', N'/iam', N'shield-check', N'PAGE_IAM', 22, N'ACTIVE', GETDATE(), GETDATE(), N'system', N'system', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = N'iam');

-- 2.3 menus - 菜单管理
INSERT INTO sys_menu (code, name_zh, name_en, parent_code, path, icon, permission_code, sort_order, status, created_at, updated_at, created_by, updated_by, version)
SELECT N'menus', N'菜单管理', N'Menu Management', N'foundation', N'/menus', N'menu-square', N'PAGE_IAM', 23, N'ACTIVE', GETDATE(), GETDATE(), N'system', N'system', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = N'menus');

-- 2.4 dictionaries - 数据字典
INSERT INTO sys_menu (code, name_zh, name_en, parent_code, path, icon, permission_code, sort_order, status, created_at, updated_at, created_by, updated_by, version)
SELECT N'dictionaries', N'数据字典', N'Data Dictionary', N'foundation', N'/dictionaries', N'book-open-text', N'PAGE_IAM', 24, N'ACTIVE', GETDATE(), GETDATE(), N'system', N'system', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = N'dictionaries');

-- 3. engineering - 产品工程 (group)
INSERT INTO sys_menu (code, name_zh, name_en, parent_code, path, icon, permission_code, sort_order, status, created_at, updated_at, created_by, updated_by, version)
SELECT N'engineering', N'产品工程', N'Product Engineering', NULL, NULL, N'boxes', NULL, 30, N'ACTIVE', GETDATE(), GETDATE(), N'system', N'system', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = N'engineering');

-- 3.1 products - 产品主数据
INSERT INTO sys_menu (code, name_zh, name_en, parent_code, path, icon, permission_code, sort_order, status, created_at, updated_at, created_by, updated_by, version)
SELECT N'products', N'产品主数据', N'Product Master Data', N'engineering', N'/products', N'package-search', N'PAGE_PRODUCT', 31, N'ACTIVE', GETDATE(), GETDATE(), N'system', N'system', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = N'products');

-- 3.2 boms - BOM
INSERT INTO sys_menu (code, name_zh, name_en, parent_code, path, icon, permission_code, sort_order, status, created_at, updated_at, created_by, updated_by, version)
SELECT N'boms', N'BOM', N'BOM', N'engineering', N'/boms', N'network', N'PAGE_BOM', 32, N'ACTIVE', GETDATE(), GETDATE(), N'system', N'system', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = N'boms');

-- 3.3 routes - 工艺路线
INSERT INTO sys_menu (code, name_zh, name_en, parent_code, path, icon, permission_code, sort_order, status, created_at, updated_at, created_by, updated_by, version)
SELECT N'routes', N'工艺路线', N'Process Routes', N'engineering', N'/routes', N'route', N'PAGE_ROUTE', 33, N'ACTIVE', GETDATE(), GETDATE(), N'system', N'system', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = N'routes');

-- 3.4 quality - 检验规则
INSERT INTO sys_menu (code, name_zh, name_en, parent_code, path, icon, permission_code, sort_order, status, created_at, updated_at, created_by, updated_by, version)
SELECT N'quality', N'检验规则', N'Inspection Rules', N'engineering', N'/quality', N'clipboard-check', N'PAGE_QUALITY', 34, N'ACTIVE', GETDATE(), GETDATE(), N'system', N'system', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = N'quality');

-- 3.5 batches - 批次管理
INSERT INTO sys_menu (code, name_zh, name_en, parent_code, path, icon, permission_code, sort_order, status, created_at, updated_at, created_by, updated_by, version)
SELECT N'batches', N'批次管理', N'Batch Management', N'engineering', N'/batches', N'boxes', N'PAGE_BATCH', 35, N'ACTIVE', GETDATE(), GETDATE(), N'system', N'system', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = N'batches');

-- 4. production - 生产执行 (group)
INSERT INTO sys_menu (code, name_zh, name_en, parent_code, path, icon, permission_code, sort_order, status, created_at, updated_at, created_by, updated_by, version)
SELECT N'production', N'生产执行', N'Production Execution', NULL, NULL, N'activity', NULL, 40, N'ACTIVE', GETDATE(), GETDATE(), N'system', N'system', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = N'production');

-- 4.1 orders - 生产工单
INSERT INTO sys_menu (code, name_zh, name_en, parent_code, path, icon, permission_code, sort_order, status, created_at, updated_at, created_by, updated_by, version)
SELECT N'orders', N'生产工单', N'Work Orders', N'production', N'/orders', N'list-checks', N'PAGE_WORK_ORDER', 41, N'ACTIVE', GETDATE(), GETDATE(), N'system', N'system', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = N'orders');

-- 5. integration - 集成中心 (group)
INSERT INTO sys_menu (code, name_zh, name_en, parent_code, path, icon, permission_code, sort_order, status, created_at, updated_at, created_by, updated_by, version)
SELECT N'integration', N'集成中心', N'Integration Center', NULL, NULL, N'plug-zap', NULL, 50, N'ACTIVE', GETDATE(), GETDATE(), N'system', N'system', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = N'integration');

-- 5.1 apiLogs - 接口调用日志
INSERT INTO sys_menu (code, name_zh, name_en, parent_code, path, icon, permission_code, sort_order, status, created_at, updated_at, created_by, updated_by, version)
SELECT N'apiLogs', N'接口调用日志', N'API Call Logs', N'integration', N'/apiLogs', N'file-text', N'PAGE_INTEGRATION', 51, N'ACTIVE', GETDATE(), GETDATE(), N'system', N'system', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = N'apiLogs');

-- 5.2 jobs - 定时任务
INSERT INTO sys_menu (code, name_zh, name_en, parent_code, path, icon, permission_code, sort_order, status, created_at, updated_at, created_by, updated_by, version)
SELECT N'jobs', N'定时任务', N'Scheduled Jobs', N'integration', N'/jobs', N'calendar-clock', N'PAGE_SYNC_JOB', 52, N'ACTIVE', GETDATE(), GETDATE(), N'system', N'system', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = N'jobs');
