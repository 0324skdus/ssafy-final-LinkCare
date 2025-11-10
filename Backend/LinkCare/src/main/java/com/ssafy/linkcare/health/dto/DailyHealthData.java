package com.ssafy.linkcare.health.dto;

import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyHealthData {
    private ActivitySummaryDto activitySummary;
    private List<HeartRateDto> heartRate;
    private List<SleepDataDto> sleep;
    private WaterIntakeDto waterIntake;
    private List<BloodPressureDto> bloodPressure;
    private ExerciseDto exercise;
    private StepDto step;
}
