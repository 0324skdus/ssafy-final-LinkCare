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
public class BloodPressureStaticsResponse {

    private LocalDate startDate;
    private LocalDate endDate;

    // 최고
    private Double maxSystolic;
    private Double maxDiastolic;

    // 최저
    private Double minSystolic;
    private Double minDiastolic;

    private Double avgDiastolic;
    private Double avgSystolic;

    @Override
    public String toString() {
        return "BloodPressureStaticsResponse{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                ", maxDiastolic=" + maxDiastolic +
                ", minDiastolic=" + minDiastolic +
                ", avgDiastolic=" + avgDiastolic +
                ", avgSystolic=" + avgSystolic +
                '}';
    }
}
