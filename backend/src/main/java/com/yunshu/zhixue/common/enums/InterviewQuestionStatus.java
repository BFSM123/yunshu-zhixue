package com.yunshu.zhixue.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

/** 面试题状态：1 已上架 / 2 已下架（AI 面试仅取已上架）。 */
@Getter
@RequiredArgsConstructor
public enum InterviewQuestionStatus {

    PUBLISHED(1, "已上架"),
    OFFLINE(2, "已下架");

    private final int code;
    private final String label;

    public static Optional<InterviewQuestionStatus> of(int code) {
        return Arrays.stream(values()).filter(v -> v.code == code).findFirst();
    }
}
