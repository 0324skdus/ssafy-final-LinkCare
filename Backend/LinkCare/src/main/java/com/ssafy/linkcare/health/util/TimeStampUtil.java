package com.ssafy.linkcare.health.util;

import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class TimeStampUtil {
    public Timestamp parseTimestamp(String dateTimeStr) {
        if (dateTimeStr == null) {
            return null;
        }

        try {
            // ISO 8601 형식: "2025-10-22T15:13:03.814"
            LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr);
            return Timestamp.valueOf(localDateTime);
        } catch (DateTimeParseException e) {
            try {
                // 공백 형식: "2025-10-22 15:13:03.814"
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
                LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr, formatter);
                return Timestamp.valueOf(localDateTime);
            } catch (DateTimeParseException ex) {
                // 밀리초 없는 형식: "2025-10-22 15:13:03"
                DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr, formatter2);
                return Timestamp.valueOf(localDateTime);
            }
        }
    }

    public Long getCurrentTimestamp() {
        return Instant.now().getEpochSecond();
    }

    public Long getThreeWeeksAgoTimestamp() {
        return Instant.now().minus(Duration.ofDays(21)).getEpochSecond();
    }

    public Long getDaysAgoTimestamp(int days) {
        return LocalDate.now()
                .minusDays(days - 1)
                .atStartOfDay(ZoneId.systemDefault())
                .toEpochSecond();
    }

    public Long getTodayStartTimestamp() {
        return LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toEpochSecond();
    }

    public Long getThisWeekStartTimestamp() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        return weekStart.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
    }

    public Long getThisMonthStartTimestamp() {
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        return monthStart.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
    }

    public Long getDateStartTimestamp(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
    }

    public Long getDateEndTimestamp(LocalDate date) {
        return date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toEpochSecond();
    }
}
