package com.ssafy.linkcare.group.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/*
    * 그룹 멤버의 건강 정보 공유 동의 엔티티

    * 역할: 각 그룹 멤버가 실제로 동의한 건강 정보 항목을 저장

    * 주의: GroupRequiredPermission과 역할이 다름!
        * - GroupRequiredPermission: 그룹이 요구하는 권한 (그룹 정책)
        * - GroupHealthPermission: 멤버가 실제로 동의한 권한 (개인 동의)

    * 권한 항목:
        * - 필수 동의: 걸음수, 심박수, 운동 (모든 그룹 필수)
        * - 선택 동의: 수면, 물섭취량, 혈압, 혈당 (CARE 그룹만 해당)

    * 연관 엔티티:
        * - GroupMember (1:1) - 이 권한을 소유한 그룹 멤버
*/
@Entity
@Table(name = "group_health_permissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class GroupHealthPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_seq")
    private Long permissionSeq;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_member_seq", nullable = false, unique = true)
    private GroupMember groupMember;

    // 필수 동의 항목 (기본값 true)
    @Column(name = "is_daily_step_allowed", nullable = false)
    private Boolean isDailyStepAllowed = true;

    @Column(name = "is_heart_rate_allowed", nullable = false)
    private Boolean isHeartRateAllowed = true;

    @Column(name = "is_exercise_allowed", nullable = false)
    private Boolean isExerciseAllowed = true;

    // 선택 동의 항목 (기본값 false)
    @Column(name = "is_sleep_allowed", nullable = false)
    private Boolean isSleepAllowed = false;

    @Column(name = "is_water_intake_allowed", nullable = false)
    private Boolean isWaterIntakeAllowed = false;

    @Column(name = "is_blood_pressure_allowed", nullable = false)
    private Boolean isBloodPressureAllowed = false;

    @Column(name = "is_blood_sugar_allowed", nullable = false)
    private Boolean isBloodSugarAllowed = false;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public GroupHealthPermission(GroupMember groupMember,
                                 Boolean isDailyStepAllowed,
                                 Boolean isHeartRateAllowed,
                                 Boolean isExerciseAllowed,
                                 Boolean isSleepAllowed,
                                 Boolean isWaterIntakeAllowed,
                                 Boolean isBloodPressureAllowed,
                                 Boolean isBloodSugarAllowed) {

        this.groupMember = groupMember;

        // 필수 항목 (null이면 true)
        this.isDailyStepAllowed = isDailyStepAllowed != null ? isDailyStepAllowed : true;
        this.isHeartRateAllowed = isHeartRateAllowed != null ? isHeartRateAllowed : true;
        this.isExerciseAllowed = isExerciseAllowed != null ? isExerciseAllowed : true;

        // 선택 항목 (null이면 false)
        this.isSleepAllowed = isSleepAllowed != null ? isSleepAllowed : false;
        this.isWaterIntakeAllowed = isWaterIntakeAllowed != null ? isWaterIntakeAllowed : false;
        this.isBloodPressureAllowed = isBloodPressureAllowed != null ? isBloodPressureAllowed : false;
        this.isBloodSugarAllowed = isBloodSugarAllowed != null ? isBloodSugarAllowed : false;
    }

    // 권한 업데이트 메서드
    public void updatePermissions(Boolean isDailyStepAllowed,
                                  Boolean isHeartRateAllowed,
                                  Boolean isExerciseAllowed,
                                  Boolean isSleepAllowed,
                                  Boolean isWaterIntakeAllowed,
                                  Boolean isBloodPressureAllowed,
                                  Boolean isBloodSugarAllowed) {
        this.isDailyStepAllowed = isDailyStepAllowed;
        this.isHeartRateAllowed = isHeartRateAllowed;
        this.isExerciseAllowed = isExerciseAllowed;
        this.isSleepAllowed = isSleepAllowed;
        this.isWaterIntakeAllowed = isWaterIntakeAllowed;
        this.isBloodPressureAllowed = isBloodPressureAllowed;
        this.isBloodSugarAllowed = isBloodSugarAllowed;
    }
}
