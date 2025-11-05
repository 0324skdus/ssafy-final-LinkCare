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
    * 그룹이 요구하는 건강 정보 공유 권한 엔티티

    * 역할: CARE 그룹이 멤버들에게 요구하는 권한 항목을 저장 (그룹 정책)

    * 사용 범위:
        * - CARE 그룹만 사용 (방장이 그룹 생성 시 설정)
        * - HEALTH 그룹은 사용 안 함 (필수 권한만 있으므로 별도 저장 불필요)

    * 주의: GroupHealthPermission과 역할이 다름!
        * - GroupRequiredPermission: 그룹이 요구하는 권한 (그룹 정책, 불변)
        * - GroupHealthPermission: 멤버가 실제로 동의한 권한 (개인 동의)

    * 저장 항목:
        * - 선택 권한만 저장: 수면, 물섭취량, 혈압, 혈당
        * - 필수 권한(걸음수, 심박수, 운동)은 항상 true이므로 저장 안 함

    * 연관 엔티티:
        * - Group (1:1, @MapsId) - 이 권한 정책을 가진 그룹
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