package com.ssafy.linkcare.ai.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 한줄평 업로드 요청 DTO
 */
@Getter
@NoArgsConstructor
public class CommentUploadRequest {

    @NotNull(message = "그룹 시퀀스는 필수입니다.")
    private Long groupSeq;

    @NotBlank(message = "한줄평은 필수입니다")
    @Size(min = 1, max = 200, message = "한줄평은 1~200자까지 입력 가능합니다")
    private String comment;
}
