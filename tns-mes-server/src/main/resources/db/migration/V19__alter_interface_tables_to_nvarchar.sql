-- =====================================================
-- V19: Recreate interface management tables with NVARCHAR
-- V18 used VARCHAR but sqlserver-local uses use_nationalized_character_data=true
-- Drop V18 tables and recreate with NVARCHAR (idempotent for fresh DB)
-- =====================================================

-- Drop tables if they exist (from V18)
IF OBJECT_ID(N'dbo.sys_interface_def', N'U') IS NOT NULL DROP TABLE sys_interface_def;
IF OBJECT_ID(N'dbo.sys_external_system', N'U') IS NOT NULL DROP TABLE sys_external_system;
IF OBJECT_ID(N'dbo.sys_interface_category', N'U') IS NOT NULL DROP TABLE sys_interface_category;

-- 1. Interface Category
CREATE TABLE sys_interface_category (
    id BIGINT PRIMARY KEY IDENTITY,
    code NVARCHAR(64) NOT NULL UNIQUE,
    name_zh NVARCHAR(128),
    name_en NVARCHAR(128),
    sort_order INT DEFAULT 0,
    status NVARCHAR(16) DEFAULT 'ACTIVE',
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    created_by NVARCHAR(64),
    updated_by NVARCHAR(64),
    version BIGINT DEFAULT 0
);

-- 2. External System
CREATE TABLE sys_external_system (
    id BIGINT PRIMARY KEY IDENTITY,
    code NVARCHAR(64) NOT NULL UNIQUE,
    name_zh NVARCHAR(128),
    name_en NVARCHAR(128),
    base_url NVARCHAR(512),
    auth_type NVARCHAR(32) DEFAULT 'BASIC',
    auth_config NVARCHAR(MAX),
    sort_order INT DEFAULT 0,
    status NVARCHAR(16) DEFAULT 'ACTIVE',
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    created_by NVARCHAR(64),
    updated_by NVARCHAR(64),
    version BIGINT DEFAULT 0
);

-- 3. Interface Definition
CREATE TABLE sys_interface_def (
    id BIGINT PRIMARY KEY IDENTITY,
    category_code NVARCHAR(64) NOT NULL,
    system_code NVARCHAR(64) NOT NULL,
    code NVARCHAR(128) NOT NULL,
    name_zh NVARCHAR(256),
    name_en NVARCHAR(256),
    method NVARCHAR(8) DEFAULT 'GET',
    path NVARCHAR(512),
    request_template NVARCHAR(MAX),
    response_mapping NVARCHAR(MAX),
    sync_direction NVARCHAR(16) DEFAULT 'INBOUND',
    schedule_cron NVARCHAR(64),
    description NVARCHAR(512),
    sort_order INT DEFAULT 0,
    status NVARCHAR(16) DEFAULT 'ACTIVE',
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    created_by NVARCHAR(64),
    updated_by NVARCHAR(64),
    version BIGINT DEFAULT 0,
    CONSTRAINT uq_interface_def_code UNIQUE (code)
);

-- =====================================================
-- Seed Data (same as V18)
-- =====================================================

-- Categories
INSERT INTO sys_interface_category (code, name_zh, name_en, sort_order, status, created_at, updated_at) VALUES (N'MASTER_DATA', N'主数据同步', N'Master Data Sync', 1, 'ACTIVE', GETDATE(), GETDATE());
INSERT INTO sys_interface_category (code, name_zh, name_en, sort_order, status, created_at, updated_at) VALUES (N'PRODUCTION', N'生产执行', N'Production Execution', 2, 'ACTIVE', GETDATE(), GETDATE());
INSERT INTO sys_interface_category (code, name_zh, name_en, sort_order, status, created_at, updated_at) VALUES (N'WAREHOUSE', N'仓储管理', N'Warehouse Management', 3, 'ACTIVE', GETDATE(), GETDATE());
INSERT INTO sys_interface_category (code, name_zh, name_en, sort_order, status, created_at, updated_at) VALUES (N'QUALITY', N'质量检验', N'Quality Inspection', 4, 'ACTIVE', GETDATE(), GETDATE());
INSERT INTO sys_interface_category (code, name_zh, name_en, sort_order, status, created_at, updated_at) VALUES (N'EQUIPMENT', N'设备管理', N'Equipment Management', 5, 'ACTIVE', GETDATE(), GETDATE());

-- Systems
INSERT INTO sys_external_system (code, name_zh, name_en, base_url, auth_type, auth_config, sort_order, status, created_at, updated_at) VALUES (N'SAP', N'SAP S/4HANA', N'SAP S/4HANA', N'https://my200725.s4hana.sapcloud.cn', 'BASIC', N'{"username":"MES_P"}', 1, 'ACTIVE', GETDATE(), GETDATE());
INSERT INTO sys_external_system (code, name_zh, name_en, base_url, auth_type, auth_config, sort_order, status, created_at, updated_at) VALUES (N'MIDDLEWARE', N'中台', N'Middleware Platform', N'', 'BEARER', N'{}', 2, 'ACTIVE', GETDATE(), GETDATE());
INSERT INTO sys_external_system (code, name_zh, name_en, base_url, auth_type, auth_config, sort_order, status, created_at, updated_at) VALUES (N'WMS', N'仓储管理系统', N'WMS', N'', 'BASIC', N'{}', 3, 'ACTIVE', GETDATE(), GETDATE());
INSERT INTO sys_external_system (code, name_zh, name_en, base_url, auth_type, auth_config, sort_order, status, created_at, updated_at) VALUES (N'MES_LOCAL', N'MES本地', N'MES Local', N'', 'NONE', N'{}', 4, 'ACTIVE', GETDATE(), GETDATE());

-- Definitions
INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, schedule_cron, description, sort_order, status, created_at, updated_at) VALUES (N'MASTER_DATA', N'SAP', N'SAP_PRODUCT_SYNC', N'产品主数据同步', N'Product Master Data Sync', 'GET', N'/sap/opu/odata/sap/API_PRODUCT_SRV/A_Product', 'INBOUND', '0 */15 * * * *', N'从SAP同步产品主数据', 1, 'ACTIVE', GETDATE(), GETDATE());
INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, schedule_cron, description, sort_order, status, created_at, updated_at) VALUES (N'MASTER_DATA', N'SAP', N'SAP_WORK_ORDER_SYNC', N'工单主数据同步', N'Work Order Master Data Sync', 'GET', N'/sap/opu/odata/sap/API_PRODUCTION_ORDER_2_SRV/A_ProductionOrder_2', 'INBOUND', '0 */15 * * * *', N'从SAP同步生产工单', 2, 'ACTIVE', GETDATE(), GETDATE());
INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, schedule_cron, description, sort_order, status, created_at, updated_at) VALUES (N'MASTER_DATA', N'SAP', N'SAP_BATCH_SYNC', N'批次主数据同步', N'Batch Master Data Sync', 'GET', N'/sap/opu/odata/sap/API_BATCH_SRV/Batch', 'INBOUND', '0 */15 * * * *', N'从SAP同步批次主数据', 3, 'ACTIVE', GETDATE(), GETDATE());
INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at) VALUES (N'MASTER_DATA', N'SAP', N'SAP_OPERATION_QUERY', N'工序数据查询', N'Operation Data Query', 'GET', N'/sap/opu/odata/sap/YY1_C_POOPERATIONS_CDS/YY1_C_POOperations', 'INBOUND', N'查询工单关联的工序明细', 4, 'ACTIVE', GETDATE(), GETDATE());
INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at) VALUES (N'PRODUCTION', N'MIDDLEWARE', N'MIDDLEWARE_LOADING_CHECK', N'上料校验', N'Loading Material Check', 'POST', N'/api/loading/check', 'OUTBOUND', N'扫描工单和物料标签后调用中台接口校验上料合法性', 1, 'ACTIVE', GETDATE(), GETDATE());
INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at) VALUES (N'PRODUCTION', N'MIDDLEWARE', N'MIDDLEWARE_WORK_ORDER_QUERY', N'工单信息查询', N'Work Order Info Query', 'GET', N'/api/work-order/query', 'INBOUND', N'从中台查询工单生产信息', 2, 'ACTIVE', GETDATE(), GETDATE());
INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at) VALUES (N'PRODUCTION', N'MIDDLEWARE', N'MIDDLEWARE_MATERIAL_QUERY', N'物料信息查询', N'Material Info Query', 'GET', N'/api/material/query', 'INBOUND', N'从中台查询物料基本信息', 3, 'ACTIVE', GETDATE(), GETDATE());
INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at) VALUES (N'WAREHOUSE', N'WMS', N'WMS_STOCK_QUERY', N'库存查询', N'Stock Query', 'GET', N'/api/stock/query', 'INBOUND', N'从WMS查询库存信息', 1, 'ACTIVE', GETDATE(), GETDATE());
INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at) VALUES (N'WAREHOUSE', N'WMS', N'WMS_GOODS_MOVEMENT', N'货物移动', N'Goods Movement', 'POST', N'/api/goods-movement', 'BIDIRECTIONAL', N'向WMS发起货物移动', 2, 'ACTIVE', GETDATE(), GETDATE());
INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at) VALUES (N'PRODUCTION', N'MES_LOCAL', N'MES_COMPLETION_REPORT', N'完工报数', N'Completion Report', 'POST', N'/api/v1/production/completion', 'OUTBOUND', N'工单完工报数', 1, 'ACTIVE', GETDATE(), GETDATE());
INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at) VALUES (N'QUALITY', N'MES_LOCAL', N'MES_QUALITY_RECORD', N'质量记录', N'Quality Record', 'POST', N'/api/v1/quality/record', 'OUTBOUND', N'记录质量检验结果', 1, 'ACTIVE', GETDATE(), GETDATE());
