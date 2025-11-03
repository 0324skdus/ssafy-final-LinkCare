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
}
