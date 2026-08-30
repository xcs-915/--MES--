-- Add product fields from SAP to_Plant expand and product description
ALTER TABLE eng_product ADD
    procurement_type NVARCHAR(20),
    safety_stock_qty DECIMAL(18,6),
    lot_size_rounding_qty DECIMAL(18,6),
    over_delivery_tolerance DECIMAL(18,6),
    unlimited_over_delivery BIT NOT NULL CONSTRAINT df_eng_product_unl_over_deliv DEFAULT 0,
    production_storage_location NVARCHAR(20),
    default_storage_location NVARCHAR(20),
    product_description NVARCHAR(500);
