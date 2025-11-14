package com.ssafy.linkcare.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Steps 성장률 예측 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepsPredictRequest {
    private int member_count;
    private double avg_age;
    private double avg_bmi;
    private double group_steps_mean_3w;
    private double group_steps_var_3w;  // 표준편차 (필드명은 var이지만 실제 값은 std)
    private double group_duration_mean_3w;
    private double member_steps_var;    // 표준편차 (필드명은 var이지만 실제 값은 std)
}
