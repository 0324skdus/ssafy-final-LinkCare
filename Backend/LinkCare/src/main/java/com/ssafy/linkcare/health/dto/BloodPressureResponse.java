package com.ssafy.linkcare.health.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloodPressureResponse {

    private int bloodPressureId;
    private String deviceId;
    private String deviceType;
    private String uid;
    private LocalDateTime startTime;
    private float systolic;
    private float diastolic;
    private float mean;
    private int pulseRate;

}
