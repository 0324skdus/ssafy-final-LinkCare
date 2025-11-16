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
}
