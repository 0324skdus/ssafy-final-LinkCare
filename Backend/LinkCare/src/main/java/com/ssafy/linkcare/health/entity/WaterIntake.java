package com.ssafy.linkcare.health.entity;

import com.ssafy.linkcare.health.dto.DataSource;
import com.ssafy.linkcare.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "water_intake")
public class WaterIntake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int waterIntakeId;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "device_type")
    private String deviceType;

    @Column(name = "uid")
    private String uid;

    @Column(name = "zone_off_set")
    private String zoneOffset;

    @Column(name = "start_time")
    private Long startTime;

    @Embedded
    private DataSource dataSource;

    @Column(name = "amount")
    private float amount;

    @Column(name = "goal")
    private float goal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_seq")
    private User user;

    public void updateAmountAndGoal(float amount, float goal) {
        this.amount = amount;
        this.goal = goal;
    }
}
