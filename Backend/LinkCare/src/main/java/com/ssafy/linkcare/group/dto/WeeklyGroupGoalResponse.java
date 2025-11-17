package com.ssafy.linkcare.group.dto;

import com.ssafy.linkcare.group.entity.WeeklyGroupGoals;
import com.ssafy.linkcare.group.enums.MetricType;

import java.time.LocalDate;

/**
 * 주간 그룹 목표 응답 DTO
 */
public record WeeklyGroupGoalResponse(
        Integer weeklyGroupGoalsSeq,
        Long groupSeq,
        LocalDate weekStart,
        Long goalSteps,
        Float goalKcal,
        Integer goalDuration,
        Float goalDistance,
        Double predictedGrowthRateSteps,
        Double predictedGrowthRateKcal,
        Double predictedGrowthRateDuration,
        Double predictedGrowthRateDistance,
        MetricType selectedMetricType
) {
    public static WeeklyGroupGoalResponse from(WeeklyGroupGoals goal) {
        return new WeeklyGroupGoalResponse(
                goal.getWeeklyGroupGoalsSeq(),
                goal.getGroup().getGroupSeq(),
                goal.getWeekStart(),
                goal.getGoalSteps(),
                goal.getGoalKcal(),
                goal.getGoalDuration(),
                goal.getGoalDistance(),
                goal.getPredictedGrowthRateSteps(),
                goal.getPredictedGrowthRateKcal(),
                goal.getPredictedGrowthRateDuration(),
                goal.getPredictedGrowthRateDistance(),
                goal.getSelectedMetricType()
        );
    }
}
