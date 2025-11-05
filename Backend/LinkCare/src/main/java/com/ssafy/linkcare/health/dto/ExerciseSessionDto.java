package com.ssafy.linkcare.health.dto;

import lombok.Data;

@Data
public class ExerciseSessionDto {

    private String startTime;
    private String endTime;
    private String exerciseType;
    private float calories;
    private float distance;

}
