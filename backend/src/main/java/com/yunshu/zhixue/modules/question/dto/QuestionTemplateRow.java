package com.yunshu.zhixue.modules.question.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 导入模板表头行（A-Q14）。
 */
@Data
public class QuestionTemplateRow {

    @ExcelProperty("题干")
    private String content;

    @ExcelProperty("学科")
    private String subject;

    @ExcelProperty("阶段")
    private String stage;

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
}
