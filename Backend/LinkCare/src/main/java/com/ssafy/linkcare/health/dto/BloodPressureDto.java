package com.ssafy.linkcare.health.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BloodPressureDto {

    private String deviceId;
    private String deviceType;
    private String uid;
    private LocalDateTime startTime;
    private DataSource dataSource;
    private float systolic;
    private float diastolic;
    private float mean;
    private int pulseRate;

}
