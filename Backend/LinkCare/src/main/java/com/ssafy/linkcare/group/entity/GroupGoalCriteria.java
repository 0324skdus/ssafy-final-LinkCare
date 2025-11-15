package com.ssafy.linkcare.group.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/*
    * 그룹 목표 기준 엔티티

    * 역할: HEALTH 그룹의 운동 목표 기준을 저장

    * 사용 범위:
        * - HEALTH 그룹만 사용 (운동 목표 기반 모임)
        * - CARE 그룹은 사용 안 함 (건강 데이터 공유 기반 모임)

    * 목표 항목 (모두 선택 항목, null 가능):
        * - minCalorie: 최소 칼로리 소모 (kcal)
        * - minStep: 최소 걸음수 (보)
        * - minDistance: 최소 이동 거리 (km)
        * - minDuration: 최소 운동 시간 (분)

    * 연관 엔티티:
        * - Group (1:1) - 이 목표 기준을 가진 HEALTH 그룹
*/
@Entity
@Table(name = "group_goal_criteria")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class GroupGoalCriteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "criteria_seq")
    private Long criteriaSeq;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_seq", nullable = false, unique = true)
    private Group group;

    @Column(name = "min_calorie")
    private Float minCalorie;  // 최소 칼로리 (kcal)

    @Column(name = "min_step")
    private Integer minStep;  // 최소 걸음수 (보)

    @Column(name = "min_distance")
    private Float minDistance;  // 최소 거리 (km)

    @Column(name = "min_duration")
    private Integer minDuration;  // 최소 운동 시간 (분)

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public GroupGoalCriteria(Group group, Float minCalorie, Integer minStep, Float minDistance, Integer minDuration) {
        this.group = group;
        this.minCalorie = minCalorie;
        this.minStep = minStep;
        this.minDistance = minDistance;
        this.minDuration = minDuration;
    }

    // 비즈니스 로직: 목표 기준 업데이트
    public void updateGoalCriteria(Float minCalorie, Integer minStep, Float minDistance, Integer minDuration) {
        this.minCalorie = minCalorie;
        this.minStep = minStep;
        this.minDistance = minDistance;
        this.minDuration = minDuration;
    }
}
