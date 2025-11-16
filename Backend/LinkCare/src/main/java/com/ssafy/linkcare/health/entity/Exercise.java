package com.ssafy.linkcare.health.entity;

import com.ssafy.linkcare.health.dto.DataSource;
import com.ssafy.linkcare.health.dto.ExerciseTypeDto;
import com.ssafy.linkcare.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "exercise")
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int exerciseId;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "device_type")
    private String deviceType;

    @Column(name = "uid")
    private String uid;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "zone_off_set")
    private String zoneOffset;

    @Column(name = "start_time")
    private Long startTime;

    @Column(name = "end_time")
    private Long endTime;

    @Embedded
    private DataSource dataSource;

    @Column(name = "exercise_type")
    private String exerciseType;

    @Column(name = "avg_heart_rate")
    private Integer avgHeartRate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_seq")
    private User user;

    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExerciseSession> sessions = new ArrayList<>();

    /**
     * 운동 데이터 업데이트
     */
    public void updateFromDto(ExerciseTypeDto dto, User user) {
        this.deviceId = dto.getDeviceId();
        this.deviceType = dto.getDeviceType();
        this.uid = dto.getUid();
        this.exerciseType = dto.getExerciseType();
        this.zoneOffset = dto.getZoneOffset();
        this.dataSource = dto.getDataSource();
        this.startTime = dto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond();
        this.endTime = dto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond();
        this.user = user;
    }


}
