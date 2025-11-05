package com.ssafy.linkcare.health.dto;

import lombok.Data;

@Data
public class HeartRateDto {
    private String deviceId;
    private String deviceType;
    private String uid;
    private String startTime;
    private String endTime;
    private String zoneOffset;
    private DataSource dataSource;
    private Double heartRate;
}
