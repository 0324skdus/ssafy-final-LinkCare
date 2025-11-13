package com.ssafy.linkcare.health.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterIntakeResponse {
    private int waterIntakeId;
    private LocalDateTime startTime;
    private float amount;
}
