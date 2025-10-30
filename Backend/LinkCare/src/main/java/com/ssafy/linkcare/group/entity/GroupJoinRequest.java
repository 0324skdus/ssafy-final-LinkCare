package com.ssafy.linkcare.group.entity;

import com.ssafy.linkcare.group.enums.RequestStatus;
import com.ssafy.linkcare.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_join_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class GroupJoinRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_seq")
    private Long requestSeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_seq", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_seq", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RequestStatus status = RequestStatus.PENDING;

    @CreatedDate
    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    // 선택 권한 동의 정보 (신청 시 사용자가 선택)
    @Column(name = "agreed_sleep")
    private Boolean agreedSleep;

    @Column(name = "agreed_water_intake")
    private Boolean agreedWaterIntake;

    @Column(name = "agreed_blood_pressure")
    private Boolean agreedBloodPressure;

    @Column(name = "agreed_blood_sugar")
    private Boolean agreedBloodSugar;

    @Builder
    public GroupJoinRequest(Group group, User user, Boolean agreedSleep,
                            Boolean agreedWaterIntake, Boolean agreedBloodPressure,
                            Boolean agreedBloodSugar) {
        this.group = group;
        this.user = user;
        this.status = RequestStatus.PENDING;
        this.agreedSleep = agreedSleep;
        this.agreedWaterIntake = agreedWaterIntake;
        this.agreedBloodPressure = agreedBloodPressure;
        this.agreedBloodSugar = agreedBloodSugar;
    }

    // 승인 처리
    public void approve() {
        this.status = RequestStatus.APPROVED;
        this.respondedAt = LocalDateTime.now();
    }

    // 거절 처리
    public void reject() {
        this.status = RequestStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
    }
}
