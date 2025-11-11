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
public class StepStatisticsResponse {

    private int userSeq;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long totalSteps;
    private Float averageSteps;
}
