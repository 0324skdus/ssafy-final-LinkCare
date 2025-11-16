package com.ssafy.linkcare.health.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
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


    /**
     * Unix timestamp (초 단위)를 LocalDateTime으로 변환
     * @param timestamp Unix timestamp (초 단위)
     * @return LocalDateTime
     */
    public LocalDateTime toLocalDateTime(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                ZoneId.systemDefault()
        );
    }


    /**
     * 특정 날짜가 포함된 주의 시작 날짜 구하기 (월요일 기준)
     * @param date 기준 날짜
     * @return 해당 주의 월요일
     */
    public LocalDate getWeekStartDate(LocalDate date) {
        return date.with(DayOfWeek.MONDAY);
    }

    /**
     * 특정 날짜가 포함된 주의 끝 날짜 구하기 (일요일 기준)
     * @param date 기준 날짜
     * @return 해당 주의 일요일
     */
    public LocalDate getWeekEndDate(LocalDate date) {
        return date.with(DayOfWeek.SUNDAY);
    }

    /**
     * 특정 날짜가 포함된 주의 시작 timestamp 구하기 (월요일 00:00:00)
     * @param date 기준 날짜
     * @return 해당 주의 월요일 00:00:00 timestamp
     */
    public Long getWeekStartTimestamp(LocalDate date) {
        LocalDate weekStart = date.with(DayOfWeek.MONDAY);
        return weekStart.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
    }

    /**
     * 특정 날짜가 포함된 주의 끝 timestamp 구하기 (일요일 23:59:59)
     * @param date 기준 날짜
     * @return 해당 주의 일요일 23:59:59 timestamp
     */
    public Long getWeekEndTimestamp(LocalDate date) {
        LocalDate weekEnd = date.with(DayOfWeek.SUNDAY);
        return weekEnd.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    /**
     * Unix timestamp가 포함된 주의 시작 timestamp 구하기 (월요일 00:00:00)
     * @param timestamp Unix timestamp (초 단위)
     * @return 해당 주의 월요일 00:00:00 timestamp
     */
    public Long getWeekStartTimestamp(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        LocalDate date = toLocalDate(timestamp);
        return getWeekStartTimestamp(date);
    }

    /**
     * Unix timestamp가 포함된 주의 끝 timestamp 구하기 (일요일 23:59:59)
     * @param timestamp Unix timestamp (초 단위)
     * @return 해당 주의 일요일 23:59:59 timestamp
     */
    public Long getWeekEndTimestamp(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        LocalDate date = toLocalDate(timestamp);
        return getWeekEndTimestamp(date);
    }

    /**
     * 특정 날짜가 포함된 주의 모든 날짜 리스트 구하기 (월요일~일요일)
     * @param date 기준 날짜
     * @return 해당 주의 모든 날짜 리스트 (월요일부터 일요일까지)
     */
    public List<LocalDate> getWeekDates(LocalDate date) {
        LocalDate weekStart = date.with(DayOfWeek.MONDAY);
        List<LocalDate> weekDates = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            weekDates.add(weekStart.plusDays(i));
        }

        return weekDates;
    }

    /**
     * 특정 날짜가 그 주의 몇 번째 날인지 구하기 (1=월요일, 7=일요일)
     * @param date 기준 날짜
     * @return 주의 몇 번째 날 (1~7)
     */
    public int getDayOfWeekNumber(LocalDate date) {
        return date.getDayOfWeek().getValue();
    }

    /**
     * 특정 년월의 주차 정보 구하기
     * @param year 년도
     * @param month 월 (1~12)
     * @return 해당 월의 주차 정보 리스트 (각 주의 시작일과 끝일)
     */
    public List<WeekRange> getWeeksInMonth(int year, int month) {
        List<WeekRange> weeks = new ArrayList<>();

        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
        LocalDate lastDayOfMonth = firstDayOfMonth.withDayOfMonth(
                firstDayOfMonth.lengthOfMonth()
        );

        LocalDate currentWeekStart = firstDayOfMonth.with(DayOfWeek.MONDAY);

        while (!currentWeekStart.isAfter(lastDayOfMonth)) {
            LocalDate currentWeekEnd = currentWeekStart.with(DayOfWeek.SUNDAY);

            // 해당 주가 해당 월과 겹치는 부분만 추가
            LocalDate weekStartInMonth = currentWeekStart.isBefore(firstDayOfMonth)
                    ? firstDayOfMonth : currentWeekStart;
            LocalDate weekEndInMonth = currentWeekEnd.isAfter(lastDayOfMonth)
                    ? lastDayOfMonth : currentWeekEnd;

            weeks.add(new WeekRange(weekStartInMonth, weekEndInMonth));

            currentWeekStart = currentWeekStart.plusWeeks(1);
        }

        return weeks;
    }

    /**
     * Unix timestamp가 유효한 범위인지 확인
     * @param timestamp Unix timestamp (초 단위)
     * @return 유효 여부
     */
    public boolean isValidTimestamp(Long timestamp) {
        if (timestamp == null || timestamp < 0) {
            return false;
        }
        // 2100년을 최대값으로 설정 (4102444800초)
        return timestamp <= 4102444800L;
    }

    /**
     * 두 timestamp 사이의 일수 차이 계산
     * @param startTimestamp 시작 timestamp
     * @param endTimestamp 종료 timestamp
     * @return 일수 차이
     */
    public long getDaysBetween(Long startTimestamp, Long endTimestamp) {
        if (startTimestamp == null || endTimestamp == null) {
            return 0;
        }
        LocalDate startDate = toLocalDate(startTimestamp);
        LocalDate endDate = toLocalDate(endTimestamp);
        return Duration.between(startDate.atStartOfDay(), endDate.atStartOfDay()).toDays();
    }

    /**
     * 두 timestamp 사이의 시간 차이 계산 (초 단위)
     * @param startTimestamp 시작 timestamp
     * @param endTimestamp 종료 timestamp
     * @return 초 단위 차이
     */
    public long getSecondsBetween(Long startTimestamp, Long endTimestamp) {
        if (startTimestamp == null || endTimestamp == null) {
            return 0;
        }
        return endTimestamp - startTimestamp;
    }

    /**
     * Unix timestamp (초 단위)를 LocalDate로 변환
     * @param timestamp Unix timestamp (초 단위)
     * @return LocalDate
     */
    public LocalDate toLocalDate(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                ZoneId.systemDefault()
        ).toLocalDate();
    }

    /**
     * LocalDate를 Unix timestamp (초 단위)로 변환 (00:00:00 시작 시간)
     * @param localDate LocalDate
     * @return Unix timestamp (초 단위)
     */
    public Long toTimestamp(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return localDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
    }

    /**
     * LocalDateTime을 Unix timestamp (초 단위)로 변환 (00:00:00 시작 시간)
     * @param localDateTime LocalDateTime
     * @return Unix timestamp (초 단위)
     */
    public Long toTimeStamp(LocalDateTime localDateTime) {
        if(localDateTime == null) {
            return null;
        }
        return toTimestamp(localDateTime.toLocalDate());
    }


    /**
     * 주차 범위를 나타내는 내부 클래스
     */
    @Getter
    @AllArgsConstructor
    public static class WeekRange {
        private LocalDate startDate;
        private LocalDate endDate;

        public Long getStartTimestamp() {
            return startDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
        }

        public Long getEndTimestamp() {
            return endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toEpochSecond();
        }

        @Override
        public String toString() {
            return startDate + " ~ " + endDate;
        }
    }

    /**
     * 현재 날짜 기준 지난주 월요일 날짜 반환
     * @return 지난주 월요일 LocalDate
     */
    public LocalDate getLastWeekMonday() {
        LocalDate today = LocalDate.now();
        LocalDate thisMonday = today.with(DayOfWeek.MONDAY);
        return thisMonday.minusWeeks(1);
    }

    public LocalDate getLastWeekMonday(LocalDate date) {
        LocalDate today = LocalDate.now();
        LocalDate thisMonday = date.with(DayOfWeek.MONDAY);
        return thisMonday.minusWeeks(1);
    }



    /**
     * 현재 날짜 기준 지난주 일요일 날짜 반환
     * @return 지난주 일요일 LocalDate
     */
    public LocalDate getLastWeekSunday() {
        LocalDate today = LocalDate.now();
        LocalDate thisMonday = today.with(DayOfWeek.MONDAY);
        return thisMonday.minusDays(1);
    }

    public LocalDate getLastWeekSunday(LocalDate date) {
        LocalDate thisMonday = date.with(DayOfWeek.MONDAY);
        return thisMonday.minusDays(1);
    }

    /**
     * 주어진 timestamp(초 단위)에 하루(24시간)를 더한 timestamp 반환
     */
    public Long plusOneDay(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        // 24시간 = 86400초
        return timestamp + 86400L;
    }

    /**
     * LocalDate 기준으로 다음날 00시 timestamp 반환
     */
    public Long toNextDayTimestamp(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return localDate.plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toEpochSecond();
    }
}
