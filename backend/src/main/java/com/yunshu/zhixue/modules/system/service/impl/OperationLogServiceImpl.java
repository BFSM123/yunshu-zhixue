package com.yunshu.zhixue.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.enums.OperationAction;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.common.util.TimeUtils;
import com.yunshu.zhixue.modules.system.dto.Actor;
import com.yunshu.zhixue.modules.system.dto.OperationLogVO;
import com.yunshu.zhixue.modules.system.entity.OperationLog;
import com.yunshu.zhixue.modules.system.mapper.OperationLogMapper;
import com.yunshu.zhixue.modules.system.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 操作日志落库、查询、导出与清空。审计失败不阻断业务主流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String TARGET_TYPE = "操作日志";

    private final JdbcTemplate jdbcTemplate;
    private final OperationLogMapper operationLogMapper;

    @Override
    public void record(Long userId, String username, String action,
                       String targetType, String targetName, String ip) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO operation_log
                        (user_id, username, action, target_type, target_name, ip, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    userId, username, action, targetType, targetName, ip,
                    LocalDateTime.now(ZoneOffset.UTC));
        } catch (DataAccessException ex) {
            log.warn("操作日志写入失败：{}", ex.getMostSpecificCause().getMessage());
        }
    }

    @Override
    public PageResult<OperationLogVO> page(String keyword, String operationType,
                                           LocalDate startDate, LocalDate endDate,
                                           long page, long pageSize) {
        long pageNo = Math.max(1, page);
        long size = Math.min(Math.max(1, pageSize), AppConstants.MAX_PAGE_SIZE);
        Page<OperationLog> result = operationLogMapper.selectPage(
                new Page<>(pageNo, size),
                buildQuery(keyword, operationType, startDate, endDate));
        List<OperationLogVO> list = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public OperationLogVO detail(Long id) {
        OperationLog logRow = operationLogMapper.selectById(id);
        if (logRow == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "日志不存在");
        }
        return toVO(logRow);
    }

    @Override
    public List<OperationLogVO> recentByUser(Long userId, int limit) {
        int size = Math.min(Math.max(1, limit), 50);
        Page<OperationLog> result = operationLogMapper.selectPage(
                new Page<>(1, size),
                Wrappers.lambdaQuery(OperationLog.class)
                        .eq(OperationLog::getUserId, userId)
                        .orderByDesc(OperationLog::getCreatedAt)
                        .orderByDesc(OperationLog::getId));
        return result.getRecords().stream().map(this::toVO).toList();
    }

    @Override
    public byte[] exportCsv(String keyword, String operationType,
                            LocalDate startDate, LocalDate endDate, Actor actor) {
        LambdaQueryWrapper<OperationLog> query = buildQuery(keyword, operationType, startDate, endDate);
        query.last("LIMIT " + AppConstants.MAX_EXPORT_ROWS);
        List<OperationLog> rows = operationLogMapper.selectList(query);

        StringBuilder csv = new StringBuilder("﻿");
        csv.append("日志ID,用户,操作,对象类型,对象,IP地址,操作时间\n");
        for (OperationLog row : rows) {
            csv.append(csvField(row.getId() == null ? "" : String.valueOf(row.getId()))).append(',')
                    .append(csvField(row.getUsername())).append(',')
                    .append(csvField(row.getAction())).append(',')
                    .append(csvField(row.getTargetType())).append(',')
                    .append(csvField(row.getTargetName())).append(',')
                    .append(csvField(row.getIp())).append(',')
                    .append(csvField(formatTime(row.getCreatedAt()))).append('\n');
        }

        record(actor.userId(), actor.username(), OperationAction.EXPORT.getLabel(),
                TARGET_TYPE, "导出日志", actor.ip());
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    @Transactional
    public void clearAll(Actor actor) {
        operationLogMapper.deleteAll();
        record(actor.userId(), actor.username(), OperationAction.DELETE.getLabel(),
                TARGET_TYPE, "清空日志", actor.ip());
    }

    private LambdaQueryWrapper<OperationLog> buildQuery(String keyword, String operationType,
                                                        LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<OperationLog> wrapper = Wrappers.lambdaQuery();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(OperationLog::getUsername, kw)
                    .or()
                    .like(OperationLog::getAction, kw)
                    .or()
                    .like(OperationLog::getTargetName, kw));
        }
        if (operationType != null && !operationType.isBlank()) {
            OperationAction action = OperationAction.of(operationType)
                    .orElseThrow(() -> new BizException(ErrorCode.VALIDATION_FAILED, "操作类型不合法"));
            wrapper.eq(OperationLog::getAction, action.getLabel());
        }
        if (startDate != null) {
            wrapper.ge(OperationLog::getCreatedAt, TimeUtils.startOfShanghaiDayUtc(startDate));
        }
        if (endDate != null) {
            wrapper.lt(OperationLog::getCreatedAt, TimeUtils.startOfNextShanghaiDayUtc(endDate));
        }
        wrapper.orderByDesc(OperationLog::getCreatedAt).orderByDesc(OperationLog::getId);
        return wrapper;
    }

    private OperationLogVO toVO(OperationLog row) {
        return new OperationLogVO(row.getId(), row.getUserId(), row.getUsername(), row.getAction(),
                row.getTargetType(), row.getTargetName(), row.getIp(),
                TimeUtils.toShanghai(row.getCreatedAt()));
    }

    private String formatTime(LocalDateTime utc) {
        LocalDateTime shanghai = TimeUtils.toShanghai(utc);
        return shanghai == null ? "" : CSV_TIME.format(shanghai);
    }

    private String csvField(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
