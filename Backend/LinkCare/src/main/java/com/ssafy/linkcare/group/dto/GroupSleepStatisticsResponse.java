package com.ssafy.linkcare.group.dto;

import com.ssafy.linkcare.health.dto.SleepDetailResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupSleepStatisticsResponse {
    private List<SleepDetailResponse> members;
    private Long avgDuration;
    private LocalDate startDate;
    private LocalDate endDate;
}
