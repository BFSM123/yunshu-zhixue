package com.yunshu.zhixue.modules.interviewbank.dto;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 面试题 Excel 导入行（按表头名称映射，列顺序不敏感）。
 * 表头：所属学科 | 题干 | 难度 | 主题 | 标签 | 参考答案
 */
@Data
public class InterviewImportExcelRow {

    /** Excel 实际行号（表头为第 1 行，数据从第 2 行开始）。 */
    @ExcelIgnore
    private Integer rowNo;

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