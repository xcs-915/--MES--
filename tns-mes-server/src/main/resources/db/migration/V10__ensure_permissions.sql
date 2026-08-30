-- Ensure PAGE_BATCH permission exists and all permissions are assigned to MES_ADMIN role.
-- SQL Server compatible: uses INSERT ... SELECT ... WHERE NOT EXISTS idempotent pattern.

-- 1. Ensure PAGE_BATCH permission exists in iam_permission
INSERT INTO iam_permission (code, name_zh, name_en, name_ar, permission_type, group_code, sort_order)
SELECT N'PAGE_BATCH', N'访问批次管理', N'Access batch management', N'الوصول إلى إدارة الدفعات', N'PAGE', N'QUALITY', 165
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = N'PAGE_BATCH');

-- 2. Assign every existing permission to the MES_ADMIN role if not already linked.
--    Resolves the MES_ADMIN role id inline, then inserts any missing role-permission rows.
INSERT INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM iam_permission p
CROSS JOIN (SELECT id FROM iam_role WHERE code = N'MES_ADMIN') r
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);
