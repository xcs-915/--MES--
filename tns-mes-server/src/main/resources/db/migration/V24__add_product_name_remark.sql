-- V24: 添加 product 表 name 和 remark 列
-- name: 存储 SAP A_ProductDescription 同步的产品描述
-- remark: 产品备注

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[product]') AND name = N'name')
    ALTER TABLE [dbo].[product] ADD [name] NVARCHAR(200) NULL;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[product]') AND name = N'remark')
    ALTER TABLE [dbo].[product] ADD [remark] NVARCHAR(500) NULL;
GO

-- 将现有 name_zh 值回填到 name 列
-- 使用 EXEC 避免批处理编译时列不存在错误
EXEC('UPDATE [dbo].[product] SET [name] = [name_zh] WHERE [name] IS NULL AND [name_zh] IS NOT NULL');
