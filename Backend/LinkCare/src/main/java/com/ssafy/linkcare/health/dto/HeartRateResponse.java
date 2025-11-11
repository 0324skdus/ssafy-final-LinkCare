package com.ssafy.linkcare.health.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeartRateResponse {
    private int heartRateId;
    private String deviceId;
    private String deviceType;
    private String uid;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double heartRate;
}
