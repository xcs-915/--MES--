# TNS-MES Android

TNS 制造执行系统（MES）Android 客户端。首个界面为 **上料（Loading Material）** 扫描界面。

## 项目概览

| 项目属性 | 值 |
| --- | --- |
| 包名 | `com.tns.mes` |
| 语言 | Java 17 |
| compileSdk / targetSdk | 34 |
| minSdk | 24（Android 7.0） |
| 构建系统 | Gradle 8.x（AGP 8.1.2） |
| UI 框架 | Material Design 3（Material Components 1.11.0） |
| 网络栈 | Retrofit 2.9 + OkHttp 4.12 + Gson 2.10 |
| 主色 | 紫色 `#7C3AED`（与 Web 端一致） |
| 后端基地址 | `http://10.0.2.2:8080/tns-mes/`（Android 模拟器 → 宿主机 localhost） |

## 目录结构

```
tns-mes-android/
├── build.gradle                    # 项目级构建脚本
├── settings.gradle                 # Gradle 设置（仓库、模块）
├── gradle.properties               # Gradle / AndroidX 配置
└── app/
    ├── build.gradle                # 模块级构建脚本
    ├── proguard-rules.pro          # 混淆规则（Retrofit / Gson）
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/tns/mes/
        │   ├── TnsApplication.java         # Application 入口
        │   ├── LoginActivity.java          # 登录界面
        │   ├── ScanningActivity.java      # 上料扫描界面
        │   ├── api/
        │   │   ├── ApiClient.java          # Retrofit 客户端（JWT 拦截器）
        │   │   ├── MesApi.java             # REST 接口定义
        │   │   └── ApiResponses.java       # 通用响应体 / 接口定义模型
        │   ├── model/
        │   │   ├── LoginRequest.java       # 登录请求体
        │   │   └── LoginResponse.java     # 登录响应体（token + user）
        │   └── util/
        │       └── SessionManager.java    # JWT Token 持久化（SharedPreferences）
        └── res/
            ├── layout/
            │   ├── activity_login.xml
            │   └── activity_scanning.xml
            ├── drawable/                  # 矢量图标 + 背景
            ├── mipmap-anydpi-v26/         # 自适应启动图标
            ├── values/                   # colors / strings / themes
            ├── menu/scanning_menu.xml     # 退出登录菜单
            └── xml/                       # 备份规则
```

## 环境要求

- **JDK 17**（构建时指定 `sourceCompatibility`/`targetCompatibility = 17`）
- **Android Studio**：Hedgehog（2023.1）或更高（带 AGP 8.1 支持）
- **Android SDK**：Platform 34 + Build-Tools 34.x
- 系统已安装 Android SDK 并配置 `ANDROID_HOME` 环境变量

## 构建步骤

### 方式一：Android Studio（推荐）

1. 启动 Android Studio → `File > Open` → 选择 `tns-mes-android` 目录。
2. 等待 Gradle 同步完成（首次会自动下载依赖）。
3. 连接真机或启动模拟器（API 24+，建议 API 34）。
4. 点击 `Run 'app'`（Shift+F10）。

### 方式二：命令行

> 项目已包含 `gradle/wrapper/gradle-wrapper.properties`（指定 Gradle 8.1.1）。
> 首次使用 CLI 构建时，需先生成 Wrapper 脚本与 jar（二选一）：
> - 已安装 Gradle：执行 `gradle wrapper --gradle-version 8.1.1`；
> - 用 Android Studio 打开一次本项目，IDE 会自动补全 `gradlew` / `gradlew.bat` / `gradle-wrapper.jar`。

```bash
# 进入项目根目录
cd tns-mes-android

# 首次生成 Gradle Wrapper（若尚未包含 gradlew）
gradle wrapper --gradle-version 8.1.1

# Debug 构建
./gradlew assembleDebug          # Windows PowerShell: .\gradlew.bat assembleDebug

# 安装到已连接的设备 / 模拟器
./gradlew installDebug

# 单元测试
./gradlew test
```

> 若提示找不到 `gradle` 命令，请使用 Android Studio 自带的 Gradle，或单独安装 Gradle 8.x。
> 输出 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

## 后端对接

Android 模拟器中 `10.0.2.2` 映射到运行模拟器的电脑的 `localhost`/`127.0.0.1`。
因此当 MES 后端在本机 `http://localhost:8080/tns-mes/` 运行时，App 通过
`http://10.0.2.2:8080/tns-mes/` 即可访问。

基地址常量定义在 `ApiClient.java`：

```java
public static final String BASE_URL = "http://10.0.2.2:8080/tns-mes/";
```

如需指向真实设备或远程服务器，修改此常量即可（注意 Android 9+ 默认禁用明文 HTTP，
本项目已在 `AndroidManifest.xml` 中通过 `android:usesCleartextTraffic="true"` 放行）。

### 已实现接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `api/v1/auth/login` | 登录，请求体 `{username, password}`，返回 `{token, user}` |
| POST | `api/v1/interfaces/definitions` | 按接口编码查询接口定义 |
| GET  | `api/v1/interfaces/definitions/{code}` | 按编码获取接口定义 |
| POST | `api/v1/loading/check`（待配置） | 上料校验，待后端确定后补充 |

### 上料校验流程（当前实现）

`ScanningActivity` 的「确认上料」按钮会：
1. 本地校验工单编号与物料标签非空；
2. 调用 `POST api/v1/interfaces/definitions` 查询编码为 `LOADING_CHECK` 的接口定义；
3. 接口定义就绪后，按其 `method` 与 `interfaceUrl` 动态调用上料校验（TODO 锚点见
   `ScanningActivity.submitLoading()`）。

## 认证机制

- 登录成功后，`SessionManager` 将 JWT Token 与用户信息存入
  `SharedPreferences`（文件名 `tns_mes_session`）。
- `ApiClient` 的 OkHttp 拦截器会为每个请求自动附加
  `Authorization: Bearer <token>` 头。
- 启动时若发现已存在有效 Token，则跳过登录直接进入扫描界面。

## 明文流量与权限

- `AndroidManifest.xml` 已声明 `INTERNET`、`ACCESS_NETWORK_STATE`、`CAMERA` 权限；
- 开启 `usesCleartextTraffic` 以便在开发期通过 HTTP 连接本地后端（生产环境建议改用 HTTPS）。

## 后续扩展

- 接入相机扫码：可集成 ML Kit / ZXing，将扫描按钮的 `focusForScan()` 替换为相机扫描流程。
- 上料校验真实调用：在 `ScanningActivity` 的 TODO 处按接口定义动态发起请求。
- 列表/详情界面：基于已有的 Retrofit + SessionManager 模式扩展新的 Activity 与 `MesApi` 方法。
