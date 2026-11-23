package com.yunshu.zhixue.modules.student.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 面试记录查询行，Service 层转换为 {@link StudentInterviewVO}（UTC → 上海时间）。
 */
@Data
public class StudentInterviewRow {

    private Long id;

    private Long subjectId;

    private String subjectName;

    private Integer difficulty;

    private Integer questionCount;

    private BigDecimal overallScore;

    private Integer totalDuration;

    private LocalDateTime completedAt;
}