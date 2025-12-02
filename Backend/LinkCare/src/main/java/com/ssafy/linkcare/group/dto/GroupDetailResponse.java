package com.ssafy.linkcare.group.dto;

import com.ssafy.linkcare.group.enums.GroupType;

import java.time.LocalDateTime;
import java.util.List;

/*
    * 그룹 상세 정보 응답 DTO
        * - 그룹 상세 조회 시 사용
        * - 멤버 목록, 목표 기준 포함
*/
public record GroupDetailResponse(
        Long groupSeq,
        String groupName,
        String groupDescription,
        GroupType type,
        Integer capacity,
        Integer currentMembers,  // 현재 인원 수
        String imageUrl,
        LocalDateTime createdAt,
        GoalCriteriaDto goalCriteria,  // HEALTH 그룹인 경우에만 존재 (nullable)
        List<MemberDto> members,
        Long currentUserSeq  // 현재 로그인한 사용자의 userSeq
) {
    // 멤버 정보 (Nested Record)
    public record MemberDto(
            Long groupMemberSeq,
            Long userSeq,
            String userName,
            Boolean isLeader,
            String mainCharacterBaseImageUrl
    ) {}
}
