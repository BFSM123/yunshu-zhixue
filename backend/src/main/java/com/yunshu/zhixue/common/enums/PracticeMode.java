package com.yunshu.zhixue.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

/** 练习模式：1 顺序 / 2 随机。 */
@Getter
@RequiredArgsConstructor
public enum PracticeMode {

    SEQUENTIAL(1, "顺序"),
    RANDOM(2, "随机");

    private final int code;
    private final String label;

    public static Optional<PracticeMode> of(int code) {
        return Arrays.stream(values()).filter(v -> v.code == code).findFirst();
    }
}
