package com.ssafy.linkcare.group.dto;

import com.ssafy.linkcare.health.dto.StepDetailResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupStepStatisticsResponse {
    private List<StepDetailResponse> members;
    private int totalSteps;
}
