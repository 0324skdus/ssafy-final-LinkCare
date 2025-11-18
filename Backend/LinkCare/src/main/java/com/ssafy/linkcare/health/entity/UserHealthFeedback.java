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

import java.time.LocalDate;
import java.time.LocalDateTime;

/*
 * 유저 건강 피드백 엔티티
 *
 * 역할: 사용자의 건강 상태와 AI 피드백을 저장
 *
 * 특징:
 *   - 사용자당 하루에 1개의 피드백만 존재 (N:1 관계)
 *   - 사용자의 해당 날짜 건강 상태 기록
 *   - AI가 생성한 건강 관련 피드백/조언 저장
 *
 * 연관 엔티티:
 *   - User (N:1) - 하루에 한개 피드백을 받는 사용자 (사용자는 일별로 1개의 피드백을 가지고 있음)
 */
@Entity
@Table(name = "user_health_feedback",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_date",
                        columnNames = {"user_seq", "created_at"}
                )
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserHealthFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_health_feedback_id")
    private Integer userHealthFeedbackId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_seq")
    private User user;

    @Column(name = "health_status", length = 20)
    private String healthStatus;

    @Column(name = "content", length = 100)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public UserHealthFeedback(User user, String healthStatus, String content, LocalDateTime createdAt) {
        this.user = user;
        this.healthStatus = healthStatus;
        this.content = content;
        this.createdAt = createdAt;
    }

    // 피드백 수정 메서드
    public void updateFeedback(String healthStatus, String content) {
        this.healthStatus = healthStatus;
        this.content = content;
    }
}