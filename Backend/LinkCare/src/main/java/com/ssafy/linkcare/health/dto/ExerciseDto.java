package com.ssafy.linkcare.health.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExerciseDto {
    private List<ExerciseTypeDto> exercises;
}
