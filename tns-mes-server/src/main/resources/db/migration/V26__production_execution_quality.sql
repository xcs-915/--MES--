CREATE TABLE prd_material_issue (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    issue_no NVARCHAR(64) NOT NULL,
    work_order_id BIGINT NOT NULL,
    operation_id BIGINT,
    status NVARCHAR(20) NOT NULL DEFAULT N'DRAFT',
    posting_date DATETIME2 NOT NULL,
    warehouse_code NVARCHAR(40),
    operator_code NVARCHAR(64) NOT NULL,
    external_doc_id NVARCHAR(80),
    external_message NVARCHAR(1000),
    posted_at DATETIME2,
    remark NVARCHAR(1000),
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(64), updated_by NVARCHAR(64), version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_prd_material_issue_no UNIQUE (issue_no),
    CONSTRAINT fk_prd_material_issue_order FOREIGN KEY (work_order_id) REFERENCES prd_work_order(id),
    CONSTRAINT fk_prd_material_issue_operation FOREIGN KEY (operation_id) REFERENCES prd_work_order_operation(id)
);
CREATE INDEX ix_prd_material_issue_order_status ON prd_material_issue(work_order_id, status, posting_date);

CREATE TABLE prd_material_issue_item (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    issue_id BIGINT NOT NULL,
    line_no INT NOT NULL,
    product_code NVARCHAR(64) NOT NULL,
    product_name NVARCHAR(200),
    batch_no NVARCHAR(64),
    quantity DECIMAL(18,6) NOT NULL,
    unit NVARCHAR(20) NOT NULL,
    storage_location NVARCHAR(40),
    reservation_no NVARCHAR(64),
    reservation_item NVARCHAR(20),
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(64), updated_by NVARCHAR(64), version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_prd_material_issue_item_line UNIQUE (issue_id, line_no),
    CONSTRAINT fk_prd_material_issue_item_header FOREIGN KEY (issue_id) REFERENCES prd_material_issue(id)
);
CREATE INDEX ix_prd_material_issue_item_product ON prd_material_issue_item(product_code, batch_no);

CREATE TABLE prd_work_report (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    report_no NVARCHAR(64) NOT NULL,
    work_order_id BIGINT NOT NULL,
    operation_id BIGINT,
    status NVARCHAR(20) NOT NULL DEFAULT N'DRAFT',
    qualified_quantity DECIMAL(18,6) NOT NULL,
    unqualified_quantity DECIMAL(18,6) NOT NULL,
    operator_code NVARCHAR(64) NOT NULL,
    shift_code NVARCHAR(40),
    equipment_code NVARCHAR(64),
    reported_at DATETIME2 NOT NULL,
    confirmation_group NVARCHAR(80),
    external_doc_id NVARCHAR(80),
    external_message NVARCHAR(1000),
    posted_at DATETIME2,
    remark NVARCHAR(1000),
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(64), updated_by NVARCHAR(64), version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_prd_work_report_no UNIQUE (report_no),
    CONSTRAINT fk_prd_work_report_order FOREIGN KEY (work_order_id) REFERENCES prd_work_order(id),
    CONSTRAINT fk_prd_work_report_operation FOREIGN KEY (operation_id) REFERENCES prd_work_order_operation(id),
    CONSTRAINT ck_prd_work_report_quantity CHECK (qualified_quantity > 0 AND unqualified_quantity >= 0)
);
CREATE INDEX ix_prd_work_report_order_status ON prd_work_report(work_order_id, operation_id, status, reported_at);

CREATE TABLE qua_inspection (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    inspection_no NVARCHAR(64) NOT NULL,
    inspection_type NVARCHAR(30) NOT NULL,
    work_order_id BIGINT NOT NULL,
    operation_id BIGINT,
    work_report_id BIGINT,
    status NVARCHAR(20) NOT NULL DEFAULT N'PENDING',
    sample_quantity DECIMAL(18,6) NOT NULL,
    qualified_quantity DECIMAL(18,6),
    unqualified_quantity DECIMAL(18,6),
    requested_by NVARCHAR(64) NOT NULL,
    requested_at DATETIME2 NOT NULL,
    inspector_code NVARCHAR(64),
    inspected_at DATETIME2,
    defect_code NVARCHAR(64),
    disposition NVARCHAR(40),
    remark NVARCHAR(1000),
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(64), updated_by NVARCHAR(64), version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_qua_inspection_no UNIQUE (inspection_no),
    CONSTRAINT fk_qua_inspection_order FOREIGN KEY (work_order_id) REFERENCES prd_work_order(id),
    CONSTRAINT fk_qua_inspection_operation FOREIGN KEY (operation_id) REFERENCES prd_work_order_operation(id),
    CONSTRAINT fk_qua_inspection_report FOREIGN KEY (work_report_id) REFERENCES prd_work_report(id),
    CONSTRAINT ck_qua_inspection_type CHECK (inspection_type IN (N'FIRST',N'LAST',N'PATROL',N'COMPLETION',N'GP')),
    CONSTRAINT ck_qua_inspection_sample CHECK (sample_quantity > 0)
);
CREATE INDEX ix_qua_inspection_order_type ON qua_inspection(work_order_id, operation_id, inspection_type, status, requested_at);

CREATE TABLE qua_inspection_item (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    inspection_id BIGINT NOT NULL,
    line_no INT NOT NULL,
    item_code NVARCHAR(64) NOT NULL,
    item_name NVARCHAR(200) NOT NULL,
    specification NVARCHAR(500),
    min_value DECIMAL(18,6),
    max_value DECIMAL(18,6),
    measured_value NVARCHAR(500),
    unit NVARCHAR(20),
    result NVARCHAR(20) NOT NULL,
    remark NVARCHAR(500),
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(64), updated_by NVARCHAR(64), version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_qua_inspection_item_line UNIQUE (inspection_id, line_no),
    CONSTRAINT fk_qua_inspection_item_header FOREIGN KEY (inspection_id) REFERENCES qua_inspection(id)
);

CREATE TABLE wh_goods_receipt (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    receipt_no NVARCHAR(64) NOT NULL,
    work_order_id BIGINT NOT NULL,
    work_report_id BIGINT,
    inspection_id BIGINT NOT NULL,
    quantity DECIMAL(18,6) NOT NULL,
    unit NVARCHAR(20) NOT NULL,
    batch_no NVARCHAR(64),
    warehouse_code NVARCHAR(40) NOT NULL,
    storage_location NVARCHAR(40) NOT NULL,
    movement_type NVARCHAR(20) NOT NULL DEFAULT N'1101P',
    status NVARCHAR(20) NOT NULL DEFAULT N'DRAFT',
    operator_code NVARCHAR(64) NOT NULL,
    posting_date DATETIME2 NOT NULL,
    external_doc_id NVARCHAR(80),
    external_message NVARCHAR(1000),
    posted_at DATETIME2,
    remark NVARCHAR(1000),
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(64), updated_by NVARCHAR(64), version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_wh_goods_receipt_no UNIQUE (receipt_no),
    CONSTRAINT fk_wh_goods_receipt_order FOREIGN KEY (work_order_id) REFERENCES prd_work_order(id),
    CONSTRAINT fk_wh_goods_receipt_report FOREIGN KEY (work_report_id) REFERENCES prd_work_report(id),
    CONSTRAINT fk_wh_goods_receipt_inspection FOREIGN KEY (inspection_id) REFERENCES qua_inspection(id),
    CONSTRAINT ck_wh_goods_receipt_quantity CHECK (quantity > 0)
);
CREATE INDEX ix_wh_goods_receipt_order_status ON wh_goods_receipt(work_order_id, status, posting_date);

INSERT INTO iam_permission(code,name_zh,name_en,name_ar,permission_type,group_code,sort_order)
SELECT N'EXECUTION_READ',N'生产执行查看',N'View production execution',N'عرض تنفيذ الإنتاج',N'API',N'PRODUCTION',350
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code=N'EXECUTION_READ');
INSERT INTO iam_permission(code,name_zh,name_en,name_ar,permission_type,group_code,sort_order)
SELECT N'EXECUTION_WRITE',N'生产执行录入',N'Create production transactions',N'إنشاء معاملات الإنتاج',N'API',N'PRODUCTION',360
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code=N'EXECUTION_WRITE');
INSERT INTO iam_permission(code,name_zh,name_en,name_ar,permission_type,group_code,sort_order)
SELECT N'EXECUTION_POST',N'生产过账',N'Post production transactions',N'ترحيل معاملات الإنتاج',N'ACTION',N'PRODUCTION',370
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code=N'EXECUTION_POST');
INSERT INTO iam_permission(code,name_zh,name_en,name_ar,permission_type,group_code,sort_order)
SELECT N'PAGE_EXECUTION',N'访问生产作业',N'Access production operations',N'الوصول إلى عمليات الإنتاج',N'PAGE',N'PRODUCTION',380
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code=N'PAGE_EXECUTION');

INSERT INTO iam_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM iam_role r CROSS JOIN iam_permission p
WHERE r.code=N'MES_ADMIN' AND p.code IN (N'EXECUTION_READ',N'EXECUTION_WRITE',N'EXECUTION_POST',N'PAGE_EXECUTION')
AND NOT EXISTS (SELECT 1 FROM iam_role_permission rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);

INSERT INTO sys_menu(code,name_zh,name_en,name_ar,parent_code,path,icon,permission_code,sort_order,status,created_at,updated_at,created_by,updated_by,version)
SELECT N'execution',N'生产作业',N'Production Operations',N'عمليات الإنتاج',N'production',N'/execution',N'clipboard-list',N'PAGE_EXECUTION',42,N'ACTIVE',GETDATE(),GETDATE(),N'system',N'system',0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code=N'execution');
