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

    private String dailyActivityReview;
    private String bloodPressureReview;
    private String waterIntakeReview;
    private String sleepReview;
    private String heartRateReview;

    @Override
    public String toString() {
        return "TodayHealthSummary{" +
                "dailyActivityReview='" + dailyActivityReview + '\'' +
                ", bloodPressureReview='" + bloodPressureReview + '\'' +
                ", waterIntakeReview='" + waterIntakeReview + '\'' +
                ", sleepReview='" + sleepReview + '\'' +
                ", heartRateReview='" + heartRateReview + '\'' +
                '}';
    }
}

