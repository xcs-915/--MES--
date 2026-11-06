-- Align requirement categories with the stable API codes used by the database.
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'ck_itrm_req_category')
BEGIN
    ALTER TABLE dbo.itrm_requirement DROP CONSTRAINT ck_itrm_req_category;
END;

ALTER TABLE dbo.itrm_requirement ADD CONSTRAINT ck_itrm_req_category CHECK (category IN (
    N'NEW_FUNC', N'FUNC_OPT', N'BUG', N'INTERFACE', N'REPORT', N'OTHER'
));

-- Requirement attachments are stored in SQL Server so they survive pod replacement.
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'itrm_attachment' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.itrm_attachment (
        id              BIGINT IDENTITY(1,1) NOT NULL,
        requirement_id  BIGINT               NOT NULL,
        file_name       NVARCHAR(255)        NOT NULL,
        content_type    NVARCHAR(150)        NULL,
        file_size       BIGINT               NOT NULL,
        is_image        BIT                  NOT NULL DEFAULT 0,
        file_data       VARBINARY(MAX)       NOT NULL,
        uploaded_by     NVARCHAR(64)         NULL,
        uploaded_at     DATETIME2            NOT NULL DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT pk_itrm_attachment PRIMARY KEY CLUSTERED (id),
        CONSTRAINT fk_itrm_attachment_req FOREIGN KEY (requirement_id)
            REFERENCES dbo.itrm_requirement(id) ON DELETE CASCADE
    );
    CREATE NONCLUSTERED INDEX ix_itrm_attachment_req
        ON dbo.itrm_attachment(requirement_id, uploaded_at DESC);
END;

-- Backfill historical time logs that were entered directly against a requirement.
-- Each affected requirement receives one traceable legacy task before task_id becomes mandatory.
INSERT INTO dbo.itrm_task (
    requirement_id, task_no, title, description, status, priority, assignee,
    estimated_hours, actual_hours, progress, sort_order,
    created_at, updated_at, created_by, updated_by, version
)
SELECT
    tl.requirement_id,
    CONCAT(N'LEGACY-', tl.requirement_id),
    N'历史工时归集',
    N'系统升级时自动归集的原无任务工时记录',
    N'DONE', N'MEDIUM', NULL,
    SUM(tl.hours), SUM(tl.hours), 100, 9999,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, N'system', N'system', 0
FROM dbo.itrm_time_log tl
WHERE (tl.task_id IS NULL OR NOT EXISTS (SELECT 1 FROM dbo.itrm_task linked WHERE linked.id = tl.task_id))
  AND NOT EXISTS (
      SELECT 1 FROM dbo.itrm_task t
      WHERE t.requirement_id = tl.requirement_id
        AND t.task_no = CONCAT(N'LEGACY-', tl.requirement_id)
  )
GROUP BY tl.requirement_id;

UPDATE tl
SET task_id = t.id
FROM dbo.itrm_time_log tl
JOIN dbo.itrm_task t
  ON t.requirement_id = tl.requirement_id
 AND t.task_no = CONCAT(N'LEGACY-', tl.requirement_id)
WHERE tl.task_id IS NULL OR NOT EXISTS (SELECT 1 FROM dbo.itrm_task linked WHERE linked.id = tl.task_id);

IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.itrm_time_log')
      AND name = 'task_id'
      AND is_nullable = 1
)
BEGIN
    ALTER TABLE dbo.itrm_time_log ALTER COLUMN task_id BIGINT NOT NULL;
END;

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_itrm_time_task')
BEGIN
    ALTER TABLE dbo.itrm_time_log ADD CONSTRAINT fk_itrm_time_task
        FOREIGN KEY (task_id) REFERENCES dbo.itrm_task(id);
END;

