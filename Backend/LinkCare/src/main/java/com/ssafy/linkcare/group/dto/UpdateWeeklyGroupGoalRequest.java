package com.ssafy.linkcare.group.dto;

import com.ssafy.linkcare.group.enums.MetricType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 주간 그룹 목표 수정 요청 DTO
 */
public record UpdateWeeklyGroupGoalRequest(
        @NotNull(message = "메트릭 타입은 필수입니다")
        MetricType selectedMetricType,

        @NotNull(message = "목표값은 필수입니다")
        @Positive(message = "목표값은 양수여야 합니다")
        Long goalValue
) {
}
