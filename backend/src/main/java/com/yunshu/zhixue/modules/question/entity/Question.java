package com.yunshu.zhixue.modules.question.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题库题目（question），挂载在学科阶段下。
 */
@Data
@TableName("question")
public class Question {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long stageId;

    /** 1 单选 / 2 多选 / 3 判断 / 4 简答。 */
    private Integer type;

    /** 1 简单 / 2 中等 / 3 困难。 */
    private Integer difficulty;

    private String content;

    /** 判断/简答答案；单选多选为空（正确项在选项表）。 */
    private String answer;

    private String analysis;

    /** 逗号分隔。 */
    private String tags;

    /** 1 已上架 / 2 已下架 / 3 待审核 / 4 草稿(预留) / 5 已驳回。 */
    private Integer status;

    private String rejectReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleteFlag;
}
