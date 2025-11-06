package com.ssafy.linkcare.health.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExerciseTypeDto {
    private String deviceId;
    private String deviceType;
    private String uid;
    private String zoneOffset;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private DataSource dataSource;
    private String exerciseType;
    private List<ExerciseSessionDto> sessions;
}
