package com.ssafy.linkcare.ai.comment.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 한줄평 업로드 응답 DTO
 */
@Getter
@Builder
public class CommentUploadResponse {
    private Long userSeq;
    private Long groupSeq;
    private String comment;
    private LocalDateTime updatedAt;
}
