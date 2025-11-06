package com.ssafy.linkcare.group.dto;

import java.time.LocalDateTime;

/*
    * 초대 링크 응답 DTO
*/
public record InvitationResponse(
        Long invitationSeq,
        Long groupSeq,
        String groupName,
        String invitationToken,
        LocalDateTime createdAt,
        LocalDateTime expiredAt,
        String invitationUrl  // 프론트엔드에서 사용할 전체 URL
) {}
