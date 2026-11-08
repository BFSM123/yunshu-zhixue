-- =====================================================================
-- 云枢智学 数据库初始化脚本
-- 依据：docs/design/03.数据库设计.md（含 D3-x / D4-8 / D4-9）
-- 规范：自增主键、禁物理外键、snake_case、UTC 存储、全站逻辑删除
-- 说明：本脚本用于空库首次初始化（Docker MySQL 首次启动自动执行）
-- 字符集：utf8mb4 / utf8mb4_0900_ai_ci（MySQL 8）
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `yszx`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE `yszx`;

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ---------------------------------------------------------------------
-- 1. 学员账号
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `student` (
    `id`                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `mobile`                VARCHAR(11)     NOT NULL COMMENT '手机号（登录账号）',
    `password_hash`         VARCHAR(100)    NOT NULL COMMENT 'BCrypt 哈希',
    `nickname`              VARCHAR(50)     NOT NULL COMMENT '用户名（注册时应用层赋默认值）',
    `avatar`                VARCHAR(500)    DEFAULT NULL COMMENT '头像 URL（MinIO）',
    `email`                 VARCHAR(100)    DEFAULT NULL COMMENT '邮箱：全局唯一，允许多行 NULL',
    `gender`                TINYINT         NOT NULL DEFAULT 0 COMMENT '0 保密 / 1 男 / 2 女',
    `preferred_subject_id`  BIGINT UNSIGNED DEFAULT NULL COMMENT '技术方向 -> subject.id',
    `guide_finished`        TINYINT         NOT NULL DEFAULT 0 COMMENT '新手引导是否看过',
    `status`                TINYINT         NOT NULL DEFAULT 1 COMMENT '1 已启用 / 0 已禁用',
    `last_login_at`         DATETIME        DEFAULT NULL COMMENT '最后登录（UTC）',
    `created_at`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`           TINYINT         NOT NULL DEFAULT 0 COMMENT '0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_mobile` (`mobile`),
    UNIQUE KEY `uk_student_email` (`email`),
    KEY `idx_student_created_at` (`created_at`),
    KEY `idx_student_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学员表';

-- ---------------------------------------------------------------------
-- 2. 内容管理：学科 / 阶段 / 课程 / 题目 / 面试题库 / 学习路线
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `subject` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`          VARCHAR(50)     NOT NULL COMMENT '学科名称',
    `icon`          VARCHAR(50)     NOT NULL COMMENT '图标（Emoji）',
    `description`   VARCHAR(255)    NOT NULL COMMENT '描述',
    `theme_color`   VARCHAR(20)     NOT NULL DEFAULT '#000000' COMMENT '主题色',
    `sort_order`    INT             NOT NULL DEFAULT 0 COMMENT '排序（越小越靠前）',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`   TINYINT         NOT NULL DEFAULT 0 COMMENT '0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_subject_name` (`name`),
    KEY `idx_subject_sort` (`sort_order`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学科表';

CREATE TABLE IF NOT EXISTS `stage` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `subject_id`    BIGINT UNSIGNED NOT NULL COMMENT '-> subject.id',
    `name`          VARCHAR(50)     NOT NULL COMMENT '阶段名称',
    `icon`          VARCHAR(50)     DEFAULT NULL COMMENT '图标（留空默认）',
    `sort_order`    INT             NOT NULL DEFAULT 0 COMMENT '排序',
    `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 禁用',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`   TINYINT         NOT NULL DEFAULT 0 COMMENT '0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stage_subject_name` (`subject_id`, `name`),
    KEY `idx_stage_subject` (`subject_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '阶段表';

CREATE TABLE IF NOT EXISTS `course` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`              VARCHAR(100)    NOT NULL COMMENT '课程名称',
    `cover_url`         VARCHAR(500)    DEFAULT NULL COMMENT '封面 URL（外链）',
    `duration_minutes`  INT             NOT NULL DEFAULT 0 COMMENT '时长（分钟）',
    `source`            TINYINT         NOT NULL DEFAULT 1 COMMENT '1 B站 / 2 其他',
    `link`              VARCHAR(500)    DEFAULT NULL COMMENT '课程链接',
    `bvid`              VARCHAR(20)     DEFAULT NULL COMMENT 'BV 号（非 B 站来源为空）',
    `subject_id`        BIGINT UNSIGNED DEFAULT NULL COMMENT '主学科 -> subject.id；NULL=其他学科',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`       TINYINT         NOT NULL DEFAULT 0 COMMENT '0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_course_bvid` (`bvid`),
    KEY `idx_course_subject` (`subject_id`),
    KEY `idx_course_source` (`source`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'B站课程库';

CREATE TABLE IF NOT EXISTS `course_subject` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `course_id`     BIGINT UNSIGNED NOT NULL COMMENT '-> course.id',
    `subject_id`    BIGINT UNSIGNED NOT NULL COMMENT '-> subject.id',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`   TINYINT         NOT NULL DEFAULT 0 COMMENT '0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_course_subject` (`course_id`, `subject_id`),
    KEY `idx_course_subject_subject` (`subject_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '课程-学科多学科关联';

CREATE TABLE IF NOT EXISTS `question` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `stage_id`       BIGINT UNSIGNED NOT NULL COMMENT '-> stage.id',
    `type`           TINYINT         NOT NULL COMMENT '1 单选 / 2 多选 / 3 判断 / 4 简答',
    `difficulty`     TINYINT         NOT NULL DEFAULT 2 COMMENT '1 简单 / 2 中等 / 3 困难',
    `content`        TEXT            NOT NULL COMMENT '题干',
    `answer`         TEXT            DEFAULT NULL COMMENT '判断/简答答案；单选多选为空（正确项在选项表）',
    `analysis`       TEXT            DEFAULT NULL COMMENT '解析',
    `tags`           VARCHAR(255)    DEFAULT NULL COMMENT '标签，逗号分隔',
    `status`         TINYINT         NOT NULL DEFAULT 3 COMMENT '1 已上架 / 2 已下架 / 3 待审核 / 4 草稿(预留不启用) / 5 已驳回',
    `reject_reason`  VARCHAR(255)    DEFAULT NULL COMMENT '驳回原因（审核不通过必填，通过/重新提交清空）',
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`    TINYINT         NOT NULL DEFAULT 0 COMMENT '0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_question_stage` (`stage_id`),
    KEY `idx_question_status` (`status`),
    KEY `idx_question_type_difficulty` (`type`, `difficulty`),
    KEY `idx_question_created` (`created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '题库题目表';

CREATE TABLE IF NOT EXISTS `question_option` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `question_id`   BIGINT UNSIGNED NOT NULL COMMENT '-> question.id',
    `option_key`    VARCHAR(4)      NOT NULL COMMENT 'A/B/C/D',
    `option_text`   VARCHAR(500)    NOT NULL COMMENT '选项内容',
    `is_correct`    TINYINT         NOT NULL DEFAULT 0 COMMENT '1 正确 / 0 否',
    `sort_order`    INT             NOT NULL DEFAULT 0 COMMENT '选项排序',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`   TINYINT         NOT NULL DEFAULT 0 COMMENT '0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_question_option` (`question_id`, `option_key`),
    KEY `idx_question_option_qid` (`question_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '题目选项表';

CREATE TABLE IF NOT EXISTS `interview_question` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `subject_id`        BIGINT UNSIGNED NOT NULL COMMENT '方向 -> subject.id',
    `content`           TEXT            NOT NULL COMMENT '题干',
    `reference_answer`  TEXT            DEFAULT NULL COMMENT '参考答案（可留空）',
    `topic`             VARCHAR(50)     DEFAULT NULL COMMENT '主题（NULL=未分类）',
    `difficulty`        TINYINT         NOT NULL DEFAULT 2 COMMENT '1 简单 / 2 中等 / 3 困难',
    `tags`              VARCHAR(255)    DEFAULT NULL COMMENT '标签，逗号分隔',
    `source_type`       TINYINT         NOT NULL DEFAULT 1 COMMENT '1 文档解析 / 2 手工录入 / 3 手动导入',
    `source_doc_name`   VARCHAR(255)    DEFAULT NULL COMMENT '来源文档名（文件不入库）',
    `source_seq`        INT             DEFAULT NULL COMMENT '文档内第 N 题',
    `status`            TINYINT         NOT NULL DEFAULT 1 COMMENT '1 已上架 / 2 已下架',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`       TINYINT         NOT NULL DEFAULT 0 COMMENT '0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_interview_question_subject_status` (`subject_id`, `status`),
    KEY `idx_interview_question_topic` (`topic`),
    KEY `idx_interview_question_difficulty` (`difficulty`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '面试题库表';

CREATE TABLE IF NOT EXISTS `interview_topic` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `subject_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '-> subject.id；NULL=全局主题',
    `name`          VARCHAR(50)     NOT NULL COMMENT '主题名称',
    `sort_order`    INT             NOT NULL DEFAULT 0 COMMENT '排序',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`   TINYINT         NOT NULL DEFAULT 0 COMMENT '0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_interview_topic_subject` (`subject_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '面试题主题词表';

CREATE TABLE IF NOT EXISTS `learning_route` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`              VARCHAR(100)    NOT NULL COMMENT '路线名称',
    `subject_id`        BIGINT UNSIGNED NOT NULL COMMENT '关联学科 -> subject.id',
    `status`            TINYINT         NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 禁用',
    `icon`              VARCHAR(50)     DEFAULT NULL COMMENT '路线图标',
    `target_education`  VARCHAR(100)    DEFAULT NULL COMMENT '适合学历',
    `description`       TEXT            DEFAULT NULL COMMENT '路线描述',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`       TINYINT         NOT NULL DEFAULT 0 COMMENT '0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_learning_route_subject` (`subject_id`),
    KEY `idx_learning_route_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学习路线表';

CREATE TABLE IF NOT EXISTS `route_course` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `route_id`      BIGINT UNSIGNED NOT NULL COMMENT '-> learning_route.id',
    `course_id`     BIGINT UNSIGNED NOT NULL COMMENT '-> course.id',
    `sort_order`    INT             NOT NULL DEFAULT 0 COMMENT '课程顺序（即学习顺序）',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`   TINYINT         NOT NULL DEFAULT 0 COMMENT '0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_route_course` (`route_id`, `course_id`),
    KEY `idx_route_course_course` (`course_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '路线-课程关联表';

CREATE TABLE IF NOT EXISTS `route_course_completion` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `student_id`        BIGINT UNSIGNED NOT NULL COMMENT '-> student.id',
    `route_course_id`   BIGINT UNSIGNED NOT NULL COMMENT '-> route_course.id',
    `completed_at`      DATETIME        NOT NULL COMMENT '完成时间（UTC）',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`       TINYINT         NOT NULL DEFAULT 0 COMMENT '0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_route_course_completion` (`student_id`, `route_course_id`),
    KEY `idx_route_course_completion_student` (`student_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学员课程完成表';

-- ---------------------------------------------------------------------
-- 3. 学习训练：作答明细 / 错题本
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `practice_record` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `student_id`        BIGINT UNSIGNED NOT NULL COMMENT '-> student.id',
    `question_id`       BIGINT UNSIGNED NOT NULL COMMENT '-> question.id',
    `mode`              TINYINT         NOT NULL DEFAULT 1 COMMENT '1 顺序 / 2 随机',
    `is_correct`        TINYINT         NOT NULL DEFAULT 0 COMMENT '1 对 / 0 错',
    `duration_seconds`  INT             DEFAULT NULL COMMENT '该题已用时长（秒）',
    `practiced_at`      DATETIME        NOT NULL COMMENT '作答时间（UTC）',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`       TINYINT         NOT NULL DEFAULT 0 COMMENT '0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_practice_record_student_time` (`student_id`, `practiced_at`),
    KEY `idx_practice_record_student_question` (`student_id`, `question_id`),
    KEY `idx_practice_record_question` (`question_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '刷题作答明细表';

CREATE TABLE IF NOT EXISTS `wrong_question` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `student_id`        BIGINT UNSIGNED NOT NULL COMMENT '-> student.id',
    `question_id`       BIGINT UNSIGNED NOT NULL COMMENT '-> question.id',
    `error_count`       INT             NOT NULL DEFAULT 1 COMMENT '错误次数',
    `corrected_count`   INT             NOT NULL DEFAULT 0 COMMENT '连续答对次数（达 3 已掌握）',
    `status`            TINYINT         NOT NULL DEFAULT 1 COMMENT '1 待复习 / 2 已掌握',
    `last_wrong_at`     DATETIME        DEFAULT NULL COMMENT '最近答错时间（UTC）',
    `last_answer`       TEXT            DEFAULT NULL COMMENT '最近一次错误作答快照',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`       TINYINT         NOT NULL DEFAULT 0 COMMENT '0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wrong_question` (`student_id`, `question_id`),
    KEY `idx_wrong_question_student_status` (`student_id`, `status`),
    KEY `idx_wrong_question_last_wrong` (`last_wrong_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '错题本表';

-- ---------------------------------------------------------------------
-- 4. AI 面试：记录 / 逐题评分 / 报告
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `interview_record` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `student_id`        BIGINT UNSIGNED NOT NULL COMMENT '-> student.id',
    `subject_id`        BIGINT UNSIGNED NOT NULL COMMENT '面试方向 -> subject.id',
    `difficulty`        TINYINT         NOT NULL COMMENT '1 简单 / 2 中等 / 3 困难',
    `question_count`    INT             NOT NULL COMMENT '题数（3/5/8/10/15）',
    `overall_score`     DECIMAL(3, 1)   DEFAULT NULL COMMENT '综合评分 0.0-10.0',
    `total_duration`    INT             DEFAULT NULL COMMENT '累计时长（秒）',
    `started_at`        DATETIME        NOT NULL COMMENT '开始时间（UTC）',
    `completed_at`      DATETIME        NOT NULL COMMENT '结束时间（UTC）',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`       TINYINT         NOT NULL DEFAULT 0 COMMENT '0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_interview_record_student_time` (`student_id`, `created_at`),
    KEY `idx_interview_record_subject` (`subject_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI面试记录表';

CREATE TABLE IF NOT EXISTS `interview_question_score` (
    `id`                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `interview_record_id`   BIGINT UNSIGNED NOT NULL COMMENT '-> interview_record.id',
    `question_order`        INT             NOT NULL COMMENT '第 N 题（从 1 起）',
    `question_id`           BIGINT UNSIGNED DEFAULT NULL COMMENT '-> interview_question.id；NULL=AI 快照题',
    `question_text`         TEXT            NOT NULL COMMENT '题干快照',
    `student_answer`        TEXT            DEFAULT NULL COMMENT '学员作答',
    `score`                 DECIMAL(3, 1)   NOT NULL COMMENT '本题得分 0.0-10.0',
    `ai_feedback`           TEXT            DEFAULT NULL COMMENT 'AI 评分理由/反馈',
    `created_at`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`           TINYINT         NOT NULL DEFAULT 0 COMMENT '0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_interview_score_record` (`interview_record_id`, `question_order`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '面试逐题评分表';

CREATE TABLE IF NOT EXISTS `interview_report` (
    `id`                        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `interview_record_id`       BIGINT UNSIGNED NOT NULL COMMENT '-> interview_record.id',
    `radar_json`                TEXT            DEFAULT NULL COMMENT '6 维能力分析（JSON）',
    `strengths`                 TEXT            DEFAULT NULL COMMENT '优势',
    `weaknesses`                TEXT            DEFAULT NULL COMMENT '不足',
    `suggestions`               TEXT            DEFAULT NULL COMMENT '改进建议',
    `improvement_cards_json`    TEXT            DEFAULT NULL COMMENT '3 张能力提升卡（JSON）',
    `created_at`                DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`                DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`               TINYINT         NOT NULL DEFAULT 0 COMMENT '0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_interview_report_record` (`interview_record_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '面试报告表';

-- ---------------------------------------------------------------------
-- 5. 系统管理：后台用户 / 角色 / 权限 / 日志 / 设置
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `admin_user` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`          VARCHAR(50)     NOT NULL COMMENT '用户名（登录账号）',
    `password_hash`     VARCHAR(100)    NOT NULL COMMENT 'BCrypt 哈希',
    `mobile`            VARCHAR(11)     DEFAULT NULL COMMENT '手机号',
    `email`             VARCHAR(100)    DEFAULT NULL COMMENT '邮箱',
    `role_id`           BIGINT UNSIGNED NOT NULL COMMENT '-> role.id（单角色）',
    `status`            TINYINT         NOT NULL DEFAULT 1 COMMENT '1 已启用 / 0 已禁用',
    `is_builtin`        TINYINT         NOT NULL DEFAULT 0 COMMENT '1 内置（超级管理员）',
    `last_login_at`     DATETIME        DEFAULT NULL COMMENT '最后登录（UTC）',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`       TINYINT         NOT NULL DEFAULT 0 COMMENT '0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_admin_user_username` (`username`),
    UNIQUE KEY `uk_admin_user_mobile` (`mobile`),
    KEY `idx_admin_user_role` (`role_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '后台用户表';

CREATE TABLE IF NOT EXISTS `role` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`          VARCHAR(50)     NOT NULL COMMENT '角色名称',
    `code`          VARCHAR(50)     NOT NULL COMMENT '角色编码',
    `description`   VARCHAR(255)    DEFAULT NULL COMMENT '描述',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`   TINYINT         NOT NULL DEFAULT 0 COMMENT '0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色表';

CREATE TABLE IF NOT EXISTS `permission` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `parent_id`     BIGINT UNSIGNED DEFAULT NULL COMMENT '父节点（NULL=组节点）',
    `name`          VARCHAR(50)     NOT NULL COMMENT '组名/权限点名',
    `code`          VARCHAR(100)    NOT NULL COMMENT '权限编码（如 question:view）',
    `type`          TINYINT         NOT NULL COMMENT '1 组 / 2 操作点',
    `sort_order`    INT             NOT NULL DEFAULT 0 COMMENT '排序',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`   TINYINT         NOT NULL DEFAULT 0 COMMENT '0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`code`),
    KEY `idx_permission_parent` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '权限表';

CREATE TABLE IF NOT EXISTS `role_permission` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `role_id`           BIGINT UNSIGNED NOT NULL COMMENT '-> role.id',
    `permission_id`     BIGINT UNSIGNED NOT NULL COMMENT '-> permission.id',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    KEY `idx_role_permission_permission` (`permission_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色-权限关联表（覆盖式授权，无逻辑删除）';

CREATE TABLE IF NOT EXISTS `operation_log` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '-> admin_user.id',
    `username`      VARCHAR(50)     NOT NULL COMMENT '用户名快照',
    `action`        VARCHAR(50)     NOT NULL COMMENT '登录/登出/新增/修改/删除/禁用启用/导出/其他',
    `target_type`   VARCHAR(50)     NOT NULL COMMENT '操作对象类型',
    `target_name`   VARCHAR(255)    NOT NULL COMMENT '操作对象描述',
    `ip`            VARCHAR(64)     NOT NULL COMMENT 'IP 地址',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间（UTC）',
    PRIMARY KEY (`id`),
    KEY `idx_operation_log_user` (`user_id`),
    KEY `idx_operation_log_time` (`created_at`),
    KEY `idx_operation_log_action` (`action`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '操作日志表（审计，无逻辑删除）';

CREATE TABLE IF NOT EXISTS `system_setting` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_key`        VARCHAR(50)     NOT NULL COMMENT '配置键',
    `config_value`      TEXT            DEFAULT NULL COMMENT '配置值',
    `remark`            VARCHAR(255)    DEFAULT NULL COMMENT '说明',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_system_setting_key` (`config_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统设置表';

-- =====================================================================
-- 种子数据（仅在空库首次初始化时执行）
-- =====================================================================

-- 8 个 AI 学科（AI应用与智能体开发默认第一）
INSERT INTO `subject` (`id`, `name`, `icon`, `description`, `theme_color`, `sort_order`) VALUES
(1, 'AI应用与智能体开发', '🤖', '围绕大模型应用、智能体编排与业务落地，训练可交付的 AI 应用能力', '#2563EB', 1),
(2, 'AI大模型开发', '🧠', '大模型原理、微调、推理优化与工程化实践', '#7C3AED', 2),
(3, 'AI具身智能机器人开发', '🦾', '具身智能、感知决策与机器人控制', '#059669', 3),
(4, 'AI嵌入式_机器人开发', '🔧', '嵌入式系统、传感器与机器人硬件开发', '#D97706', 4),
(5, 'AI测试', '🧪', 'AI 产品质量保障、自动化测试与评测体系', '#DC2626', 5),
(6, 'AI运维', '🛠️', 'AI 基础设施、模型服务运维与稳定性保障', '#0891B2', 6),
(7, 'AI视觉设计', '🎨', 'AIGC 视觉创作、图像生成与设计工作流', '#DB2777', 7),
(8, 'AI视频与短剧创作', '🎬', 'AI 视频生成、分镜与短剧内容创作', '#EA580C', 8);

-- 3 个预置角色
INSERT INTO `role` (`id`, `name`, `code`, `description`) VALUES
(1, '超级管理员', 'super_admin', '系统内置角色，拥有全部权限'),
(2, '内容审核员', 'content_reviewer', '查看内容并审核题目'),
(3, '运营人员', 'operator', '除系统管理与题目审核外的日常运营权限');

-- 权限树：9 组 + 34 项操作点（D4-5 定稿）
INSERT INTO `permission` (`id`, `parent_id`, `name`, `code`, `type`, `sort_order`) VALUES
(1, NULL, '题库管理', 'question:group', 1, 1),
(2, NULL, '面试题库', 'interviewbank:group', 1, 2),
(3, NULL, '学习路线', 'route:group', 1, 3),
(4, NULL, '学员管理', 'student:group', 1, 4),
(5, NULL, '用户管理', 'user:group', 1, 5),
(6, NULL, '系统管理', 'system:group', 1, 6),
(7, NULL, '学科管理', 'subject:group', 1, 7),
(8, NULL, '课程库', 'course:group', 1, 8),
(9, NULL, '数据看板', 'dashboard:group', 1, 9),

(11, 1, '查看', 'question:view', 2, 1),
(12, 1, '新增', 'question:add', 2, 2),
(13, 1, '编辑', 'question:edit', 2, 3),
(14, 1, '删除', 'question:delete', 2, 4),
(15, 1, '导入', 'question:import', 2, 5),
(16, 1, '导出', 'question:export', 2, 6),
(17, 1, '审核', 'question:audit', 2, 7),

(21, 2, '查看', 'interviewbank:view', 2, 1),
(22, 2, '文档解析上传', 'interviewbank:upload', 2, 2),
(23, 2, '编辑/导入', 'interviewbank:edit', 2, 3),
(24, 2, '删除', 'interviewbank:delete', 2, 4),
(25, 2, '检索测试', 'interviewbank:search_test', 2, 5),

(41, 3, '查看', 'route:view', 2, 1),
(42, 3, '新增', 'route:add', 2, 2),
(43, 3, '编辑/发布', 'route:edit', 2, 3),
(44, 3, '删除', 'route:delete', 2, 4),

(51, 4, '查看/导出', 'student:view', 2, 1),
(52, 4, '编辑/禁用', 'student:edit', 2, 2),
(53, 4, '删除', 'student:delete', 2, 3),

(31, 5, '查看', 'user:view', 2, 1),
(32, 5, '新增', 'user:add', 2, 2),
(33, 5, '编辑/重置密码', 'user:edit', 2, 3),
(34, 5, '启用/禁用及删除', 'user:disable', 2, 4),

(61, 6, '操作日志', 'system:log_view', 2, 1),
(62, 6, '系统设置', 'system:setting', 2, 2),

(71, 7, '查看', 'subject:view', 2, 1),
(72, 7, '新增（含阶段）', 'subject:add', 2, 2),
(73, 7, '编辑/排序/阶段启停', 'subject:edit', 2, 3),
(74, 7, '删除', 'subject:delete', 2, 4),

(75, 8, '查看', 'course:view', 2, 1),
(76, 8, '新增/导入', 'course:add', 2, 2),
(77, 8, '编辑', 'course:edit', 2, 3),
(78, 8, '删除', 'course:delete', 2, 4),

(79, 9, '查看', 'dashboard:view', 2, 1);

-- 超级管理员：全部权限
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9),
(1, 11), (1, 12), (1, 13), (1, 14), (1, 15), (1, 16), (1, 17),
(1, 21), (1, 22), (1, 23), (1, 24), (1, 25),
(1, 31), (1, 32), (1, 33), (1, 34),
(1, 41), (1, 42), (1, 43), (1, 44),
(1, 51), (1, 52), (1, 53),
(1, 61), (1, 62),
(1, 71), (1, 72), (1, 73), (1, 74),
(1, 75), (1, 76), (1, 77), (1, 78),
(1, 79);

-- 内容审核员：查看内容 + 题目审核
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES
(2, 1), (2, 2), (2, 3), (2, 7), (2, 8), (2, 9),
(2, 11), (2, 17), (2, 21), (2, 41), (2, 71), (2, 75), (2, 79);

-- 运营人员：除系统管理与题目审核外全部
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES
(3, 1), (3, 2), (3, 3), (3, 4), (3, 5), (3, 7), (3, 8), (3, 9),
(3, 11), (3, 12), (3, 13), (3, 14), (3, 15), (3, 16),
(3, 21), (3, 22), (3, 23), (3, 24), (3, 25),
(3, 31), (3, 32), (3, 33), (3, 34),
(3, 41), (3, 42), (3, 43), (3, 44),
(3, 51), (3, 52), (3, 53),
(3, 71), (3, 72), (3, 73), (3, 74),
(3, 75), (3, 76), (3, 77), (3, 78),
(3, 79);

-- 系统设置 4 项
INSERT INTO `system_setting` (`id`, `config_key`, `config_value`, `remark`) VALUES
(1, 'site_name', '云枢智学', '平台名称'),
(2, 'site_description', 'AI 赋能的智能面试训练平台', '平台简介'),
(3, 'logo_url', '', '平台 Logo 地址'),
(4, 'default_avatar', '', '默认头像地址');

-- 内置超级管理员：密码哈希为占位符，应用首次启动时按 app.default-password 初始化
INSERT INTO `admin_user` (`id`, `username`, `password_hash`, `mobile`, `email`, `role_id`, `status`, `is_builtin`) VALUES
(1, 'admin', 'CHANGE_ME_ON_STARTUP', '13800000000', 'admin@yunshu.local', 1, 1, 1);
