package com.ssafy.linkcare.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    // 그룹 관련 알림
    GROUP_JOIN_REQUEST("참가 신청", "GROUP"),          // 방장에게: 누군가 그룹 참가 신청
    GROUP_JOIN_APPROVED("참가 승인", "GROUP"),         // 신청자에게: 참가 신청 승인됨
    GROUP_JOIN_REJECTED("참가 거절", "GROUP"),         // 신청자에게: 참가 신청 거절됨
    GROUP_PERMISSION_CHANGED("권한 변경", "GROUP");    // 그룹원에게: 그룹 권한 설정 변경됨

    private final String description;  // 알림 타입 설명
    private final String category;     // 알림 카테고리 (탭 구분용: GROUP, FRIEND, etc.)
}
