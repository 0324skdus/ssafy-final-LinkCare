package com.ssafy.linkcare.notification.dto;

import com.ssafy.linkcare.notification.enums.NotificationType;

import java.time.LocalDateTime;

/*
    * 알림 목록 응답 DTO
        * - 알림함에서 보여줄 알림 정보
*/
public record NotificationResponse(
        Long notificationId,
        NotificationType type,
        String title,                // 그룹 이름
        String content,              // 알림 내용
        Long relatedGroupSeq,        // 관련 그룹 ID (nullable)
        Long relatedRequestSeq,      // 관련 참가 신청 ID (nullable - 승인/거절 버튼용)
        Boolean isRead,
        LocalDateTime createdAt
) {
}
