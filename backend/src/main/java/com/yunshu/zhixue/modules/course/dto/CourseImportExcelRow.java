package com.yunshu.zhixue.modules.course.dto;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 课程导入表格行（按表头名称映射，列顺序不敏感）。
 */
@Data
public class CourseImportExcelRow {

    /** Excel 实际行号（表头为第 1 行，数据从第 2 行开始）。 */
    @ExcelIgnore
    private Integer rowNo;

    @ExcelProperty("课程名称")
    private String name;

    @ExcelProperty("所属学科")
    private String subject;

    @ExcelProperty("来源")
    private String source;

    @ExcelProperty("时长（分钟）")
    private String durationMinutes;

    @ExcelProperty("BV号")
    private String bvid;

    @ExcelProperty("封面")
    private String coverUrl;

    @ExcelProperty("播放链接")
    private String link;
}
