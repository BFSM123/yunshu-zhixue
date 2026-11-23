package com.yunshu.zhixue.modules.student.dto;

import lombok.Data;

/**
 * 按学员聚合的计数行（面试次数 / 已学课程数）。
 */
@Data
public class StudentCountStat {

    private Long studentId;

    private Long total;
}