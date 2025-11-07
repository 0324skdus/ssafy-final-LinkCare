package com.ssafy.linkcare.health.controller;

import com.ssafy.linkcare.health.dto.ActivitySummaryResponse;
import com.ssafy.linkcare.health.service.ActivitySummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/health/activity-summary")
@RequiredArgsConstructor
public class ActivitySummaryController {

    private final ActivitySummaryService activitySummaryService;

    /**
     * 1. 기간별 ActivitySummary 조회 (핵심 API)
     * GET /api/health/activity-summary/{userSeq}/period?startTime={startTime}&endTime={endTime}
     *
     * Query Parameters:
     * - startTime: Unix timestamp (선택, 기본값: 1년 전)
     * - endTime: Unix timestamp (선택, 기본값: 현재)
     */
    @GetMapping("/{userSeq}/period")
    public ResponseEntity<List<ActivitySummaryResponse>> getActivitySummariesByPeriod(
            @PathVariable Long userSeq,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime
    ) {
        log.info("기간별 ActivitySummary 조회 - userSeq: {}, startTime: {}, endTime: {}",
                userSeq, startTime, endTime);

        List<ActivitySummaryResponse> responses = activitySummaryService
                .getActivitySummariesByPeriod(userSeq, startTime, endTime);

        return ResponseEntity.ok(responses);
    }
}
