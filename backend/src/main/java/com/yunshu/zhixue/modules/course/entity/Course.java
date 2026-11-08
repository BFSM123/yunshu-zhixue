package com.yunshu.zhixue.modules.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * B 站课程库（course）。主学科可为空，空即「其他学科」。
 */
@Data
@TableName("course")
public class Course {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String coverUrl;

    private Integer durationMinutes;

    /** 1 B站 / 2 其他。 */
    private Integer source;

    private String link;

    /** BV 号，由 B 站课程链接解析，非 B 站或无法解析时为空。 */
    private String bvid;

    /** 主学科；NULL 表示「其他学科」。 */
    private Long subjectId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleteFlag;
}
