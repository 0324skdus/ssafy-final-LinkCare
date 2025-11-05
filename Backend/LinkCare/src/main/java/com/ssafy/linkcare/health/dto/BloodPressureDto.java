package com.ssafy.linkcare.health.dto;

import lombok.Data;

@Data
public class BloodPressureDto {

    private String deviceId;
    private String deviceType;
    private String uid;
    private String startTime;
    private String endTime;
    private DataSource dataSource;
    private float systolic;
    private float diastolic;
    private float mean;
    private int pulseRate;

}
