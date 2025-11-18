package com.ssafy.linkcare.ai.comment.controller;

import com.ssafy.linkcare.ai.comment.dto.BatchCommentsResponse;
import com.ssafy.linkcare.ai.comment.dto.CommentUploadRequest;
import com.ssafy.linkcare.ai.comment.dto.CommentUploadResponse;
import com.ssafy.linkcare.ai.comment.service.AiCommentService;
import com.ssafy.linkcare.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 한줄평 컨트롤러
 * - POST /api/ai/comment: 한줄평 업로드/갱신
 * -  GET /api/ai/comments/group/{groupSeq}: 그룹 멤버 한줄평 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Comment", description = "AI 생성 한줄평 API")
public class AiCommentController {

    private final AiCommentService aiCommentService;

    /**
     * 한줄평 업로드/갱신
     *
     * @param authentication 인증 정보
     * @param request 한줄평 업로드 요청 (groupSeq, comment)
     * @return 저장된 한줄평 정보
     */
    @PostMapping("/comment")
    @Operation(
        summary = "한줄평 업로드",
        description = "사용자의 AI 생성 한줄평을 업로드하거나 갱신합니다. " +
                      "그룹 멤버만 작성 가능하며, 기존 한줄평이 있으면 수정됩니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "한줄평 저장 성공"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (Validation 실패)"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "권한 없음 (그룹 멤버가 아님)"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "사용자 또는 그룹을 찾을 수 없음"
        )
    })
    public ResponseEntity<ApiResponse<CommentUploadResponse>> uploadComment(
            Authentication authentication,
            @Valid @RequestBody CommentUploadRequest request) {

        // 인증된 사용자 PK 추출
        Long userPk = Long.parseLong(authentication.getName());

        log.info("한줄평 업로드 API 호출 - userPk: {}, groupSeq: {}", userPk, request.getGroupSeq());

        CommentUploadResponse response = aiCommentService.upsertComment(userPk, request);

        return ResponseEntity.ok(
            ApiResponse.success("한줄평이 저장되었습니다", HttpStatus.OK.value(), response)
        );
    }



    /**
     * 그룹의 모든 멤버 한줄평 조회
     *
     * @param groupSeq 그룹 PK
     * @return 그룹 멤버별 한줄평 목록 (한줄평이 없는 멤버는 comment가 null)
     */
    @GetMapping("/comments/group/{groupSeq}")
    @Operation(
        summary = "그룹 멤버 한줄평 조회",
        description = "특정 그룹의 모든 멤버의 한줄평을 조회합니다. " +
                      "한줄평이 없는 멤버도 포함되며, comment와 updatedAt은 null로 반환됩니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "그룹을 찾을 수 없음"
        )
    })
    public ResponseEntity<ApiResponse<BatchCommentsResponse>> getGroupComments(
            @PathVariable Long groupSeq) {

        log.info("그룹 멤버 한줄평 조회 API 호출 - groupSeq: {}", groupSeq);

        BatchCommentsResponse response = aiCommentService.getGroupMembersComments(groupSeq);

        return ResponseEntity.ok(
            ApiResponse.success("조회 완료", HttpStatus.OK.value(), response)
        );
    }
}
