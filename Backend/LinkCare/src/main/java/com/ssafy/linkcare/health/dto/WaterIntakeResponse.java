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
    private Float goal;

    @Override
    public String toString() {
        return "음수량 정보{" +
                ", 섭취시간=" + startTime +
                ", 섭취량=" + amount +
                ", 음수량 목표=" + goal +
                '}';
    }
}
