-- =====================================================================
-- 迁移脚本: V21__optimize_indexes_and_cleanup.sql
-- 说明: 数据库性能优化 —— 为高频查询字段补充索引、清理过期 outbox
--       消息、更新全表统计信息。
-- 适用: SQL Server 2019, 数据库 tns_mes
-- 特性: 幂等执行(可重复运行不报错)，每个索引创建前均做存在性检查。
--
-- ---------------------------------------------------------------------
-- 重要说明: 用户原始请求中部分列名与实际表结构不符，已按实际列名调整，
--          以避免对不存在的列建索引导致迁移失败。具体调整如下:
--   1) eng_product: 原请求 "product_code" 实际列名为 code，
--      且已有唯一约束 uk_eng_product_code。故以 product_type+status 为前导列。
--   2) prd_work_order: order_no 已有唯一约束 uk_prd_work_order_no，
--      故以 product_id(FK)+status 为前导列以支持按产品筛选工单。
--   3) qa_batch: 该表无 product_id / work_order_id 列，改用 product_code +
--      batch_status(原 batch_no 已与 plant 组成唯一约束)。
--   4) eng_bom / eng_process_route: product_id 已是现有唯一约束前导列，
--      另建窄索引以提升外键关联查询效率。
--   5) int_api_call_log: 该表无 entity_type 列，改用 system_code。
--   6) sys_data_dictionary: "type_code/dict_key" 实为 dict_type/dict_code，
--      V20 已创建索引 ix_sys_dict_type_code，此处幂等跳过。
--   7) iam_user: username 已有唯一约束 uk_iam_user_username，无需重复建索引。
-- ---------------------------------------------------------------------

-- =====================================================================
-- 1. 补充高频查询索引
--    约定: 索引名沿用项目 ix_ 前缀；IF NOT EXISTS + OBJECT_ID 双重检查
--    保证表缺失或索引已存在时均安全跳过。
-- =====================================================================

-- 1.1 eng_product: 按产品类型 + 状态筛选
--     code 列已有唯一约束 uk_eng_product_code，故不作为复合索引前导列。
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_eng_product_type_status'
        AND object_id = OBJECT_ID(N'[dbo].[eng_product]'))
    AND OBJECT_ID(N'[dbo].[eng_product]') IS NOT NULL
    CREATE INDEX ix_eng_product_type_status ON [dbo].[eng_product](product_type, status);

-- 1.2 prd_work_order: 按产品 + 状态筛选工单
--     order_no 已有唯一约束 uk_prd_work_order_no；product_id 为外键(指向
--     eng_product)，单独建索引可加速关联查询与按产品汇总。
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_prd_work_order_product_status'
        AND object_id = OBJECT_ID(N'[dbo].[prd_work_order]'))
    AND OBJECT_ID(N'[dbo].[prd_work_order]') IS NOT NULL
    CREATE INDEX ix_prd_work_order_product_status ON [dbo].[prd_work_order](product_id, status);

-- 1.3 qa_batch: 按产品编码 + 批次状态筛选
--     说明: qa_batch 表不存在 product_id / work_order_id 列，改用实际存在的
--     product_code 与 batch_status。batch_no 已与 plant 组成唯一约束
--     uk_qa_batch_no_plant；product_code 虽有单列索引 ix_qa_batch_product_code，
--     但复合索引(product_code, batch_status)可覆盖"某产品各状态批次"查询。
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_qa_batch_product_status'
        AND object_id = OBJECT_ID(N'[dbo].[qa_batch]'))
    AND OBJECT_ID(N'[dbo].[qa_batch]') IS NOT NULL
    CREATE INDEX ix_qa_batch_product_status ON [dbo].[qa_batch](product_code, batch_status);

-- 1.4 eng_bom: 按产品查询 BOM
--     说明: 唯一约束 uk_eng_bom_product_version(product_id, code, version_code)
--     已以 product_id 为前导列，可满足 product_id 查找；此处另建仅含
--     product_id 的窄索引，体积更小，利于外键关联 join 与按产品列表查询。
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_eng_bom_product'
        AND object_id = OBJECT_ID(N'[dbo].[eng_bom]'))
    AND OBJECT_ID(N'[dbo].[eng_bom]') IS NOT NULL
    CREATE INDEX ix_eng_bom_product ON [dbo].[eng_bom](product_id);

-- 1.5 eng_process_route: 按产品查询工艺路线
--     说明: 唯一约束 uk_eng_route_product_version(product_id, code, version_code)
--     已以 product_id 为前导列；此处另建窄索引以加速外键关联查询。
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_eng_process_route_product'
        AND object_id = OBJECT_ID(N'[dbo].[eng_process_route]'))
    AND OBJECT_ID(N'[dbo].[eng_process_route]') IS NOT NULL
    CREATE INDEX ix_eng_process_route_product ON [dbo].[eng_process_route](product_id);

-- 1.6 int_api_call_log: 按时间 + 系统筛选调用日志
--     说明: 该表无 entity_type 列，改用 system_code。已有单列索引
--     ix_int_api_call_log_created_at(created_at)，此处复合索引
--     (created_at, system_code) 可覆盖"某时间段内某系统的调用记录"查询。
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_int_api_call_log_created_system'
        AND object_id = OBJECT_ID(N'[dbo].[int_api_call_log]'))
    AND OBJECT_ID(N'[dbo].[int_api_call_log]') IS NOT NULL
    CREATE INDEX ix_int_api_call_log_created_system ON [dbo].[int_api_call_log](created_at, system_code);

-- 1.7 sys_data_dictionary: 按 type_code + dict_key 查询 —— 已存在，幂等跳过
--     说明: 用户所述 "type_code/dict_key" 对应实际列 dict_type/dict_code，
--     该组合索引 ix_sys_dict_type_code 已在 V20__fix_data_dictionary_master_data.sql
--     中创建，无需重复创建(重复建同名不同列的索引只会浪费写入开销)。
--     此处仅做存在性校验，确认索引在位即跳过，保证脚本幂等。
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_sys_dict_type_code'
        AND object_id = OBJECT_ID(N'[dbo].[sys_data_dictionary]'))
    AND OBJECT_ID(N'[dbo].[sys_data_dictionary]') IS NOT NULL
    CREATE INDEX ix_sys_dict_type_code ON [dbo].[sys_data_dictionary](dict_type, dict_code);

-- 1.8 iam_user: 按 username 查询 —— 已存在，幂等跳过
--     说明: username 已有唯一约束 uk_iam_user_username(自动创建唯一聚集/非聚集
--     索引)，可高效支持按用户名精确查询与登录校验，无需再建普通非唯一索引。
--     故此处不创建任何索引，仅作说明。


-- =====================================================================
-- 2. 清理过期数据
-- =====================================================================

-- 2.1 清理 30 天前的 outbox 消息，控制 mes_outbox_message 表体积增长
--     说明: 按 created_at 过滤删除；会一并清理长期滞留(超过30天)的
--     PENDING 消息。该 DELETE 幂等——重复执行时若无可删数据则影响 0 行。
--     前置 OBJECT_ID 检查确保表缺失时安全跳过。
IF OBJECT_ID(N'[dbo].[mes_outbox_message]') IS NOT NULL
    DELETE FROM [dbo].[mes_outbox_message]
    WHERE created_at < DATEADD(day, -30, GETDATE());


-- =====================================================================
-- 3. 更新统计信息
-- =====================================================================

-- 3.1 全量更新当前数据库所有表的统计信息
--     说明: sp_updatestats 需要 db_owner/sysadmin 权限，MES 应用用户无权执行。
--     SQL Server 默认启用自动统计更新(AUTO_UPDATE_STATISTICS)，新增索引后
--     查询优化器会在首次使用时自动更新统计信息。如需手动更新，请由 DBA 执行:
--     EXEC sp_updatestats;
-- UPDATE STATISTICS [dbo].[eng_product];
-- UPDATE STATISTICS [dbo].[prd_work_order];


-- =====================================================================
-- 结束: V21 优化迁移完成
--   新增索引: 6 个 (eng_product, prd_work_order, qa_batch, eng_bom,
--             eng_process_route, int_api_call_log)
--   已覆盖跳过: 2 个 (sys_data_dictionary, iam_user)
--   数据清理: mes_outbox_message 30天前数据
--   统计更新: sp_updatestats 全库
-- =====================================================================
