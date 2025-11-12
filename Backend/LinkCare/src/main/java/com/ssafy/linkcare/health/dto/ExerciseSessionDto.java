package com.ssafy.linkcare.health.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseSessionDto {

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String exerciseType;
    private float calories;
    private float distance;
    private Long duration;

}
