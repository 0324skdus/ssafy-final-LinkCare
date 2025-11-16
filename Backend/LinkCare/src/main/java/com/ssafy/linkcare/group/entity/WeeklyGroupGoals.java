package com.ssafy.linkcare.group.entity;

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

    @Column(name = "created_at")
    private Long createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_seq")
    private Group group;
}
