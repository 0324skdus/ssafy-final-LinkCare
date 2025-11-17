package com.ssafy.linkcare.group.entity;

import com.ssafy.linkcare.group.enums.GroupType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

/*
    * 그룹 (헬스/케어 모임) 엔티티

    * 역할: 헬스 그룹과 케어 그룹의 기본 정보를 저장

    * 그룹 타입:
        * - HEALTH 그룹: 운동 목표 기반 모임 (GroupGoalCriteria와 1:1 관계)
        * - CARE 그룹: 건강 데이터 공유 기반 모임 (GroupRequiredPermission과 1:1 관계)

    * 연관 엔티티:
        * - GroupMember (1:N) - 그룹 멤버들
        * - GroupGoalCriteria (1:1) - 헬스 그룹의 목표 기준
        * - GroupRequiredPermission (1:1) - 케어 그룹이 요구하는 권한
        * - GroupInvitation (1:N) - 초대 링크들
        * - GroupJoinRequest (1:N) - 가입 신청들
*/
@Entity
@Table(name = "`groups`")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_seq")
    private Long groupSeq;

    @Column(name = "group_name", nullable = false, length = 50)
    private String groupName;

    @Column(name = "group_description", columnDefinition = "TEXT")
    private String groupDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private GroupType type;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ===== AI 생성 주간 헤더 필드 =====
    @Column(name = "weekly_header_message", columnDefinition = "TEXT")
    private String weeklyHeaderMessage;

    @Column(name = "header_generated_at")
    private LocalDateTime headerGeneratedAt;

    @Builder
    public Group(String groupName, String groupDescription, GroupType type, Integer capacity, String imageUrl) {
        this.groupName = groupName;
        this.groupDescription = groupDescription;
        this.type = type;
        this.capacity = capacity;
        this.imageUrl = imageUrl;
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void updateGroupInfo(String groupName, String groupDescription) {
        this.groupName = groupName;
        this.groupDescription = groupDescription;
    }

    public void updateWeeklyHeader(
            String message,
            LocalDateTime generatedAt
    ) {
        this.weeklyHeaderMessage = message;
        this.headerGeneratedAt = generatedAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        // 신규 그룹 기본 헤더 설정
        this.weeklyHeaderMessage = "함께 건강해져봐요! 💪";
        this.headerGeneratedAt = LocalDateTime.now();
    }

    /**
     * 그룹이 생성된 주인지 확인
     */
    public boolean isCreatedThisWeek() {
        if (this.createdAt == null) {
            return false;
        }

        LocalDate createdDate = this.createdAt.toLocalDate();
        LocalDate thisWeekMonday = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate thisWeekSunday = thisWeekMonday.plusDays(6);

        return !createdDate.isBefore(thisWeekMonday) && !createdDate.isAfter(thisWeekSunday);
    }
}
