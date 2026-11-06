-- Button-level RBAC and user/role management.
-- Existing user.role is retained as the role code to keep the deployment backward compatible.

DECLARE @userRoleConstraint NVARCHAR(128);
SELECT @userRoleConstraint = cc.name
FROM sys.check_constraints cc
JOIN sys.columns c ON cc.parent_object_id = c.object_id AND cc.parent_column_id = c.column_id
WHERE cc.parent_object_id = OBJECT_ID(N'dbo.itrm_user') AND c.name = N'role';
IF @userRoleConstraint IS NOT NULL
    EXEC(N'ALTER TABLE dbo.itrm_user DROP CONSTRAINT [' + @userRoleConstraint + N']');

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = N'itrm_role' AND schema_id = SCHEMA_ID(N'dbo'))
BEGIN
    CREATE TABLE dbo.itrm_role (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        code NVARCHAR(40) NOT NULL,
        name NVARCHAR(80) NOT NULL,
        description NVARCHAR(300) NULL,
        status NVARCHAR(20) NOT NULL DEFAULT N'ACTIVE',
        built_in BIT NOT NULL DEFAULT 0,
        created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        created_by NVARCHAR(64) NULL,
        updated_by NVARCHAR(64) NULL,
        version BIGINT NOT NULL DEFAULT 0,
        CONSTRAINT uk_itrm_role_code UNIQUE (code)
    );
END

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = N'itrm_permission' AND schema_id = SCHEMA_ID(N'dbo'))
BEGIN
    CREATE TABLE dbo.itrm_permission (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        code NVARCHAR(80) NOT NULL,
        name NVARCHAR(80) NOT NULL,
        module NVARCHAR(40) NOT NULL,
        action NVARCHAR(30) NOT NULL,
        sort_order INT NOT NULL DEFAULT 0,
        created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        created_by NVARCHAR(64) NULL,
        updated_by NVARCHAR(64) NULL,
        version BIGINT NOT NULL DEFAULT 0,
        CONSTRAINT uk_itrm_permission_code UNIQUE (code)
    );
END

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = N'itrm_role_permission' AND schema_id = SCHEMA_ID(N'dbo'))
BEGIN
    CREATE TABLE dbo.itrm_role_permission (
        role_id BIGINT NOT NULL,
        permission_id BIGINT NOT NULL,
        CONSTRAINT pk_itrm_role_permission PRIMARY KEY (role_id, permission_id),
        CONSTRAINT fk_itrm_rp_role FOREIGN KEY (role_id) REFERENCES dbo.itrm_role(id) ON DELETE CASCADE,
        CONSTRAINT fk_itrm_rp_permission FOREIGN KEY (permission_id) REFERENCES dbo.itrm_permission(id) ON DELETE CASCADE
    );
END

MERGE dbo.itrm_role AS t
USING (VALUES
 (N'ADMIN',N'系统管理员',N'平台配置、用户、角色及全部业务权限',1),
 (N'BUSINESS_USER',N'业务需求人',N'提交并跟踪本部门需求',1),
 (N'IT_REVIEWER',N'需求评审人',N'接收、评审及变更需求',1),
 (N'IT_PLANNER',N'项目经理',N'排期、拆解任务与资源协调',1),
 (N'DEVELOPER',N'开发人员',N'执行开发任务并更新进度',1),
 (N'TESTER',N'测试人员',N'缺陷跟踪、测试与验收',1),
 (N'RELEASE_ENGINEER',N'发布工程师',N'发布确认与上线管理',1)
) AS s(code,name,description,built_in)
ON t.code=s.code
WHEN MATCHED THEN UPDATE SET name=s.name,description=s.description,built_in=s.built_in,updated_at=CURRENT_TIMESTAMP
WHEN NOT MATCHED THEN INSERT(code,name,description,status,built_in) VALUES(s.code,s.name,s.description,N'ACTIVE',s.built_in);

MERGE dbo.itrm_permission AS t
USING (VALUES
 (N'dashboard:view',N'查看工作台',N'工作台',N'VIEW',10),
 (N'requirement:view',N'查看需求池',N'需求管理',N'VIEW',100),
 (N'requirement:create',N'新建需求',N'需求管理',N'CREATE',110),
 (N'requirement:edit',N'编辑需求',N'需求管理',N'EDIT',120),
 (N'requirement:delete',N'删除需求',N'需求管理',N'DELETE',130),
 (N'requirement:submit',N'提交需求',N'需求管理',N'SUBMIT',140),
 (N'requirement:receive',N'接收需求',N'需求管理',N'RECEIVE',150),
 (N'requirement:review',N'评审需求',N'需求管理',N'REVIEW',160),
 (N'requirement:plan',N'需求排期',N'需求管理',N'PLAN',170),
 (N'requirement:change',N'需求变更',N'需求管理',N'CHANGE',180),
 (N'requirement:release',N'发布需求',N'需求管理',N'RELEASE',190),
 (N'board:view',N'查看研发看板',N'研发管理',N'VIEW',200),
 (N'board:move',N'移动看板卡片',N'研发管理',N'MOVE',210),
 (N'task:view',N'查看开发任务',N'研发管理',N'VIEW',220),
 (N'task:create',N'拆分开发任务',N'研发管理',N'CREATE',230),
 (N'task:edit',N'编辑开发任务',N'研发管理',N'EDIT',240),
 (N'task:delete',N'删除开发任务',N'研发管理',N'DELETE',250),
 (N'task:assign',N'分配负责人',N'研发管理',N'ASSIGN',260),
 (N'task:transition',N'更新任务状态',N'研发管理',N'TRANSITION',270),
 (N'schedule:view',N'查看甘特排期',N'计划与资源',N'VIEW',300),
 (N'schedule:edit',N'调整排期',N'计划与资源',N'EDIT',310),
 (N'test:view',N'查看测试与缺陷',N'质量管理',N'VIEW',400),
 (N'test:create',N'新建缺陷',N'质量管理',N'CREATE',410),
 (N'test:edit',N'编辑缺陷',N'质量管理',N'EDIT',420),
 (N'test:delete',N'删除缺陷',N'质量管理',N'DELETE',430),
 (N'test:transition',N'处理缺陷状态',N'质量管理',N'TRANSITION',440),
 (N'timelog:view',N'查看工时',N'数据分析',N'VIEW',500),
 (N'timelog:create',N'登记工时',N'数据分析',N'CREATE',510),
 (N'timelog:delete',N'删除工时',N'数据分析',N'DELETE',520),
 (N'report:view',N'查看统计报表',N'数据分析',N'VIEW',530),
 (N'workflow:view',N'查看审批流',N'系统设置',N'VIEW',600),
 (N'workflow:manage',N'配置审批流',N'系统设置',N'MANAGE',610),
 (N'user:view',N'查看用户',N'系统设置',N'VIEW',700),
 (N'user:create',N'新建用户',N'系统设置',N'CREATE',710),
 (N'user:edit',N'编辑用户',N'系统设置',N'EDIT',720),
 (N'user:disable',N'启停用户',N'系统设置',N'DISABLE',730),
 (N'user:reset-password',N'重置密码',N'系统设置',N'RESET',740),
 (N'role:view',N'查看角色权限',N'系统设置',N'VIEW',800),
 (N'role:manage',N'配置角色权限',N'系统设置',N'MANAGE',810)
) AS s(code,name,module,action,sort_order)
ON t.code=s.code
WHEN MATCHED THEN UPDATE SET name=s.name,module=s.module,action=s.action,sort_order=s.sort_order,updated_at=CURRENT_TIMESTAMP
WHEN NOT MATCHED THEN INSERT(code,name,module,action,sort_order) VALUES(s.code,s.name,s.module,s.action,s.sort_order);

-- Administrator receives the full catalogue.
INSERT INTO dbo.itrm_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM dbo.itrm_role r CROSS JOIN dbo.itrm_permission p
WHERE r.code=N'ADMIN' AND NOT EXISTS (
 SELECT 1 FROM dbo.itrm_role_permission rp WHERE rp.role_id=r.id AND rp.permission_id=p.id
);

-- Default least-privilege mappings for the built-in business roles.
DECLARE @roleMap TABLE(role_code NVARCHAR(40), permission_code NVARCHAR(80));
INSERT INTO @roleMap VALUES
 (N'BUSINESS_USER',N'dashboard:view'),(N'BUSINESS_USER',N'requirement:view'),(N'BUSINESS_USER',N'requirement:create'),(N'BUSINESS_USER',N'requirement:edit'),(N'BUSINESS_USER',N'requirement:submit'),(N'BUSINESS_USER',N'timelog:view'),(N'BUSINESS_USER',N'timelog:create'),
 (N'IT_REVIEWER',N'dashboard:view'),(N'IT_REVIEWER',N'requirement:view'),(N'IT_REVIEWER',N'requirement:edit'),(N'IT_REVIEWER',N'requirement:receive'),(N'IT_REVIEWER',N'requirement:review'),(N'IT_REVIEWER',N'requirement:change'),(N'IT_REVIEWER',N'workflow:view'),
 (N'IT_PLANNER',N'dashboard:view'),(N'IT_PLANNER',N'requirement:view'),(N'IT_PLANNER',N'requirement:plan'),(N'IT_PLANNER',N'board:view'),(N'IT_PLANNER',N'board:move'),(N'IT_PLANNER',N'task:view'),(N'IT_PLANNER',N'task:create'),(N'IT_PLANNER',N'task:edit'),(N'IT_PLANNER',N'task:delete'),(N'IT_PLANNER',N'task:assign'),(N'IT_PLANNER',N'task:transition'),(N'IT_PLANNER',N'schedule:view'),(N'IT_PLANNER',N'schedule:edit'),(N'IT_PLANNER',N'timelog:view'),(N'IT_PLANNER',N'report:view'),
 (N'DEVELOPER',N'dashboard:view'),(N'DEVELOPER',N'requirement:view'),(N'DEVELOPER',N'board:view'),(N'DEVELOPER',N'board:move'),(N'DEVELOPER',N'task:view'),(N'DEVELOPER',N'task:edit'),(N'DEVELOPER',N'task:transition'),(N'DEVELOPER',N'schedule:view'),(N'DEVELOPER',N'timelog:view'),(N'DEVELOPER',N'timelog:create'),
 (N'TESTER',N'dashboard:view'),(N'TESTER',N'requirement:view'),(N'TESTER',N'board:view'),(N'TESTER',N'task:view'),(N'TESTER',N'test:view'),(N'TESTER',N'test:create'),(N'TESTER',N'test:edit'),(N'TESTER',N'test:transition'),(N'TESTER',N'timelog:view'),(N'TESTER',N'timelog:create'),
 (N'RELEASE_ENGINEER',N'dashboard:view'),(N'RELEASE_ENGINEER',N'requirement:view'),(N'RELEASE_ENGINEER',N'requirement:release'),(N'RELEASE_ENGINEER',N'board:view'),(N'RELEASE_ENGINEER',N'schedule:view'),(N'RELEASE_ENGINEER',N'report:view');

INSERT INTO dbo.itrm_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM @roleMap m
JOIN dbo.itrm_role r ON r.code=m.role_code
JOIN dbo.itrm_permission p ON p.code=m.permission_code
WHERE NOT EXISTS (SELECT 1 FROM dbo.itrm_role_permission rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
