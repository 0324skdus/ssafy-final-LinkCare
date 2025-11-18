package com.ssafy.linkcare.ai.comment.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자 한줄평 DTO
 */
@Getter
@Builder
public class UserCommentDto {
    private Long userSeq;
    private String userName;
    private String comment;  // nullable (한줄평이 없을 수 있음)
    private LocalDateTime updatedAt;  // nullable
}
