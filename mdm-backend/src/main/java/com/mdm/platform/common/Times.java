package com.mdm.platform.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时间工具：SQLite 以 ISO-8601 文本存储时间。
 */
public final class Times {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Times() {
    }

    /** 当前时间的 ISO-8601 文本。 */
    public static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }

    /** 格式化。 */
    public static String format(LocalDateTime time) {
        return time == null ? null : time.format(FORMATTER);
    }
}
