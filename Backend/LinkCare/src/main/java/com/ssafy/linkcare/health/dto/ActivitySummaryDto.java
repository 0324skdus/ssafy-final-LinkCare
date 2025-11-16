package com.ssafy.linkcare.health.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivitySummaryDto {

    private String deviceId;
    private String deviceType;
    private LocalDateTime startTime;
    private Double totalCaloriesBurned;
    private Double totalDistance;

}
