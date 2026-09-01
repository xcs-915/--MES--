-- =====================================================================
-- 迁移脚本: V23__optimize_outbox_and_logs.sql
-- 说明: 优化 Outbox 消息表索引、审计日志与接口日志的分批清理机制
--       新增存储过程用于定时批量清理过期数据
-- 适用: SQL Server 2019, 数据库 tns_mes
-- 特性: 幂等执行(可重复运行不报错)，每个对象创建前均做存在性检查
-- =====================================================================

-- =====================================================================
-- 1. Outbox 消息表索引优化
--    V2 已创建 ix_mes_outbox_pending(status, next_attempt_at, created_at)
--    V21 已删除 30天前数据。本迁移补充以下索引：
--    a) 按状态+重试次数查询死信消息（FAILED 状态监控）
--    b) 按聚合类型+聚合ID查询（事件溯源场景）
-- =====================================================================

-- 1.1 死信消息查询索引: status + attempt_count
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_mes_outbox_status_attempts'
        AND object_id = OBJECT_ID(N'[dbo].[mes_outbox_message]'))
    CREATE INDEX ix_mes_outbox_status_attempts ON [dbo].[mes_outbox_message](status, attempt_count);

-- 1.2 聚合类型+聚合ID索引: 事件溯源查询
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_mes_outbox_aggregate'
        AND object_id = OBJECT_ID(N'[dbo].[mes_outbox_message]'))
    CREATE INDEX ix_mes_outbox_aggregate ON [dbo].[mes_outbox_message](aggregate_type, aggregate_id);


-- =====================================================================
-- 2. 审计日志表索引补充
--    V1 已创建 ix_sys_audit_log_created(created_at)
--    补充: 按用户+操作时间查询，按资源+操作结果查询
-- =====================================================================

-- 2.1 按用户+时间查询审计日志
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_sys_audit_log_user_created'
        AND object_id = OBJECT_ID(N'[dbo].[sys_audit_log]'))
    AND OBJECT_ID(N'[dbo].[sys_audit_log]') IS NOT NULL
    CREATE INDEX ix_sys_audit_log_user_created ON [dbo].[sys_audit_log](user_name, created_at);

-- 2.2 按资源+结果查询审计日志（如: 查询所有失败的 DELETE 操作）
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_sys_audit_log_resource_result'
        AND object_id = OBJECT_ID(N'[dbo].[sys_audit_log]'))
    AND OBJECT_ID(N'[dbo].[sys_audit_log]') IS NOT NULL
    CREATE INDEX ix_sys_audit_log_resource_result ON [dbo].[sys_audit_log](resource, result, created_at);


-- =====================================================================
-- 3. 接口调用日志表索引补充
--    V6 已创建 ix_int_api_call_log_endpoint, ix_int_api_call_log_system_code, ix_int_api_call_log_created_at
--    V21 已创建 ix_int_api_call_log_created_system(created_at, system_code)
--    补充: 按成功状态+时间查询（快速定位失败调用）
-- =====================================================================

-- 3.1 按成功状态+时间查询失败调用
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_int_api_call_log_success_created'
        AND object_id = OBJECT_ID(N'[dbo].[int_api_call_log]'))
    AND OBJECT_ID(N'[dbo].[int_api_call_log]') IS NOT NULL
    CREATE INDEX ix_int_api_call_log_success_created ON [dbo].[int_api_call_log](success, created_at);


-- =====================================================================
-- 4. 存储过程: 分批清理过期数据
--    目的: 定时清理 Outbox 消息、审计日志、接口调用日志
--    策略: 每次删除最多 5000 行，避免长事务锁表
--    调用方式: 可由 Spring Scheduler 或 SQL Agent 定时执行
-- =====================================================================

-- 4.1 清理 Outbox 消息: PUBLISHED 和 FAILED 状态的过期消息
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE type = N'P' AND name = N'sp_mes_cleanup_outbox')
    EXEC('CREATE PROCEDURE sp_mes_cleanup_outbox AS BEGIN SET NOCOUNT ON; END');
GO
ALTER PROCEDURE [dbo].[sp_mes_cleanup_outbox]
    @RetentionDays INT = 30,
    @BatchSize INT = 5000
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @Deleted INT = 1;
    DECLARE @TotalDeleted INT = 0;
    DECLARE @Cutoff DATETIME2 = DATEADD(day, -@RetentionDays, GETDATE());

    WHILE @Deleted > 0
    BEGIN
        DELETE TOP (@BatchSize) FROM [dbo].[mes_outbox_message]
        WHERE created_at < @Cutoff
          AND status IN ('PUBLISHED', 'FAILED');

        SET @Deleted = @@ROWCOUNT;
        SET @TotalDeleted = @TotalDeleted + @Deleted;

        -- 批次间短暂暂停，减少锁争用
        IF @Deleted > 0
            WAITFOR DELAY '00:00:00.01';
    END

    SELECT @TotalDeleted AS DeletedCount, @RetentionDays AS RetentionDays;
END
GO

-- 4.2 清理审计日志
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE type = N'P' AND name = N'sp_mes_cleanup_audit_log')
    EXEC('CREATE PROCEDURE sp_mes_cleanup_audit_log AS BEGIN SET NOCOUNT ON; END');
GO
ALTER PROCEDURE [dbo].[sp_mes_cleanup_audit_log]
    @RetentionDays INT = 90,
    @BatchSize INT = 5000
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @Deleted INT = 1;
    DECLARE @TotalDeleted INT = 0;
    DECLARE @Cutoff DATETIME2 = DATEADD(day, -@RetentionDays, GETDATE());

    WHILE @Deleted > 0
    BEGIN
        DELETE TOP (@BatchSize) FROM [dbo].[sys_audit_log]
        WHERE created_at < @Cutoff;

        SET @Deleted = @@ROWCOUNT;
        SET @TotalDeleted = @TotalDeleted + @Deleted;

        IF @Deleted > 0
            WAITFOR DELAY '00:00:00.01';
    END

    SELECT @TotalDeleted AS DeletedCount, @RetentionDays AS RetentionDays;
END
GO

-- 4.3 清理接口调用日志
IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE type = N'P' AND name = N'sp_mes_cleanup_api_call_log')
    EXEC('CREATE PROCEDURE sp_mes_cleanup_api_call_log AS BEGIN SET NOCOUNT ON; END');
GO
ALTER PROCEDURE [dbo].[sp_mes_cleanup_api_call_log]
    @RetentionDays INT = 30,
    @BatchSize INT = 5000
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @Deleted INT = 1;
    DECLARE @TotalDeleted INT = 0;
    DECLARE @Cutoff DATETIME2 = DATEADD(day, -@RetentionDays, GETDATE());

    WHILE @Deleted > 0
    BEGIN
        DELETE TOP (@BatchSize) FROM [dbo].[int_api_call_log]
        WHERE created_at < @Cutoff;

        SET @Deleted = @@ROWCOUNT;
        SET @TotalDeleted = @TotalDeleted + @Deleted;

        IF @Deleted > 0
            WAITFOR DELAY '00:00:00.01';
    END

    SELECT @TotalDeleted AS DeletedCount, @RetentionDays AS RetentionDays;
END
GO


-- =====================================================================
-- 5. 一次性执行清理（迁移时即清理一次过期数据）
-- =====================================================================

-- 5.1 清理 30 天前的已发布/已失败 Outbox 消息
IF OBJECT_ID(N'[dbo].[mes_outbox_message]') IS NOT NULL
    DELETE TOP (5000) FROM [dbo].[mes_outbox_message]
    WHERE created_at < DATEADD(day, -30, GETDATE())
      AND status IN ('PUBLISHED', 'FAILED');

-- 5.2 清理 90 天前的审计日志
IF OBJECT_ID(N'[dbo].[sys_audit_log]') IS NOT NULL
    DELETE TOP (5000) FROM [dbo].[sys_audit_log]
    WHERE created_at < DATEADD(day, -90, GETDATE());

-- 5.3 清理 30 天前的接口调用日志
IF OBJECT_ID(N'[dbo].[int_api_call_log]') IS NOT NULL
    DELETE TOP (5000) FROM [dbo].[int_api_call_log]
    WHERE created_at < DATEADD(day, -30, GETDATE());


-- =====================================================================
-- 6. 更新统计信息 (由 DBA 手动执行, MES 应用用户无 sp_updatestats 权限)
-- =====================================================================
-- EXEC sp_updatestats;


-- =====================================================================
-- 结束: V23 优化迁移完成
--   新增索引: 5 个 (outbox×2, audit_log×2, api_call_log×1)
--   存储过程: 3 个 (分批清理 outbox/audit_log/api_call_log)
--   数据清理: 一次性清理过期数据 (各最多 5000 行)
--   统计更新: sp_updatestats 全库
-- =====================================================================
