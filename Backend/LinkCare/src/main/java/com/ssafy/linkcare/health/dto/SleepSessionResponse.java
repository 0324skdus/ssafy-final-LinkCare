package com.ssafy.linkcare.health.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SleepSessionResponse {

    private int sleepSessionId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int duration;

}
