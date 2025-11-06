package com.ssafy.linkcare.health.entity;

import com.ssafy.linkcare.health.dto.DataSource;
import com.ssafy.linkcare.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sleep")
public class Sleep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int sleepId;

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

    @Column(name = "duration", nullable = false)
    private int duration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_seq")
    private User user;

    @OneToMany(mappedBy = "sleep", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SleepSession> sessions = new ArrayList<>();

    public void updateDuration(int duration) {
        this.duration = duration;
    }

}
