package com.yunshu.zhixue.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

/** 性别：0 保密 / 1 男 / 2 女。 */
@Getter
@RequiredArgsConstructor
public enum Gender {

    UNKNOWN(0, "保密"),
    MALE(1, "男"),
    FEMALE(2, "女");

    private final int code;
    private final String label;

    public static Optional<Gender> of(int code) {
        return Arrays.stream(values()).filter(v -> v.code == code).findFirst();
    }
}
