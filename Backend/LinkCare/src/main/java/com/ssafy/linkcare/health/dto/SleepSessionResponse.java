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

    @Override
    public String toString() {
        return "수면세션 정보{" +
                ", 취침시간=" + startTime +
                ", 기상시간=" + endTime +
                ", 수면시간=" + duration +
                '}';
    }
}
