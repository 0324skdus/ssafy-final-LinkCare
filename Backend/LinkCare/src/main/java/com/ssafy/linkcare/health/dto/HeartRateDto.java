package com.ssafy.linkcare.health.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HeartRateDto {
    private String deviceId;
    private String deviceType;
    private String uid;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String zoneOffset;
    private DataSource dataSource;
    private Double heartRate;
}
