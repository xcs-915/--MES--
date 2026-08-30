-- Create batch table for quality management (SAP batch sync)
CREATE TABLE qa_batch (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    batch_no NVARCHAR(64) NOT NULL,
    product_code NVARCHAR(64),
    product_name NVARCHAR(200),
    plant NVARCHAR(20),
    batch_status NVARCHAR(20),
    availability_date DATE,
    expiration_date DATE,
    shelf_life_expiration_date DATE,
    manufacture_date DATE,
    supplier_batch NVARCHAR(64),
    vendor NVARCHAR(64),
    quantity DECIMAL(18,6),
    unit NVARCHAR(20),
    restricted_use BIT NOT NULL CONSTRAINT df_qa_batch_restricted DEFAULT 0,
    inspection_lot NVARCHAR(64),
    inspection_status NVARCHAR(20),
    batch_class NVARCHAR(40),
    remark NVARCHAR(1000),
    source NVARCHAR(30) NOT NULL CONSTRAINT df_qa_batch_source DEFAULT 'SAP',
    sap_created_at DATETIME2,
    sap_changed_at DATETIME2,
    sap_last_sync_at DATETIME2,
    sap_payload NVARCHAR(MAX),
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by NVARCHAR(64),
    updated_by NVARCHAR(64),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_qa_batch_no_plant UNIQUE (batch_no, plant)
);

CREATE INDEX ix_qa_batch_product_code ON qa_batch(product_code);
CREATE INDEX ix_qa_batch_status ON qa_batch(batch_status);
CREATE INDEX ix_qa_batch_expiration ON qa_batch(expiration_date);
CREATE INDEX ix_qa_batch_sap_changed ON qa_batch(sap_changed_at);
