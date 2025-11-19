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

    @Override
    public String toString() {
        return "수면정보{" +
                ", 취침시간=" + startTime +
                ", 기상시간=" + endTime +
                ", 수면시간=" + duration +
                ", 수면세션=" + sessions +
                '}';
    }

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int duration;
    private List<SleepSessionResponse> sessions;
}
