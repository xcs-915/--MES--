# SAP 集成接口开发报告（评审稿）

> 项目：TNS-MES（新MES） · 突尼斯
> 日期：2026-09-12
> 状态：**待评审 —— 确认后再开发**
> 信息来源：老 MES（OrBit MOMpro，10.80.10.50/51）数据库 `orbit_mompro` 实测 + 新中台连通性实测

---

## 一、背景与目标

老 MES（嘉兴 OrBit MOMpro）与 SAP 的集成采用**双通道**架构，本次目标是将老 MES 的第三方接口能力完整迁移到新 MES：

| 通道 | 老MES | 新MES现状 |
|---|---|---|
| A. SAP S/4HANA Cloud 直连（OData/Basic） | my200725 / MES_P | ✅ 已开发：产品、工单、批次（已切 my200683/MES_T 并部署 v31） |
| B. SAP 集成中台（REST/Token） | `http://121.41.173.27:9999`（阿里云） | ❌ 未开发 |

**本次需求：**
1. 按老 MES 逻辑，开发**通道 B 全部接口**：领料、报工、入库、退料（+ 库存/设备/供应商查询）；
2. 中台地址变更：`http://121.41.173.27:9999` → **`http://10.30.10.42:9999`**，Token 账号 **MES-JX**；
3. 通道 A 补充老 MES 已有但新 MES 缺失的接口：工单工序、客户、供应商；
4. 全部接口登记到**接口管理**模块（`sys_interface_def`）；
5. 对已开发的产品/工单/批次同步按老 MES 逻辑做**优化对齐**。

---

## 二、中台连通性验证（已实测 ✅）

2026-09-12 从办公网实测新中台：

| 项目 | 结果 |
|---|---|
| TCP `10.30.10.42:9999` | ✅ 连通 |
| `POST /sys/loginGetToken`（MES-JX / 123456） | ✅ HTTP 200，返回 JWT |
| `GET /warehouse/deliveryHeader/list`（带 x-access-token） | ✅ HTTP 200，MyBatis-Plus 分页数据 |

**Token 响应实测：**
```json
{"success":true,"message":"登录成功","code":200,
 "result":{"token":"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ0ZW1wSUYiOnRydWUsInRlbmFudElkIjoiIiwiZXhwIjoxNzg5MTkyNzczLCJ1c2VybmFtZSI6Ik1FUy1KWCJ9.OqrzQ18RMUnr1wwYbw6ZxSVtXpsO8_HqGSYNQ1rRRoc"},
 "timestamp":1789174773255}
```

- JWT 内含 `"username":"MES-JX"`，有效期约 **5 小时**（老 MES 缓存 3 小时，沿用此策略）；
- 统一响应信封：`{success, message, code, result, timestamp}`，成功判定 **`code == 200 && success == true`**，业务数据在 `result`；
- 查询类接口分页格式：`result: {records[], total, size, current, pages}`。

---

## 三、目标配置（新 MES）

```yaml
# application.yml 新增
mes:
  middleware:                      # SAP 集成中台（通道B）
    enabled: true
    base-url: ${MES_MIDDLEWARE_BASE_URL:http://10.30.10.42:9999}
    username: ${MES_MIDDLEWARE_USERNAME:MES-JX}
    password: ${MES_MIDDLEWARE_PASSWORD:}          # 走 K8s Secret
    token-ttl-minutes: 180         # token 缓存 3 小时（实际5h，提前1/3刷新）
    connect-timeout-ms: 5000
    read-timeout-ms: 30000
```

K8s Secret（`tns-mes-secrets`）新增：`middleware-password`、`middleware-username`。

通道 A（SAP 直连）已是现状：`my200683.s4hana.sapcloud.cn` / `MES_T`，无需变更。

---

## 四、通道 A：SAP S/4HANA Cloud 直连接口（OData）

> 认证：Basic（MES_T）；新 MES 已具备 `ExternalApiClient` + Basic 认证 + Flyway 分页（`$skip/$top/$inlinecount`）能力。

| # | 接口 | 方法 | 路径 | 用途 | 新MES状态 |
|---|---|---|---|---|---|
| A1 | 产品主数据 | GET | `/sap/opu/odata/sap/API_PRODUCT_SRV/A_Product` | 物料同步（增量：LastChangeDateTime） | ✅ 已开发 |
| A2 | 生产工单 | GET | `/sap/opu/odata/sap/API_PRODUCTION_ORDER_2_SRV/A_ProductionOrder_2` | 工单同步（增量） | ✅ 已开发 |
| A3 | 批次 | GET | `/sap/opu/odata/sap/API_BATCH_SRV/Batch` | 批次同步 | ✅ 已开发 |
| A4 | **工单工序** | GET | `/sap/opu/odata/sap/YY1_C_POOPERATIONS_CDS/YY1_C_POOperations` | 工序级数据（老MES调用量最大：507万次，报工核心数据源） | ⏳ 待开发（已登记 #4） |
| A5 | **客户主数据** | GET | `/sap/opu/odata/sap/API_BUSINESS_PARTNER/A_BusinessPartner` | 客户同步（每日增量） | ⏳ 待开发 |
| A6 | **供应商主数据** | GET | `/sap/opu/odata/sap/API_BUSINESS_PARTNER/A_BusinessPartner` | 供应商同步（每日增量） | ⏳ 待开发 |
| A7 | 工单标准值 | GET | `/sap/opu/odata/sap/YY1_C_MFGORDERSTDVH_CDS/YY1_C_MfgOrderStdVH` | 工单标准工时/成本（自定义CDS） | ⏸ 二期（可选项） |

**老 MES 实测报文（A2 工单增量查询）：**
```json
{"$top":200,"$skip":0,"sap-language":"zh","saml2":"disabled","$inlinecount":"allpages",
 "$filter":"((MfgOrderCreationDate ge datetime'2026-09-10T23:39:53' and MfgOrderCreationDate lt datetime'2026-09-13T08:50:00') or (LastChangeDateTime ge '20260910233953' and LastChangeDateTime lt '20260913085000')) and Plant eq 'TK10'",
 "$expand":"to_ProductionOrderOperation,to_ProductionOrderComponent,to_ProductionOrderItem"}
```

**老 MES 实测报文（A4 工序按工单查询）：**
```
GET .../YY1_C_POOperations?sap-language=zh&$filter=ManufacturingOrder eq '2029373'
```

---

## 五、通道 B：SAP 集成中台接口（27个，按业务分组）

> Base URL：`http://10.30.10.42:9999`；鉴权：请求头 `x-access-token: <JWT>`；Token 经 `POST /sys/loginGetToken` 获取。
> 优先级：**P0** = 用户明确要求的四大业务（领料/报工/入库/退料）；**P1** = 支撑查询；**P2** = 可选。

### B0. 认证（P0）

| api_name | 方法 | 路径 | 说明 |
|---|---|---|---|
| MES_GetSAPToken | POST | `/sys/loginGetToken` | body `{"username":"MES-JX","password":"..."}`，返回 `result.token` |

### B1. 领料（发料出库，MES ← 中台 拉取 + 推送过账，P0）

| api_name | 方法 | 路径 | 说明 |
|---|---|---|---|
| MES_GetPickingList_ViewList | GET | `/warehouse/deliveryHeader/list` | 领料单列表（分页增量） |
| MES_GetPickingListItem_ViewList | GET | `/warehouse/deliveryHeader/queryDeliveryItemByMainId` | 领料单行明细 |
| MES_GetPickingListItemlot_ViewList | GET | `/warehouse/deliveryHeader/queryDeliveryItemDetail` | 领料单行批次明细 |
| MES_GetPickingListByReceive_ViewList | GET | `/warehouse/receiveHeader/list` | 领料单（收货头视角）列表 |
| MES_GetPickingListItemByReceive_ViewList | GET | `/warehouse/receiveHeader/queryReceiveItemByMainId` | 行明细（收货头视角） |
| MES_GetPickingListItemlotByReceive_ViewList | GET | `/warehouse/receiveHeader/queryReceiveItemDetail` | 行批次明细（收货头视角） |
| MES_GetPickinglist_All_ViewList | GET | `/paymentInOrder/productInAndOutOfStoragePage/queryData-MES` | 领料单全量（按时间窗+移动类型） |
| **MES_PostPickingList_ViewList** | POST | `/warehouse/deliveryCommandHeader/add` | **领料过账**（发货指令，驱动 SAP 261 发料） |
| MES_PostMOFeeding_ViewList | POST | `/warehouse/deliveryHeader/approvalAndSave` | 工单上料审核+保存（发料确认） |

### B2. 报工（工时与物料消耗，MES → 中台 推送，P0）

| api_name | 方法 | 路径 | 说明 |
|---|---|---|---|
| MES_GetFeedingList_ViewList | GET | `/workCheckOrderService/workCheckOrder/list` | 报工单列表（增量：cancelDate_begin） |
| **MES_PostFeedback_ViewList** | POST | `/consumeMaterialOrderService/consumeMaterialOrderHeader/saveConsumeMaterialData` | **报工推送**（物料消耗，带 SAP 预留号 reservation） |

### B3. 入库（完工入库，MES → 中台 推送，P0）

| api_name | 方法 | 路径 | 说明 |
|---|---|---|---|
| MES_GetPickingListByReceive…（同 B1 收货头系列） | GET | `/warehouse/receiveHeader/*` | 入库单/完工单拉取 |
| **MES_PostFinishEntity_ViewList** | POST | `/warehouse/receiveCommand/add` | **完工入库推送**（移动类型 1101P 等） |
| MES_PostFinishEntityOter_ViewLsit | POST | `/warehouse/receiveCommand/add` | 完工入库（其他入库） |
| MES_receiveCommand_ViewList | PUT | `/warehouse/receiveCommand/close` | 入库指令关闭 |

### B4. 退料（MES → 中台 推送，P0）

| api_name | 方法 | 路径 | 说明 |
|---|---|---|---|
| **MES_PostProductBack_ViewList** | POST | `/warehouse/receiveCommand/add` | **线边仓退料**（移动类型 1301XR 等） |
| MES_PostProductBack_WaerHost_ViewList | POST | `/returnMaterialOrderService/returnMaterialOrderHeader/saveReturnMaterialData` | 退料到仓库（加工费退料） |
| MES_receive_prodCancel_ViewList | POST | `/warehouse/receiveHeader/prodCancelToSap` | 退料取消推送 SAP |

### B5. 库存 / 批次 / 供应商查询（P1）

| api_name | 方法 | 路径 | 说明 |
|---|---|---|---|
| MES_GetWarehouseList_ViewList | GET | `/warehouse/inventoryItem/list` | 仓库库存列表 |
| MES_GetWarehouseInfo_ViewList | GET | `/warehouse/inventoryItemDetail/queryInventoryBatchPage` | 库存批次明细 |
| MES_GetZT_KC | GET | `/warehouse/inventoryItemDetail/queryInventoryBatchPageMes` | 在途库存（MES 视角） |
| MES_GetBatchList_viewList | GET | `/productionOrderService/productionOrder/getBatchByOrderId` | 按工单取批次 |
| MES_GetVendorProductBatch_ViewList | POST | `/supplier/supplier/queryDeliveryItems` | 供应商交货批次 |

### B6. 设备（P2，可选）

| api_name | 方法 | 路径 | 说明 |
|---|---|---|---|
| MES_GetAssetList_ViewList | GET | `/productionOrderService/productionModuleEquipment/list` | 工单设备清单 |
| MES_GetProductAsset_ViewList | GET | `/productionOrderService/productionModuleEquipmentUsage/list` | 工单设备使用 |
| MES_GetAssetDetails_ViewList | GET | `/productionOrderService/productionModuleMaintain/list` | 设备保养明细 |
| RunTask_get_reelid_DoMethod | POST | `/warehouse/sfcdatShelfLog/list` | 智能货架日志（料盘） |

> 老 MES 还有 `/ecsb/gw/soa/rf`（SOAP ESB，ZFM_* RFC，对接 U8/老ERP）共约 40 条配置——**建议新 MES 不迁移**（属旧通道）。

---

## 六、关键报文样本（老 MES 生产实测，780 万条调用日志提炼）

### 6.1 领料单拉取 `MES_GetPickinglist_All_ViewList`（每 8 分钟窗口增量）
```json
{"movementTypeIdList":"1312X,1312Z,1301Z,1301X,1312Y,1Z15,1Z16,1261X,101,1311Z,1301ZR,1305X,1301XR,1303X,1311Y,1311X,131101R,131301R,201,1262X,131101,131301,1653,1305,1303",
 "orderType":2,
 "startTime":"2026-07-16 19:37:02","endTime":"2026-07-16 19:45:00",
 "productStoreId":"TK10"}
```
响应 `result[]` 关键字段：`docId, baseEntry, orderNo, movementTypeId, inFacilityId, outFacilityId, inProductStoreId, outProductStoreId, createBy, productInAndOutOfStorageToMesItemVOList[{lineNo, baseLineNo, productId, productName, productDetailId, quantity}]`

### 6.2 领料过账 `MES_PostPickingList_ViewList`
```json
{"receiver":"100000131369;泰康电子(嘉兴);TK506",
 "postingDate":"2026-07-03 22:14:13",
 "baseEntry":"JX1F126070300100",
 "productStoreId":"TK10","productStoreIdTo":"TK10",
 "facilityId":"J127","facilityIdTo":"J202",
 "docStatus":"2","businessStatus":"1",
 "movementTypeId":"1311Y",
 "createBy":"TK00472","orderNo":"100000131369",
 "isPick":"","pickBy":null,
 "deliveryCommandItemList":[
   {"lineNo":1,"unit":"PCS","reservation":null,"reservationItem":null,
    "baseEntry":"JX1F126070300100","baseLineNo":"",
    "productId":"30105453AA","productStoreId":"TK10","productStoreIdTo":"TK10",
    "facilityId":"J127","facilityIdTo":"J202","productDetailId":"","quantity":3}
 ]}
```
成功响应：`{"success":true,"message":"提交成功！","code":200,"result":{"docId":"DC2026070300871"},"timestamp":...}`

### 6.3 报工（上料确认）`MES_PostMOFeeding_ViewList`
> 注意：此接口调用时需带 `request_header: {"x-access-token": "<JWT>"}`（老 MES 报文证实），其余写接口同。
```json
{"app_type":"ERP","api_name":"MES_PostMOFeeding_ViewList",
 "request_body":{
   "productionOrderId":"100000129690","productId":"30106018AA","productName":"YF01710S01...",
   "processId":"0010","processName":"贴片",
   "qualifiedQuantity":4000,"unqualifiedQuantity":0,
   "productStoreId":"TK10","unit":"PCS",
   "workCheckOrderTime":"2026-07-04 03:37:37",
   "statisticsHour":2,"statisticalMachine":2,"staffHour":2,"equipmentHour":2,"materialCost":2,
   "mouldCode":"","mouldQuantity":"","orderType":"TK01","personnel":"TK26052613","equipment":"",
   "production_feedback_id":"8679605295080669184","operationConfirmation":"0000582000",
   "defectiveProductQuantity":0,"baseEntry":"PFB2026070400009"},
 "request_header":{"x-access-token":"<JWT>"}}
```
成功响应：`{"success":true,"code":200,"result":{"confirmationCount":"1","id":"BG2026070400009","workCheckOrderld":"BG2026070400009","confirmationGroup":"0000582000"}}` —— `confirmationGroup` 即 SAP 报工凭证号。

### 6.4 报工（物料消耗）`MES_PostFeedback_ViewList`
```json
{"consumeMaterialOrders":[
   {"no":"001","productId":"5020000287AA","productName":"...FM33FG045ALQFP48",
    "currentPatchQuantity":"832.000000","unit":"PCS",
    "facilityId":"J124","batchId":"2605240314",
    "reservation":"1528879","reservationItem":"10"}],
 "productOrder":"100000131338","orderType":"TK01","plant":"TK10",
 "postingDate":"2026-07-04 03:44:00",
 "isAccessoryBindingButton":"N",
 "feeding_id":8679606022221987840,"channel":"MES",
 "baseEntry":"JXF2026070400014"}
```
成功响应：`{"success":true,"docId":"D2026070400064"}`

### 6.5 完工入库 `MES_PostFinishEntity_ViewList`
```json
{"docStatus":"2","businessStatus":"1",
 "movementTypeId":"1101P","productStoreId":"TK10",
 "orderNo":"100000129646","baseEntry":"1J126070400017",
 "facilityId":"J127","postingDate":"2026-07-04 03:33:54",
 "sender":"100000129646;泰康茅箭充电枪(组装);TK506",
 "receiveCommandItemList":[
   {"baseLineNo":"","baseEntry":"1J126070400017","lineNo":1,
    "productId":"30105181BA","productName":"TQ04YB10601黄色面壳组件NA",
    "unit":"PCS","productStoreId":"TK10","facilityId":"J127",
    "quantity":2000,"productDetailId":"2607040012",
    "receiveCommandItemDetailList":[]}]}
```
成功响应：`{"success":true,"code":200,"result":{"docId":"RC2026070400017"}}`

### 6.6 线边仓退料 `MES_PostProductBack_ViewList`
```json
{"sender":"","postingDate":"2026-07-03 18:13:06",
 "baseEntry":"JXBACK2026070300013",
 "productStoreId":"TK10","productStoreIdFrom":"TK10",
 "facilityId":"J108","facilityIdFrom":"J124",
 "docStatus":"2","businessStatus":"1",
 "movementTypeId":"1301XR","createBy":"TK04791","pickId":"",
 "attrName9":"S","attrName10":"MES","orderNo":null,
 "receiveCommandItemList":[
   {"lineNo":1,"unit":"PCS","attrName8":"","attrName9":"",
    "baseEntry":"JXBACK2026070300013","baseLineNo":null,
    "productId":"30200248AA","productStoreId":"TK10","productStoreIdFrom":"TK10",
    "facilityId":"J108","facilityIdFrom":"J124",
    "productDetailId":"2604220579","quantity":962,
    "receiveCommandItemDetailList":[]}],
 "consumeOrderNo":"JXJXBACK2026070300013"}
```
成功响应：`{"success":true,"code":200,"result":{"docId":"RC2026070301429"}}`

### 6.7 报工单增量拉取 `MES_GetFeedingList_ViewList`
```json
{"pageNo":1,"pageSize":10,"column":"createTime","order":"desc",
 "workCenterCode":"TK501,TK516","cancelDate_begin":"2026-07-16 18:55:00"}
```

---

## 七、新 MES 现状与差距分析

| 能力 | 新MES现状 | 差距 |
|---|---|---|
| HTTP 客户端 | `ExternalApiClient`（RestTemplate，Basic/Bearer，超时/SAML检测/日志完善） | 需扩展支持 **x-access-token 头 + Token 自动管理** |
| Token 管理 | 无 | 需新增 `MiddlewareTokenManager`（3h 缓存 + 过期重取） |
| 同步业务 | 产品/工单/批次（拉取方向） | 缺：工序/客户/供应商；缺全部中台业务 |
| 接口登记 | `sys_interface_def` 11 条（其中 MIDDLEWARE/WMS/MES_LOCAL 7 条为**占位假路径**） | 需替换为真实中台接口 + 新增 SAP 直连 3 条 |
| 调用日志 | `int_api_call_log`（已有） | ✅ 复用，按 `interface_code` 记录请求/响应/状态 |
| 失败重试 | `integration/outbox`（已有 Outbox 模式） | ✅ 写操作（报工/入库/退料/领料过账）走 Outbox 重试 |
| 定时调度 | `SapSyncScheduler`（固定延迟） | 新增中台同步 Job（见 §九） |
| 敏感配置 | K8s Secret 注入 | 新增 middleware 凭据键 |

---

## 八、开发方案设计

### 8.1 新增代码结构（沿用现有分层）

```
com.tns.mes.integration.middleware
├── MiddlewareProperties            // base-url/账号/超时/token-ttl
├── MiddlewareTokenManager          // token 缓存(3h)+自动重取+401 重试一次
├── MiddlewareApiClient             // 统一加 x-access-token；复用 RestTemplate；写 int_api_call_log
├── domain/
│   ├── PickingDocument(.Item)      // 领料单/行/批次
│   ├── WorkReport                  // 报工单（含 confirmationGroup）
│   ├── GoodsReceipt(.Item)         // 完工入库单
│   └── MaterialReturn(.Item)       // 退料单
├── repository/…                    // JPA Repository
├── service/
│   ├── PickingSyncService          // B1: 拉取领料单 + 领料过账推送
│   ├── WorkReportPushService       // B2: 报工推送（消耗+工时）
│   ├── GoodsReceiptPushService     // B3: 完工入库推送/关闭
│   ├── MaterialReturnPushService   // B4: 退料推送/退料取消
│   └── StockQueryService           // B5: 库存/批次/在途查询
└── MiddlewareSyncScheduler         // 定时任务编排
```

### 8.2 同步方向与触发方式（对齐老 MES 日志频率）

| 业务 | 方向 | 触发 | 频率（老MES实测） |
|---|---|---|---|
| 领料单拉取 | 中台→MES | 定时增量 | 每 8 分钟时间窗（startTime/endTime） |
| 领料过账 | MES→中台 | 事件（PDA/页面确认）+ Outbox | 实时 |
| 上料确认 | MES→中台 | 事件（上料扫码） | 实时 |
| 报工单拉取 | 中台→MES | 定时增量 | cancelDate_begin 增量，~10 分钟 |
| 报工推送 | MES→中台 | 事件（报工确认）+ Outbox | 实时 |
| 完工入库 | MES→中台 | 事件（完工提交）+ Outbox | 实时 |
| 退料 | MES→中台 | 事件（退料确认）+ Outbox | 实时 |
| 库存/批次查询 | 中台→MES | 按需 + 定时 | 15 分钟 |
| 工序（A4） | SAP→MES | 定时增量 | ~10 分钟（按 ManufacturingOrder 过滤） |
| 客户/供应商（A5/A6） | SAP→MES | 定时增量 | 每日 04:00 |

### 8.3 数据库迁移（V25）

`V25__middleware_interfaces.sql`：
1. `sys_interface_def`：将 #5–#11 占位记录（`/api/loading/check` 等假路径）**停用（status=INACTIVE）**，新增中台真实接口记录（system_code=`MIDDLEWARE`，path 存完整 URL 路径，request_template 存报文模板，response_mapping 存 `code==200&&success==true` 提取规则）；
2. 新增 SAP 直连接口记录：工序（A4）、客户（A5）、供应商（A6）；
3. 新增 `sys_interface_category`：`INTEGRATION_PLATFORM`（SAP集成中台）；
4. 业务表：`mes_picking_doc/item`、`mes_work_report`、`mes_goods_receipt/item`、`mes_material_return/item`（含 `confirmation_group`、`sap_doc_id`、`sync_state`、`retry_count` 字段）。

### 8.4 成功判定与幂等（对齐老 MES）

- 成功：HTTP 2xx 且 `code == 200 && success == true`（Token 接口与全部中台接口实测一致）；
- 幂等：以 `baseEntry + lineNo + movementTypeId` 去重；推送记录保存中台返回的 `docId`/`confirmationGroup`；
- 失败：写操作进 Outbox，指数退避重试（1m/5m/30m，最多 5 次），接口管理页面可见失败明细。

---

## 九、已有接口（产品/工单/批次）优化建议

| # | 现状问题（对比老MES） | 优化项 |
|---|---|---|
| 1 | 工单查询仅按 `LastChangeDateTime` 增量 | 增加 **`MfgOrderCreationDate or LastChangeDateTime` 双条件 + `Plant eq 'TK10'`**（老MES实测报文），避免漏新建工单 |
| 2 | 工单 `$expand` 未含子实体 | 补 `to_ProductionOrderOperation,to_ProductionOrderComponent,to_ProductionOrderItem`（工序/组件/行项目，报工与领料都依赖） |
| 3 | 产品 `$expand` 较简（Description/BasicText/Plant/UnitsOfMeasure/SalesDelivery/Valuation） | 老MES还拉 `to_Plant/to_ProductSupplyPlanning、QualityMgmt、Costing、Procurement、WorkScheduling、StorageLocation、PurchaseText` —— 按 MES 页面需要取舍（建议至少补 StorageLocation + WorkScheduling） |
| 4 | 批次仅全量直连 | 老MES另有 **按工单取批次**（中台 getBatchByOrderId）与异常批次同步；建议批次同步保留直连 + 报工场景走中台按工单取 |
| 5 | SAP 同步调度为全局固定 15 分钟 | 产品/工单/批次/工序分 Job 配置频率（工单 10 分钟，主数据每日 + 可手动触发） |
| 6 | 接口管理页面已展示 11 条假接口 | 停用假记录，真实接口全部登记（含健康状态=最近调用时间/成功率） |

---

## 十、开发计划与工作量估算

| 阶段 | 内容 | 工作量 |
|---|---|---|
| M1 | 配置 + Secret + TokenManager + MiddlewareApiClient + 日志 | 0.5d |
| M2 | 领料：拉取（3个查询接口）+ 过账推送 + 数据模型 | 1.5d |
| M3 | 报工：工单拉取 + 上料确认 + 消耗推送 | 1d |
| M4 | 完工入库：推送 + 关闭 | 1d |
| M5 | 退料：线边仓退 + 退仓库 + 取消 | 1d |
| M6 | 库存/批次/在途/供应商批次查询 | 0.5d |
| M7 | SAP直连：工序 + 客户 + 供应商 + 工单/产品$expand优化 | 1d |
| M8 | V25 迁移 + 接口管理登记 + 前端页面适配 | 1d |
| M9 | 联调测试（新中台实测）+ 部署 v32 | 2d |
| **合计** | | **≈ 9.5 人天** |

交付顺序建议：M1→M2→M3→M4→M5（P0 主线）→ M7/M8 并行 → M6/M9。

---

## 十一、待确认问题（开发前请拍板）

1. **范围确认**：B6 设备类 4 个接口（P2）本期是否需要？（老 MES 在用但与四大业务无直接耦合）
2. **MES-JX 密码**：实测 `123456` 有效 —— 是否沿用？建议正式环境改密后配到 K8s Secret。
3. **移动类型清单**：领料全量接口的 25 个移动类型（1312X/1301Z/…）是否原样沿用？突尼斯工厂是否有新增（如 101/201/1261X 等是否使用）？
4. **组织代码**：工厂 `TK10`、库存地 `J1xx/J2xx`、工作中心 `TK50x`、用户工号 `TK0xxxx` 是否与突尼斯实际一致？
5. **同步方向确认**：领料=中台拉取+MES过账回传；报工/入库/退料=MES 推送。是否符合新 MES 的业务流程设计？
6. **占位接口处理**：现有 `sys_interface_def` #5–#11（MIDDLEWARE/WMS/MES_LOCAL 占位）停用并替换，是否同意？
7. **老MES SOAP/ESB 通道**（/ecsb/gw/soa/rf，U8 对接）确认**不迁移**？
8. **A7 工单标准值 CDS**（YY1_C_MFGORDERSTDVH）是否需要（报工工时/成本核算可能依赖）？
9. 新 MES 侧的领料/报工/入库/退料**业务页面**是否已规划？（本报告仅覆盖接口层，页面开发另估）

---

## 附：信息来源

- 老MES DB：`10.80.10.50/orbit_mompro`（`data_sync_conf` 96条、`sys_dynamic_api` 3102条、`erp_sap_interfacelog` 780万条、`third_party_app`、`sys_scheduled_interface` 67条）
- 老MES应用：`10.80.10.51`（OrBit MOMpro 微服务 + 中台调用链实测）
- 新中台实测：`10.30.10.42:9999`（Token + 领料列表接口 2026-09-12 实测通过）
- 新MES代码：`D:\TNS-MES`（v31 已部署）
