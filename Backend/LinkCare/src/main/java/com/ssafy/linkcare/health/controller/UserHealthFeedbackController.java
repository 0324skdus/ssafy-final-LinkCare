package com.ssafy.linkcare.health.controller;

import com.ssafy.linkcare.gpt.dto.HealthSummaryResponse;
import com.ssafy.linkcare.health.service.UserHealthFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Tag(name = "Health Feedback", description = "건강 피드백 API")
public class UserHealthFeedbackController {

    private final UserHealthFeedbackService feedbackService;

    @GetMapping("/feedback/{userSeq}")
    @Operation(
            summary = "건강 피드백 조회",
            description = "저장된 건강 한줄평을 조회합니다. 없으면 백그라운드에서 생성을 시작하고 기본 메시지를 반환합니다."
    )
    public ResponseEntity<HealthSummaryResponse> getHealthFeedback(
            @PathVariable int userSeq) {
        HealthSummaryResponse response = feedbackService.getHealthFeedback(userSeq);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/feedback/{userSeq}/refresh")
    @Operation(
            summary = "건강 피드백 갱신",
            description = "기존 피드백을 즉시 반환하고, 백그라운드에서 최신 건강 데이터로 새로운 피드백을 생성합니다."
    )
    public ResponseEntity<HealthSummaryResponse> refreshHealthFeedback(
            @PathVariable int userSeq) {
        HealthSummaryResponse response = feedbackService.refreshHealthFeedback(userSeq);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health-summary/{userSeq}")
    @Operation(
            summary = "개인 건강 한줄평 즉시 생성",
            description = "사용자의 일일 건강 데이터를 분석하여 즉시 한줄평과 건강 상태를 생성합니다 (동기)"
    )
    public ResponseEntity<HealthSummaryResponse> getHealthSummary(
            @PathVariable int userSeq) {
        HealthSummaryResponse response = feedbackService.generateFeedbackSync(userSeq);
        return ResponseEntity.ok(response);
    }
}
