package com.yunshu.zhixue.modules.interviewbank.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 面试题导入模板行。
 */
@Data
public class InterviewTemplateRow {

    @ExcelProperty("所属学科")
    private String subject;

    @ExcelProperty("题干")
    private String content;

    @ExcelProperty("难度")
    private String difficulty;

    @ExcelProperty("主题")
    private String topic;

    @ExcelProperty("标签")
    private String tags;

    @ExcelProperty("参考答案")
    private String referenceAnswer;
}