package com.yunshu.zhixue.modules.route.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习路线（learning_route），单学科归属。
 */
@Data
@TableName("learning_route")
public class LearningRoute {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Long subjectId;

    /** 1 启用 / 0 禁用。 */
    private Integer status;

    private String icon;

    private String targetEducation;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleteFlag;
}
