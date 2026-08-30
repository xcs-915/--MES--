-- =====================================================================
-- V16: Fix eng_bom_item column lengths + menus menu visibility
-- 1) Widen eng_bom_item columns that are too short for SAP payloads.
--    requirement_time receives ISO 8601 durations like 'PT08H00M00' (11
--    chars) which truncate in NVARCHAR(10), breaking work order component
--    / operation sync ("String or binary data would be truncated").
-- 2) Clear the permission_code of the 'menus' menu so the navigation
--    filter (permissionCode == null) shows it to every authenticated user,
--    matching the other group menus (foundation, engineering, production...).
-- Idempotent: uses IF COL_LENGTH checks + guarded UPDATE.
-- SQL Server compatible.
-- =====================================================================

-- ========== 1) eng_bom_item: widen too-short NVARCHAR columns ==========
-- requirement_time: ISO 8601 duration (e.g. 'PT08H00M00' = 11 chars)
IF COL_LENGTH('eng_bom_item','requirement_time') IS NOT NULL ALTER TABLE eng_bom_item ALTER COLUMN requirement_time NVARCHAR(32);
-- manufacturing_order_operation: SAP operation can exceed 4 chars
IF COL_LENGTH('eng_bom_item','manufacturing_order_operation') IS NOT NULL ALTER TABLE eng_bom_item ALTER COLUMN manufacturing_order_operation NVARCHAR(10);
-- supply_area: SAP production supply area can exceed 10 chars
IF COL_LENGTH('eng_bom_item','supply_area') IS NOT NULL ALTER TABLE eng_bom_item ALTER COLUMN supply_area NVARCHAR(20);
-- goods_recipient_name: SAP goods recipient name can exceed 12 chars
IF COL_LENGTH('eng_bom_item','goods_recipient_name') IS NOT NULL ALTER TABLE eng_bom_item ALTER COLUMN goods_recipient_name NVARCHAR(40);

-- ========== 2) sys_menu: show 'menus' menu to all authenticated users ==========
-- The 'menus' menu was seeded (V11) with permission_code = 'PAGE_IAM'.
-- Clearing it makes the NavigationController filter show the menu to all
-- authenticated users, consistent with the group menus that use NULL.
UPDATE sys_menu
SET permission_code = NULL,
    updated_at = GETDATE()
WHERE code = N'menus'
  AND permission_code IS NOT NULL;
