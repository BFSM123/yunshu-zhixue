package com.yunshu.zhixue.common.constant;

/**
 * 业务固定常量（第一阶段 C5 / B2 已确认：阈值不做后台可配置）。
 */
public final class AppConstants {

    private AppConstants() {
    }

    /** AI 面试每日配额（次），按 Asia/Shanghai 自然日重置。 */
    public static final int INTERVIEW_DAILY_QUOTA = 5;

    /** 面试默认题量。 */
    public static final int INTERVIEW_DEFAULT_QUESTION_COUNT = 10;

    /** 面试单题回答超时（分钟）。 */
    public static final int INTERVIEW_QUESTION_TIMEOUT_MINUTES = 5;

    /** 简答 AI 批改通过分。 */
    public static final int SHORT_ANSWER_PASS_SCORE = 6;

    /** 错题连续答对多少次自动标记已掌握。 */
    public static final int MASTERED_CORRECT_STREAK = 3;

    /** 列表分页默认页码。 */
    public static final long DEFAULT_PAGE = 1;

    /** 列表分页默认页大小。 */
    public static final long DEFAULT_PAGE_SIZE = 10;

    /** 列表分页最大页大小。 */
    public static final long MAX_PAGE_SIZE = 100;

    /** 批量导入单次最大行数。 */
    public static final int MAX_IMPORT_ROWS = 500;

    /** 同步导出单次最大行数。 */
    public static final int MAX_EXPORT_ROWS = 10_000;

    /** 面试分数满分。 */
    public static final int MAX_SCORE = 10;

    /** 连续密码错误达到该次数后锁定账号（次）。 */
    public static final int LOGIN_FAIL_LIMIT = 5;

    /** 登录锁定持续时间（分钟）。 */
    public static final int LOGIN_LOCK_MINUTES = 5;
}
