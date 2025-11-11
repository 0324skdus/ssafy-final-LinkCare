package com.ssafy.linkcare.health.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SleepResponse {
    private int sleepId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int duration;
    private List<SleepSessionResponse> sessions;
}
