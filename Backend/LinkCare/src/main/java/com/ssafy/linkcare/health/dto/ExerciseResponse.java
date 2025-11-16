package com.ssafy.linkcare.health.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseResponse {
    private int exerciseId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String exerciseType;
    private Integer avgHeartRate;
    private List<ExerciseSessionDto> sessions;
}
