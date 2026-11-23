package com.yunshu.zhixue.modules.student.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 学员列表导出 / 预览表格行（A-U08）。
 */
@Data
public class StudentExportRow {

    @ExcelProperty("序号")
    private Integer seq;

    @ExcelProperty("昵称")
    private String nickname;

    @ExcelProperty("手机号")
    private String mobile;

    @ExcelProperty("邮箱")
    private String email;

    @ExcelProperty("注册时间")
    private String createdAt;

    @ExcelProperty("学习天数")
    private Long studyDays;

    @ExcelProperty("刷题总数")
    private Long practiceCount;

    @ExcelProperty("正确率")
    private String accuracyRate;

    @ExcelProperty("已学课程")
    private Long completedCourseCount;

    @ExcelProperty("面试次数")
    private Long interviewCount;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("最后登录")
    private String lastLoginAt;
}