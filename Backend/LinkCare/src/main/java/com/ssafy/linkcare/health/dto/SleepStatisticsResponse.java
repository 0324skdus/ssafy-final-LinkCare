package com.ssafy.linkcare.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SleepStatisticsResponse {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long totalDuration;
    private Double averageDuration;
    private Integer maxDuration;
    private Integer minDuration;

    @Override
    public String toString() {
        return "SleepStatisticsResponse{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                ", totalDuration=" + totalDuration +
                ", averageDuration=" + averageDuration +
                ", maxDuration=" + maxDuration +
                ", minDuration=" + minDuration +
                '}';
    }
}
