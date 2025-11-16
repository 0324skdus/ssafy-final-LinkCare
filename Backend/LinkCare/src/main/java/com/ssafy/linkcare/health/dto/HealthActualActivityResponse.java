package com.ssafy.linkcare.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthActualActivityResponse {
    private Long totalDuration;
    private Float totalDistances;
    private Double totalCalories;
    private Long totalSteps;
}
