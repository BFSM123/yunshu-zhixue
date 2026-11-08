package com.yunshu.zhixue.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

/**
 * 题目状态：1 已上架 / 2 已下架 / 3 待审核 / 4 草稿 / 5 已驳回。
 *
 * <p>4 草稿为 DB 预留值，当前不启用：接口入参与筛选均不接受该值。</p>
 */
@Getter
@RequiredArgsConstructor
public enum QuestionStatus {

    PUBLISHED(1, "已上架"),
    OFFLINE(2, "已下架"),
    PENDING(3, "待审核"),
    DRAFT(4, "草稿"),
    REJECTED(5, "已驳回");

    private final int code;
    private final String label;

    public static Optional<QuestionStatus> of(int code) {
        return Arrays.stream(values()).filter(v -> v.code == code).findFirst();
    }
}
