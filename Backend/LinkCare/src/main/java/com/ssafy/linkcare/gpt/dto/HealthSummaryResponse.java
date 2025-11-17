package com.ssafy.linkcare.gpt.dto;

import com.ssafy.linkcare.health.entity.UserHealthFeedback;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * API 응답 전용 DTO
 * 엔티티 데이터를 포함한 완전한 응답
 */
@Getter
@Builder
@AllArgsConstructor
public class HealthSummaryResponse {

    private String summary;
    private HealthStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum HealthStatus {
        PERFECT, GOOD, CAUTION
    }

    /**
     * 분석 중 상태 반환
     */
    public static HealthSummaryResponse analyzing() {
        return HealthSummaryResponse.builder()
                .summary("건강 데이터를 분석 중입니다...")
                .status(HealthStatus.GOOD)
                .build();
    }

    /**
     * 엔티티로부터 응답 생성
     */
    public static HealthSummaryResponse from(UserHealthFeedback feedback) {
        return HealthSummaryResponse.builder()
                .summary(feedback.getContent())
                .status(HealthStatus.valueOf(feedback.getHealthStatus()))
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build();
    }
}