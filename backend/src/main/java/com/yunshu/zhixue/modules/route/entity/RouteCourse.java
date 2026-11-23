package com.yunshu.zhixue.modules.route.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 路线-课程关联（route_course），sort_order 即学习顺序。
 */
@Data
@TableName("route_course")
public class RouteCourse {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long routeId;

    private Long courseId;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleteFlag;
}
