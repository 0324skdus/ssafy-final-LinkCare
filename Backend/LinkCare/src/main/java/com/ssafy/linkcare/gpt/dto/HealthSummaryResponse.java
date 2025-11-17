package com.ssafy.linkcare.gpt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthSummaryResponse {
    private String summary;           // 한줄평
    private HealthStatus status;      // 건강상태
    private LocalDateTime updatedAt;
    public enum HealthStatus {
        ANALYZING("분석 중"),
        PERFECT("완벽"),
        GOOD("양호"),
        CAUTION("주의"),
        UNKNOWN("알 수 없음");   // 에러 발생 시


        private final String description;

        HealthStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 분석 중 상태 생성 헬퍼 메서드
    public static HealthSummaryResponse analyzing() {
        return HealthSummaryResponse.builder()
                .summary("건강 데이터를 분석하고 있습니다. 잠시만 기다려주세요.")
                .status(HealthStatus.ANALYZING)
                .build();
    }

    // 데이터 없음 상태 생성 헬퍼 메서드
    public static HealthSummaryResponse noData() {
        return HealthSummaryResponse.builder()
                .summary("아직 건강 데이터가 충분하지 않습니다. 활동을 기록해보세요!")
                .status(HealthStatus.UNKNOWN)
                .build();
    }
}
