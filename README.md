# TNS-MES (Tunisia Manufacturing Execution System)

突尼斯制造执行系统（MES），面向离散/流程制造的生产、质量、工程与集成一体化管理平台。

## 技术栈

| 类别 | 技术 / 版本 |
| --- | --- |
| 后端框架 | Spring Boot 2.7.18 |
| 编程语言 | Java 11 |
| 关系数据库 | SQL Server 2019 |
| 缓存 / 锁 | Redis |
| 消息队列 | RocketMQ |
| API 文档 | Springdoc OpenAPI（Swagger UI） |
| 安全认证 | Spring Security + JWT |
| 数据库迁移 | Flyway |
| 构建工具 | Maven |

## 项目结构 / 模块说明

后端源码位于 `tns-mes-server/`，按业务领域划分模块（包路径 `com.tns.mes.*`）：

| 模块 | 说明 |
| --- | --- |
| `identity` | 用户、角色、权限、菜单、数据字典（用户权限体系） |
| `engineering` | 产品、BOM、工艺路线、检验规则（产品工程） |
| `production` | 生产工单及工单工序、工单状态（生产工单） |
| `quality` | 批次质量管理（批次质量） |
| `integration` | SAP 集成、接口管理、同步任务、Outbox 消息（SAP 集成） |
| `basic` | 主数据与主数据类型等基础数据 |
| `common` | 公共组件：安全、Redis、异常、审计、国际化、限流、幂等等 |

其它目录：
- `tns-mes-android/`：Android 扫码终端 App
- `dist/` 与 `tns-mes-server/src/main/resources/static/`：前端单页应用（SPA）
- `k8s-deploy/`：Kubernetes 部署清单与脚本
- `tns-mes-architecture/`：架构说明

## 本地运行

### 1. 构建

在 `tns-mes-server/` 目录下执行：

```bash
mvn clean package -DskipTests
```

构建产物为 `tns-mes-server/target/tns-mes-server.jar`。

### 2. 启动（SQL Server 本地实例）

```bash
java -jar target/tns-mes-server.jar --spring.profiles.active=sqlserver-local
```

`sqlserver-local` profile 默认通过 Windows 集成认证连接本地 SQL Server 命名实例
`BARTHOLDER`，数据库名 `tns_mes`，并启用 Flyway 迁移。启动后服务监听
`8080` 端口，上下文路径为 `/tns-mes`。

> 其它可用 profile：
> - `local`：使用内嵌 H2 文件库，便于快速本地调试；
> - `sqlserver`：通过环境变量配置 SQL Server 连接；
> - `prod`（生产）：见 `application-prod.yml`。

## K8s 部署

生产环境部署在 Kubernetes 集群中：

- 部署服务器：`10.30.10.140`
- 服务暴露方式：NodePort `31180`
- 访问地址：http://10.30.10.140:31180/tns-mes/

部署清单见 `k8s-deploy/manifests/`。

## 默认管理员

系统首次启动时会按配置初始化管理员账号：

- 用户名：`admin`
- 密码：`admin123`

> 请在生产环境部署后立即修改默认密码。

## 许可

私有项目，版权所有。
