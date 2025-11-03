package com.ssafy.linkcare.group.enums;

/*
    * 가입 신청 상태
        * - PENDING: 승인 대기 중
        * - APPROVED: 승인됨
        * - REJECTED: 거절됨
*/
public enum RequestStatus {
    PENDING,    // 승인 대기 중
    APPROVED,   // 승인됨
    REJECTED    // 거절됨
}
