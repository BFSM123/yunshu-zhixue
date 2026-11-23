package com.yunshu.zhixue.modules.student.dto;

import lombok.Data;

/**
 * 刷题聚合行：学习天数 / 刷题总数 / 正确数。
 */
@Data
public class StudentPracticeStat {

    private Long studentId;

    private Long studyDays;

    private Long practiceCount;

    private Long correctCount;
}