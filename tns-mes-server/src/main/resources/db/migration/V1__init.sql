CREATE TABLE md_master_data (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    data_type NVARCHAR(40) NOT NULL,
    code NVARCHAR(64) NOT NULL,
    name_zh NVARCHAR(200) NOT NULL,
    name_en NVARCHAR(200),
    name_ar NVARCHAR(200),
    parent_id BIGINT,
    description NVARCHAR(1000),
    status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    sort_order INT NOT NULL DEFAULT 0,
    attributes NVARCHAR(4000),
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(64),
    updated_by NVARCHAR(64),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_md_master_data_type_code UNIQUE (data_type, code),
    CONSTRAINT fk_md_master_data_parent FOREIGN KEY (parent_id) REFERENCES md_master_data(id)
);
CREATE INDEX ix_md_master_data_type_status ON md_master_data(data_type, status);
CREATE INDEX ix_md_master_data_parent ON md_master_data(parent_id);

CREATE TABLE iam_user (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    username NVARCHAR(64) NOT NULL,
    password_hash NVARCHAR(200) NOT NULL,
    display_name NVARCHAR(200) NOT NULL,
    email NVARCHAR(200),
    language_code NVARCHAR(16) NOT NULL DEFAULT 'zh-CN',
    status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(64),
    updated_by NVARCHAR(64),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_iam_user_username UNIQUE (username)
);
CREATE TABLE iam_role (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    code NVARCHAR(64) NOT NULL,
    name_zh NVARCHAR(200) NOT NULL,
    name_en NVARCHAR(200),
    name_ar NVARCHAR(200),
    status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(64),
    updated_by NVARCHAR(64),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_iam_role_code UNIQUE (code)
);
CREATE TABLE iam_permission (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    code NVARCHAR(100) NOT NULL,
    name_zh NVARCHAR(200) NOT NULL,
    name_en NVARCHAR(200),
    name_ar NVARCHAR(200),
    CONSTRAINT uk_iam_permission_code UNIQUE (code)
);
CREATE TABLE iam_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_iam_user_role_user FOREIGN KEY (user_id) REFERENCES iam_user(id),
    CONSTRAINT fk_iam_user_role_role FOREIGN KEY (role_id) REFERENCES iam_role(id)
);
CREATE TABLE iam_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_iam_role_permission_role FOREIGN KEY (role_id) REFERENCES iam_role(id),
    CONSTRAINT fk_iam_role_permission_permission FOREIGN KEY (permission_id) REFERENCES iam_permission(id)
);

CREATE TABLE eng_product (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    code NVARCHAR(64) NOT NULL,
    name_zh NVARCHAR(200) NOT NULL,
    name_en NVARCHAR(200),
    name_ar NVARCHAR(200),
    product_type NVARCHAR(40) NOT NULL DEFAULT 'FINISHED',
    unit NVARCHAR(20) NOT NULL DEFAULT 'PCS',
    specification NVARCHAR(500),
    status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    traceable BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(64),
    updated_by NVARCHAR(64),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_eng_product_code UNIQUE (code)
);
CREATE INDEX ix_eng_product_status ON eng_product(status);

CREATE TABLE eng_bom (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    code NVARCHAR(64) NOT NULL,
    version_code NVARCHAR(32) NOT NULL,
    name_zh NVARCHAR(200) NOT NULL,
    name_en NVARCHAR(200),
    name_ar NVARCHAR(200),
    status NVARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    effective_from DATE,
    effective_to DATE,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(64),
    updated_by NVARCHAR(64),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_eng_bom_product_version UNIQUE (product_id, code, version_code),
    CONSTRAINT fk_eng_bom_product FOREIGN KEY (product_id) REFERENCES eng_product(id)
);
CREATE TABLE eng_bom_item (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    bom_id BIGINT NOT NULL,
    component_product_id BIGINT NOT NULL,
    sequence_no INT NOT NULL,
    quantity DECIMAL(18,6) NOT NULL,
    scrap_rate DECIMAL(8,4) NOT NULL DEFAULT 0,
    unit NVARCHAR(20) NOT NULL DEFAULT 'PCS',
    issue_method NVARCHAR(20) NOT NULL DEFAULT 'BACKFLUSH',
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(64),
    updated_by NVARCHAR(64),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_eng_bom_item_seq UNIQUE (bom_id, sequence_no),
    CONSTRAINT fk_eng_bom_item_bom FOREIGN KEY (bom_id) REFERENCES eng_bom(id),
    CONSTRAINT fk_eng_bom_item_product FOREIGN KEY (component_product_id) REFERENCES eng_product(id)
);

CREATE TABLE eng_process_route (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    code NVARCHAR(64) NOT NULL,
    version_code NVARCHAR(32) NOT NULL,
    name_zh NVARCHAR(200) NOT NULL,
    name_en NVARCHAR(200),
    name_ar NVARCHAR(200),
    status NVARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(64),
    updated_by NVARCHAR(64),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_eng_route_product_version UNIQUE (product_id, code, version_code),
    CONSTRAINT fk_eng_route_product FOREIGN KEY (product_id) REFERENCES eng_product(id)
);
CREATE TABLE eng_process_operation (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    route_id BIGINT NOT NULL,
    sequence_no INT NOT NULL,
    code NVARCHAR(64) NOT NULL,
    name_zh NVARCHAR(200) NOT NULL,
    name_en NVARCHAR(200),
    name_ar NVARCHAR(200),
    work_center_id BIGINT,
    standard_time_seconds INT NOT NULL DEFAULT 0,
    queue_time_seconds INT NOT NULL DEFAULT 0,
    is_inspection BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(64),
    updated_by NVARCHAR(64),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_eng_operation_route_seq UNIQUE (route_id, sequence_no),
    CONSTRAINT fk_eng_operation_route FOREIGN KEY (route_id) REFERENCES eng_process_route(id),
    CONSTRAINT fk_eng_operation_work_center FOREIGN KEY (work_center_id) REFERENCES md_master_data(id)
);

CREATE TABLE eng_inspection_rule (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    code NVARCHAR(64) NOT NULL,
    name_zh NVARCHAR(200) NOT NULL,
    name_en NVARCHAR(200),
    name_ar NVARCHAR(200),
    inspection_type NVARCHAR(30) NOT NULL DEFAULT 'IN_PROCESS',
    sampling_method NVARCHAR(30) NOT NULL DEFAULT 'FULL',
    status NVARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(64),
    updated_by NVARCHAR(64),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_eng_inspection_rule_code UNIQUE (code)
);
CREATE TABLE eng_inspection_item (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    sequence_no INT NOT NULL,
    code NVARCHAR(64) NOT NULL,
    name_zh NVARCHAR(200) NOT NULL,
    name_en NVARCHAR(200),
    name_ar NVARCHAR(200),
    specification NVARCHAR(500),
    unit NVARCHAR(20),
    min_value DECIMAL(18,6),
    max_value DECIMAL(18,6),
    data_type NVARCHAR(20) NOT NULL DEFAULT 'TEXT',
    mandatory BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(64),
    updated_by NVARCHAR(64),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_eng_inspection_item_seq UNIQUE (rule_id, sequence_no),
    CONSTRAINT fk_eng_inspection_item_rule FOREIGN KEY (rule_id) REFERENCES eng_inspection_rule(id)
);

CREATE TABLE prd_work_order (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    order_no NVARCHAR(64) NOT NULL,
    product_id BIGINT NOT NULL,
    bom_id BIGINT,
    route_id BIGINT,
    factory_id BIGINT,
    workshop_id BIGINT,
    quantity DECIMAL(18,6) NOT NULL,
    completed_quantity DECIMAL(18,6) NOT NULL DEFAULT 0,
    priority INT NOT NULL DEFAULT 50,
    planned_start DATETIME2,
    planned_end DATETIME2,
    status NVARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    source NVARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    remark NVARCHAR(1000),
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(64),
    updated_by NVARCHAR(64),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_prd_work_order_no UNIQUE (order_no),
    CONSTRAINT fk_prd_work_order_product FOREIGN KEY (product_id) REFERENCES eng_product(id),
    CONSTRAINT fk_prd_work_order_bom FOREIGN KEY (bom_id) REFERENCES eng_bom(id),
    CONSTRAINT fk_prd_work_order_route FOREIGN KEY (route_id) REFERENCES eng_process_route(id),
    CONSTRAINT fk_prd_work_order_factory FOREIGN KEY (factory_id) REFERENCES md_master_data(id),
    CONSTRAINT fk_prd_work_order_workshop FOREIGN KEY (workshop_id) REFERENCES md_master_data(id)
);
CREATE INDEX ix_prd_work_order_status ON prd_work_order(status, planned_start);

CREATE TABLE prd_work_order_operation (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    work_order_id BIGINT NOT NULL,
    operation_id BIGINT,
    sequence_no INT NOT NULL,
    status NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
    planned_quantity DECIMAL(18,6) NOT NULL,
    completed_quantity DECIMAL(18,6) NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(64),
    updated_by NVARCHAR(64),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_prd_wo_operation_seq UNIQUE (work_order_id, sequence_no),
    CONSTRAINT fk_prd_wo_operation_wo FOREIGN KEY (work_order_id) REFERENCES prd_work_order(id),
    CONSTRAINT fk_prd_wo_operation_operation FOREIGN KEY (operation_id) REFERENCES eng_process_operation(id)
);

CREATE TABLE sys_audit_log (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    request_id NVARCHAR(64) NOT NULL,
    user_name NVARCHAR(64),
    action NVARCHAR(30) NOT NULL,
    resource NVARCHAR(100) NOT NULL,
    resource_id NVARCHAR(64),
    http_method NVARCHAR(10),
    request_path NVARCHAR(500),
    result NVARCHAR(20) NOT NULL,
    detail NVARCHAR(4000),
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX ix_sys_audit_log_created ON sys_audit_log(created_at);
