package com.ssafy.linkcare.group.dto;

import jakarta.validation.constraints.Min;

/*
    * 헬스 그룹 목표 기준 DTO
        * - 최소 칼로리, 걸음수, 거리, 운동시간 설정
        * - 모두 선택 사항 (하나라도 설정 가능)
*/
public record GoalCriteriaDto(
        @Min(value = 0, message = "최소 칼로리는 0 이상이어야 합니다")
        Float minCalorie,  // 최소 칼로리 (kcal)

        @Min(value = 0, message = "최소 걸음수는 0 이상이어야 합니다")
        Integer minStep,  // 최소 걸음수 (보)

        @Min(value = 0, message = "최소 거리는 0 이상이어야 합니다")
        Float minDistance,  // 최소 거리 (km)

        @Min(value = 0, message = "최소 운동시간은 0 이상이어야 합니다")
        Integer minDuration  // 최소 운동시간 (분)
) {}
