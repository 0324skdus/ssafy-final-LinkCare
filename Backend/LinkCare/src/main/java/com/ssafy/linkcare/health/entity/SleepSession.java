package com.ssafy.linkcare.health.entity;

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
@Table(name = "sleep_session")
public class SleepSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int sleepSessionId;

    @Column(name = "state_time")
    private Long startTime;

    @Column(name = "end_time")
    private Long endTime;

    @Column(name = "duration")
    private int duration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sleep_id")
    private Sleep sleep;

    public void updateDuration(int duration) {
        this.duration = duration;
    }

}
