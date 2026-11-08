# 本地开发环境

## 1. 启动中间件

```bash
# 启动 MySQL + Redis + MinIO
docker compose -f deploy/docker-compose.yml up -d

# 查看状态
docker compose -f deploy/docker-compose.yml ps
```

首次启动时 MySQL 会自动执行 `scripts/sql/schema.sql`（建库 `yszx`、23 张表、种子数据）。
**初始化脚本只在数据卷为空时执行**；如需重置：

```bash
docker compose -f deploy/docker-compose.yml down -v
docker compose -f deploy/docker-compose.yml up -d
```

> 官方 `minio/minio`、`minio/mc` 镜像当前无法直连拉取，compose 默认使用本地可获取的
> Bitnami 兼容镜像 `docker.1ms.run/bitnamilegacy/minio:latest`（内置 `mc`，用于初始化 bucket）。
> 如后续能拉取官方镜像，可通过 `MINIO_IMAGE=minio/minio:latest` 覆盖，并注意恢复 `minio-init` 的 `mc` 入口。

## 2. 服务与端口

| 服务 | 地址 | 说明 |
|---|---|---|
| MySQL | `localhost:3307` | 库 `yszx`，用户 `yszx / yszx123456` |
| Redis | `localhost:6380` | 无密码 |
| MinIO API | `http://localhost:9000` | bucket `yszx`（公开读） |
| MinIO 控制台 | `http://localhost:9001` | `minioadmin / minioadmin` |

> 本机 3306 / 6379 已被其他项目容器占用，本项目默认映射到 3307 / 6380。
> 如端口空闲，可通过 `.env` 中的 `MYSQL_PORT`、`REDIS_PORT` 改回标准端口，
> 并同步设置后端的 `MYSQL_PORT`、`REDIS_PORT` 环境变量。

## 3. 环境变量

复制模板并按需修改：

```bash
cp deploy/.env.example deploy/.env
```

Compose 会自动读取同目录 `.env`；不创建时使用 compose 文件内的开发默认值。

## 4. 内置后台账号

- 用户名：`admin`
- 初始密码：`Admin@123456`（由后端 `app.default-password` 在首次启动时写入 BCrypt 哈希）

生产部署务必通过 `APP_DEFAULT_PASSWORD` 覆盖，并在首次登录后修改。
