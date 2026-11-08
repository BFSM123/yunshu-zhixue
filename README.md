# 云枢智学

AI 赋能的智能面试训练平台。模块化单体后端 + 学员端 / 管理端两个独立前端工程。

## 目录结构

```
云枢智学/
├── backend/          # Spring Boot 3 后端（模块化分包）
├── portal-web/       # 学员端（Vue 3 + TS + Vite）
├── admin-web/        # 管理端（Vue 3 + TS + Vite）
├── deploy/           # Docker Compose（MySQL / Redis / MinIO）
├── scripts/sql/      # 数据库初始化脚本
├── docs/             # 需求与设计文档
└── CLAUDE.md         # 项目协作规则
```

## 环境要求

- JDK 17+（本机 JDK 21，按 release 17 编译）
- Maven 3.9+
- Node.js 20+（本机 Node 24）
- pnpm 9+（本机 12.x，位于 `D:\tools\node-global\pnpm.cmd`）
- Docker Desktop（已配镜像加速）

## 快速启动

### 1. 启动中间件

```bash
# 默认启动 MySQL + Redis
docker compose -f deploy/docker-compose.yml up -d

# MinIO 为可选能力（Logo / 头像上传），需要时再加 profile
docker compose -f deploy/docker-compose.yml --profile minio up -d
```

首次启动自动执行 `scripts/sql/schema.sql`（23 张表 + 种子数据）。
本机 Docker Hub 对 MinIO 镜像拉取受限，默认不启动 MinIO。
本机 3306 / 6379 已被其他项目容器占用，本项目默认使用 **3307 / 6380**。

### 2. 启动后端

```bash
export JAVA_HOME="/d/tools/java/jdk-21.0.12.1+1"
export PATH="$JAVA_HOME/bin:/d/tools/maven/apache-maven-3.9.16/bin:$PATH"

# 本机全局 Maven settings.xml 存在拼写错误，使用项目内正确配置覆盖
mvn -gs scripts/maven-settings.xml -s scripts/maven-settings.xml \
  -f backend/pom.xml -DskipTests spring-boot:run
```

> 全局 `D:\tools\maven\apache-maven-3.9.16\conf\settings.xml` 把 `<mirrorOf>` 写成了 `<mirror0f>`，
> 直接执行 `mvn` 会在读取配置阶段失败。项目内 `scripts/maven-settings.xml` 已提供正确镜像配置；
> 是否修复全局文件需你确认后再动。

- 服务：`http://localhost:8080`
- 健康检查：`http://localhost:8080/actuator/health`
- Swagger UI：`http://localhost:8080/swagger-ui.html`

### 3. 启动前端

```bash
# 学员端
cd portal-web && pnpm install && pnpm dev     # http://localhost:5173

# 管理端
cd admin-web && pnpm install && pnpm dev      # http://localhost:5174/admin/
```

> 本机 pnpm 不在 PATH 时使用 `D:/tools/node-global/pnpm.cmd install`，
> 或使用 `npm install --cache .npm-cache`（npm 默认缓存在 E:\node 无写权限）。

## 默认账号

- 后台：`admin / Admin@123456`（首次启动由后端写入 BCrypt 哈希，生产用 `APP_DEFAULT_PASSWORD` 覆盖）

## 验证命令

```bash
# 后端
mvn -gs scripts/maven-settings.xml -s scripts/maven-settings.xml \
  -f backend/pom.xml -DskipTests package

# 学员端
cd portal-web && pnpm type-check && pnpm build

# 管理端
cd admin-web && pnpm type-check && pnpm build
```

## 接口契约

后端启动后导出 OpenAPI，两端分别生成类型：

```bash
cd portal-web && pnpm gen:types
cd admin-web && pnpm gen:types
```

生成物位于各自的 `src/types/generated/`，禁止手工修改。

## 当前阶段说明

- 已完成：工程骨架、统一响应/异常/错误码、JWT 与 RBAC 权限骨架、MyBatis-Plus/Redis/MinIO/OpenAPI 配置、数据库初始化脚本、两端路由与布局占位页。
- 待接入：各业务模块的 Controller / Service / Mapper、登录接口、AI 能力层、真实页面交互。
