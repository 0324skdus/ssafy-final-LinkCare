package com.ssafy.linkcare.group.dto;

import com.ssafy.linkcare.group.enums.GroupType;
import java.time.LocalDateTime;

/*
    * 그룹 기본 정보 응답 DTO
    * - 그룹 목록 조회, 검색 결과에 사용
*/
public record GroupResponse(
        Long groupSeq,
        String groupName,
        String groupDescription,
        GroupType type,
        Integer capacity,
        Integer currentMembers,  // 현재 멤버 수
        String imageUrl,
        LocalDateTime createdAt,
        String joinStatus  // "NONE", "PENDING", "MEMBER" - 추가
) {}
