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
public class ExerciseSessionResponse {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Float distance;
    private Float calories;
    private Integer meanPulseRate;
    private Long duration;
}
