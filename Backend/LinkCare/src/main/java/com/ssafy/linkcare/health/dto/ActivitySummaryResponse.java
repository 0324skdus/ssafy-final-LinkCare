package com.ssafy.linkcare.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivitySummaryResponse {
//    private int activitySummaryId;
    private LocalDateTime startTime;
    private double totalCaloriesBurned;
    private double totalDistance;
}
