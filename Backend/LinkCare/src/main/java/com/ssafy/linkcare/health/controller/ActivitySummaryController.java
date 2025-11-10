package com.ssafy.linkcare.health.controller;

import com.ssafy.linkcare.health.dto.ActivitySummaryResponse;
import com.ssafy.linkcare.health.service.ActivitySummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Activity Summary", description = "활동 요약 API")
@Slf4j
@RestController
@RequestMapping("/api/health/activity-summary")
@RequiredArgsConstructor
public class ActivitySummaryController {

    private final ActivitySummaryService activitySummaryService;

    @Operation(summary = "기간별 활동 요약 조회",
            description = "지정된 기간의 활동 요약 데이터를 조회합니다. startTime과 endTime을 지정하지 않으면 최근 1년치 데이터를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userSeq}/period")
    public ResponseEntity<List<ActivitySummaryResponse>> getActivitySummariesByPeriod(
            @Parameter(description = "사용자 시퀀스", required = true) @PathVariable int userSeq,
            @Parameter(description = "시작 시간 (Unix timestamp, 초 단위)") @RequestParam(required = false) Long startTime,
            @Parameter(description = "종료 시간 (Unix timestamp, 초 단위)") @RequestParam(required = false) Long endTime
    ) {
        log.info("기간별 ActivitySummary 조회 - userSeq: {}, startTime: {}, endTime: {}",
                userSeq, startTime, endTime);

        List<ActivitySummaryResponse> responses = activitySummaryService
                .getActivitySummariesByPeriod(userSeq, startTime, endTime);

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "당일 활동 요약 조회",
            description = "오늘 하루의 활동 요약 데이터를 조회합니다. 데이터가 없으면 0으로 초기화된 응답을 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userSeq}/today")
    public ResponseEntity<ActivitySummaryResponse> getTodayActivitySummaries(
            @Parameter(description = "사용자 시퀀스", required = true) @PathVariable int userSeq
    ) {
        log.info("API 호출 - 당일 조회: userSeq={}", userSeq);

        ActivitySummaryResponse responses = activitySummaryService
                .getTodayActivitySummary(userSeq);

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "이번 주 활동 요약 조회",
            description = "이번 주 월요일부터 현재까지의 활동 요약 데이터를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userSeq}/this-week")
    public ResponseEntity<List<ActivitySummaryResponse>> getThisWeekActivitySummaries(
            @Parameter(description = "사용자 시퀀스", required = true) @PathVariable int userSeq
    ) {
        log.info("API 호출 - 이번 주 조회: userSeq={}", userSeq);

        List<ActivitySummaryResponse> responses = activitySummaryService
                .getThisWeekActivitySummaries(userSeq);

        return ResponseEntity.ok(responses);
    }


}
