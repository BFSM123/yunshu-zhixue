package com.yunshu.zhixue.modules.question.service;

import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.question.dto.BatchAuditRequest;
import com.yunshu.zhixue.modules.question.dto.BatchIdsRequest;
import com.yunshu.zhixue.modules.question.dto.BatchQuestionStatusRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionAuditRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionDetailVO;
import com.yunshu.zhixue.modules.question.dto.QuestionImportPreviewVO;
import com.yunshu.zhixue.modules.question.dto.QuestionImportResultVO;
import com.yunshu.zhixue.modules.question.dto.QuestionListItemVO;
import com.yunshu.zhixue.modules.question.dto.QuestionSaveRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionStatsVO;
import com.yunshu.zhixue.modules.question.dto.QuestionStatusRequest;
import com.yunshu.zhixue.modules.system.dto.Actor;
import org.springframework.web.multipart.MultipartFile;

/**
 * 题库管理（A-Q01 ~ A-Q15）。
 */
public interface QuestionService {

    QuestionStatsVO stats();

    PageResult<QuestionListItemVO> page(String keyword, Long subjectId, Long stageId,
                                        Integer type, Integer difficulty, Integer status,
                                        long page, long pageSize);

    QuestionDetailVO detail(Long id);

    void create(QuestionSaveRequest request, Actor actor);

    void update(Long id, QuestionSaveRequest request, Actor actor);

    void delete(Long id, Actor actor);

    void audit(Long id, QuestionAuditRequest request, Actor actor);

    void updateStatus(Long id, QuestionStatusRequest request, Actor actor);

    void resubmit(Long id, Actor actor);

    int batchUpdateStatus(BatchQuestionStatusRequest request, Actor actor);

    int batchDelete(BatchIdsRequest request, Actor actor);

    int batchAudit(BatchAuditRequest request, Actor actor);

    QuestionImportPreviewVO previewImport(MultipartFile file, Actor actor);

    QuestionImportResultVO confirmImport(String token, Actor actor);

    byte[] template();

    byte[] export(String keyword, Long subjectId, Long stageId, Integer type,
                  Integer difficulty, Integer status, Actor actor);
}
