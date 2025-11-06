package com.ssafy.linkcare.health.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExerciseSessionDto {

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String exerciseType;
    private float calories;
    private float distance;

}
