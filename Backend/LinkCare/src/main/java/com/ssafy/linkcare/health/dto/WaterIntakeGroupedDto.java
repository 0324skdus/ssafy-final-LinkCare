package com.ssafy.linkcare.health.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WaterIntakeGroupedDto {

    private String deviceId;
    private String deviceType;
    private String uid;
    private String zoneOffset;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private DataSource dataSource;
    private float amount;
}
