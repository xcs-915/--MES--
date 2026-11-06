IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.itrm_test_record') AND name = 'attachments'
)
BEGIN
    ALTER TABLE dbo.itrm_test_record ADD attachments NVARCHAR(MAX) NULL;
END;

