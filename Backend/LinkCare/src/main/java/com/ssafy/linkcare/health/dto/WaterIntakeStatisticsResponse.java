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
public class WaterIntakeStatisticsResponse {

    private Float totalAmount;
    private Float averageAmount;
    private Float dailyGoal;
    private Float goalAchievementRate;
    private Long intakeCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
