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
public class DailyHealthResponse {
    private ActivitySummaryResponse activitySummary;
    private List<HeartRateResponse> heartRate;
    private List<SleepResponse> sleep;
    private List<WaterIntakeResponse> waterIntake;
    private List<BloodPressureResponse> bloodPressure;
    private List<ExerciseResponse> exercise;
    private StepResponse step;
}
