package com.ssafy.linkcare.group.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "weekly_group_stats")
public class WeeklyGroupStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "weekly_group_stats_seq")
    private int weeklyGroupStatsSeq;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "week_end", nullable = false)
    private LocalDate weekEnd;

    @Column(name = "member_count", nullable = false)
    private int memberCount;

    @Column(name = "avg_age", nullable = false)
    private Float avgAge;

    @Column(name = "avg_bmi", nullable = false)
    private Float avgBmi;

    @Column(name = "group_steps_total", nullable = false)
    private Long groupStepsTotal;

    @Column(name = "group_kcal_total", nullable = false)
    private Float groupKcalTotal;

    @Column(name = "group_duration_total", nullable = false)
    private int groupDurationTotal;

    @Column(name = "group_distance_total", nullable = false)
    private Float groupDistanceTotal;

    @Column(name = "member_steps_var", nullable = false)
    private Float memberStepsVar;

    @Column(name = "created_at")
    private Long createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_seq")
    private Group group;
}
