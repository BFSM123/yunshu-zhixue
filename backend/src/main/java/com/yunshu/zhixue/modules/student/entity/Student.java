package com.yunshu.zhixue.modules.student.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学员（student）。学员账号由学员端注册产生，管理端只读资料并提供启禁用/删除。
 */
@Data
@TableName("student")
public class Student {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String mobile;

    private String passwordHash;

    private String nickname;

    private String avatar;

    private String email;

    /** 0 保密 / 1 男 / 2 女。 */
    private Integer gender;

    private Long preferredSubjectId;

    /** 1 已看过新手引导 / 0 未看过。 */
    private Integer guideFinished;

    /** 1 已启用 / 0 已禁用。 */
    private Integer status;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleteFlag;
}