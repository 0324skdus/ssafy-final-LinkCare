package com.ssafy.linkcare.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseSessionResponse {
    @Override
    public String toString() {
        return "ExerciseSessionResponse{" +
                "시작시간=" + startTime +
                ", 종료시간=" + endTime +
                ", 운동거리=" + distance +
                ", 소모칼로리=" + calories +
                ", 평균심박수=" + meanPulseRate +
                ", 운동시간=" + duration +
                '}';
    }

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String exerciseType;
    private Float distance;
    private Float calories;
    private Integer meanPulseRate;
    private Long duration;
}
