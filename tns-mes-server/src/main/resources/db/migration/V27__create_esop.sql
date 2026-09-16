CREATE TABLE eng_esop_document (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    document_no NVARCHAR(64) NOT NULL,
    version_code NVARCHAR(32) NOT NULL,
    title NVARCHAR(200) NOT NULL,
    product_code NVARCHAR(64),
    product_name NVARCHAR(200),
    document_type NVARCHAR(30) NOT NULL,
    status NVARCHAR(20) NOT NULL DEFAULT N'DRAFT',
    effective_date DATE,
    change_summary NVARCHAR(1000),
    prepared_by NVARCHAR(64),
    reviewed_by NVARCHAR(64),
    approved_by NVARCHAR(64),
    published_at DATETIME2,
    published_by NVARCHAR(64),
    content_json NVARCHAR(MAX) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(64),
    updated_by NVARCHAR(64),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_eng_esop_document_version UNIQUE (document_no, version_code),
    CONSTRAINT ck_eng_esop_document_status CHECK (status IN (N'DRAFT',N'PUBLISHED',N'OBSOLETE')),
    CONSTRAINT ck_eng_esop_document_type CHECK (document_type IN (N'ASSEMBLY',N'WEAVING',N'GENERAL'))
);
CREATE INDEX ix_eng_esop_product_status ON eng_esop_document(product_code, status);
CREATE INDEX ix_eng_esop_document_status ON eng_esop_document(document_no, status, updated_at);

INSERT INTO iam_permission(code,name_zh,name_en,name_ar,permission_type,group_code,sort_order)
SELECT N'ESOP_READ',N'ESOP查看',N'View ESOP',N'عرض تعليمات العمل',N'API',N'ENGINEERING',41
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code=N'ESOP_READ');
INSERT INTO iam_permission(code,name_zh,name_en,name_ar,permission_type,group_code,sort_order)
SELECT N'ESOP_WRITE',N'ESOP编制',N'Edit ESOP',N'تحرير تعليمات العمل',N'ACTION',N'ENGINEERING',42
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code=N'ESOP_WRITE');
INSERT INTO iam_permission(code,name_zh,name_en,name_ar,permission_type,group_code,sort_order)
SELECT N'ESOP_PUBLISH',N'ESOP发布',N'Publish ESOP',N'نشر تعليمات العمل',N'ACTION',N'ENGINEERING',43
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code=N'ESOP_PUBLISH');
INSERT INTO iam_permission(code,name_zh,name_en,name_ar,permission_type,group_code,sort_order)
SELECT N'PAGE_ESOP',N'访问ESOP',N'Access ESOP',N'الوصول إلى تعليمات العمل',N'PAGE',N'ENGINEERING',155
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code=N'PAGE_ESOP');

INSERT INTO iam_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM iam_role r CROSS JOIN iam_permission p
WHERE r.code=N'MES_ADMIN' AND p.code IN (N'ESOP_READ',N'ESOP_WRITE',N'ESOP_PUBLISH',N'PAGE_ESOP')
AND NOT EXISTS (SELECT 1 FROM iam_role_permission rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);

INSERT INTO sys_menu(code,name_zh,name_en,name_ar,parent_code,path,icon,permission_code,sort_order,status,created_at,updated_at,created_by,updated_by,version)
SELECT N'esops',N'电子作业指导书',N'Electronic SOP',N'تعليمات العمل الإلكترونية',N'engineering',N'/esops',N'notebook-tabs',N'PAGE_ESOP',36,N'ACTIVE',GETDATE(),GETDATE(),N'system',N'system',0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code=N'esops');
