IF COL_LENGTH('dbo.itrm_user', 'workload_capacity_hours') IS NULL
    ALTER TABLE dbo.itrm_user ADD workload_capacity_hours DECIMAL(10,2) NOT NULL CONSTRAINT df_itrm_user_capacity DEFAULT 160;

ALTER TABLE dbo.itrm_requirement ALTER COLUMN source_dept NVARCHAR(200) NULL;
IF COL_LENGTH('dbo.itrm_requirement', 'receiver') IS NULL
    ALTER TABLE dbo.itrm_requirement ADD receiver NVARCHAR(64) NULL;
IF COL_LENGTH('dbo.itrm_requirement', 'expected_completion_date') IS NULL
    ALTER TABLE dbo.itrm_requirement ADD expected_completion_date DATE NULL;
IF COL_LENGTH('dbo.itrm_requirement', 'import_source') IS NULL
    ALTER TABLE dbo.itrm_requirement ADD import_source NVARCHAR(100) NULL;
IF COL_LENGTH('dbo.itrm_requirement', 'external_ref') IS NULL
    ALTER TABLE dbo.itrm_requirement ADD external_ref NVARCHAR(100) NULL;
IF COL_LENGTH('dbo.itrm_requirement', 'legacy_status') IS NULL
    ALTER TABLE dbo.itrm_requirement ADD legacy_status NVARCHAR(100) NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ux_itrm_req_import_ref')
    EXEC(N'CREATE UNIQUE NONCLUSTERED INDEX ux_itrm_req_import_ref ON dbo.itrm_requirement(import_source, external_ref)
    WHERE import_source IS NOT NULL AND external_ref IS NOT NULL');

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = N'itrm_notification' AND schema_id = SCHEMA_ID(N'dbo'))
BEGIN
    CREATE TABLE dbo.itrm_notification (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        recipient NVARCHAR(64) NOT NULL,
        requirement_id BIGINT NULL,
        event_type NVARCHAR(50) NOT NULL,
        title NVARCHAR(200) NOT NULL,
        content NVARCHAR(1000) NULL,
        read_flag BIT NOT NULL DEFAULT 0,
        wecom_status NVARCHAR(20) NULL,
        created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
    CREATE NONCLUSTERED INDEX ix_itrm_notification_recipient ON dbo.itrm_notification(recipient, read_flag, created_at DESC);
END;

MERGE dbo.itrm_role AS target
USING (VALUES
 (N'THIRD_PARTY',N'第三方协作用户',N'仅查看分配给自己的需求，并接收需求或开发任务',1),
 (N'IT_APPROVER',N'需求审批人',N'审批需求文档并查看需求',1),
 (N'IT_APPROVER_DEVELOPER',N'需求审批与开发',N'审批需求并承担开发、工时登记',1)
) AS source(code,name,description,built_in)
ON target.code=source.code
WHEN MATCHED THEN UPDATE SET name=source.name,description=source.description,built_in=source.built_in,updated_at=CURRENT_TIMESTAMP
WHEN NOT MATCHED THEN INSERT(code,name,description,status,built_in) VALUES(source.code,source.name,source.description,N'ACTIVE',source.built_in);

MERGE dbo.itrm_permission AS target
USING (VALUES
 (N'requirement:assign-receiver',N'指派需求接收人',N'需求管理',N'ASSIGN',145),
 (N'task:accept',N'接收开发任务',N'研发管理',N'ACCEPT',265)
) AS source(code,name,module,action,sort_order)
ON target.code=source.code
WHEN MATCHED THEN UPDATE SET name=source.name,module=source.module,action=source.action,sort_order=source.sort_order,updated_at=CURRENT_TIMESTAMP
WHEN NOT MATCHED THEN INSERT(code,name,module,action,sort_order) VALUES(source.code,source.name,source.module,source.action,source.sort_order);

DECLARE @newRoleMap TABLE(role_code NVARCHAR(40), permission_code NVARCHAR(80));
INSERT INTO @newRoleMap VALUES
 (N'THIRD_PARTY',N'dashboard:view'),(N'THIRD_PARTY',N'requirement:view'),(N'THIRD_PARTY',N'requirement:receive'),(N'THIRD_PARTY',N'task:view'),(N'THIRD_PARTY',N'task:accept'),
 (N'IT_APPROVER',N'dashboard:view'),(N'IT_APPROVER',N'requirement:view'),(N'IT_APPROVER',N'requirement:review'),
 (N'IT_APPROVER_DEVELOPER',N'dashboard:view'),(N'IT_APPROVER_DEVELOPER',N'requirement:view'),(N'IT_APPROVER_DEVELOPER',N'requirement:review'),(N'IT_APPROVER_DEVELOPER',N'requirement:plan'),
 (N'IT_APPROVER_DEVELOPER',N'board:view'),(N'IT_APPROVER_DEVELOPER',N'task:view'),(N'IT_APPROVER_DEVELOPER',N'task:edit'),(N'IT_APPROVER_DEVELOPER',N'task:transition'),
 (N'IT_APPROVER_DEVELOPER',N'task:accept'),(N'IT_APPROVER_DEVELOPER',N'timelog:view'),(N'IT_APPROVER_DEVELOPER',N'timelog:create'),
 (N'DEVELOPER',N'task:accept'),(N'BUSINESS_USER',N'requirement:change'),(N'IT_REVIEWER',N'requirement:assign-receiver');

INSERT INTO dbo.itrm_role_permission(role_id,permission_id)
SELECT role.id,permission.id FROM @newRoleMap mapping
JOIN dbo.itrm_role role ON role.code=mapping.role_code
JOIN dbo.itrm_permission permission ON permission.code=mapping.permission_code
WHERE NOT EXISTS (SELECT 1 FROM dbo.itrm_role_permission currentMapping WHERE currentMapping.role_id=role.id AND currentMapping.permission_id=permission.id);

INSERT INTO dbo.itrm_role_permission(role_id,permission_id)
SELECT role.id,permission.id FROM dbo.itrm_role role CROSS JOIN dbo.itrm_permission permission
WHERE role.code=N'ADMIN' AND NOT EXISTS (
    SELECT 1 FROM dbo.itrm_role_permission currentMapping WHERE currentMapping.role_id=role.id AND currentMapping.permission_id=permission.id
);

DECLARE @standardTemplateId BIGINT = (SELECT TOP 1 id FROM dbo.itrm_approval_template WHERE code=N'STANDARD_REQ');
IF @standardTemplateId IS NOT NULL
BEGIN
    DELETE FROM dbo.itrm_approval_node WHERE template_id=@standardTemplateId;
    INSERT INTO dbo.itrm_approval_node(template_id,node_name,node_type,approver_user,sort_order,created_at)
    VALUES(@standardTemplateId,N'信息部上级审批（任一人通过）',N'ANY_APPROVE',N'TK03599,TK04729',1,CURRENT_TIMESTAMP);
    UPDATE dbo.itrm_approval_template SET name=N'信息部需求审批',description=N'需求文档输出后，刘文斌或徐昌师任一人审批即可进入待开发排期',updated_at=CURRENT_TIMESTAMP WHERE id=@standardTemplateId;
END;
