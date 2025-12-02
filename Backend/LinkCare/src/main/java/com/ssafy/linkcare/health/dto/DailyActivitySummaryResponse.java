package com.ssafy.linkcare.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyActivitySummaryResponse {
    @Override
    public String toString() {
        return "DailyActivitySummaryResponse{" +
                "운동=" + exercises +
                ", 걸음수=" + steps +
                '}';
    }

    private List<ExerciseSessionResponse> exercises;
    private int steps;
}
