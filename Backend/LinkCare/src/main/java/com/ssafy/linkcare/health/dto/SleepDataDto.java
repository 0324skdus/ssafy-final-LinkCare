package com.ssafy.linkcare.health.dto;

import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SleepDataDto {

    private String deviceId;
    private String deviceType;
    private String uid;
    private String zoneOffset;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private DataSource dataSource;
    private int duration;
    private List<SleepSessionDto> sessions;
}
