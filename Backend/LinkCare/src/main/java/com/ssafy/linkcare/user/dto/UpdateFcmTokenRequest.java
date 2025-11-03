package com.ssafy.linkcare.user.dto;

/*
    * FCM 토큰 업데이트 요청 DTO
        * - 안드로이드 앱에서 FCM 토큰을 서버로 전송할 때 사용
*/
public record UpdateFcmTokenRequest(
        String fcmToken
) {}
