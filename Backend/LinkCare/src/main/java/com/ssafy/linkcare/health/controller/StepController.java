package com.ssafy.linkcare.health.controller;

import com.ssafy.linkcare.health.dto.ActivitySummaryResponse;
import com.ssafy.linkcare.health.dto.StepResponse;
import com.ssafy.linkcare.health.dto.StepStatisticsResponse;
import com.ssafy.linkcare.health.service.ActivitySummaryService;
import com.ssafy.linkcare.health.service.StepService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/health/step")
@RequiredArgsConstructor
public class StepController {

    private final StepService stepService;

    /**
     * 기간별 Step 조회 (핵심 API)
     * GET /api/health/step/{userSeq}/period?startTime={startTime}&endTime={endTime}
     *
     * Query Parameters:
     * - startTime: Unix timestamp (선택, 기본값: 1년 전)
     * - endTime: Unix timestamp (선택, 기본값: 현재)
     */
    @GetMapping("/{userSeq}/period")
    public ResponseEntity<List<StepResponse>> getStepsByPeriod(
            @PathVariable int userSeq,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime
    ) {
        log.info("기간별 Step 조회 - userSeq: {}, startTime: {}, endTime: {}",
                userSeq, startTime, endTime);

        List<StepResponse> responses = stepService
                .getStepsByPeriod(userSeq, startTime, endTime);

        return ResponseEntity.ok(responses);
    }

    /**
     * 당일 데이터 조회
     * GET /api/health/step/{userSeq}/today
     */
    @GetMapping("/{userSeq}/today")
    public ResponseEntity<StepResponse> getTodayStep(
            @PathVariable int userSeq
    ) {
        log.info("API 호출 - 당일 조회: userSeq={}", userSeq);

        StepResponse responses = stepService
                .getTodayStep(userSeq);

        return ResponseEntity.ok(responses);
    }

    /**
     * 이번 주 데이터 조회
     * GET /api/health/step/{userSeq}/this-week
     */
    @GetMapping("/{userSeq}/this-week")
    public ResponseEntity<List<StepResponse>> getThisWeekActivitySummaries(
            @PathVariable int userSeq
    ) {
        log.info("API 호출 - 이번 주 조회: userSeq={}", userSeq);

        List<StepResponse> responses = stepService
                .getThisWeekSteps(userSeq);

        return ResponseEntity.ok(responses);
    }

    /**
     * 기간 별 총 걸음 수 조회
     * GET /api/health/step/{userSeq}/statistics?startTime={startTime}&endTime={endTime}
     */
    @GetMapping("/{userSeq}/statistics")
    public ResponseEntity<StepStatisticsResponse> getStepStatistics(
            @PathVariable int userSeq,
            @RequestParam Long startTime,
            @RequestParam Long endTIme
    ) {
        log.info("API 호출 - 기간별 총 걸음수 조회: userSeq={}", userSeq);

        StepStatisticsResponse response = stepService.getStepStatistics(userSeq,startTime, endTIme);

        return ResponseEntity.ok(response);
    }

    /**
     * 해당 날짜가 포함된 일주일(월~일) 총 걸음 수 조회
     * GET /api/health/step/{userSeq}/weekly-statistics/{date}
     */
    @GetMapping("/{userSeq}/weekly-statistics/{startDate}")
    public ResponseEntity<StepStatisticsResponse> getWeekStepStatistics(
            @PathVariable int userSeq,
            @PathVariable LocalDate startDate) {

        log.info("API 호출 - 해당 날짜 포함 일주일 총 걸음 수 조회: userSeq ={}, startDate ={}", userSeq, startDate);

        StepStatisticsResponse response =
                stepService.getWeekStepStatistics(userSeq, startDate);

        return ResponseEntity.ok(response);
    }

    /**
     * 기간별 총 걸음 수 조회
     * GET /api/health/step/{userSeq}/weekly-statistics
     */
    @GetMapping("/{userSeq}/weekly-statistics")
    public ResponseEntity<StepStatisticsResponse> getWeekStepStatistics(
            @PathVariable int userSeq,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        log.info("API 호출 - 해당 날짜 포함 일주일 총 걸음 수 조회: userSeq ={}, startDate ={}, endDate= {}",
                userSeq, startDate, endDate);

        StepStatisticsResponse response =
                stepService.getWeekStepStatistics(userSeq, startDate, endDate);

        return ResponseEntity.ok(response);
    }

}
