-- =====================================================
-- V18: Multi-System Interface Management
-- Tables: sys_interface_category, sys_external_system, sys_interface_def
-- =====================================================

-- 1. Interface Category (大类)
CREATE TABLE sys_interface_category (
    id BIGINT PRIMARY KEY IDENTITY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name_zh NVARCHAR(128),
    name_en VARCHAR(128),
    sort_order INT DEFAULT 0,
    status VARCHAR(16) DEFAULT 'ACTIVE',
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    version BIGINT DEFAULT 0
);

-- 2. External System (外部系统)
CREATE TABLE sys_external_system (
    id BIGINT PRIMARY KEY IDENTITY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name_zh NVARCHAR(128),
    name_en VARCHAR(128),
    base_url NVARCHAR(512),
    auth_type VARCHAR(32) DEFAULT 'BASIC',
    auth_config NVARCHAR(MAX),
    sort_order INT DEFAULT 0,
    status VARCHAR(16) DEFAULT 'ACTIVE',
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    version BIGINT DEFAULT 0
);

-- 3. Interface Definition (接口定义)
CREATE TABLE sys_interface_def (
    id BIGINT PRIMARY KEY IDENTITY,
    category_code VARCHAR(64) NOT NULL,
    system_code VARCHAR(64) NOT NULL,
    code VARCHAR(128) NOT NULL,
    name_zh NVARCHAR(256),
    name_en VARCHAR(256),
    method VARCHAR(8) DEFAULT 'GET',
    path NVARCHAR(512),
    request_template NVARCHAR(MAX),
    response_mapping NVARCHAR(MAX),
    sync_direction VARCHAR(16) DEFAULT 'INBOUND',
    schedule_cron VARCHAR(64),
    description NVARCHAR(512),
    sort_order INT DEFAULT 0,
    status VARCHAR(16) DEFAULT 'ACTIVE',
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    version BIGINT DEFAULT 0,
    CONSTRAINT uq_interface_def_code UNIQUE (code)
);

-- =====================================================
-- Seed Data
-- =====================================================

-- Interface Categories (大类)
INSERT INTO sys_interface_category (code, name_zh, name_en, sort_order, status, created_at, updated_at)
SELECT 'MASTER_DATA', N'主数据同步', N'Master Data Sync', 1, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_interface_category WHERE code = 'MASTER_DATA');

INSERT INTO sys_interface_category (code, name_zh, name_en, sort_order, status, created_at, updated_at)
SELECT 'PRODUCTION', N'生产执行', N'Production Execution', 2, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_interface_category WHERE code = 'PRODUCTION');

INSERT INTO sys_interface_category (code, name_zh, name_en, sort_order, status, created_at, updated_at)
SELECT 'WAREHOUSE', N'仓储管理', N'Warehouse Management', 3, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_interface_category WHERE code = 'WAREHOUSE');

INSERT INTO sys_interface_category (code, name_zh, name_en, sort_order, status, created_at, updated_at)
SELECT 'QUALITY', N'质量检验', N'Quality Inspection', 4, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_interface_category WHERE code = 'QUALITY');

INSERT INTO sys_interface_category (code, name_zh, name_en, sort_order, status, created_at, updated_at)
SELECT 'EQUIPMENT', N'设备管理', N'Equipment Management', 5, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_interface_category WHERE code = 'EQUIPMENT');

-- External Systems (外部系统)
INSERT INTO sys_external_system (code, name_zh, name_en, base_url, auth_type, auth_config, sort_order, status, created_at, updated_at)
SELECT 'SAP', N'SAP S/4HANA', N'SAP S/4HANA', N'https://my200683.s4hana.sapcloud.cn', 'BASIC', N'{"username":"MES_T"}', 1, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_external_system WHERE code = 'SAP');

INSERT INTO sys_external_system (code, name_zh, name_en, base_url, auth_type, auth_config, sort_order, status, created_at, updated_at)
SELECT 'MIDDLEWARE', N'中台', N'Middleware Platform', N'', 'BEARER', N'{}', 2, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_external_system WHERE code = 'MIDDLEWARE');

INSERT INTO sys_external_system (code, name_zh, name_en, base_url, auth_type, auth_config, sort_order, status, created_at, updated_at)
SELECT 'WMS', N'仓储管理系统', N'WMS', N'', 'BASIC', N'{}', 3, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_external_system WHERE code = 'WMS');

INSERT INTO sys_external_system (code, name_zh, name_en, base_url, auth_type, auth_config, sort_order, status, created_at, updated_at)
SELECT 'MES_LOCAL', N'MES本地', N'MES Local', N'', 'NONE', N'{}', 4, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_external_system WHERE code = 'MES_LOCAL');

-- Interface Definitions (接口定义)
-- SAP Product Sync
INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, schedule_cron, description, sort_order, status, created_at, updated_at)
SELECT 'MASTER_DATA', 'SAP', 'SAP_PRODUCT_SYNC', N'产品主数据同步', N'Product Master Data Sync', 'GET', N'/sap/opu/odata/sap/API_PRODUCT_SRV/A_Product', 'INBOUND', '0 */15 * * * *', N'从SAP同步产品主数据，含基本信息、重量尺寸、自定义字段', 1, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_interface_def WHERE code = 'SAP_PRODUCT_SYNC');

-- SAP Work Order Sync
INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, schedule_cron, description, sort_order, status, created_at, updated_at)
SELECT 'MASTER_DATA', 'SAP', 'SAP_WORK_ORDER_SYNC', N'工单主数据同步', N'Work Order Master Data Sync', 'GET', N'/sap/opu/odata/sap/API_PRODUCTION_ORDER_2_SRV/A_ProductionOrder_2', 'INBOUND', '0 */15 * * * *', N'从SAP同步生产工单，含组件明细和工序', 2, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_interface_def WHERE code = 'SAP_WORK_ORDER_SYNC');

-- SAP Batch Sync
INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, schedule_cron, description, sort_order, status, created_at, updated_at)
SELECT 'MASTER_DATA', 'SAP', 'SAP_BATCH_SYNC', N'批次主数据同步', N'Batch Master Data Sync', 'GET', N'/sap/opu/odata/sap/API_BATCH_SRV/Batch', 'INBOUND', '0 */15 * * * *', N'从SAP同步批次主数据，含状态、有效期、检验信息', 3, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_interface_def WHERE code = 'SAP_BATCH_SYNC');

-- SAP Operation
INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at)
SELECT 'MASTER_DATA', 'SAP', 'SAP_OPERATION_QUERY', N'工序数据查询', N'Operation Data Query', 'GET', N'/sap/opu/odata/sap/YY1_C_POOPERATIONS_CDS/YY1_C_POOperations', 'INBOUND', N'查询工单关联的工序明细', 4, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_interface_def WHERE code = 'SAP_OPERATION_QUERY');

-- Middleware Loading Check (中台上料校验)
INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at)
SELECT 'PRODUCTION', 'MIDDLEWARE', 'MIDDLEWARE_LOADING_CHECK', N'上料校验', N'Loading Material Check', 'POST', N'/api/loading/check', 'OUTBOUND', N'扫描工单和物料标签后，调用中台接口校验上料合法性', 1, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_interface_def WHERE code = 'MIDDLEWARE_LOADING_CHECK');

-- Middleware Work Order Query
INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at)
SELECT 'PRODUCTION', 'MIDDLEWARE', 'MIDDLEWARE_WORK_ORDER_QUERY', N'工单信息查询', N'Work Order Info Query', 'GET', N'/api/work-order/query', 'INBOUND', N'从中台查询工单生产信息', 2, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_interface_def WHERE code = 'MIDDLEWARE_WORK_ORDER_QUERY');

-- Middleware Material Query
INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at)
SELECT 'PRODUCTION', 'MIDDLEWARE', 'MIDDLEWARE_MATERIAL_QUERY', N'物料信息查询', N'Material Info Query', 'GET', N'/api/material/query', 'INBOUND', N'从中台查询物料基本信息', 3, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_interface_def WHERE code = 'MIDDLEWARE_MATERIAL_QUERY');

-- WMS Stock Query
INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at)
SELECT 'WAREHOUSE', 'WMS', 'WMS_STOCK_QUERY', N'库存查询', N'Stock Query', 'GET', N'/api/stock/query', 'INBOUND', N'从WMS查询库存信息', 1, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_interface_def WHERE code = 'WMS_STOCK_QUERY');

-- WMS Goods Movement
INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at)
SELECT 'WAREHOUSE', 'WMS', 'WMS_GOODS_MOVEMENT', N'货物移动', N'Goods Movement', 'POST', N'/api/goods-movement', 'BIDIRECTIONAL', N'向WMS发起货物移动（收货/发料/转移）', 2, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_interface_def WHERE code = 'WMS_GOODS_MOVEMENT');

-- MES Local Completion Report
INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at)
SELECT 'PRODUCTION', 'MES_LOCAL', 'MES_COMPLETION_REPORT', N'完工报数', N'Completion Report', 'POST', N'/api/v1/production/completion', 'OUTBOUND', N'工单完工报数，记录产出数量', 1, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_interface_def WHERE code = 'MES_COMPLETION_REPORT');

-- MES Local Quality Record
INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at)
SELECT 'QUALITY', 'MES_LOCAL', 'MES_QUALITY_RECORD', N'质量记录', N'Quality Record', 'POST', N'/api/v1/quality/record', 'OUTBOUND', N'记录质量检验结果', 1, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_interface_def WHERE code = 'MES_QUALITY_RECORD');

-- =====================================================
-- Menu Entry: Interface Management
-- =====================================================
INSERT INTO sys_menu (code, name_zh, name_en, parent_code, path, icon, permission_code, sort_order, status, created_at, updated_at, created_by, updated_by, version)
SELECT N'interfaces', N'接口管理', N'Interface Management', N'integration', N'/interfaces', N'network', N'PAGE_INTEGRATION', 53, N'ACTIVE', GETDATE(), GETDATE(), N'system', N'system', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = N'interfaces');
