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
    @Override
    public String toString() {
        return "DailyHealthResponse{" +
                "활동 요약=" + activitySummary +
                ", 심박수=" + heartRate +
                ", 수면=" + sleep +
                ", 음수량=" + waterIntake +
                ", 혈압=" + bloodPressure +
                ", 운동기록=" + exercise +
                ", 걸음수=" + step +
                '}';
    }

    private ActivitySummaryResponse activitySummary;
    private List<HeartRateResponse> heartRate;
    private List<SleepResponse> sleep;
    private List<WaterIntakeResponse> waterIntake;
    private List<BloodPressureResponse> bloodPressure;
    private List<ExerciseResponse> exercise;
    private int step;
}
