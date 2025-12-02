package com.ssafy.linkcare.ai.comment.entity;

import com.ssafy.linkcare.group.entity.Group;
import com.ssafy.linkcare.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * AI 생성 한줄평 엔티티
 * - 사용자가 AI로 생성한 한줄평 저장
 * - 그룹별로 1개씩 저장 가능
 */
@Entity
@Table(
    name = "user_ai_comments",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_group", columnNames = {"user_pk", "group_seq"})
    },
    indexes = {
        @Index(name = "idx_user_group", columnList = "user_pk, group_seq"),
        @Index(name = "idx_group_updated", columnList = "group_seq, updated_at")
    }
)
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserAiComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_ai_comment_seq")
    private Long userAiCommentSeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_pk", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_seq", nullable = false)
    private Group group;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String comment;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public UserAiComment(User user, Group group, String comment) {
        this.user = user;
        this.group = group;
        this.comment = comment;
    }

    /**
     * 한줄평 업데이트
     */
    public void updateComment(String comment) {
        this.comment = comment;
    }
}
