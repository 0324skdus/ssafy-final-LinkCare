package com.ssafy.linkcare.health.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SleepSessionDto {

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int duration;

}
