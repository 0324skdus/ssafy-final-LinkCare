package com.ssafy.linkcare.ai.comment.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 한줄평 일괄 조회 응답 DTO
 */
@Getter
@Builder
public class BatchCommentsResponse {
    private List<UserCommentDto> comments;
}
