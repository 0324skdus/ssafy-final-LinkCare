package com.ssafy.linkcare.group.entity;

import com.ssafy.linkcare.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/*
    * 그룹 멤버 엔티티

    * 역할: 그룹에 속한 멤버 정보를 저장 (User와 Group의 중간 테이블)

    * 특징:
        * - 한 사용자는 여러 그룹에 속할 수 있음
        * - 각 그룹마다 방장(isLeader=true)이 1명 존재
        * - 같은 그룹에 같은 사용자는 중복 불가 (uniqueConstraints)

    * 연관 엔티티:
        * - Group (N:1) - 속한 그룹
        * - User (N:1) - 멤버 사용자
        * - GroupHealthPermission (1:1) - 이 멤버가 동의한 건강 정보 공유 권한
*/
@Entity
@Table(
        name = "group_members",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"group_seq", "user_seq"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_member_seq")
    private Long groupMemberSeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_seq", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_seq", nullable = false)
    private User user;

    @Column(name = "is_leader", nullable = false)
    private Boolean isLeader = false;

    @CreatedDate
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @Builder
    public GroupMember(Group group, User user, Boolean isLeader) {
        this.group = group;
        this.user = user;
        this.isLeader = isLeader != null ? isLeader : false;
    }

    // 그룹장 권한 변경 메서드
    public void updateLeaderStatus(Boolean isLeader) {
        this.isLeader = isLeader;
    }
}
