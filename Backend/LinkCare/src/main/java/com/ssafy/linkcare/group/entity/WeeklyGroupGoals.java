package com.ssafy.linkcare.group.entity;

import com.ssafy.linkcare.group.enums.MetricType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * 주간 그룹 목표 엔티티
 * - AI 모델이 예측한 성장률 기반으로 생성된 주간 목표를 저장
 * - week_start 기준 월요일부터 일요일까지의 목표
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "weekly_group_goals")
public class WeeklyGroupGoals {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "weekly_group_goals_seq")
    private Integer weeklyGroupGoalsSeq;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "goal_steps", nullable = false)
    private Long goalSteps;

    @Column(name = "goal_kcal", nullable = false)
    private Float goalKcal;

    @Column(name = "goal_duration", nullable = false)
    private Integer goalDuration;

    @Column(name = "goal_distance", nullable = false)
    private Float goalDistance;

    @Column(name = "predicted_growth_rate_steps", nullable = false)
    private Double predictedGrowthRateSteps;

    @Column(name = "predicted_growth_rate_kcal", nullable = false)
    private Double predictedGrowthRateKcal;

    @Column(name = "predicted_growth_rate_duration", nullable = false)
    private Double predictedGrowthRateDuration;

    @Column(name = "predicted_growth_rate_distance", nullable = false)
    private Double predictedGrowthRateDistance;

    @Enumerated(EnumType.STRING)
    @Column(name = "selected_metric_type", length = 20)
    private MetricType selectedMetricType;

    @Column(name = "created_at")
    private Long createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_seq")
    private Group group;

    /**
     * 선택된 메트릭 타입에 따라 목표값 업데이트 (사용자가 선택)
     */
    public void updateGoal(MetricType metricType, Number goalValue) {
        this.selectedMetricType = metricType;

        switch (metricType) {
            case STEPS -> this.goalSteps = goalValue.longValue();
            case KCAL -> this.goalKcal = goalValue.floatValue();
            case DURATION -> this.goalDuration = goalValue.intValue();
            case DISTANCE -> this.goalDistance = goalValue.floatValue();
        }
    }

    /**
     * AI 예측 기반으로 목표 전체 업데이트
     * selectedMetricType은 null로 리셋 (새로 선택해야 함)
     */
    public void updateFromAiPrediction(
            Long goalSteps, Float goalKcal, Integer goalDuration, Float goalDistance,
            Double predictedGrowthRateSteps, Double predictedGrowthRateKcal,
            Double predictedGrowthRateDuration, Double predictedGrowthRateDistance) {

        this.goalSteps = goalSteps;
        this.goalKcal = goalKcal;
        this.goalDuration = goalDuration;
        this.goalDistance = goalDistance;
        this.predictedGrowthRateSteps = predictedGrowthRateSteps;
        this.predictedGrowthRateKcal = predictedGrowthRateKcal;
        this.predictedGrowthRateDuration = predictedGrowthRateDuration;
        this.predictedGrowthRateDistance = predictedGrowthRateDistance;
        this.selectedMetricType = null;  // AI 재생성 시 선택 초기화
    }
}
