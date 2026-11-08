package com.yunshu.zhixue.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

/** 面试题来源：1 知识库文档解析 / 2 手工录入 / 3 手动导入。 */
@Getter
@RequiredArgsConstructor
public enum InterviewQuestionSource {

    DOC_PARSE(1, "知识库文档解析"),
    MANUAL(2, "手工录入"),
    IMPORT(3, "手动导入");

    private final int code;
    private final String label;

    public static Optional<InterviewQuestionSource> of(int code) {
        return Arrays.stream(values()).filter(v -> v.code == code).findFirst();
    }
}
