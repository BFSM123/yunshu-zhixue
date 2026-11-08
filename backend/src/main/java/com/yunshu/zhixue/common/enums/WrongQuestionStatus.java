package com.yunshu.zhixue.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

/** 错题状态：1 待复习 / 2 已掌握。 */
@Getter
@RequiredArgsConstructor
public enum WrongQuestionStatus {

    TO_REVIEW(1, "待复习"),
    MASTERED(2, "已掌握");

    private final int code;
    private final String label;

    public static Optional<WrongQuestionStatus> of(int code) {
        return Arrays.stream(values()).filter(v -> v.code == code).findFirst();
    }
}
