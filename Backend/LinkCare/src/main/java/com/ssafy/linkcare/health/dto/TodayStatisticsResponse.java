package com.ssafy.linkcare.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodayStatisticsResponse {
    //걸음수, 운동거리, 운동시간, 칼로리, 평균 심박수, 수면시간, 음수량, 혈압(가장 최근)
    private int totalSteps;
    private Double totalDistances;
    private Long totalDuration;
    private Double totalCalories;
    private Double avgHeartRates;
    private Long sleepDuration;
    private Float totalWaterIntakes;
    private String lastBloodPressure;
}
