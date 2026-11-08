package com.yunshu.zhixue.modules.course.dto;

import java.util.List;

/**
 * 批量导入解析结果（A-C06）。status：valid 可导入 / warning 可导入但需留意 /
 * invalid 不可导入；errors 为原因说明。
 */
public record CourseImportPreviewVO(
        String token,
        int totalRows,
        int validCount,
        int warningCount,
        int invalidCount,
        List<Row> rows
) {

    public record Row(
            int row,
            String name,
            Integer durationMinutes,
            String subjectName,
            Long subjectId,
            List<String> extraSubjectNames,
            Integer source,
            String link,
            String coverUrl,
            String bvid,
            String status,
            List<String> errors
    ) {
    }
}
