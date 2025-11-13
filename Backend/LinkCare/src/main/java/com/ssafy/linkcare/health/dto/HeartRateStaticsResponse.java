package com.ssafy.linkcare.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeartRateStaticsResponse {

    private LocalDate startTime;
    private LocalDate endTime;
    private Double avgHeartRate;
    private Double maxHeartRate;
    private Double minHeartRate;

    @Override
    public String toString() {
        return "HeartRateStaticsResponse{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                ", avgHeartRate=" + avgHeartRate +
                ", maxHeartRate=" + maxHeartRate +
                ", minHeartRate=" + minHeartRate +
                '}';
    }
}
