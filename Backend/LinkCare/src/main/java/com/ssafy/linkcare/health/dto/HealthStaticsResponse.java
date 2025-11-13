package com.ssafy.linkcare.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthStaticsResponse {

    private ActivitySummaryStaticsResponse activitySummaryStats;
    private ExerciseStatisticsResponse exerciseStats;
    private BloodPressureStaticsResponse bloodPressureStats;
    private HeartRateStaticsResponse heartRateStats;
    private SleepStatisticsResponse sleepStats;
    private WaterIntakeStatisticsResponse waterIntakeStats;
    private StepStatisticsResponse stepStats;
}
