package com.yunshu.zhixue.modules.interviewbank;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewImportExcelRow;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验真实面试题 Excel 能按表头正确映射到导入 DTO。
 * 文件不存在时跳过，避免在无 data 目录的环境中失败。
 */
class InterviewImportMappingTest {

    private static final Path DATA_DIR = Path.of("..", "data");
    private static final String FILE_NAME = "AI应用与智能体开发-面试题.xlsx";

    @Test
    void readsInterviewQuestionExcel() throws Exception {
        Path file = DATA_DIR.resolve(FILE_NAME);
        Assumptions.assumeTrue(Files.exists(file), "缺少测试文件：" + file);

        List<InterviewImportExcelRow> rows = new ArrayList<>();
        List<Map<Integer, String>> heads = new ArrayList<>();
        try (InputStream inputStream = Files.newInputStream(file)) {
            EasyExcel.read(inputStream, InterviewImportExcelRow.class,
                    new AnalysisEventListener<InterviewImportExcelRow>() {
                        @Override
                        public void invokeHeadMap(Map<Integer, String> headMap,
                                                  AnalysisContext context) {
                            heads.add(headMap);
                        }

                        @Override
                        public void invoke(InterviewImportExcelRow data,
                                           AnalysisContext context) {
                            rows.add(data);
                        }

                        @Override
                        public void doAfterAllAnalysed(AnalysisContext context) {
                            // 无需后处理
                        }
                    }).sheet().headRowNumber(1).doRead();
        }

        assertEquals(1, heads.size());
        assertTrue(heads.get(0).containsValue("所属学科"));
        assertTrue(heads.get(0).containsValue("题干"));
        assertTrue(heads.get(0).containsValue("难度"));
        assertTrue(heads.get(0).containsValue("主题"));
        assertTrue(heads.get(0).containsValue("标签"));
        assertTrue(heads.get(0).containsValue("参考答案"));

        assertEquals(375, rows.size());
        InterviewImportExcelRow first = rows.get(0);
        assertEquals("AI应用与智能体开发", first.getSubject());
        assertEquals("ollama是什么，有什么作用？", first.getContent());
        assertEquals("中等", first.getDifficulty());
        assertEquals("大模型与本地部署", first.getTopic());
        assertNotNull(first.getTags());
        assertTrue(first.getTags().contains("Ollama"));
        assertNotNull(first.getReferenceAnswer());
        assertTrue(first.getReferenceAnswer().contains("Ollama"));
        assertFalse(first.getContent().isBlank());
    }
}