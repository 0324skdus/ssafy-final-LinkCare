package com.ssafy.linkcare.health.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StepDto {

    private String deviceId;
    private String deviceType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int count;
    private int goal;
}
