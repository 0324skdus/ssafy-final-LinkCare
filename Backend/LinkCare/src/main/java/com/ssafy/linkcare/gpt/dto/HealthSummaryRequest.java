package com.ssafy.linkcare.gpt.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * GPT 파싱 전용 DTO
 * 날짜 정보 없이 GPT 응답의 상태와 요약만 담음
 */
@Getter
@AllArgsConstructor
public class HealthSummaryRequest {
    private String summary;
    private HealthStatus status;

    public enum HealthStatus {
        PERFECT, GOOD, CAUTION
    }
}