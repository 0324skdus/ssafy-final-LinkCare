package com.ssafy.linkcare.health.dto;

import lombok.Data;

@Data
public class SleepSessionDto {

    private String startTime;
    private String endTime;
    private int duration;

}
