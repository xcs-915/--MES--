# TNS MES - 部署指南

## 架构说明

本系统采用前后端分离架构：
- **前端**：纯静态文件（HTML/CSS/JS），可部署到 Nginx、IIS 或任意静态文件服务器
- **后端**：Spring Boot 应用（JAR 包），提供 REST API

## 目录结构

```
dist/
├── frontend/          # 前端静态文件
│   ├── index.html
│   ├── app.css
│   ├── app.js
│   └── vendor/
└── backend/           # 后端应用
    ├── tns-mes-server.jar
    ├── start.ps1
    └── application-prod.yml
```

## 后端部署

### 1. 数据库准备
- 创建 SQL Server 数据库 `tns_mes`
- Flyway 会自动创建表结构和初始数据

### 2. 配置
- 复制 `application-prod.yml` 并重命名为 `application.yml`
- 修改数据库连接信息和 SAP 配置

### 3. 启动
```powershell
.\start.ps1
```
或直接运行：
```powershell
java -jar tns-mes-server.jar --spring.config.location=application.yml
```

默认端口：8080，访问路径：`/tns-mes`

## 前端部署

### 方式一：Nginx（推荐）

```nginx
server {
    listen 80;
    server_name mes.example.com;

    # 前端静态文件
    root /path/to/frontend;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 代理到后端
    location /tns-mes/api/ {
        proxy_pass http://backend-server:8080/tns-mes/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 方式二：IIS
- 将 frontend 目录内容复制到 IIS 站点目录
- 配置 URL 重写规则，将 `/tns-mes/api/*` 请求代理到后端

## 默认账号

- 用户名：`admin`
- 密码：`admin123`（生产环境请立即修改）

## 注意事项

1. 生产环境请务必修改 JWT 密钥
2. 建议配置 HTTPS
3. SAP 密码请使用环境变量配置，不要硬编码在配置文件中
4. 数据库用户权限建议限制为仅必要权限
