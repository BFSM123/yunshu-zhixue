package com.yunshu.zhixue.modules.interviewbank.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试题主题词表（interview_topic）。subject_id 为空表示全局主题。
 */
@Data
@TableName("interview_topic")
public class InterviewTopic {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联学科；NULL=全局主题。 */
    private Long subjectId;

    private String name;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleteFlag;
}