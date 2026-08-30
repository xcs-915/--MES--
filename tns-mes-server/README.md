# TNS MES Server

Java 11 / Spring Boot 2.7 的新 MES 后端，当前覆盖前三个开发阶段：

- 基础资料：企业、工厂、车间、部门、仓库、工作中心、产线、工位、人员、岗位、客户、供应商、制造商
- 产品工程准备：产品、BOM、工序路线、检验规则
- 生产执行基础：工单创建、发布、开工、完工、取消和进度记录

## 本地运行

```powershell
cd D:\TNS-MES\tns-mes-server
mvn test
mvn package
java -jar target\tns-mes-server.jar --spring.profiles.active=local
```

也可以从 PowerShell 执行完整冒烟测试：

```powershell
powershell -ExecutionPolicy Bypass -File D:\TNS-MES\scripts\smoke-test.ps1
```

脚本会创建带时间后缀的测试数据，并验证工单 `DRAFT -> RELEASED -> IN_PROGRESS -> COMPLETED` 状态链路。

本地默认使用 `./data/tns_mes_local` H2 文件库，服务地址为
`http://127.0.0.1:8080/tns-mes`。打开根地址会自动跳转到 Swagger；首次运行自动创建管理员：`admin / admin123`，生产环境必须修改密码和 `MES_JWT_SECRET`。

## 语言

错误消息支持 `zh-CN`、`en`、`ar-TN`。请求可使用 `?lang=en`、`?lang=ar-TN` 或 `Accept-Language`；业务实体同时保存 `nameZh`、`nameEn`、`nameAr`。

## 主要接口

| 模块 | 接口 |
| --- | --- |
| 登录 | `POST /api/v1/auth/login`、`GET /api/v1/auth/me` |
| 基础资料 | `GET/POST /api/v1/master-data/{type}`、`GET/PUT/DELETE /api/v1/master-data/{type}/{id}` |
| 产品 | `GET/POST /api/v1/products`、`GET/PUT/DELETE /api/v1/products/{id}` |
| BOM | `GET/POST /api/v1/boms`、`GET/PUT /api/v1/boms/{id}`、`POST /api/v1/boms/{id}/publish` |
| 工序路线 | `GET/POST /api/v1/process-routes`、`GET/PUT /api/v1/process-routes/{id}`、`POST /api/v1/process-routes/{id}/publish` |
| 检验规则 | `GET/POST /api/v1/inspection-rules`、`GET/PUT /api/v1/inspection-rules/{id}`、`POST /api/v1/inspection-rules/{id}/publish` |
| 工单 | `GET/POST /api/v1/work-orders`、`GET/PUT /api/v1/work-orders/{id}`、`POST /api/v1/work-orders/{id}/release|start|complete|cancel` |
| SAP 集成 | `POST /api/v1/integrations/sap/request`、`POST /api/v1/integrations/sap/products/sync`、`POST /api/v1/integrations/sap/work-orders/sync` |

所有接口返回统一的 `code/message/data/requestId/timestamp` 结构。写接口可携带 `X-Idempotency-Key`，避免重复提交。

## SAP 产品与工单同步

SAP 集成默认关闭，避免本地开发环境误访问外部系统。启用时通过环境变量配置，不把凭据写入源码：

```text
MES_SAP_ENABLED=true
MES_SAP_BASE_URL=https://my200683.s4hana.sapcloud.cn
MES_SAP_PRODUCT_PATH=/实际产品接口路径
MES_SAP_WORK_ORDER_PATH=/实际工单接口路径
MES_SAP_COMPONENT_PATH=/可选组件接口路径
MES_SAP_OPERATION_PATH=/工序接口路径
MES_SAP_USERNAME=...
MES_SAP_PASSWORD=...
MES_SAP_SCHEDULE_ENABLED=true
MES_SAP_SYNC_FIXED_DELAY_MS=900000
```

也可使用 `MES_SAP_BEARER_TOKEN` 替代 Basic 认证。通用请求接口只接受相对于已配置 SAP 域名的路径，防止被用作任意 URL 代理。产品按 SAP 物料/产品编码 Upsert；工单按 SAP 工单号 Upsert，且工单引用的产品必须已经同步。工单同步会读取 OData 导航集合中的组件和工序；没有嵌套集合时，可配置组件接口和工序接口按工单号过滤，自动生成 SAP BOM 与 SAP 工艺路线。定时同步默认关闭，开启后按固定间隔依次同步产品和工单；产品页和工单页也提供单条同步按钮。

## 数据库与 Redis

- `src/main/resources/db/migration/V1__init.sql` 是 SQL Server 2019 迁移脚本，生产使用 `--spring.profiles.active=sqlserver` 并通过 `MES_DB_URL`、`MES_DB_USERNAME`、`MES_DB_PASSWORD` 配置连接。
- 本地 profile 为了不依赖远程 SQL Server，使用 Hibernate 建表；部署 SQL Server 前必须执行 Flyway 迁移并将 `spring.jpa.hibernate.ddl-auto` 保持为 `validate`。
- Redis 用于缓存、权限缓存、幂等键、分布式锁、限流和 Streams 队列。通过 `REDIS_HOST`、`REDIS_PORT` 配置。

当前代码和脚本均在本地目录完成，没有连接或修改用户提供的远程服务器。
