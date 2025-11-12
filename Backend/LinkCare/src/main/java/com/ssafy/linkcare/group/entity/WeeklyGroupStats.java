package com.ssafy.linkcare.group.entity;

import jakarta.persistence.*;
import lombok.*;

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


}
