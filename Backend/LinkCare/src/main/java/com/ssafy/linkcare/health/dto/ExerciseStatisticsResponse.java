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
    private Long totalDuration;

    @Override
    public String toString() {
        return "ExerciseStatisticsResponse{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                ", totalCalories=" + totalCalories +
                ", totalDistance=" + totalDistance +
                ", averageCalories=" + averageCalories +
                ", exerciseCount=" + exerciseCount +
                ", totalDuration=" + totalDuration +
                '}';
    }
}
