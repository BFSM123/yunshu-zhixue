package com.yunshu.zhixue.common.response;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 统一分页结构：{@code {list, total, page, pageSize}}（04 文档 2.4）。
 */
public record PageResult<T>(List<T> list, long total, long page, long pageSize) {

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public static <T> PageResult<T> of(List<T> list, long total, long page, long pageSize) {
        return new PageResult<>(list, total, page, pageSize);
    }
}
