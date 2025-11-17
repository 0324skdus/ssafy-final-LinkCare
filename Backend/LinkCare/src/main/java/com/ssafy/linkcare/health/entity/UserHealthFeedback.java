package com.ssafy.linkcare.health.entity;

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

/*
 * 유저 건강 피드백 엔티티
 *
 * 역할: 사용자의 건강 상태와 AI 피드백을 저장
 *
 * 특징:
 *   - 사용자당 1개의 피드백만 존재 (1:1 관계)
 *   - 사용자의 현재 건강 상태 기록
 *   - AI가 생성한 건강 관련 피드백/조언 저장
 *
 * 연관 엔티티:
 *   - User (1:1) - 피드백을 받는 사용자
 */
@Entity
@Table(name = "user_health_feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserHealthFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_health_feedback_id")
    private Integer userHealthFeedbackId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_seq", nullable = false, unique = true)  // unique 제약조건 추가
    private User user;

    @Column(name = "health_status", length = 20)
    private String healthStatus;

    @Column(name = "content", length = 100)
    private String content;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public UserHealthFeedback(User user, String healthStatus, String content) {
        this.user = user;
        this.healthStatus = healthStatus;
        this.content = content;
    }

    // 피드백 수정 메서드
    public void updateFeedback(String healthStatus, String content) {
        this.healthStatus = healthStatus;
        this.content = content;
    }
}