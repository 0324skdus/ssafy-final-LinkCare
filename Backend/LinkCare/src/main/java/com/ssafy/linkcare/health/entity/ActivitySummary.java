package com.ssafy.linkcare.health.entity;

import com.ssafy.linkcare.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivitySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long activitySummaryId;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "device_type")
    private String deviceType;

    @Column(name = "start_time")
    private String startTime;

    @Column(name = "total_calories_burned")
    private Double totalCaloriesBurned;

    @Column(name = "total_distance")
    private Double totalDistance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_seq")
    private User user;
}
