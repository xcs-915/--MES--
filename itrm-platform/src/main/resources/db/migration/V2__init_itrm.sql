-- =====================================================================
-- IT 需求管理平台 初始化 SQL 脚本 (V2 - 新状态机 + 审批流 + 变更记录)
-- 数据库: SQL Server
-- Flyway 记录表: itrm_flyway_schema_history
-- =====================================================================

-- ============================================================
-- 1. 用户表 itrm_user
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'itrm_user' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.itrm_user (
        id              BIGINT IDENTITY(1,1) NOT NULL,
        username        NVARCHAR(64)        NOT NULL,
        password        NVARCHAR(128)       NOT NULL,
        real_name       NVARCHAR(64)        NULL,
        role            NVARCHAR(30)        NULL,
        department      NVARCHAR(64)        NULL,
        status          NVARCHAR(20)        NOT NULL DEFAULT N'ACTIVE',
        email           NVARCHAR(200)        NULL,
        created_at      DATETIME2           NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at      DATETIME2           NOT NULL DEFAULT CURRENT_TIMESTAMP,
        created_by      NVARCHAR(64)        NULL,
        updated_by      NVARCHAR(64)        NULL,
        version         INT                 NOT NULL DEFAULT 0,
        CONSTRAINT pk_itrm_user PRIMARY KEY CLUSTERED (id),
        CONSTRAINT uk_itrm_user_username UNIQUE NONCLUSTERED (username),
        CONSTRAINT ck_itrm_user_role CHECK (role IN ('ADMIN','BUSINESS_USER','IT_REVIEWER','IT_PLANNER','DEVELOPER','TESTER','RELEASE_ENGINEER')),
        CONSTRAINT ck_itrm_user_status CHECK (status IN ('ACTIVE','DISABLED'))
    );
END

-- ============================================================
-- 2. 审批模板表 itrm_approval_template
--    不同类型/系统的需求走不同审批路线
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'itrm_approval_template' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.itrm_approval_template (
        id              BIGINT IDENTITY(1,1) NOT NULL,
        name            NVARCHAR(100)       NOT NULL,
        code            NVARCHAR(50)        NOT NULL,
        description     NVARCHAR(500)       NULL,
        category        NVARCHAR(30)        NULL,        -- 适用需求类型
        belong_system   NVARCHAR(30)        NULL,        -- 适用系统
        is_default      BIT                 NOT NULL DEFAULT 0,
        status          NVARCHAR(20)        NOT NULL DEFAULT N'ACTIVE',
        created_at      DATETIME2           NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at      DATETIME2           NOT NULL DEFAULT CURRENT_TIMESTAMP,
        created_by      NVARCHAR(64)        NULL,
        updated_by      NVARCHAR(64)        NULL,
        version         INT                 NOT NULL DEFAULT 0,
        CONSTRAINT pk_itrm_appr_tpl PRIMARY KEY CLUSTERED (id),
        CONSTRAINT uk_itrm_appr_tpl_code UNIQUE NONCLUSTERED (code)
    );
END

-- ============================================================
-- 3. 审批节点表 itrm_approval_node
--    一个模板包含多个审批节点，按顺序执行
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'itrm_approval_node' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.itrm_approval_node (
        id              BIGINT IDENTITY(1,1) NOT NULL,
        template_id     BIGINT              NOT NULL,
        node_name       NVARCHAR(100)       NOT NULL,
        node_type       NVARCHAR(30)        NOT NULL,   -- REVIEW/APPROVE/ACKNOWLEDGE
        approver_role   NVARCHAR(30)        NULL,       -- 审批人角色
        approver_user   NVARCHAR(64)        NULL,       -- 指定审批人(可空)
        sort_order      INT                 NOT NULL DEFAULT 0,
        created_at      DATETIME2           NOT NULL DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT pk_itrm_appr_node PRIMARY KEY CLUSTERED (id)
    );
END

-- ============================================================
-- 4. 需求主表 itrm_requirement
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'itrm_requirement' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.itrm_requirement (
        id                  BIGINT IDENTITY(1,1) NOT NULL,
        req_no              NVARCHAR(32)        NOT NULL,
        title               NVARCHAR(200)       NOT NULL,
        category            NVARCHAR(30)        NOT NULL DEFAULT N'NEW_FUNC',
        belong_system       NVARCHAR(30)        NOT NULL DEFAULT N'MES',
        urgency             NVARCHAR(20)        NOT NULL DEFAULT N'MEDIUM',
        source_dept         NVARCHAR(64)        NULL,
        proposer            NVARCHAR(64)        NULL,
        current_problem     NVARCHAR(MAX)       NULL,        -- 当前问题
        expected_effect     NVARCHAR(MAX)       NULL,        -- 预期效果
        acceptance_criteria NVARCHAR(MAX)       NULL,        -- 验收标准
        status              NVARCHAR(30)        NOT NULL DEFAULT N'DRAFT',
        review_owner        NVARCHAR(64)        NULL,        -- 评审负责人（IT接收人）
        dev_owner           NVARCHAR(64)        NULL,        -- 开发负责人
        tester              NVARCHAR(64)        NULL,        -- 测试人员
        approval_template_id BIGINT             NULL,        -- 审批模板ID
        approval_node_idx   INT                 NOT NULL DEFAULT 0,  -- 当前审批节点序号
        planned_start_date  DATE                NULL,
        planned_end_date    DATE                NULL,
        dev_planned_end     DATE                NULL,        -- 开发预计完成时间
        actual_start_date   DATE                NULL,
        actual_end_date     DATE                NULL,
        test_start_date     DATE                NULL,
        test_end_date       DATE                NULL,
        release_date        DATE                NULL,        -- 发布日期
        complete_date       DATE                NULL,        -- 完成日期
        progress            INT                 NOT NULL DEFAULT 0,
        estimated_hours     DECIMAL(10,2)       NOT NULL DEFAULT 0,
        actual_hours        DECIMAL(10,2)       NOT NULL DEFAULT 0,
        tags                NVARCHAR(500)       NULL,
        remark              NVARCHAR(1000)      NULL,
        submitted_at        DATETIME2           NULL,
        received_at         DATETIME2           NULL,        -- IT接收时间
        reviewed_at         DATETIME2           NULL,
        created_at          DATETIME2           NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at          DATETIME2           NOT NULL DEFAULT CURRENT_TIMESTAMP,
        created_by          NVARCHAR(64)        NULL,
        updated_by          NVARCHAR(64)        NULL,
        version             INT                 NOT NULL DEFAULT 0,
        CONSTRAINT pk_itrm_req PRIMARY KEY CLUSTERED (id),
        CONSTRAINT uk_itrm_req_no UNIQUE NONCLUSTERED (req_no),
        CONSTRAINT ck_itrm_req_status CHECK (status IN (
            N'DRAFT', N'PENDING_RECEIVE', N'PENDING_PLAN',
            N'IN_DEVELOPMENT', N'IN_TESTING',
            N'PENDING_RELEASE', N'COMPLETED', N'REJECTED'
        )),
        CONSTRAINT ck_itrm_req_urgency CHECK (urgency IN (N'URGENT',N'HIGH',N'MEDIUM',N'LOW')),
        CONSTRAINT ck_itrm_req_category CHECK (category IN (
            N'NEW_FUNC',N'FUNC_OPT',N'BUG',N'INTERFACE',N'REPORT',N'OTHER'
        )),
        CONSTRAINT ck_itrm_req_system CHECK (belong_system IN (
            N'MES',N'WMS',N'SAP',N'OA',N'SCADA',N'OTHER'
        ))
    );
    CREATE NONCLUSTERED INDEX ix_itrm_req_status ON dbo.itrm_requirement(status, updated_at DESC);
    CREATE NONCLUSTERED INDEX ix_itrm_req_dev_owner ON dbo.itrm_requirement(dev_owner, status);
    CREATE NONCLUSTERED INDEX ix_itrm_req_dept ON dbo.itrm_requirement(source_dept, status);
    CREATE NONCLUSTERED INDEX ix_itrm_req_proposer ON dbo.itrm_requirement(proposer, created_at DESC);
END

-- ============================================================
-- 5. 审批记录表 itrm_approval_record
--    每个需求的审批过程记录
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'itrm_approval_record' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.itrm_approval_record (
        id              BIGINT IDENTITY(1,1) NOT NULL,
        requirement_id  BIGINT              NOT NULL,
        template_id     BIGINT              NOT NULL,
        node_idx        INT                 NOT NULL,
        node_name       NVARCHAR(100)       NOT NULL,
        node_type       NVARCHAR(30)        NOT NULL,
        approver        NVARCHAR(64)        NULL,
        action          NVARCHAR(20)        NOT NULL,   -- APPROVE/REJECT/PENDING
        comment         NVARCHAR(1000)      NULL,
        acted_at        DATETIME2           NULL,
        created_at      DATETIME2           NOT NULL DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT pk_itrm_appr_rec PRIMARY KEY CLUSTERED (id),
        CONSTRAINT fk_itrm_appr_rec_req FOREIGN KEY (requirement_id) REFERENCES dbo.itrm_requirement(id)
    );
    CREATE NONCLUSTERED INDEX ix_itrm_appr_rec_req ON dbo.itrm_approval_record(requirement_id, created_at DESC);
END

-- ============================================================
-- 6. 开发任务表 itrm_task
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'itrm_task' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.itrm_task (
        id              BIGINT IDENTITY(1,1) NOT NULL,
        requirement_id  BIGINT              NOT NULL,
        task_no         NVARCHAR(32)        NOT NULL,
        title           NVARCHAR(200)       NOT NULL,
        description     NVARCHAR(1000)      NULL,
        status          NVARCHAR(30)        NOT NULL DEFAULT N'TODO',
        priority        NVARCHAR(20)        NOT NULL DEFAULT N'MEDIUM',
        assignee        NVARCHAR(64)        NULL,
        estimated_hours DECIMAL(10,2)       NOT NULL DEFAULT 0,
        actual_hours    DECIMAL(10,2)       NOT NULL DEFAULT 0,
        progress        INT                 NOT NULL DEFAULT 0,
        start_date      DATE                NULL,
        due_date        DATE                NULL,
        completed_date  DATE                NULL,
        sort_order      INT                 NOT NULL DEFAULT 0,
        remark          NVARCHAR(500)       NULL,
        created_at      DATETIME2           NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at      DATETIME2           NOT NULL DEFAULT CURRENT_TIMESTAMP,
        created_by      NVARCHAR(64)        NULL,
        updated_by      NVARCHAR(64)        NULL,
        version         INT                 NOT NULL DEFAULT 0,
        CONSTRAINT pk_itrm_task PRIMARY KEY CLUSTERED (id),
        CONSTRAINT fk_itrm_task_req FOREIGN KEY (requirement_id) REFERENCES dbo.itrm_requirement(id),
        CONSTRAINT ck_itrm_task_status CHECK (status IN (N'TODO',N'IN_PROGRESS',N'TESTING',N'DONE',N'BLOCKED')),
        CONSTRAINT ck_itrm_task_priority CHECK (priority IN (N'LOW',N'MEDIUM',N'HIGH',N'URGENT'))
    );
    CREATE NONCLUSTERED INDEX ix_itrm_task_req ON dbo.itrm_task(requirement_id, sort_order);
    CREATE NONCLUSTERED INDEX ix_itrm_task_assignee ON dbo.itrm_task(assignee, status);
END

-- ============================================================
-- 7. 测试记录表 itrm_test_record
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'itrm_test_record' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.itrm_test_record (
        id              BIGINT IDENTITY(1,1) NOT NULL,
        requirement_id  BIGINT              NOT NULL,
        task_id         BIGINT              NULL,
        title           NVARCHAR(200)       NOT NULL,
        type            NVARCHAR(20)        NOT NULL DEFAULT N'TEST',
        severity        NVARCHAR(20)        NOT NULL DEFAULT N'LOW',
        status          NVARCHAR(20)        NOT NULL DEFAULT N'OPEN',
        tester          NVARCHAR(64)        NULL,
        handler         NVARCHAR(64)        NULL,
        description     NVARCHAR(MAX)       NULL,
        expected_result NVARCHAR(1000)      NULL,
        actual_result   NVARCHAR(1000)      NULL,
        accept_result   NVARCHAR(20)        NULL,
        accept_by       NVARCHAR(64)        NULL,
        accept_at       DATETIME2           NULL,
        created_at      DATETIME2           NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at      DATETIME2           NOT NULL DEFAULT CURRENT_TIMESTAMP,
        created_by      NVARCHAR(64)        NULL,
        updated_by      NVARCHAR(64)        NULL,
        version         INT                 NOT NULL DEFAULT 0,
        CONSTRAINT pk_itrm_test PRIMARY KEY CLUSTERED (id),
        CONSTRAINT fk_itrm_test_req FOREIGN KEY (requirement_id) REFERENCES dbo.itrm_requirement(id),
        CONSTRAINT ck_itrm_test_type CHECK (type IN (N'TEST',N'BUG')),
        CONSTRAINT ck_itrm_test_severity CHECK (severity IN (N'LOW',N'MEDIUM',N'HIGH',N'CRITICAL')),
        CONSTRAINT ck_itrm_test_status CHECK (status IN (N'OPEN',N'IN_PROGRESS',N'RESOLVED',N'CLOSED',N'REOPENED'))
    );
    CREATE NONCLUSTERED INDEX ix_itrm_test_req ON dbo.itrm_test_record(requirement_id, created_at DESC);
    CREATE NONCLUSTERED INDEX ix_itrm_test_tester ON dbo.itrm_test_record(tester, status);
END

-- ============================================================
-- 8. 工时记录表 itrm_time_log
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'itrm_time_log' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.itrm_time_log (
        id              BIGINT IDENTITY(1,1) NOT NULL,
        requirement_id  BIGINT              NOT NULL,
        task_id         BIGINT              NULL,
        worker          NVARCHAR(64)        NOT NULL,
        work_date       DATE                NOT NULL,
        hours           DECIMAL(5,1)        NOT NULL,
        work_type       NVARCHAR(20)        NOT NULL DEFAULT N'DEV',
        description     NVARCHAR(500)       NULL,
        created_at      DATETIME2           NOT NULL DEFAULT CURRENT_TIMESTAMP,
        created_by      NVARCHAR(64)        NULL,
        CONSTRAINT pk_itrm_time PRIMARY KEY CLUSTERED (id),
        CONSTRAINT fk_itrm_time_req FOREIGN KEY (requirement_id) REFERENCES dbo.itrm_requirement(id),
        CONSTRAINT ck_itrm_time_type CHECK (work_type IN (N'DEV',N'TEST',N'REVIEW',N'DOC',N'MEETING',N'OTHER'))
    );
    CREATE NONCLUSTERED INDEX ix_itrm_time_req ON dbo.itrm_time_log(requirement_id, work_date DESC);
    CREATE NONCLUSTERED INDEX ix_itrm_time_worker ON dbo.itrm_time_log(worker, work_date DESC);
END

-- ============================================================
-- 9. 变更记录表 itrm_change_log
--    需求变更留痕（业务部门变更需求时记录）
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'itrm_change_log' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.itrm_change_log (
        id              BIGINT IDENTITY(1,1) NOT NULL,
        requirement_id  BIGINT              NOT NULL,
        field_name      NVARCHAR(50)        NOT NULL,   -- 变更字段名
        old_value       NVARCHAR(MAX)       NULL,       -- 旧值
        new_value       NVARCHAR(MAX)       NULL,       -- 新值
        change_reason   NVARCHAR(500)       NULL,       -- 变更原因
        changed_by      NVARCHAR(64)        NOT NULL,
        created_at      DATETIME2           NOT NULL DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT pk_itrm_change PRIMARY KEY CLUSTERED (id),
        CONSTRAINT fk_itrm_change_req FOREIGN KEY (requirement_id) REFERENCES dbo.itrm_requirement(id)
    );
    CREATE NONCLUSTERED INDEX ix_itrm_change_req ON dbo.itrm_change_log(requirement_id, created_at DESC);
END

-- ============================================================
-- 10. 附件表 itrm_attachment
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'itrm_attachment' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.itrm_attachment (
        id              BIGINT IDENTITY(1,1) NOT NULL,
        ref_type        NVARCHAR(20)        NOT NULL,   -- REQUIREMENT/TASK/TEST
        ref_id          BIGINT              NOT NULL,
        file_name       NVARCHAR(255)       NOT NULL,
        file_type       NVARCHAR(50)        NULL,       -- IMAGE/DOCUMENT/OTHER
        file_size       BIGINT              NULL,
        file_path       NVARCHAR(500)       NULL,       -- 存储路径或Base64引用
        mime_type       NVARCHAR(100)       NULL,
        uploaded_by     NVARCHAR(64)        NULL,
        created_at      DATETIME2           NOT NULL DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT pk_itrm_att PRIMARY KEY CLUSTERED (id)
    );
    CREATE NONCLUSTERED INDEX ix_itrm_att_ref ON dbo.itrm_attachment(ref_type, ref_id);
END

-- ============================================================
-- 初始化数据
-- ============================================================

-- 默认管理员
IF NOT EXISTS (SELECT 1 FROM dbo.itrm_user WHERE username = N'admin')
BEGIN
    INSERT INTO dbo.itrm_user (username, password, real_name, role, department, status, created_by, updated_by)
    VALUES (N'admin', N'$2a$10$Ripm7C3qUTInzjN8NxW9luiMnYFsRMkq6/0tMHORC0VPzAtYN8tKK',
            N'系统管理员', N'ADMIN', N'信息部', N'ACTIVE', N'system', N'system');
END

-- 测试用户 - 业务部门
IF NOT EXISTS (SELECT 1 FROM dbo.itrm_user WHERE username = N'business01')
BEGIN
    INSERT INTO dbo.itrm_user (username, password, real_name, role, department, status, created_by, updated_by)
    VALUES (N'business01', N'$2a$10$Ripm7C3qUTInzjN8NxW9luiMnYFsRMkq6/0tMHORC0VPzAtYN8tKK',
            N'张三', N'BUSINESS_USER', N'生产部', N'ACTIVE', N'system', N'system');
END

-- 测试用户 - IT评审
IF NOT EXISTS (SELECT 1 FROM dbo.itrm_user WHERE username = N'reviewer01')
BEGIN
    INSERT INTO dbo.itrm_user (username, password, real_name, role, department, status, created_by, updated_by)
    VALUES (N'reviewer01', N'$2a$10$Ripm7C3qUTInzjN8NxW9luiMnYFsRMkq6/0tMHORC0VPzAtYN8tKK',
            N'李四', N'IT_REVIEWER', N'信息部', N'ACTIVE', N'system', N'system');
END

-- 测试用户 - 开发
IF NOT EXISTS (SELECT 1 FROM dbo.itrm_user WHERE username = N'dev01')
BEGIN
    INSERT INTO dbo.itrm_user (username, password, real_name, role, department, status, created_by, updated_by)
    VALUES (N'dev01', N'$2a$10$Ripm7C3qUTInzjN8NxW9luiMnYFsRMkq6/0tMHORC0VPzAtYN8tKK',
            N'王五', N'DEVELOPER', N'信息部', N'ACTIVE', N'system', N'system');
END

-- 测试用户 - 测试
IF NOT EXISTS (SELECT 1 FROM dbo.itrm_user WHERE username = N'tester01')
BEGIN
    INSERT INTO dbo.itrm_user (username, password, real_name, role, department, status, created_by, updated_by)
    VALUES (N'tester01', N'$2a$10$Ripm7C3qUTInzjN8NxW9luiMnYFsRMkq6/0tMHORC0VPzAtYN8tKK',
            N'赵六', N'TESTER', N'信息部', N'ACTIVE', N'system', N'system');
END

-- 默认审批模板 - 标准需求审批
IF NOT EXISTS (SELECT 1 FROM dbo.itrm_approval_template WHERE code = N'STANDARD_REQ')
BEGIN
    INSERT INTO dbo.itrm_approval_template (name, code, description, is_default, status, created_by, updated_by)
    VALUES (N'标准需求审批', N'STANDARD_REQ', N'标准需求审批流程：IT接收 → 需求编写 → 实施负责人审批 → 开发', 1, N'ACTIVE', N'system', N'system');
    DECLARE @tpl_id BIGINT = SCOPE_IDENTITY();
    INSERT INTO dbo.itrm_approval_node (template_id, node_name, node_type, approver_role, sort_order)
    VALUES (@tpl_id, N'IT部门接收', N'REVIEW', N'IT_REVIEWER', 1);
    INSERT INTO dbo.itrm_approval_node (template_id, node_name, node_type, approver_role, sort_order)
    VALUES (@tpl_id, N'需求编写确认', N'APPROVE', N'IT_REVIEWER', 2);
    INSERT INTO dbo.itrm_approval_node (template_id, node_name, node_type, approver_role, sort_order)
    VALUES (@tpl_id, N'实施负责人审批', N'APPROVE', N'IT_PLANNER', 3);
END

-- BUG 修复审批模板
IF NOT EXISTS (SELECT 1 FROM dbo.itrm_approval_template WHERE code = N'BUG_FIX')
BEGIN
    INSERT INTO dbo.itrm_approval_template (name, code, description, category, is_default, status, created_by, updated_by)
    VALUES (N'BUG修复流程', N'BUG_FIX', N'BUG修复简化流程：IT接收 → 开发 → 测试 → 发布', N'BUG', 0, N'ACTIVE', N'system', N'system');
    DECLARE @tpl2 BIGINT = SCOPE_IDENTITY();
    INSERT INTO dbo.itrm_approval_node (template_id, node_name, node_type, approver_role, sort_order)
    VALUES (@tpl2, N'IT部门接收', N'REVIEW', N'IT_REVIEWER', 1);
    INSERT INTO dbo.itrm_approval_node (template_id, node_name, node_type, approver_role, sort_order)
    VALUES (@tpl2, N'开发完成确认', N'APPROVE', N'DEVELOPER', 2);
END
