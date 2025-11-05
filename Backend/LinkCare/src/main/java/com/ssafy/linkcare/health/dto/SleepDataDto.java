package com.ssafy.linkcare.health.dto;

import lombok.Data;
import lombok.Getter;

import java.util.List;

@Data
public class SleepDataDto {

    private String deviceId;
    private String deviceType;
    private String uid;
    private String zoneOffset;
    private String startTime;
    private String endTime;
    private DataSource dataSource;
    private int duration;
    private List<SleepSessionDto> sessions;
}
