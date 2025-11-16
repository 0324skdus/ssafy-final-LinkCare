package com.ssafy.linkcare.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TotalActivityStatisticsResponse {

    // 칼로리, 운동 시간, 걸음 수
    private Double totalCalories;
    private Long totalDuration;
    private Long totalSteps;

}
