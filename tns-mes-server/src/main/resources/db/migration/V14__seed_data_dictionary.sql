-- =====================================================================
-- 迁移脚本: V14__seed_data_dictionary.sql
-- 说明: 为 sys_data_dictionary 表初始化业务数据字典种子数据。
-- 表结构: 由 V4__system_menu_dictionary.sql 创建,列如下:
--   id, dict_type(分类), dict_code(编码), label_zh(中文名称),
--   label_en, label_ar, dict_value(值/父编码), sort_order(排序), status,
--   created_at, updated_at, created_by, updated_by, version
-- 父子关系约定 (dict_value 字段存储父级编码以实现层级关联):
--   * PLANT.dict_value            = 所属公司编码(COMPANY_CODE)
--   * WAREHOUSE.dict_value         = 所属工厂编码(PLANT); dict_code 形如 "工厂-仓库号"
--   * WORK_CENTER.dict_value       = 所属工厂编码(PLANT)
--   * MRP_CONTROLLER.dict_value    = 所属工厂编码(PLANT); dict_code 形如 "工厂-控制者号"
--   * PRODUCTION_SUPERVISOR.dict_value = 所属工厂编码(PLANT); dict_code 形如 "工厂-产线号"
--   * COMPANY_CODE/MATERIAL_TYPE/MATERIAL_GROUP/PRODUCT_GROUP/ORDER_TYPE:
--     无层级,dict_value 存放编码本身。
-- 注: TK20 工厂数字段产线(101-110 等)的业务名称按 TK10 同号段命名规则推断,
--     A/B/C 段(注塑/冲压/喷漆设备)为业务方明确给出。
-- 幂等性: 所有 INSERT 使用 SELECT...WHERE NOT EXISTS,可重复执行。
-- 数据来源: TNS-MES 业务方提供的基础数据字典。
-- =====================================================================

-- 如果数据字典表不存在则创建(正常流程下 V4 已创建,此处仅作保护)
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[sys_data_dictionary]') AND type = N'U')
BEGIN
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
END

-- 1) 公司代码 COMPANY_CODE
INSERT INTO sys_data_dictionary (dict_type, dict_code, label_zh, label_en, label_ar, dict_value, sort_order, status, created_at, updated_at, created_by, updated_by, version)
SELECT N'COMPANY_CODE', v.dict_code, v.label_zh, NULL, NULL, v.dict_value, v.sort_order, N'ACTIVE', GETDATE(), GETDATE(), N'system', N'system', 0
FROM (VALUES
    (N'TK01', N'泰康电子有限公司', N'TK01', 10),
    (N'TK02', N'温州泰康智能电子股份有限公司', N'TK02', 20),
    (N'TK03', N'上海维创思汽车电子有限公司', N'TK03', 30)
) AS v(dict_code, label_zh, dict_value, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM sys_data_dictionary d WHERE d.dict_type = N'COMPANY_CODE' AND d.dict_code = v.dict_code);

-- 2) 工厂 PLANT (dict_value=所属公司)
INSERT INTO sys_data_dictionary (dict_type, dict_code, label_zh, label_en, label_ar, dict_value, sort_order, status, created_at, updated_at, created_by, updated_by, version)
SELECT N'PLANT', v.dict_code, v.label_zh, NULL, NULL, v.dict_value, v.sort_order, N'ACTIVE', GETDATE(), GETDATE(), N'system', N'system', 0
FROM (VALUES
    (N'TK10', N'嘉兴工厂', N'TK01', 10),
    (N'TK19', N'嘉兴无价值工厂', N'TK01', 20),
    (N'TK20', N'温州工厂', N'TK02', 30),
    (N'TK21', N'温州模具工厂', N'TK02', 40),
    (N'TK29', N'温州无价值工厂', N'TK02', 50),
    (N'TK30', N'上海工厂', N'TK03', 60),
    (N'TK39', N'上海无价值工厂', N'TK03', 70)
) AS v(dict_code, label_zh, dict_value, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM sys_data_dictionary d WHERE d.dict_type = N'PLANT' AND d.dict_code = v.dict_code);

-- 3) 仓库 WAREHOUSE (dict_code=工厂-仓库号, dict_value=所属工厂)
INSERT INTO sys_data_dictionary (dict_type, dict_code, label_zh, label_en, label_ar, dict_value, sort_order, status, created_at, updated_at, created_by, updated_by, version)
SELECT N'WAREHOUSE', v.dict_code, v.label_zh, NULL, NULL, v.dict_value, v.sort_order, N'ACTIVE', GETDATE(), GETDATE(), N'system', N'system', 0
FROM (VALUES
    (N'TK10-101', N'J101-成品仓', N'TK10', 10),
    (N'TK10-102', N'J102-零部件仓', N'TK10', 20),
    (N'TK10-103', N'J103-原材料仓', N'TK10', 30),
    (N'TK10-104', N'J104-隔离仓', N'TK10', 40),
    (N'TK10-105', N'J105-包材仓', N'TK10', 50),
    (N'TK10-106', N'J106-低值易耗品', N'TK10', 60),
    (N'TK10-107', N'J107-报废仓', N'TK10', 70),
    (N'TK10-108', N'J108-电子仓', N'TK10', 80),
    (N'TK10-109', N'J109-待处理仓', N'TK10', 90),
    (N'TK10-110', N'J110-危险品仓库', N'TK10', 100),
    (N'TK10-112', N'J112-新品仓', N'TK10', 110),
    (N'TK10-115', N'J115-半成品仓', N'TK10', 120),
    (N'TK10-116', N'J116-虚拟仓', N'TK10', 130),
    (N'TK10-120', N'J120-三方仓', N'TK10', 140),
    (N'TK10-121', N'J121-客户仓', N'TK10', 150),
    (N'TK10-122', N'J122-注塑车间', N'TK10', 160),
    (N'TK10-123', N'J123-线束车间', N'TK10', 170),
    (N'TK10-124', N'J124-SMT车间', N'TK10', 180),
    (N'TK10-125', N'J125-自动焊车间', N'TK10', 190),
    (N'TK10-126', N'J126-总装车间', N'TK10', 200),
    (N'TK10-127', N'J127-线束仓', N'TK10', 210),
    (N'TK10-128', N'J128-素材仓', N'TK10', 220),
    (N'TK10-201', N'J201-波峰焊车间', N'TK10', 230),
    (N'TK10-202', N'J202-端子铆接车间', N'TK10', 240),
    (N'TK10-203', N'J203-试制车间', N'TK10', 250),
    (N'TK10-204', N'J204-加热垫车间', N'TK10', 260),
    (N'TK10-205', N'J205-预处理车间', N'TK10', 270),
    (N'TK10-206', N'J206-加热垫装配车间', N'TK10', 280),
    (N'TK20-101', N'W101-塑料原材料仓库', N'TK20', 290),
    (N'TK20-102', N'W102-金属原材料仓库', N'TK20', 300),
    (N'TK20-103', N'W103-线束仓库', N'TK20', 310),
    (N'TK20-104', N'W104-电子物料仓库', N'TK20', 320),
    (N'TK20-105', N'W105-PCB板组件仓库', N'TK20', 330),
    (N'TK20-106', N'W106-零部件仓库', N'TK20', 340),
    (N'TK20-107', N'W107-成品仓库', N'TK20', 350),
    (N'TK20-108', N'W108-SMT钢网仓库', N'TK20', 360),
    (N'TK20-109', N'W109-新品仓库', N'TK20', 370),
    (N'TK20-110', N'W110-待处理仓库', N'TK20', 380),
    (N'TK20-111', N'W111-废品仓库', N'TK20', 390),
    (N'TK21-123', N'W123-模具原材料库', N'TK21', 400),
    (N'TK21-125', N'W125-模具成品库', N'TK21', 410),
    (N'TK21-127', N'W127-模具成品呆滞库', N'TK21', 420),
    (N'TK21-139', N'W139-模具委外材料仓库', N'TK21', 430),
    (N'TK21-140', N'W140-模具车间线边仓', N'TK21', 440),
    (N'TK21-141', N'W141-模具半成品库', N'TK21', 450),
    (N'TK21-146', N'W146-外协供应商模具成品仓', N'TK21', 460),
    (N'TK21-147', N'W147-温州模具辅料仓', N'TK21', 470),
    (N'TK21-225', N'W225-温州模具试模线边仓', N'TK21', 480)
) AS v(dict_code, label_zh, dict_value, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM sys_data_dictionary d WHERE d.dict_type = N'WAREHOUSE' AND d.dict_code = v.dict_code);

-- 4) 工作中心 WORK_CENTER (dict_value=所属工厂)
INSERT INTO sys_data_dictionary (dict_type, dict_code, label_zh, label_en, label_ar, dict_value, sort_order, status, created_at, updated_at, created_by, updated_by, version)
SELECT N'WORK_CENTER', v.dict_code, v.label_zh, NULL, NULL, v.dict_value, v.sort_order, N'ACTIVE', GETDATE(), GETDATE(), N'system', N'system', 0
FROM (VALUES
    (N'TK501', N'注塑车间(嘉兴)', N'TK10', 10),
    (N'TK503', N'SMT车间(嘉兴)', N'TK10', 20),
    (N'TK504', N'波峰焊车间(嘉兴)', N'TK10', 30),
    (N'TK505', N'自动焊车间(嘉兴)', N'TK10', 40),
    (N'TK506', N'端子铆接车间(嘉兴)', N'TK10', 50),
    (N'TK507', N'线束装配车间(嘉兴)', N'TK10', 60),
    (N'TK508', N'总装车间(嘉兴)', N'TK10', 70),
    (N'TK514', N'加热垫缝纫车间(嘉兴)', N'TK10', 80),
    (N'TK515', N'加热垫装配车间(嘉兴)', N'TK10', 90),
    (N'TK516', N'冲压车间(嘉兴)', N'TK10', 100),
    (N'TK517', N'预处理车间(嘉兴)', N'TK10', 110),
    (N'TK518', N'自动线车间(嘉兴)', N'TK10', 120),
    (N'WZ501', N'冲压车间(温州)', N'TK20', 130),
    (N'WZ502', N'注塑车间(温州)', N'TK20', 140),
    (N'WZ503', N'喷漆车间(温州)', N'TK20', 150),
    (N'WZ504', N'线束组装车间(温州)', N'TK20', 160),
    (N'WZ505', N'微动开关装配车间(温州)', N'TK20', 170),
    (N'WZ506', N'总装车间(温州)', N'TK20', 180),
    (N'WZ511', N'SMT车间(温州)', N'TK20', 190),
    (N'WZ512', N'端子压接车间', N'TK20', 200),
    (N'WZ513', N'时钟弹簧车间(温州)', N'TK20', 210),
    (N'MJ001', N'数控铣车间', N'TK21', 220),
    (N'MJ002', N'电脉冲车间', N'TK21', 230),
    (N'MJ003', N'慢走丝车间', N'TK21', 240),
    (N'MJ004', N'快走丝车间', N'TK21', 250),
    (N'MJ005', N'工艺磨车间', N'TK21', 260),
    (N'MJ006', N'大水磨车间', N'TK21', 270),
    (N'MJ007', N'模具装配车间', N'TK21', 280),
    (N'MJ008', N'外协', N'TK21', 290),
    (N'MJ009', N'测量组', N'TK21', 300)
) AS v(dict_code, label_zh, dict_value, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM sys_data_dictionary d WHERE d.dict_type = N'WORK_CENTER' AND d.dict_code = v.dict_code);

-- 5) 物料类型 MATERIAL_TYPE
INSERT INTO sys_data_dictionary (dict_type, dict_code, label_zh, label_en, label_ar, dict_value, sort_order, status, created_at, updated_at, created_by, updated_by, version)
SELECT N'MATERIAL_TYPE', v.dict_code, v.label_zh, NULL, NULL, v.dict_value, v.sort_order, N'ACTIVE', GETDATE(), GETDATE(), N'system', N'system', 0
FROM (VALUES
    (N'Z10', N'生产物料', N'Z10', 10),
    (N'Z20', N'非生物料', N'Z20', 20)
) AS v(dict_code, label_zh, dict_value, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM sys_data_dictionary d WHERE d.dict_type = N'MATERIAL_TYPE' AND d.dict_code = v.dict_code);

-- 6) 物料组 MATERIAL_GROUP
INSERT INTO sys_data_dictionary (dict_type, dict_code, label_zh, label_en, label_ar, dict_value, sort_order, status, created_at, updated_at, created_by, updated_by, version)
SELECT N'MATERIAL_GROUP', v.dict_code, v.label_zh, NULL, NULL, v.dict_value, v.sort_order, N'ACTIVE', GETDATE(), GETDATE(), N'system', N'system', 0
FROM (VALUES
    (N'100', N'成品', N'100', 10), (N'101', N'半成品', N'101', 20), (N'102', N'委外半成品', N'102', 30),
    (N'103', N'原材料', N'103', 40), (N'104', N'夹具', N'104', 50), (N'105', N'模具成品', N'105', 60),
    (N'106', N'辅料', N'106', 70), (N'107', N'包装', N'107', 80), (N'108', N'外协件', N'108', 90),
    (N'109', N'模具半成品', N'109', 100), (N'110', N'模具原材料', N'110', 110), (N'111', N'模具辅料', N'111', 120),
    (N'112', N'非生类物料-备品备件', N'112', 130), (N'113', N'非生类物料-快速样件', N'113', 140),
    (N'114', N'非生类物料-办公物资', N'114', 150), (N'115', N'非生类物料-试验物资', N'115', 160),
    (N'116', N'非生类物料-计量物资', N'116', 170), (N'117', N'非生类物料-工具器具', N'117', 180),
    (N'118', N'非生类物料-安全设施', N'118', 190), (N'119', N'非生类物料-IT物资', N'119', 200),
    (N'120', N'非生类物料-其他', N'120', 210), (N'121', N'ESD', N'121', 220),
    (N'201', N'工序', N'201', 230), (N'202', N'模具费', N'202', 240), (N'203', N'样件费', N'203', 250),
    (N'204', N'试验测试费', N'204', 260), (N'205', N'软件开发费', N'205', 270), (N'206', N'租赁费', N'206', 280),
    (N'207', N'检测认证费', N'207', 290), (N'208', N'咨询费', N'208', 300), (N'209', N'机物料消耗', N'209', 310),
    (N'210', N'修理费', N'210', 320), (N'211', N'工装夹具费', N'211', 330),
    (N'301', N'房屋及建筑物', N'301', 340), (N'302', N'机器设备', N'302', 350), (N'303', N'生产用工具模具', N'303', 360),
    (N'304', N'电子设备', N'304', 370), (N'305', N'运输工具', N'305', 380), (N'306', N'工装模具(2000以下)', N'306', 390),
    (N'307', N'工装模具(2000以上)', N'307', 400), (N'308', N'在建工程', N'308', 410), (N'309', N'无形资产', N'309', 420)
) AS v(dict_code, label_zh, dict_value, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM sys_data_dictionary d WHERE d.dict_type = N'MATERIAL_GROUP' AND d.dict_code = v.dict_code);

-- 7) 产品组 PRODUCT_GROUP
INSERT INTO sys_data_dictionary (dict_type, dict_code, label_zh, label_en, label_ar, dict_value, sort_order, status, created_at, updated_at, created_by, updated_by, version)
SELECT N'PRODUCT_GROUP', v.dict_code, v.label_zh, NULL, NULL, v.dict_value, v.sort_order, N'ACTIVE', GETDATE(), GETDATE(), N'system', N'system', 0
FROM (VALUES
    (N'00', N'通用', N'00', 10)
) AS v(dict_code, label_zh, dict_value, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM sys_data_dictionary d WHERE d.dict_type = N'PRODUCT_GROUP' AND d.dict_code = v.dict_code);

-- 8) MRP控制者 MRP_CONTROLLER (dict_code=工厂-控制者号, dict_value=所属工厂)
INSERT INTO sys_data_dictionary (dict_type, dict_code, label_zh, label_en, label_ar, dict_value, sort_order, status, created_at, updated_at, created_by, updated_by, version)
SELECT N'MRP_CONTROLLER', v.dict_code, v.label_zh, NULL, NULL, v.dict_value, v.sort_order, N'ACTIVE', GETDATE(), GETDATE(), N'system', N'system', 0
FROM (VALUES
    (N'TK10-101', N'吴燕军', N'TK10', 10), (N'TK10-102', N'张利霞', N'TK10', 20), (N'TK10-103', N'罗娜', N'TK10', 30),
    (N'TK10-104', N'时黎霞', N'TK10', 40), (N'TK10-105', N'罗飞林', N'TK10', 50), (N'TK10-106', N'胡双丽', N'TK10', 60),
    (N'TK10-107', N'段卫玲', N'TK10', 70), (N'TK10-108', N'黄冠梅', N'TK10', 80), (N'TK10-109', N'徐美红', N'TK10', 90),
    (N'TK10-110', N'董红', N'TK10', 100),
    (N'TK20-201', N'张蒙蒙', N'TK20', 110), (N'TK20-202', N'贺小安', N'TK20', 120), (N'TK20-203', N'蔡敬', N'TK20', 130),
    (N'TK20-204', N'朱燕珍', N'TK20', 140), (N'TK20-205', N'樊素琼', N'TK20', 150), (N'TK20-206', N'陈秋梅', N'TK20', 160),
    (N'TK20-207', N'刘飞平', N'TK20', 170),
    (N'TK21-301', N'模具计划员', N'TK21', 180)
) AS v(dict_code, label_zh, dict_value, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM sys_data_dictionary d WHERE d.dict_type = N'MRP_CONTROLLER' AND d.dict_code = v.dict_code);

-- 9) 订单类型 ORDER_TYPE (label_zh 含订单号段)
INSERT INTO sys_data_dictionary (dict_type, dict_code, label_zh, label_en, label_ar, dict_value, sort_order, status, created_at, updated_at, created_by, updated_by, version)
SELECT N'ORDER_TYPE', v.dict_code, v.label_zh, NULL, NULL, v.dict_value, v.sort_order, N'ACTIVE', GETDATE(), GETDATE(), N'system', N'system', 0
FROM (VALUES
    (N'TK01', N'量产订单(100000000000-199999999999)', N'TK01', 10),
    (N'TK02', N'新品订单(2000000-2999999)', N'TK02', 20),
    (N'TK03', N'返工订单(3000000-3999999)', N'TK03', 30),
    (N'TK04', N'拆解订单(4000000-4999999)', N'TK04', 40),
    (N'TK05', N'改型订单(5000000-5999999)', N'TK05', 50),
    (N'TK06', N'试流订单(6000000-6999999)', N'TK06', 60)
) AS v(dict_code, label_zh, dict_value, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM sys_data_dictionary d WHERE d.dict_type = N'ORDER_TYPE' AND d.dict_code = v.dict_code);

-- 数据字典种子数据初始化完成。
