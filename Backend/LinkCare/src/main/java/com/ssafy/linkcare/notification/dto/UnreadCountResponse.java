package com.ssafy.linkcare.notification.dto;

/*
    * 안 읽은 알림 개수 응답 DTO
*/
public record UnreadCountResponse(
        Long totalCount,      // 전체 안 읽은 알림 개수
        Long groupCount       // 그룹 카테고리 안 읽은 알림 개수
) {
}
