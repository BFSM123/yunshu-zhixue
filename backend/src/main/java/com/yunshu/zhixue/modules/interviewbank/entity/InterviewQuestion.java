package com.yunshu.zhixue.modules.interviewbank.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试题（interview_question），按方向（=学科）归属，供学员端 AI 面试出题。
 */
@Data
@TableName("interview_question")
public class InterviewQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 方向 -> subject.id。 */
    private Long subjectId;

    private String content;

    /** 参考答案，可留空。 */
    private String referenceAnswer;

    /** 主题，NULL 表示未分类。 */
    private String topic;

    /** 1 简单 / 2 中等 / 3 困难。 */
    private Integer difficulty;

    /** 逗号分隔。 */
    private String tags;

    /** 1 文档解析 / 2 手工录入 / 3 手动导入。 */
    private Integer sourceType;

    /** 来源文档名（文件不入库，仅留名）。 */
    private String sourceDocName;

    /** 文档内第 N 题。 */
    private Integer sourceSeq;

    /** 1 已上架 / 2 已下架。 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleteFlag;
}