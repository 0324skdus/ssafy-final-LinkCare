package com.ssafy.linkcare.gpt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthSummaryResponse {
    private String summary;           // 한줄평
    private HealthStatus status;      // 건강상태

    public enum HealthStatus {
        PERFECT("완벽"),
        GOOD("양호"),
        CAUTION("주의");

        private final String description;

        HealthStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
