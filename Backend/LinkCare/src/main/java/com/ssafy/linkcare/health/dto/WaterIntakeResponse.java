package com.ssafy.linkcare.health.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterIntakeResponse {
    private int waterIntakeId;
    private String deviceId;
    private String deviceType;
    private String uid;
    private LocalDateTime startTime;
    private float amount;
}
