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
@Table(name = "blood_pressure")
public class BloodPressure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int bloodPressureId;

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

    @Column(name = "systolic")
    private float systolic;

    @Column(name = "diastolic")
    private float diastolic;

    @Column(name = "mean")
    private float mean;

    @Column(name = "pulse_rate")
    private int pulseRate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_seq")
    private User user;

    public void updateSystolicAndDiastolicAndPulseRate(float systolic, float diastolic, int pulseRate) {
        this.systolic = systolic;
        this.diastolic = diastolic;
        this.pulseRate = pulseRate;
    }

}
