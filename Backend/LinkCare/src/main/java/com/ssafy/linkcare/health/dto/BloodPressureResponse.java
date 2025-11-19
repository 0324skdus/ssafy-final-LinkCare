package com.ssafy.linkcare.health.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloodPressureResponse {

    private int bloodPressureId;
    private String uid;
    private LocalDateTime startTime;
    private float systolic;
    private float diastolic;
    private float mean;
    private int pulseRate;

    @Override
    public String toString() {
        return "혈압 정보{" +
                ", 측정시간=" + startTime +
                ", 수축기=" + systolic +
                ", 이완기=" + diastolic +
                ", 평균=" + mean +
                ", 심박수=" + pulseRate +
                '}';
    }
}
