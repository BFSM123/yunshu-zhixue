package com.yunshu.zhixue.common.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时区工具：数据库统一 UTC 存储，业务日界按 Asia/Shanghai 0 点（D17）。
 */
public final class TimeUtils {

    public static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    public static final ZoneId UTC = ZoneOffset.UTC;

    private static final DateTimeFormatter DAY_KEY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private TimeUtils() {
    }

    /** 当前 UTC 时间（用于写库）。 */
    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(UTC);
    }

    /** 上海日期键，如 20261107，用于 Redis 每日配额/每日上限。 */
    public static String shanghaiDayKey() {
        return ZonedDateTime.now(SHANGHAI).format(DAY_KEY);
    }

    /** 距离上海次日 0 点的剩余秒数，用于 Redis TTL。 */
    public static long secondsUntilNextShanghaiMidnight() {
        ZonedDateTime now = ZonedDateTime.now(SHANGHAI);
        ZonedDateTime next = now.toLocalDate().plusDays(1).atStartOfDay(SHANGHAI);
        return Math.max(1, Duration.between(now, next).getSeconds());
    }

    /** 指定上海日期 0 点对应的 UTC 时刻。 */
    public static LocalDateTime startOfShanghaiDayUtc(LocalDate shanghaiDate) {
        return shanghaiDate.atStartOfDay(SHANGHAI)
                .withZoneSameInstant(UTC)
                .toLocalDateTime();
    }

    /** 指定上海日期次日 0 点对应的 UTC 时刻（结束日期开区间）。 */
    public static LocalDateTime startOfNextShanghaiDayUtc(LocalDate shanghaiDate) {
        return shanghaiDate.plusDays(1).atStartOfDay(SHANGHAI)
                .withZoneSameInstant(UTC)
                .toLocalDateTime();
    }

    /** UTC LocalDateTime → Asia/Shanghai 展示时间。 */
    public static LocalDateTime toShanghai(LocalDateTime utc) {
        if (utc == null) {
            return null;
        }
        return utc.atZone(UTC).withZoneSameInstant(SHANGHAI).toLocalDateTime();
    }
}
