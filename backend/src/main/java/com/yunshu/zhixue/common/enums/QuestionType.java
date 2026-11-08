package com.yunshu.zhixue.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

/** 题目类型：1 单选 / 2 多选 / 3 判断 / 4 简答。 */
@Getter
@RequiredArgsConstructor
public enum QuestionType {

    SINGLE(1, "单选"),
    MULTIPLE(2, "多选"),
    JUDGE(3, "判断"),
    SHORT_ANSWER(4, "简答");

    private final int code;
    private final String label;

    public static Optional<QuestionType> of(int code) {
        return Arrays.stream(values()).filter(v -> v.code == code).findFirst();
    }

    /** 客观题：服务端可本地判分，不需要 AI。 */
    public boolean isObjective() {
        return this != SHORT_ANSWER;
    }
}
