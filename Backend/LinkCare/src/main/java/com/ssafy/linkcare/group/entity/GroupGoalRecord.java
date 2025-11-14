package com.ssafy.linkcare.group.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 그룹 주간 목표 달성 기록
 * - 매주 일요일 23:59에 자동 생성
 * - 목표 대비 실제 달성 데이터를 기록
 */
@Entity
@Table(name = "group_goal_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GroupGoalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "group_seq", nullable = false)
    private Long groupSeq;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    // 걸음수
    @Column(name = "goal_steps")
    private Long goalSteps;

    @Column(name = "actual_steps")
    private Long actualSteps;

    @Column(name = "achievement_rate_steps")
    private Float achievementRateSteps;

    // 칼로리
    @Column(name = "goal_kcal")
    private Float goalKcal;

    @Column(name = "actual_kcal")
    private Float actualKcal;

    @Column(name = "achievement_rate_kcal")
    private Float achievementRateKcal;

    // 운동시간
    @Column(name = "goal_duration")
    private Integer goalDuration;

    @Column(name = "actual_duration")
    private Integer actualDuration;

    @Column(name = "achievement_rate_duration")
    private Float achievementRateDuration;

    // 이동거리
    @Column(name = "goal_distance")
    private Float goalDistance;

    @Column(name = "actual_distance")
    private Float actualDistance;

    @Column(name = "achievement_rate_distance")
    private Float achievementRateDistance;

    // 성공 여부 (모든 항목 100% 이상 달성)
    @Column(name = "is_succeeded")
    private Boolean isSucceeded;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
