package com.ssafy.linkcare.health.entity;

import com.ssafy.linkcare.health.dto.DataSource;
import com.ssafy.linkcare.user.entity.User;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Table(name = "heart_rate")
public class HeartRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int heartRateId;

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

    @Column(name = "end_time")
    private Long endTime;

    @Embedded
    private DataSource dataSource;

    @Column(name = "heart_rate")
    private Double heartRate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_seq")
    private User user;

}
