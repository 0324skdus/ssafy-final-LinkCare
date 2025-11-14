package com.ssafy.linkcare.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 성장률 예측 응답 DTO (공통)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiPredictResponse {
    private double predicted_growth_rate;
}
