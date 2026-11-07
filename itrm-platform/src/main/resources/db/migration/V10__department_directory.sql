IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'itrm_department' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.itrm_department (
        id          BIGINT IDENTITY(1,1) NOT NULL,
        name        NVARCHAR(100) NOT NULL,
        full_path   NVARCHAR(200) NOT NULL,
        parent_path NVARCHAR(200) NULL,
        status      NVARCHAR(20) NOT NULL DEFAULT N'ACTIVE',
        created_at  DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at  DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        created_by  NVARCHAR(64) NULL,
        updated_by  NVARCHAR(64) NULL,
        version     BIGINT NOT NULL DEFAULT 0,
        CONSTRAINT pk_itrm_department PRIMARY KEY CLUSTERED (id),
        CONSTRAINT uk_itrm_department_path UNIQUE NONCLUSTERED (full_path),
        CONSTRAINT ck_itrm_department_status CHECK (status IN ('ACTIVE','DISABLED'))
    );
END;

INSERT INTO dbo.itrm_department (name, full_path, parent_path, status, created_by, updated_by)
SELECT DISTINCT u.department, u.department, NULL, N'ACTIVE', N'flyway', N'flyway'
FROM dbo.itrm_user u
WHERE u.department IS NOT NULL AND LTRIM(RTRIM(u.department)) <> N''
  AND NOT EXISTS (SELECT 1 FROM dbo.itrm_department d WHERE d.full_path = u.department);
