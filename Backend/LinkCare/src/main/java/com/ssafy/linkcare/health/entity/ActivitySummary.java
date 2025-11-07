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
    private int activitySummaryId;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "device_type")
    private String deviceType;

    @Column(name = "start_time")
    private Long startTime;

    @Column(name = "total_calories_burned")
    private double totalCaloriesBurned;

    @Column(name = "total_distance")
    private double totalDistance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_seq")
    private User user;

    public void updateCaloriesAndDistance(double totalCaloriesBurned, double totalDistance) {
        this.totalCaloriesBurned = totalCaloriesBurned;
        this.totalDistance = totalDistance;
    }
}
