package com.ssafy.linkcare.gpt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodayHealthSummaryResponse {

    private String dailyActivityReview;
    private String bloodPressureReview;
    private String waterIntakeReview;
    private String sleepReview;
    private String heartRateReview;

}

