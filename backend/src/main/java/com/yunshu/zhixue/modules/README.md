# 业务模块约定（模块化单体）

## 标准目录

每个模块内部统一按以下结构组织（按需创建，不提前建空目录）：

```
modules/<领域>/
├── controller/
│   ├── admin/      # 管理端接口 /api/admin/v1/**
│   └── portal/     # 学员端接口 /api/portal/v1/**
├── service/        # 业务接口与实现，事务边界在此
├── mapper/         # MyBatis-Plus Mapper（仅本模块表，接口加 @Mapper）
├── entity/         # 数据库实体
├── dto/            # 入参 DTO 与出参 VO（不直接暴露实体）
└── convert/        # 实体与 DTO 转换
```

## 依赖硬约束（02 文档 §3.2）

1. 模块间单向依赖，禁止反向调用（如 question 不得调用 study）。
2. 不跨模块直连数据库：只能访问本模块拥有的表，跨模块取数经对方 Service + DTO。
3. `subject` 是内容体系根节点，其他内容模块只读不改。
4. `ai` 无状态、不落库，只接收入参并返回结构化结果。
5. `stats` 只读，不参与业务写入。
6. `security`、`common` 为最底层，可被任意模块依赖。
7. 新增后端代码必须落到 `modules/<领域>` 内；跨领域工具放 `common` 或 `support`。
