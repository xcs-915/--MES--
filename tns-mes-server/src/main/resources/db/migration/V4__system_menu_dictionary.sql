CREATE TABLE sys_menu (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    code NVARCHAR(100) NOT NULL, name_zh NVARCHAR(200) NOT NULL, name_en NVARCHAR(200), name_ar NVARCHAR(200),
    parent_code NVARCHAR(100), path NVARCHAR(300), icon NVARCHAR(80), permission_code NVARCHAR(100),
    sort_order INT NOT NULL DEFAULT 0, status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(64), updated_by NVARCHAR(64), version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_sys_menu_code UNIQUE(code)
);
CREATE INDEX ix_sys_menu_parent ON sys_menu(parent_code, sort_order);
CREATE TABLE sys_data_dictionary (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    dict_type NVARCHAR(80) NOT NULL, dict_code NVARCHAR(100) NOT NULL, label_zh NVARCHAR(200) NOT NULL,
    label_en NVARCHAR(200), label_ar NVARCHAR(200), dict_value NVARCHAR(200) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0, status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(64), updated_by NVARCHAR(64), version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_sys_dict_type_code UNIQUE(dict_type, dict_code)
);
CREATE INDEX ix_sys_dict_type_status ON sys_data_dictionary(dict_type, status, sort_order);
