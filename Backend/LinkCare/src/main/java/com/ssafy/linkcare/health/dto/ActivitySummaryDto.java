package com.ssafy.linkcare.health.dto;

import lombok.Data;

@Data
public class ActivitySummaryDto {

    private String deviceId;
    private String deviceType;
    private String startTime;
    private Double totalCaloriesBurned;
    private Double totalDistance;

}
