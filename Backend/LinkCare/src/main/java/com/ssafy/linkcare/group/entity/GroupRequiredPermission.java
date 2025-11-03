package com.ssafy.linkcare.group.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/*
    * 그룹이 요구하는 건강 정보 공유 권한
    * CARE 그룹에만 사용 (HEALTH 그룹은 필수 권한만 있으므로 이 테이블 사용 안 함)
*/
@Entity
@Table(name = "group_required_permissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class GroupRequiredPermission {

    @Id
    @Column(name = "group_seq")
    private Long groupSeq;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "group_seq")
    private Group group;

    // 선택 권한만 저장 (CARE 그룹에서 방장이 설정)
    @Column(name = "is_sleep_required", nullable = false)
    private Boolean isSleepRequired = false;

    @Column(name = "is_water_intake_required", nullable = false)
    private Boolean isWaterIntakeRequired = false;

    @Column(name = "is_blood_pressure_required", nullable = false)
    private Boolean isBloodPressureRequired = false;

    @Column(name = "is_blood_sugar_required", nullable = false)
    private Boolean isBloodSugarRequired = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public GroupRequiredPermission(Group group,
                                   Boolean isSleepRequired,
                                   Boolean isWaterIntakeRequired,
                                   Boolean isBloodPressureRequired,
                                   Boolean isBloodSugarRequired) {
        this.group = group;
        this.isSleepRequired = isSleepRequired != null ? isSleepRequired : false;
        this.isWaterIntakeRequired = isWaterIntakeRequired != null ? isWaterIntakeRequired : false;
        this.isBloodPressureRequired = isBloodPressureRequired != null ? isBloodPressureRequired : false;
        this.isBloodSugarRequired = isBloodSugarRequired != null ? isBloodSugarRequired : false;
    }
}