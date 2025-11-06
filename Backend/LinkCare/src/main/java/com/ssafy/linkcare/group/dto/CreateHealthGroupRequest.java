package com.ssafy.linkcare.group.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/*
    * 헬스 그룹 생성 요청 DTO
*/
public record CreateHealthGroupRequest(
        @NotBlank(message = "모임 이름은 필수입니다")
        String groupName,

        @NotBlank(message = "모임 소개는 필수입니다")
        String groupDescription,

        @NotNull(message = "모집 인원은 필수입니다")
        @Min(value = 2, message = "최소 2명 이상이어야 합니다")
        @Max(value = 10, message = "최대 10명까지 가능합니다")
        Integer capacity,

        @Valid
        GoalCriteriaDto goalCriteria  // null 가능 (목표 없이 생성 가능)
) {}
