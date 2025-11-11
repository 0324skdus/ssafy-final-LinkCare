package com.ssafy.linkcare.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivitySummaryStaticsResponse {

    private LocalDate startDate;
    private LocalDate endDate;
    private Double totalCalories;      // 총 소모 칼로리
    private Double totalDistance;      // 총 이동 거리
    private Double avgCalories;        // 평균 소모 칼로리
    private Double avgDistance;        // 평균 이동 거리

    @Override
    public String toString() {
        return "ActivitySummaryStaticsResponse{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                ", totalCalories=" + totalCalories +
                ", totalDistance=" + totalDistance +
                ", avgCalories=" + avgCalories +
                ", avgDistance=" + avgDistance +
                '}';
    }
}
