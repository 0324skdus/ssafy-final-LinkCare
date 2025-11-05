package com.ssafy.linkcare.health.dto;

import lombok.Data;

@Data
public class WaterIntakeGroupedDto {

    private String deviceId;
    private String deviceType;
    private String uid;
    private String zoneOffset;
    private String startTime;
    private DataSource dataSource;
    private float amount;
}
