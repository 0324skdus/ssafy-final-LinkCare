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
public class DailyHealthDetailResponse {

    private List<BloodPressureResponse> bloodPressures;
    private List<WaterIntakeResponse> waterIntakes;
    private List<SleepResponse> sleeps;
    private DailyActivitySummaryResponse dailyActivitySummary;
    private List<HeartRateResponse> heartRates;

    @Override
    public String toString() {
        return "오늘 건강 데이터 {" +
                "혈압=" + bloodPressures.toString() +
                ", 음수량=" + waterIntakes.toString() +
                ", 수면=" + sleeps.toString() +
                ", 일일 활동=" + dailyActivitySummary.toString() +
                ", 심박수=" + heartRates.toString() +
                '}';
    }
}
