-- =====================================================================
-- V3: 补充实体类中存在但表中缺失的字段
-- =====================================================================

-- 补充 description 字段
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('dbo.itrm_requirement') AND name = 'description')
BEGIN
    ALTER TABLE dbo.itrm_requirement ADD description NVARCHAR(MAX) NULL;
END

-- 补充 feasibility 字段
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('dbo.itrm_requirement') AND name = 'feasibility')
BEGIN
    ALTER TABLE dbo.itrm_requirement ADD feasibility NVARCHAR(1000) NULL;
END

-- 补充 workload_estimate 字段
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('dbo.itrm_requirement') AND name = 'workload_estimate')
BEGIN
    ALTER TABLE dbo.itrm_requirement ADD workload_estimate DECIMAL(10,2) NULL DEFAULT 0;
END

-- 补充 review_comment 字段
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('dbo.itrm_requirement') AND name = 'review_comment')
BEGIN
    ALTER TABLE dbo.itrm_requirement ADD review_comment NVARCHAR(1000) NULL;
END

-- 补充 online_date 字段
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('dbo.itrm_requirement') AND name = 'online_date')
BEGIN
    ALTER TABLE dbo.itrm_requirement ADD online_date DATE NULL;
END

-- 补充 close_date 字段
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('dbo.itrm_requirement') AND name = 'close_date')
BEGIN
    ALTER TABLE dbo.itrm_requirement ADD close_date DATE NULL;
END

-- 补充 attachments 字段（存储附件引用JSON）
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('dbo.itrm_requirement') AND name = 'attachments')
BEGIN
    ALTER TABLE dbo.itrm_requirement ADD attachments NVARCHAR(MAX) NULL;
END
