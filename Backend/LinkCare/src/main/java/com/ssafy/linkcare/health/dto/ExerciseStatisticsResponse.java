package com.ssafy.linkcare.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseStatisticsResponse {

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Float totalCalories;
    private Float totalDistance;
    private Float averageCalories;
    private Long exerciseCount;

}
