package com.ssafy.linkcare.health.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepResponse {

    private int stepId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int count;
    private int goal;
}
