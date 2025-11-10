package com.ssafy.linkcare.health.dto;

import lombok.Data;

import java.util.List;

@Data
public class AllHealthDataDto {
    private List<HeartRateDto> heartRate;
    private List<ActivitySummaryDto> activitySummary;
    private List<SleepDataDto> sleep;
    private List<WaterIntakeDto> waterIntake;
    private List<BloodPressureDto> bloodPressure;
    private List<ExerciseDto> exercise;
    private List<StepDto> step;
}
