DECLARE @receiverPlanning TABLE(role_code NVARCHAR(40), permission_code NVARCHAR(80));
INSERT INTO @receiverPlanning VALUES
 (N'IT_REVIEWER',N'requirement:plan'),
 (N'IT_REVIEWER',N'board:view'),
 (N'IT_REVIEWER',N'task:view'),
 (N'IT_REVIEWER',N'task:create'),
 (N'IT_REVIEWER',N'task:edit'),
 (N'IT_REVIEWER',N'task:delete'),
 (N'IT_REVIEWER',N'task:assign'),
 (N'IT_REVIEWER',N'schedule:view'),
 (N'IT_REVIEWER',N'schedule:edit'),
 (N'IT_REVIEWER',N'timelog:view'),
 (N'IT_APPROVER_DEVELOPER',N'task:create'),
 (N'IT_APPROVER_DEVELOPER',N'task:assign');

INSERT INTO dbo.itrm_role_permission(role_id,permission_id)
SELECT role.id,permission.id FROM @receiverPlanning mapping
JOIN dbo.itrm_role role ON role.code=mapping.role_code
JOIN dbo.itrm_permission permission ON permission.code=mapping.permission_code
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.itrm_role_permission existing
    WHERE existing.role_id=role.id AND existing.permission_id=permission.id
);
