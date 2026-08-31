/* ============================================================================== */
/* 脚本名称: init-sqlserver.sql                                                    */
/* 功能描述: SQL Server 数据库初始化脚本                                           */
/*           创建 tns_mes 数据库、登录用户并授权                                    */
/* 适用系统: Windows Server 2019 + SQL Server 2019                                  */
/* 执行账户: sa (系统管理员)                                                        */
/* ============================================================================== */

/* ============================================================================== */
/* 注意事项:                                                                       */
/*   1. 本脚本需要使用 sa 账户或有同等权限的账户执行                                */
/*   2. 数据库名称: tns_mes                                                        */
/* 3. 登录用户: tns_mes_user / <DB_PASSWORD> (部署时替换)                            */
/*   4. 排序规则: Chinese_PRC_CI_AS (中文不区分大小写)                              */
/*   5. 恢复模式: SIMPLE (开发环境，减少日志占用)                                    */
/* ============================================================================== */

/* ============================================================================== */
/* 第一部分: 创建数据库 tns_mes                                                    */
/* ============================================================================== */

USE [master];
GO

/* 检查数据库是否已存在，若存在则先删除 (开发环境，可安全重建) */
IF EXISTS (SELECT 1 FROM sys.databases WHERE name = N'tns_mes')
BEGIN
    /* 先设置为单用户模式，断开所有连接 */
    ALTER DATABASE [tns_mes] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE [tns_mes];
END
GO

/* 创建数据库 tns_mes，设置排序规则为 Chinese_PRC_CI_AS */
CREATE DATABASE [tns_mes]
    COLLATE Chinese_PRC_CI_AS;
GO

/* ============================================================================== */
/* 第二部分: 配置数据库属性                                                        */
/* ============================================================================== */

/* 设置数据库恢复模式为 SIMPLE (开发环境，减少事务日志占用) */
ALTER DATABASE [tns_mes] SET RECOVERY SIMPLE;
GO

/* 启用数据库自动增长 */
ALTER DATABASE [tns_mes] MODIFY FILE
(
    NAME = N'tns_mes',
    FILEGROWTH = 256MB
);
GO

/* 修改日志文件自动增长 */
ALTER DATABASE [tns_mes] MODIFY FILE
(
    NAME = N'tns_mes_log',
    FILEGROWTH = 64MB
);
GO

/* 设置数据库兼容性级别为 SQL Server 2019 (150) */
ALTER DATABASE [tns_mes] SET COMPATIBILITY_LEVEL = 150;
GO

/* 启用数据库参数优化 (开发环境) */
ALTER DATABASE [tns_mes] SET READ_COMMITTED_SNAPSHOT ON;
GO

ALTER DATABASE [tns_mes] SET RECURSIVE_TRIGGERS OFF;
GO

/* 设置数据库默认为多用户模式 */
ALTER DATABASE [tns_mes] SET MULTI_USER;
GO

/* ============================================================================== */
/* 第三部分: 创建登录用户 tns_mes_user                                            */
/* ============================================================================== */

USE [master];
GO

/* 检查登录用户是否已存在，若存在则先删除 */
IF EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'tns_mes_user' AND type = N'S')
BEGIN
    /* 先确保没有用户映射到此登录 */
    IF EXISTS (SELECT 1 FROM sys.databases WHERE name = N'tns_mes')
    BEGIN
        USE [tns_mes];
        IF EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'tns_mes_user' AND type = N'U')
        BEGIN
            DROP USER [tns_mes_user];
        END
    END

    USE [master];
    DROP LOGIN [tns_mes_user];
END
GO

/* 创建登录用户 tns_mes_user，设置密码 (部署时替换为实际密码) */
/* 使用 CHECK_POLICY = ON 确保密码符合策略 */
CREATE LOGIN [tns_mes_user]
    WITH PASSWORD = N'$(DB_PASSWORD)',
         DEFAULT_DATABASE = [tns_mes],
         DEFAULT_LANGUAGE = [简体中文],
         CHECK_EXPIRATION = OFF,   /* 密码不过期 */
         CHECK_POLICY = ON;        /* 启用密码策略 */
GO

/* ============================================================================== */
/* 第四部分: 创建数据库用户并授权                                                  */
/* ============================================================================== */

USE [tns_mes];
GO

/* 在 tns_mes 数据库中创建映射到 tns_mes_user 登录的用户 */
CREATE USER [tns_mes_user]
    FOR LOGIN [tns_mes_user]
    WITH DEFAULT_SCHEMA = [dbo];
GO

/* 授予 tns_mes_user 对 tns_mes 数据库的 db_owner 权限 */
/* db_owner 权限可执行: 建表、建视图、存储过程、数据增删改查、授权等 */
ALTER ROLE [db_owner] ADD MEMBER [tns_mes_user];
GO

/* 授予连接权限 */
GRANT CONNECT TO [tns_mes_user];
GO

/* ============================================================================== */
/* 第五部分: 验证创建结果                                                          */
/* ============================================================================== */

/* 验证数据库创建 */
USE [master];
GO

SELECT
    '数据库验证' AS [验证项目],
    name AS [数据库名],
    state_desc AS [状态],
    recovery_model_desc AS [恢复模式],
    collation_name AS [排序规则]
FROM sys.databases
WHERE name = N'tns_mes';
GO

/* 验证登录用户创建 */
SELECT
    '登录用户验证' AS [验证项目],
    name AS [登录名],
    type_desc AS [类型],
    default_database_name AS [默认数据库],
    is_disabled AS [是否禁用]
FROM sys.server_principals
WHERE name = N'tns_mes_user';
GO

/* 验证数据库用户和权限 */
USE [tns_mes];
GO

SELECT
    '数据库用户验证' AS [验证项目],
    dp.name AS [数据库用户],
    sp.name AS [关联登录名],
    dp.default_schema_name AS [默认架构]
FROM sys.database_principals dp
    INNER JOIN sys.server_principals sp ON dp.principal_id = sp.principal_id
WHERE sp.name = N'tns_mes_user';
GO

/* 验证角色成员 */
SELECT
    '角色权限验证' AS [验证项目],
    r.name AS [角色名],
    m.name AS [成员名]
FROM sys.database_role_members rm
    INNER JOIN sys.database_principals r ON rm.role_principal_id = r.principal_id
    INNER JOIN sys.database_principals m ON rm.member_principal_id = m.principal_id
WHERE m.name = N'tns_mes_user';
GO

/* ============================================================================== */
/* 初始化完成                                                                      */
/* ============================================================================== */
PRINT '==============================================';
PRINT '  SQL Server 数据库初始化完成!';
PRINT '==============================================';
PRINT '';
PRINT '数据库: tns_mes';
PRINT '排序规则: Chinese_PRC_CI_AS';
PRINT '恢复模式: SIMPLE';
PRINT '登录用户: tns_mes_user';
PRINT '密码: <DB_PASSWORD>';
PRINT '权限: db_owner';
PRINT '';
PRINT '连接字符串示例:';
PRINT '  Server=<DB_HOST>;Database=tns_mes;User Id=tns_mes_user;Password=<DB_PASSWORD>;TrustServerCertificate=True;';
PRINT '';
GO
