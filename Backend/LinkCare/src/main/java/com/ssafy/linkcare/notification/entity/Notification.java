package com.ssafy.linkcare.notification.entity;

import com.ssafy.linkcare.group.entity.Group;
import com.ssafy.linkcare.group.entity.GroupJoinRequest;
import com.ssafy.linkcare.notification.enums.NotificationType;
import com.ssafy.linkcare.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    // 알림 받을 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_seq", nullable = false)
    private User user;

    // 알림 타입
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    // 알림 제목 (그룹 이름)
    @Column(name = "title", length = 100)
    private String title;

    // 알림 내용
    @Column(name = "content", nullable = false, length = 200)
    private String content;

    // 관련 그룹 (nullable - 그룹 관련 알림만)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_group_seq")
    private Group relatedGroup;

    // 관련 참가 신청 (nullable - 참가 신청 알림만)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_request_seq")
    private GroupJoinRequest relatedRequest;

    // 읽음 여부
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    // 생성 시간
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 수정 시간
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Notification(User user, NotificationType type, String title, String content,
                        Group relatedGroup, GroupJoinRequest relatedRequest) {
        this.user = user;
        this.type = type;
        this.title = title;
        this.content = content;
        this.relatedGroup = relatedGroup;
        this.relatedRequest = relatedRequest;
        this.isRead = false;
    }

    // 읽음 처리
    public void markAsRead() {
        this.isRead = true;
    }
}