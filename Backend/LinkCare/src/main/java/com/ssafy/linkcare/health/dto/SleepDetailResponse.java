package com.ssafy.linkcare.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SleepDetailResponse {
    private Integer userSeq;
    private List<Long> dailySleepMinutes;  // 기간 내 일별 수면시간
    private Long averageSleepMinutes;      // 평균
    private Long totalSleepMinutes;        // 총합 (정렬 기준)
}
