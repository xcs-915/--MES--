-- Older installations already contain itrm_attachment with generic ref_id/file_path columns.
-- Extend that table in place so no historical metadata is dropped.
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.itrm_attachment') AND name = 'requirement_id'
)
BEGIN
    ALTER TABLE dbo.itrm_attachment ADD requirement_id BIGINT NULL;
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.itrm_attachment') AND name = 'content_type'
)
BEGIN
    ALTER TABLE dbo.itrm_attachment ADD content_type NVARCHAR(150) NULL;
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.itrm_attachment') AND name = 'is_image'
)
BEGIN
    ALTER TABLE dbo.itrm_attachment ADD is_image BIT NOT NULL
        CONSTRAINT df_itrm_attachment_is_image DEFAULT 0;
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.itrm_attachment') AND name = 'file_data'
)
BEGIN
    ALTER TABLE dbo.itrm_attachment ADD file_data VARBINARY(MAX) NULL;
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.itrm_attachment') AND name = 'uploaded_at'
)
BEGIN
    ALTER TABLE dbo.itrm_attachment ADD uploaded_at DATETIME2 NOT NULL
        CONSTRAINT df_itrm_attachment_uploaded_at DEFAULT CURRENT_TIMESTAMP;
END;

-- Preserve legacy requirement attachment metadata when present.
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.itrm_attachment') AND name = 'ref_id')
BEGIN
    EXEC(N'UPDATE dbo.itrm_attachment
           SET requirement_id = ref_id
           WHERE requirement_id IS NULL AND (ref_type = N''REQUIREMENT'' OR ref_type IS NULL)');
    EXEC(N'ALTER TABLE dbo.itrm_attachment ALTER COLUMN ref_id BIGINT NULL');
END;

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.itrm_attachment') AND name = 'ref_type')
BEGIN
    EXEC(N'ALTER TABLE dbo.itrm_attachment ALTER COLUMN ref_type NVARCHAR(20) NULL');
END;

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.itrm_attachment') AND name = 'mime_type')
BEGIN
    EXEC(N'UPDATE dbo.itrm_attachment SET content_type = mime_type WHERE content_type IS NULL');
END;

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.itrm_attachment') AND name = 'created_at')
BEGIN
    EXEC(N'UPDATE dbo.itrm_attachment SET uploaded_at = created_at WHERE created_at IS NOT NULL');
END;

EXEC(N'UPDATE dbo.itrm_attachment
       SET is_image = CASE WHEN content_type LIKE N''image/%'' THEN 1 ELSE 0 END');

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_itrm_attachment_req')
BEGIN
    EXEC(N'ALTER TABLE dbo.itrm_attachment ADD CONSTRAINT fk_itrm_attachment_req
           FOREIGN KEY (requirement_id) REFERENCES dbo.itrm_requirement(id) ON DELETE CASCADE');
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_itrm_attachment_req' AND object_id = OBJECT_ID('dbo.itrm_attachment'))
BEGIN
    EXEC(N'CREATE NONCLUSTERED INDEX ix_itrm_attachment_req
           ON dbo.itrm_attachment(requirement_id, uploaded_at DESC)');
END;

