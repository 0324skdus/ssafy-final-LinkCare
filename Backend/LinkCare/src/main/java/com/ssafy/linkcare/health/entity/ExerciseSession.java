package com.ssafy.linkcare.health.entity;

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
@Table(name = "exercise_session")
public class ExerciseSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int exerciseSessionId;

    @Column(name = "start_time")
    private Long startTime;

    @Column(name = "end_time")
    private Long endTime;

    @Column(name = "exercise_type")
    private String exerciseType;

    @Column(name = "calories")
    private Float calories;

    @Column(name = "distance")
    private Float distance;

    @Column(name = "duration")
    private Long duration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;
}
