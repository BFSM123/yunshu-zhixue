package com.yunshu.zhixue.modules.subject.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学科下的阶段（stage），题目挂载层级。
 */
@Data
@TableName("stage")
public class Stage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long subjectId;

    private String name;

    /** 允许更新为空（清空后前端展示默认图标）。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String icon;

    private Integer sortOrder;

    /** 1 启用 / 0 禁用。 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleteFlag;
}
