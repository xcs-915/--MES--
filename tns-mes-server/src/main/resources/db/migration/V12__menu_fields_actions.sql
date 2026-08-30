CREATE TABLE sys_menu_field (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    menu_code NVARCHAR(100) NOT NULL,
    field_code NVARCHAR(100) NOT NULL,
    field_path NVARCHAR(200) NOT NULL,
    label_zh NVARCHAR(200) NOT NULL,
    label_en NVARCHAR(200),
    label_ar NVARCHAR(200),
    field_type NVARCHAR(30) NOT NULL DEFAULT 'TEXT',
    visible_list BIT NOT NULL DEFAULT 1,
    visible_detail BIT NOT NULL DEFAULT 1,
    queryable BIT NOT NULL DEFAULT 0,
    default_visible BIT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(64), updated_by NVARCHAR(64), version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_sys_menu_field_code UNIQUE(menu_code, field_code)
);
CREATE INDEX ix_sys_menu_field_menu ON sys_menu_field(menu_code, status, sort_order);

CREATE TABLE sys_menu_action (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    menu_code NVARCHAR(100) NOT NULL,
    action_code NVARCHAR(100) NOT NULL,
    name_zh NVARCHAR(200) NOT NULL,
    name_en NVARCHAR(200),
    name_ar NVARCHAR(200),
    action_type NVARCHAR(30) NOT NULL DEFAULT 'BUTTON',
    permission_code NVARCHAR(100) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(64), updated_by NVARCHAR(64), version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_sys_menu_action_code UNIQUE(menu_code, action_code)
);
CREATE INDEX ix_sys_menu_action_menu ON sys_menu_action(menu_code, status, sort_order);
