-- Status dictionary types and values
-- All status-type fields managed through data dictionary

-- Status type entries
INSERT INTO sys_data_dictionary (dict_type, dict_code, label_zh, label_en, label_ar, dict_value, sort_order, status, created_at, updated_at)
SELECT 'STATUS_TYPE', 'PRODUCT_STATUS', '产品状态', 'Product Status', 'حالة المنتج', 'PRODUCT_STATUS', 1, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_data_dictionary WHERE dict_type = 'STATUS_TYPE' AND dict_code = 'PRODUCT_STATUS');

INSERT INTO sys_data_dictionary (dict_type, dict_code, label_zh, label_en, label_ar, dict_value, sort_order, status, created_at, updated_at)
SELECT 'STATUS_TYPE', 'WORK_ORDER_STATUS', '工单状态', 'Work Order Status', 'حالة أمر العمل', 'WORK_ORDER_STATUS', 2, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_data_dictionary WHERE dict_type = 'STATUS_TYPE' AND dict_code = 'WORK_ORDER_STATUS');

INSERT INTO sys_data_dictionary (dict_type, dict_code, label_zh, label_en, label_ar, dict_value, sort_order, status, created_at, updated_at)
SELECT 'STATUS_TYPE', 'BATCH_STATUS', '批次状态', 'Batch Status', 'حالة الدفعة', 'BATCH_STATUS', 3, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_data_dictionary WHERE dict_type = 'STATUS_TYPE' AND dict_code = 'BATCH_STATUS');

INSERT INTO sys_data_dictionary (dict_type, dict_code, label_zh, label_en, label_ar, dict_value, sort_order, status, created_at, updated_at)
SELECT 'STATUS_TYPE', 'SYNC_STATUS', '同步状态', 'Sync Status', 'حالة المزامنة', 'SYNC_STATUS', 4, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_data_dictionary WHERE dict_type = 'STATUS_TYPE' AND dict_code = 'SYNC_STATUS');

-- Product status values
INSERT INTO sys_data_dictionary (dict_type, dict_code, label_zh, label_en, label_ar, dict_value, sort_order, status, created_at, updated_at)
SELECT 'PRODUCT_STATUS', 'ACTIVE', '启用', 'Active', 'نشط', 'ACTIVE', 1, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_data_dictionary WHERE dict_type = 'PRODUCT_STATUS' AND dict_code = 'ACTIVE');

INSERT INTO sys_data_dictionary (dict_type, dict_code, label_zh, label_en, label_ar, dict_value, sort_order, status, created_at, updated_at)
SELECT 'PRODUCT_STATUS', 'INACTIVE', '停用', 'Inactive', 'غير نشط', 'INACTIVE', 2, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_data_dictionary WHERE dict_type = 'PRODUCT_STATUS' AND dict_code = 'INACTIVE');

-- Work order status values
INSERT INTO sys_data_dictionary (dict_type, dict_code, label_zh, label_en, label_ar, dict_value, sort_order, status, created_at, updated_at)
SELECT 'WORK_ORDER_STATUS', 'DRAFT', '草稿', 'Draft', 'مسودة', 'DRAFT', 1, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_data_dictionary WHERE dict_type = 'WORK_ORDER_STATUS' AND dict_code = 'DRAFT');

INSERT INTO sys_data_dictionary (dict_type, dict_code, label_zh, label_en, label_ar, dict_value, sort_order, status, created_at, updated_at)
SELECT 'WORK_ORDER_STATUS', 'RELEASED', '已下达', 'Released', 'تم الإصدار', 'RELEASED', 2, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_data_dictionary WHERE dict_type = 'WORK_ORDER_STATUS' AND dict_code = 'RELEASED');

INSERT INTO sys_data_dictionary (dict_type, dict_code, label_zh, label_en, label_ar, dict_value, sort_order, status, created_at, updated_at)
SELECT 'WORK_ORDER_STATUS', 'IN_PROGRESS', '进行中', 'In Progress', 'قيد التنفيذ', 'IN_PROGRESS', 3, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_data_dictionary WHERE dict_type = 'WORK_ORDER_STATUS' AND dict_code = 'IN_PROGRESS');

INSERT INTO sys_data_dictionary (dict_type, dict_code, label_zh, label_en, label_ar, dict_value, sort_order, status, created_at, updated_at)
SELECT 'WORK_ORDER_STATUS', 'COMPLETED', '已完成', 'Completed', 'مكتمل', 'COMPLETED', 4, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_data_dictionary WHERE dict_type = 'WORK_ORDER_STATUS' AND dict_code = 'COMPLETED');

INSERT INTO sys_data_dictionary (dict_type, dict_code, label_zh, label_en, label_ar, dict_value, sort_order, status, created_at, updated_at)
SELECT 'WORK_ORDER_STATUS', 'CANCELLED', '已取消', 'Cancelled', 'ملغى', 'CANCELLED', 5, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_data_dictionary WHERE dict_type = 'WORK_ORDER_STATUS' AND dict_code = 'CANCELLED');

-- Batch status values
INSERT INTO sys_data_dictionary (dict_type, dict_code, label_zh, label_en, label_ar, dict_value, sort_order, status, created_at, updated_at)
SELECT 'BATCH_STATUS', 'RELEASED', '已放行', 'Released', 'تم الإصدار', 'RELEASED', 1, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_data_dictionary WHERE dict_type = 'BATCH_STATUS' AND dict_code = 'RELEASED');

INSERT INTO sys_data_dictionary (dict_type, dict_code, label_zh, label_en, label_ar, dict_value, sort_order, status, created_at, updated_at)
SELECT 'BATCH_STATUS', 'RESTRICTED', '已限制', 'Restricted', 'مقيد', 'RESTRICTED', 2, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_data_dictionary WHERE dict_type = 'BATCH_STATUS' AND dict_code = 'RESTRICTED');

INSERT INTO sys_data_dictionary (dict_type, dict_code, label_zh, label_en, label_ar, dict_value, sort_order, status, created_at, updated_at)
SELECT 'BATCH_STATUS', 'UNREST', '未限制', 'Unrestricted', 'غير مقيد', 'UNREST', 3, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_data_dictionary WHERE dict_type = 'BATCH_STATUS' AND dict_code = 'UNREST');

-- Sync status values
INSERT INTO sys_data_dictionary (dict_type, dict_code, label_zh, label_en, label_ar, dict_value, sort_order, status, created_at, updated_at)
SELECT 'SYNC_STATUS', 'SUCCESS', '成功', 'Success', 'نجاح', 'SUCCESS', 1, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_data_dictionary WHERE dict_type = 'SYNC_STATUS' AND dict_code = 'SUCCESS');

INSERT INTO sys_data_dictionary (dict_type, dict_code, label_zh, label_en, label_ar, dict_value, sort_order, status, created_at, updated_at)
SELECT 'SYNC_STATUS', 'FAILED', '失败', 'Failed', 'فشل', 'FAILED', 2, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_data_dictionary WHERE dict_type = 'SYNC_STATUS' AND dict_code = 'FAILED');

INSERT INTO sys_data_dictionary (dict_type, dict_code, label_zh, label_en, label_ar, dict_value, sort_order, status, created_at, updated_at)
SELECT 'SYNC_STATUS', 'PARTIAL', '部分成功', 'Partial', 'جزئي', 'PARTIAL', 3, 'ACTIVE', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM sys_data_dictionary WHERE dict_type = 'SYNC_STATUS' AND dict_code = 'PARTIAL');
