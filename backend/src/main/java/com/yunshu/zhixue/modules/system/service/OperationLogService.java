package com.yunshu.zhixue.modules.system.service;

import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.system.dto.Actor;
import com.yunshu.zhixue.modules.system.dto.OperationLogVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 操作日志写入与查询。
 */
public interface OperationLogService {

    void record(Long userId, String username, String action,
                String targetType, String targetName, String ip);

    PageResult<OperationLogVO> page(String keyword, String operationType,
                                    LocalDate startDate, LocalDate endDate,
                                    long page, long pageSize);

    OperationLogVO detail(Long id);

    List<OperationLogVO> recentByUser(Long userId, int limit);

    byte[] exportCsv(String keyword, String operationType,
                     LocalDate startDate, LocalDate endDate, Actor actor);

    void clearAll(Actor actor);
}
