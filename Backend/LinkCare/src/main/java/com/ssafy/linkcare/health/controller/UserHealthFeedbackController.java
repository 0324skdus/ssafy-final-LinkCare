package com.ssafy.linkcare.health.controller;

import com.ssafy.linkcare.gpt.dto.HealthSummaryResponse;
import com.ssafy.linkcare.health.service.UserHealthFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Tag(name = "Health Feedback", description = "건강 피드백 API")
public class UserHealthFeedbackController {

    private final UserHealthFeedbackService feedbackService;

    @GetMapping("/feedback/{userSeq}/{date}")
    @Operation(
            summary = "건강 피드백 조회",
            description = "저장된 건강 한줄평을 조회합니다. 없으면 백그라운드에서 생성을 시작하고 기본 메시지를 반환합니다."
    )
    public ResponseEntity<HealthSummaryResponse> getHealthFeedback(
            @PathVariable int userSeq,
            @PathVariable LocalDate date
    ) {
        HealthSummaryResponse response = feedbackService.getHealthFeedback(userSeq, date);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/feedback/{userSeq}/refresh/{date}")
    @Operation(
            summary = "건강 피드백 갱신",
            description = "기존 피드백을 즉시 반환하고, 백그라운드에서 최신 건강 데이터로 새로운 피드백을 생성합니다."
    )
    public ResponseEntity<HealthSummaryResponse> refreshHealthFeedback(
            @PathVariable int userSeq,
            @PathVariable LocalDate date
    ) {
        HealthSummaryResponse response = feedbackService.refreshHealthFeedback(userSeq, date);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 날짜의 건강 피드백 조회
     */
    @GetMapping("/{userSeq}")
    @Operation(summary = "특정 날짜 건강 피드백 조회", description = "사용자의 특정 날짜 건강 피드백을 조회합니다")
    public ResponseEntity<HealthSummaryResponse> getFeedbackByDate(
            @PathVariable int userSeq,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        HealthSummaryResponse response = feedbackService.getHealthFeedback(userSeq, date);
        return ResponseEntity.ok(response);
    }


    /**
     * 피드백 수동 생성 (관리자용)
     */
    @PostMapping("/generate/{userSeq}")
    @Operation(summary = "피드백 수동 생성", description = "사용자의 건강 피드백을 수동으로 생성합니다")
    public ResponseEntity<String> generateFeedback(
            @PathVariable int userSeq,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        feedbackService.generateFeedbackAsync(userSeq, date);
        return ResponseEntity.ok("피드백 생성이 시작되었습니다.");
    }
}
