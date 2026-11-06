IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name=N'itrm_workflow_status' AND schema_id=SCHEMA_ID(N'dbo'))
BEGIN
  CREATE TABLE dbo.itrm_workflow_status(
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    code NVARCHAR(40) NOT NULL UNIQUE,
    name NVARCHAR(80) NOT NULL,
    color NVARCHAR(20) NOT NULL DEFAULT N'#64748b',
    sort_order INT NOT NULL DEFAULT 0,
    terminal BIT NOT NULL DEFAULT 0,
    built_in BIT NOT NULL DEFAULT 0,
    status NVARCHAR(20) NOT NULL DEFAULT N'ACTIVE',
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(64) NULL,
    updated_by NVARCHAR(64) NULL,
    version BIGINT NOT NULL DEFAULT 0
  );
END
MERGE dbo.itrm_workflow_status t USING (VALUES
 (N'DRAFT',N'草稿',N'#94a3b8',10,0),(N'PENDING_RECEIVE',N'待接收',N'#f59e0b',20,0),(N'PENDING_PLAN',N'待排期',N'#8b5cf6',30,0),
 (N'IN_DEVELOPMENT',N'开发中',N'#2563eb',40,0),(N'IN_TESTING',N'测试中',N'#06b6d4',50,0),(N'PENDING_RELEASE',N'待发布',N'#ec4899',60,0),
 (N'COMPLETED',N'已完成',N'#10b981',70,1),(N'REJECTED',N'已驳回',N'#ef4444',80,1)
)s(code,name,color,sort_order,terminal) ON t.code=s.code
WHEN MATCHED THEN UPDATE SET name=s.name,color=s.color,sort_order=s.sort_order,terminal=s.terminal,built_in=1,updated_at=CURRENT_TIMESTAMP
WHEN NOT MATCHED THEN INSERT(code,name,color,sort_order,terminal,built_in,status) VALUES(s.code,s.name,s.color,s.sort_order,s.terminal,1,N'ACTIVE');
