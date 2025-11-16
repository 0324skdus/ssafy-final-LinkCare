package com.ssafy.linkcare.group.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/*
    * 그룹 초대 링크 엔티티

    * 역할: 그룹에 참가할 수 있는 초대 링크를 생성하고 관리

    * 특징:
        * - 방장이 초대 링크를 생성하면 고유한 UUID 토큰 발급
        * - 초대 링크는 7일 후 자동 만료 (expiredAt)
        * - 한 그룹에 여러 개의 초대 링크 생성 가능 (1:N 관계)
        * - usedAt: 링크 사용 시점 기록 (현재는 미사용, 향후 확장 가능)

    * 사용 흐름:
        * 1. 방장이 초대 링크 생성 (POST /api/groups/{groupSeq}/invitations)
        * 2. 초대 받은 사람이 링크 미리보기 (GET /api/groups/invitations/{token}/preview)
        * 3. 참가 신청 (POST /api/groups/invitations/{token}/join)

    * 연관 엔티티:
        * - Group (N:1) - 이 초대 링크가 속한 그룹
*/
@Entity
@Table(name = "group_invitations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class GroupInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invitation_seq")
    private Long invitationSeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_seq", nullable = false)
    private Group group;

    @Column(name = "invitation_token", nullable = false, unique = true)
    private String invitationToken;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Builder
    public GroupInvitation(Group group) {
        this.group = group;
        this.invitationToken = UUID.randomUUID().toString();
        this.expiredAt = LocalDateTime.now().plusDays(7);  // 7일 후 만료
    }

    // 만료 여부 확인
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiredAt);
    }

    // 사용 처리
    public void markAsUsed() {
        this.usedAt = LocalDateTime.now();
    }
}
