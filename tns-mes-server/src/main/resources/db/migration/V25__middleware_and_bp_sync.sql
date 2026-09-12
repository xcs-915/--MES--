-- =====================================================
-- V25: 客户/供应商主数据表 + 中台推送接口登记
--   1. mes_customer / mes_supplier (SAP A_BusinessPartner 同步落地表)
--   2. sys_interface_def 新增: 中台9个推送接口 + Token + SAP客户/供应商
--   3. sys_external_system 新增: SAP集成中台
-- =====================================================

-- 1. 客户主数据表
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'mes_customer')
BEGIN
    CREATE TABLE mes_customer (
        id BIGINT PRIMARY KEY IDENTITY,
        created_at DATETIME2 NOT NULL,
        updated_at DATETIME2 NOT NULL,
        created_by VARCHAR(64),
        updated_by VARCHAR(64),
        version BIGINT DEFAULT 0,
        bp_number VARCHAR(64) NOT NULL UNIQUE,
        bp_type VARCHAR(16),
        name NVARCHAR(200) NOT NULL,
        full_name NVARCHAR(200),
        search_term VARCHAR(100),
        country VARCHAR(10),
        city NVARCHAR(100),
        postal_code VARCHAR(20),
        street NVARCHAR(200),
        tax_number VARCHAR(40),
        vat_registration VARCHAR(40),
        status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
        source VARCHAR(30) NOT NULL DEFAULT 'SAP',
        remark NVARCHAR(500)
    );
    CREATE INDEX idx_mes_customer_name ON mes_customer(name);
END
GO

-- 2. 供应商主数据表
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'mes_supplier')
BEGIN
    CREATE TABLE mes_supplier (
        id BIGINT PRIMARY KEY IDENTITY,
        created_at DATETIME2 NOT NULL,
        updated_at DATETIME2 NOT NULL,
        created_by VARCHAR(64),
        updated_by VARCHAR(64),
        version BIGINT DEFAULT 0,
        bp_number VARCHAR(64) NOT NULL UNIQUE,
        bp_type VARCHAR(16),
        name NVARCHAR(200) NOT NULL,
        full_name NVARCHAR(200),
        search_term VARCHAR(100),
        country VARCHAR(10),
        city NVARCHAR(100),
        postal_code VARCHAR(20),
        street NVARCHAR(200),
        tax_number VARCHAR(40),
        vat_registration VARCHAR(40),
        status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
        source VARCHAR(30) NOT NULL DEFAULT 'SAP',
        remark NVARCHAR(500)
    );
    CREATE INDEX idx_mes_supplier_name ON mes_supplier(name);
END
GO

-- 3. 外部系统：SAP集成中台
IF NOT EXISTS (SELECT 1 FROM sys_external_system WHERE code = 'MIDDLEWARE')
    INSERT INTO sys_external_system (code, name_zh, name_en, base_url, auth_type, auth_config, sort_order, status, created_at, updated_at, version)
    VALUES ('MIDDLEWARE', N'SAP集成中台', 'SAP Integration Platform', 'http://10.30.10.42:9999', 'TOKEN', N'{"header":"x-access-token","tokenPath":"/sys/loginGetToken","username":"MES-JX"}', 20, 'ACTIVE', SYSUTCDATETIME(), SYSUTCDATETIME(), 0);
GO

-- 4. 接口定义：中台推送类接口（9个业务 + Token）
IF NOT EXISTS (SELECT 1 FROM sys_interface_def WHERE code = 'MIDDLEWARE_GET_TOKEN')
    INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at, version)
    VALUES ('MASTER_DATA', 'MIDDLEWARE', 'MIDDLEWARE_GET_TOKEN', N'中台Token获取', 'Platform token', 'POST', '/sys/loginGetToken', 'OUTBOUND', N'获取中台x-access-token（账号MES-JX，缓存3小时）', 1, 'ACTIVE', SYSUTCDATETIME(), SYSUTCDATETIME(), 0);

IF NOT EXISTS (SELECT 1 FROM sys_interface_def WHERE code = 'MIDDLEWARE_PICKING_POST')
    INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at, version)
    VALUES ('WAREHOUSE', 'MIDDLEWARE', 'MIDDLEWARE_PICKING_POST', N'领料过账', 'Post picking list', 'POST', '/warehouse/deliveryCommandHeader/add', 'OUTBOUND', N'领料发货指令推送（老MES: MES_PostPickingList_ViewList）', 10, 'ACTIVE', SYSUTCDATETIME(), SYSUTCDATETIME(), 0);

IF NOT EXISTS (SELECT 1 FROM sys_interface_def WHERE code = 'MIDDLEWARE_MO_FEEDING_POST')
    INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at, version)
    VALUES ('PRODUCTION', 'MIDDLEWARE', 'MIDDLEWARE_MO_FEEDING_POST', N'工单上料确认', 'Post MO feeding', 'POST', '/warehouse/deliveryHeader/approvalAndSave', 'OUTBOUND', N'工单上料审核+保存（老MES: MES_PostMOFeeding_ViewList）', 20, 'ACTIVE', SYSUTCDATETIME(), SYSUTCDATETIME(), 0);

IF NOT EXISTS (SELECT 1 FROM sys_interface_def WHERE code = 'MIDDLEWARE_FEEDBACK_POST')
    INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at, version)
    VALUES ('PRODUCTION', 'MIDDLEWARE', 'MIDDLEWARE_FEEDBACK_POST', N'报工推送', 'Post work feedback', 'POST', '/consumeMaterialOrderService/consumeMaterialOrderHeader/saveConsumeMaterialData', 'OUTBOUND', N'报动物料消耗（含SAP预留号，老MES: MES_PostFeedback_ViewList）', 30, 'ACTIVE', SYSUTCDATETIME(), SYSUTCDATETIME(), 0);

IF NOT EXISTS (SELECT 1 FROM sys_interface_def WHERE code = 'MIDDLEWARE_FINISH_ENTITY_POST')
    INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at, version)
    VALUES ('WAREHOUSE', 'MIDDLEWARE', 'MIDDLEWARE_FINISH_ENTITY_POST', N'完工入库', 'Post finish entity', 'POST', '/warehouse/receiveCommand/add', 'OUTBOUND', N'完工入库推送（老MES: MES_PostFinishEntity_ViewList）', 40, 'ACTIVE', SYSUTCDATETIME(), SYSUTCDATETIME(), 0);

IF NOT EXISTS (SELECT 1 FROM sys_interface_def WHERE code = 'MIDDLEWARE_FINISH_ENTITY_OTHER_POST')
    INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at, version)
    VALUES ('WAREHOUSE', 'MIDDLEWARE', 'MIDDLEWARE_FINISH_ENTITY_OTHER_POST', N'其他入库', 'Post finish entity other', 'POST', '/warehouse/receiveCommand/add', 'OUTBOUND', N'其他入库推送（老MES: MES_PostFinishEntityOter_ViewLsit）', 50, 'ACTIVE', SYSUTCDATETIME(), SYSUTCDATETIME(), 0);

IF NOT EXISTS (SELECT 1 FROM sys_interface_def WHERE code = 'MIDDLEWARE_RECEIVE_COMMAND_CLOSE')
    INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at, version)
    VALUES ('WAREHOUSE', 'MIDDLEWARE', 'MIDDLEWARE_RECEIVE_COMMAND_CLOSE', N'入库指令关闭', 'Close receive command', 'PUT', '/warehouse/receiveCommand/close', 'OUTBOUND', N'入库指令关闭（老MES: MES_receiveCommand_ViewList）', 60, 'ACTIVE', SYSUTCDATETIME(), SYSUTCDATETIME(), 0);

IF NOT EXISTS (SELECT 1 FROM sys_interface_def WHERE code = 'MIDDLEWARE_PRODUCT_BACK_POST')
    INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at, version)
    VALUES ('WAREHOUSE', 'MIDDLEWARE', 'MIDDLEWARE_PRODUCT_BACK_POST', N'线边仓退料', 'Post product back', 'POST', '/warehouse/receiveCommand/add', 'OUTBOUND', N'线边仓退料推送（老MES: MES_PostProductBack_ViewList）', 70, 'ACTIVE', SYSUTCDATETIME(), SYSUTCDATETIME(), 0);

IF NOT EXISTS (SELECT 1 FROM sys_interface_def WHERE code = 'MIDDLEWARE_RETURN_MATERIAL_POST')
    INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at, version)
    VALUES ('WAREHOUSE', 'MIDDLEWARE', 'MIDDLEWARE_RETURN_MATERIAL_POST', N'退料到仓库', 'Post return material', 'POST', '/returnMaterialOrderService/returnMaterialOrderHeader/saveReturnMaterialData', 'OUTBOUND', N'退料到仓库/加工费退料（老MES: MES_PostProductBack_WaerHost_ViewList）', 80, 'ACTIVE', SYSUTCDATETIME(), SYSUTCDATETIME(), 0);

IF NOT EXISTS (SELECT 1 FROM sys_interface_def WHERE code = 'MIDDLEWARE_PROD_CANCEL_POST')
    INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at, version)
    VALUES ('WAREHOUSE', 'MIDDLEWARE', 'MIDDLEWARE_PROD_CANCEL_POST', N'退料取消', 'Post product cancel', 'POST', '/warehouse/receiveHeader/prodCancelToSap', 'OUTBOUND', N'退料取消推送SAP（老MES: MES_receive_prodCancel_ViewList）', 90, 'ACTIVE', SYSUTCDATETIME(), SYSUTCDATETIME(), 0);

-- 5. 接口定义：SAP直连客户/供应商
IF NOT EXISTS (SELECT 1 FROM sys_interface_def WHERE code = 'SAP_CUSTOMER_SYNC')
    INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at, version)
    VALUES ('MASTER_DATA', 'SAP', 'SAP_CUSTOMER_SYNC', N'客户主数据同步', 'Customer master sync', 'GET', '/sap/opu/odata/sap/API_BUSINESS_PARTNER/A_BusinessPartner', 'INBOUND', N'SAP业务伙伴-客户（BusinessPartnerIsCustomer，增量LastChangeDateTime）', 50, 'ACTIVE', SYSUTCDATETIME(), SYSUTCDATETIME(), 0);

IF NOT EXISTS (SELECT 1 FROM sys_interface_def WHERE code = 'SAP_SUPPLIER_SYNC')
    INSERT INTO sys_interface_def (category_code, system_code, code, name_zh, name_en, method, path, sync_direction, description, sort_order, status, created_at, updated_at, version)
    VALUES ('MASTER_DATA', 'SAP', 'SAP_SUPPLIER_SYNC', N'供应商主数据同步', 'Supplier master sync', 'GET', '/sap/opu/odata/sap/API_BUSINESS_PARTNER/A_BusinessPartner', 'INBOUND', N'SAP业务伙伴-供应商（BusinessPartnerIsSupplier，增量LastChangeDateTime）', 60, 'ACTIVE', SYSUTCDATETIME(), SYSUTCDATETIME(), 0);
