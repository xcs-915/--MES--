-- ESOP review workflow: DRAFT -> SUBMITTED -> APPROVED -> PUBLISHED (rejected returns to DRAFT)

ALTER TABLE eng_esop_document DROP CONSTRAINT ck_eng_esop_document_status;
ALTER TABLE eng_esop_document ADD CONSTRAINT ck_eng_esop_document_status CHECK (status IN (N'DRAFT',N'SUBMITTED',N'APPROVED',N'PUBLISHED',N'OBSOLETE'));

ALTER TABLE eng_esop_document ADD submitted_at DATETIME2 NULL;
ALTER TABLE eng_esop_document ADD reviewed_at DATETIME2 NULL;
ALTER TABLE eng_esop_document ADD approved_at DATETIME2 NULL;
ALTER TABLE eng_esop_document ADD review_comment NVARCHAR(1000) NULL;

CREATE TABLE eng_esop_review_log (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    action NVARCHAR(20) NOT NULL,
    actor NVARCHAR(64) NOT NULL,
    comment NVARCHAR(1000),
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_esop_review_doc FOREIGN KEY (document_id) REFERENCES eng_esop_document(id)
);
CREATE INDEX ix_esop_review_doc ON eng_esop_review_log(document_id, created_at);

INSERT INTO iam_permission(code,name_zh,name_en,name_ar,permission_type,group_code,sort_order)
SELECT N'ESOP_REVIEW',N'ESOP审核',N'Review ESOP',N'مراجعة تعليمات العمل',N'ACTION',N'ENGINEERING',44
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code=N'ESOP_REVIEW');

INSERT INTO iam_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM iam_role r CROSS JOIN iam_permission p
WHERE r.code=N'MES_ADMIN' AND p.code=N'ESOP_REVIEW'
AND NOT EXISTS (SELECT 1 FROM iam_role_permission rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
