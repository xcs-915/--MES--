-- =====================================================================
-- 迁移脚本: V22__rename_product_table.sql
-- 说明: 将产品表从 eng_product 重命名为 product，与 JPA 实体 @Table 注解一致。
--       同时重建引用该表的外键约束。
-- 适用: SQL Server 2019, 数据库 tns_mes
-- 特性: 幂等执行 —— 表不存在或已重命名时均安全跳过。
-- =====================================================================

-- =====================================================================
-- 1. 重命名表 eng_product → product
--    同时重建引用该表的外键约束
-- =====================================================================

IF OBJECT_ID(N'[dbo].[eng_product]', 'U') IS NOT NULL
    AND OBJECT_ID(N'[dbo].[product]', 'U') IS NULL
BEGIN
    -- 1.1 删除引用 eng_product 的外键约束
    --     说明: sp_rename 不会自动更新外键约束的引用对象名，
    --     需先删除旧约束，重命名表后再重建。
    IF OBJECT_ID(N'[dbo].[fk_eng_bom_product]', 'F') IS NOT NULL
        ALTER TABLE [dbo].[eng_bom] DROP CONSTRAINT [fk_eng_bom_product];

    IF OBJECT_ID(N'[dbo].[fk_eng_bom_item_product]', 'F') IS NOT NULL
        ALTER TABLE [dbo].[eng_bom_item] DROP CONSTRAINT [fk_eng_bom_item_product];

    IF OBJECT_ID(N'[dbo].[fk_eng_route_product]', 'F') IS NOT NULL
        ALTER TABLE [dbo].[eng_process_route] DROP CONSTRAINT [fk_eng_route_product];

    IF OBJECT_ID(N'[dbo].[fk_prd_work_order_product]', 'F') IS NOT NULL
        ALTER TABLE [dbo].[prd_work_order] DROP CONSTRAINT [fk_prd_work_order_product];

    -- 1.2 重命名表
    EXEC sp_rename N'[dbo].[eng_product]', N'product', 'OBJECT';

    -- 1.3 重建外键约束（指向新表名 product）
    ALTER TABLE [dbo].[eng_bom]
        ADD CONSTRAINT [fk_eng_bom_product]
        FOREIGN KEY (product_id) REFERENCES [dbo].[product](id);

    ALTER TABLE [dbo].[eng_bom_item]
        ADD CONSTRAINT [fk_eng_bom_item_product]
        FOREIGN KEY (component_product_id) REFERENCES [dbo].[product](id);

    ALTER TABLE [dbo].[eng_process_route]
        ADD CONSTRAINT [fk_eng_route_product]
        FOREIGN KEY (product_id) REFERENCES [dbo].[product](id);

    ALTER TABLE [dbo].[prd_work_order]
        ADD CONSTRAINT [fk_prd_work_order_product]
        FOREIGN KEY (product_id) REFERENCES [dbo].[product](id);

    PRINT 'Table renamed: eng_product -> product, FK constraints rebuilt.';
END
ELSE
BEGIN
    PRINT 'Table eng_product does not exist or product already exists. Skipping.';
END

-- =====================================================================
-- 2. 扩展 product_description 列为 NVARCHAR(MAX)
--    说明: SAP A_ProductBasicTextType.LongText 无长度限制，
--    原 NVARCHAR(500) 可能截断长文本描述。
-- =====================================================================
IF OBJECT_ID(N'[dbo].[product]', 'U') IS NOT NULL
BEGIN
    ALTER TABLE [dbo].[product] ALTER COLUMN [product_description] NVARCHAR(MAX);
    PRINT 'Column product_description altered to NVARCHAR(MAX).';
END
