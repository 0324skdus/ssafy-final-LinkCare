package com.ssafy.linkcare.health.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeartRateResponse {
    private int heartRateId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double heartRate;

    @Override
    public String toString() {
        return "HeartRateResponse{" +
                ", 측정시간=" + startTime +
                ", 심박수=" + heartRate +
                '}';
    }
}
