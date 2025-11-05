package com.ssafy.linkcare.health.dto;

import lombok.Data;

@Data
public class StepDto {

    private String deviceId;
    private String deviceType;
    private String startTime;
    private String endTime;
    private int count;
    private int goal;
}
