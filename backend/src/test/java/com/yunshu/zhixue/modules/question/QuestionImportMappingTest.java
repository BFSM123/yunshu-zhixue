package com.yunshu.zhixue.modules.question;

import com.alibaba.excel.EasyExcel;
import com.yunshu.zhixue.modules.question.dto.QuestionImportExcelRow;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 校验三份真实题库 Excel 能否按表头正确映射到导入 DTO。
 * 文件不存在时跳过，避免在无 data 目录的环境中失败。
 */
class QuestionImportMappingTest {

    private static final Path DATA_DIR = Path.of("..", "data");

    @Test
    void readsSingleChoiceExcel() {
        List<QuestionImportExcelRow> rows =
                read("题库管理-测试数据(单选).xlsx");
        Assumptions.assumeTrue(rows != null);

        assertEquals(77, rows.size());
        QuestionImportExcelRow first = rows.get(0);
        assertFalse(first.getContent().isBlank());
        assertEquals("AI应用与智能体开发", first.getSubject());
        assertEquals("AI工程化-基础与项目实战", first.getStage());
        assertEquals("单选", first.getType());
        assertEquals("简单", first.getDifficulty());
        assertNotNull(first.getOptionA());
        assertNotNull(first.getOptionD());
        assertNull(first.getOptionE());
        assertNotNull(first.getAnswer());
        assertNotNull(first.getTags());
    }

    @Test
    void readsMultipleChoiceExcelWithEAndFOptions() {
        List<QuestionImportExcelRow> rows =
                read("题库管理-测试数据(多选).xlsx");
        Assumptions.assumeTrue(rows != null);

        assertEquals(38, rows.size());
        assertEquals("AI应用与智能体开发", rows.get(0).getSubject());
        assertEquals("多选", rows.get(0).getType());
    }

    @Test
    void readsShortAnswerExcel() {
        List<QuestionImportExcelRow> rows =
                read("题库管理-测试数据(简答).xlsx");
        Assumptions.assumeTrue(rows != null);

        assertEquals(198, rows.size());
        QuestionImportExcelRow first = rows.get(0);
        assertEquals("AI应用与智能体开发", first.getSubject());
        assertEquals("简答题", first.getType());
        assertNull(first.getOptionA());
        assertNotNull(first.getAnswer());
    }

    private List<QuestionImportExcelRow> read(String filename) {
        Path path = DATA_DIR.resolve(filename);
        Assumptions.assumeTrue(Files.exists(path), "跳过：未找到 " + path);
        return EasyExcel.read(path.toFile())
                .head(QuestionImportExcelRow.class)
                .sheet()
                .headRowNumber(1)
                .doReadSync();
    }
}
