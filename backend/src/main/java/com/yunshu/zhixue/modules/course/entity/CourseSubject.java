package com.yunshu.zhixue.modules.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程-学科多学科关联（course_subject）。主学科存于 course.subject_id，
 * 本表只存额外关联学科。
 */
@Data
@TableName("course_subject")
public class CourseSubject {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private Long subjectId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleteFlag;
}
