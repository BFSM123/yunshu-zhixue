package com.yunshu.zhixue.modules.question.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 题库导出/预览表格行（A-Q15）。导出列同时包含学科与状态，便于核对。
 */
@Data
public class QuestionExportRow {

    @ExcelProperty("序号")
    private Integer seq;

    @ExcelProperty("题干")
    private String content;

    @ExcelProperty("学科")
    private String subjectName;

    @ExcelProperty("阶段")
    private String stageName;

    @ExcelProperty("题型")
    private String type;

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

    @ExcelProperty("状态")
    private String status;
}
