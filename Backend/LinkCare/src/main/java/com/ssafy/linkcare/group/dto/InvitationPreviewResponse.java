package com.ssafy.linkcare.group.dto;

import com.ssafy.linkcare.group.enums.GroupType;
import java.time.LocalDateTime;

/*
    * 초대 링크로 접근 시 그룹 정보 미리보기 응답
    * 사용자가 참여 여부를 결정하기 위한 정보 제공
    * 방장이 요구하는 권한 정보 포함
*/
public record InvitationPreviewResponse(
        Long groupSeq,
        String groupName,
        String groupDescription,
        GroupType type,
        Integer capacity,
        Integer currentMembers,
        String imageUrl,
        LocalDateTime createdAt,
        Boolean isExpired,          // 초대 링크 만료 여부
        Boolean isFull,             // 정원 초과 여부
        String invitationToken,     // 참여 시 사용할 토큰

        // 방장이 설정한 권한 정보 (필수 + 선택)
        RequiredPermissions requiredPermissions,
        OptionalPermissions optionalPermissions
) {
    /**
     * 필수 권한 (항상 true, 사용자가 거부 불가)
     */
    public record RequiredPermissions(
            Boolean isDailyStepAllowed,      // 걸음수 (항상 true)
            Boolean isHeartRateAllowed,      // 심박수 (항상 true)
            Boolean isExerciseAllowed        // 운동 (항상 true)
    ) {}

    /**
     * 선택 권한 (방장이 설정한 값, 사용자가 거부 가능)
     */
    public record OptionalPermissions(
            Boolean isSleepAllowed,          // 수면
            Boolean isWaterIntakeAllowed,    // 물 섭취량
            Boolean isBloodPressureAllowed,  // 혈압
            Boolean isBloodSugarAllowed      // 혈당
    ) {}
}