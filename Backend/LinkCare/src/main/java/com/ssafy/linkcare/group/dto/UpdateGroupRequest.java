package com.ssafy.linkcare.group.dto;

import jakarta.validation.constraints.NotBlank;

/*
    * 그룹 정보 수정 요청 DTO
        * - 방장만 수정 가능
        * - 그룹명, 소개 수정 가능
        * - 이미지는 별도 MultipartFile로 전달
        * - 케어 그룹인 경우 선택 권한 항목도 수정 가능
        * - 헬스 그룹인 경우 목표 기준도 수정 가능
*/
public record UpdateGroupRequest(
        @NotBlank(message = "모임 이름은 필수입니다")
        String groupName,

        @NotBlank(message = "모임 소개는 필수입니다")
        String groupDescription,

        // 케어 그룹 전용: 선택 권한 항목 (null 가능)
        Boolean isSleepRequired,
        Boolean isWaterIntakeRequired,
        Boolean isBloodPressureRequired,
        Boolean isBloodSugarRequired,

        // 헬스 그룹 전용: 목표 기준 (null 가능)
        GoalCriteriaDto goalCriteria
) {}
