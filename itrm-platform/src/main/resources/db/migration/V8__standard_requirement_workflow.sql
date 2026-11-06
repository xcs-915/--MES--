-- Standard requirement lifecycle:
-- draft -> pending receive -> document preparation -> approval -> approved -> development -> test -> release.
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'ck_itrm_req_status')
BEGIN
    ALTER TABLE dbo.itrm_requirement DROP CONSTRAINT ck_itrm_req_status;
END;

UPDATE dbo.itrm_requirement
SET status = N'PENDING_DOCUMENT'
WHERE status = N'PENDING_PLAN';

ALTER TABLE dbo.itrm_requirement ADD CONSTRAINT ck_itrm_req_status CHECK (status IN (
    N'DRAFT', N'PENDING_RECEIVE', N'PENDING_DOCUMENT', N'PENDING_APPROVAL', N'APPROVED',
    N'IN_DEVELOPMENT', N'IN_TESTING', N'PENDING_RELEASE', N'COMPLETED', N'REJECTED'
));

MERGE dbo.itrm_workflow_status t USING (VALUES
 (N'DRAFT',N'草稿',N'#94a3b8',10,0),(N'PENDING_RECEIVE',N'待接收',N'#f59e0b',20,0),
 (N'PENDING_DOCUMENT',N'待完善需求文档',N'#8b5cf6',30,0),(N'PENDING_APPROVAL',N'待审批',N'#a855f7',40,0),
 (N'APPROVED',N'已审批待排期',N'#6366f1',50,0),(N'IN_DEVELOPMENT',N'开发中',N'#2563eb',60,0),
 (N'IN_TESTING',N'测试中',N'#06b6d4',70,0),(N'PENDING_RELEASE',N'待发布',N'#ec4899',80,0),
 (N'COMPLETED',N'已完成',N'#10b981',90,1),(N'REJECTED',N'已驳回',N'#ef4444',100,0)
) s(code,name,color,sort_order,terminal) ON t.code=s.code
WHEN MATCHED THEN UPDATE SET name=s.name,color=s.color,sort_order=s.sort_order,terminal=s.terminal,built_in=1,status=N'ACTIVE',updated_at=CURRENT_TIMESTAMP
WHEN NOT MATCHED THEN INSERT(code,name,color,sort_order,terminal,built_in,status) VALUES(s.code,s.name,s.color,s.sort_order,s.terminal,1,N'ACTIVE');

UPDATE dbo.itrm_workflow_status SET status=N'INACTIVE', updated_at=CURRENT_TIMESTAMP WHERE code=N'PENDING_PLAN';

