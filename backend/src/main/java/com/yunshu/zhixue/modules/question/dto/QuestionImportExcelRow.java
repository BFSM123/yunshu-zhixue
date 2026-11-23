package com.yunshu.zhixue.modules.question.dto;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 题库导入表格行（按表头名称映射，列顺序不敏感）。
 * 同时兼容「题干/题目」「题型/类型」两套表头。
 */
@Data
public class QuestionImportExcelRow {

    /** Excel 实际行号（表头为第 1 行，数据从第 2 行开始）。 */
    @ExcelIgnore
    private Integer rowNo;

    @ExcelProperty("题干")
    private String content;

    @ExcelProperty("题目")
    private String contentAlias;

    @ExcelProperty("学科")
    private String subject;

    @ExcelProperty("阶段")
    private String stage;

    @ExcelProperty("题型")
    private String type;

    @ExcelProperty("类型")
    private String typeAlias;

    @ExcelProperty("难度")
    private String difficulty;

    @ExcelProperty("选项A")
    private String optionA;

    @ExcelProperty("选项B")
    private String optionB;

    @ExcelProperty("选项C")
    private String optionC;

    @ExcelProperty("选项D")
    private String optionD;

    @ExcelProperty("选项E")
    private String optionE;

    @ExcelProperty("选项F")
    private String optionF;

    @ExcelProperty("正确答案")
    private String answer;

    @ExcelProperty("解析")
    private String analysis;

    @ExcelProperty("标签")
    private String tags;
}
