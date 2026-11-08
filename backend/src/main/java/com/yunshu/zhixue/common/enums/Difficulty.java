package com.yunshu.zhixue.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

/** 题目/面试难度：1 简单 / 2 中等 / 3 困难。 */
@Getter
@RequiredArgsConstructor
public enum Difficulty {

    EASY(1, "简单"),
    MEDIUM(2, "中等"),
    HARD(3, "困难");

    private final int code;
    private final String label;

    public static Optional<Difficulty> of(int code) {
        return Arrays.stream(values()).filter(v -> v.code == code).findFirst();
    }
}
