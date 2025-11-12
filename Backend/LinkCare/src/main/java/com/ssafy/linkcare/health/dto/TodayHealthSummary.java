package com.ssafy.linkcare.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodayHealthSummary {

    private ActivitySummaryStaticsResponse activitySummary;
    private ExerciseStatisticsResponse exercise;
    private int step;
    private HeartRateStaticsResponse heartRate;
    private BloodPressureStaticsResponse bloodPressure;
    private WaterIntakeStatisticsResponse waterIntake;
    private SleepStatisticsResponse sleep;

}

