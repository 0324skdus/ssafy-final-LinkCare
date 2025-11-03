package com.ssafy.linkcare.group.dto;

import com.ssafy.linkcare.group.enums.RequestStatus;

import java.time.LocalDateTime;

/*
    * 그룹 참가 신청 응답 DTO
    * 방장이 대기 중인 신청 목록을 볼 때 사용
*/
public record JoinRequestResponse(
        Long requestSeq,
        Long userSeq,
        String userName,
        Integer userAge,
        String userGender,
        RequestStatus status,
        LocalDateTime requestedAt
) {}
