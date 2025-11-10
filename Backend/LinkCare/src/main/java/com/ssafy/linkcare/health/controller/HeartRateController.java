package com.ssafy.linkcare.health.controller;

import com.ssafy.linkcare.health.dto.HeartRateResponse;
import com.ssafy.linkcare.health.dto.StepResponse;
import com.ssafy.linkcare.health.service.HeartRateService;
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
@RequestMapping("/api/health/heart-rate")
@RequiredArgsConstructor
public class HeartRateController {

    private final HeartRateService heartRateService;

    /**
     * 기간별 HeartRate 조회 (핵심 API)
     * GET /api/health/heart-rate/{userSeq}/period?startTime={startTime}&endTime={endTime}
     *
     * Query Parameters:
     * - startTime: Unix timestamp (선택, 기본값: 3주 전)
     * - endTime: Unix timestamp (선택, 기본값: 현재)
     */
    @GetMapping("/{userSeq}/period")
    public ResponseEntity<List<HeartRateResponse>> getHeartRatesByPeriod(
            @PathVariable int userSeq,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime
    ) {
        log.info("기간별 HeartRate 조회 - userSeq: {}, startTime: {}, endTime: {}",
                userSeq, startTime, endTime);

        List<HeartRateResponse> responses = heartRateService
                .getHeartRatesByPeriod(userSeq, startTime, endTime);

        return ResponseEntity.ok(responses);
    }

    /**
     * (LocalDate) 기간별 HeartRate 조회
     * GET /api/health/heart-rate/{userSeq}/period?startTime={startTime}&endTime={endTime}
     */
    @GetMapping("/{userSeq}/date-period")
    public ResponseEntity<List<HeartRateResponse>> getHeartRatesByPeriod(
            @PathVariable int userSeq,
            @RequestParam(required = false) LocalDate startTime,
            @RequestParam(required = false) LocalDate endTime
    ) {
        log.info("(LocalDate) 기간별 HeartRate 조회 - userSeq: {}, startTime: {}, endTime: {}",
                userSeq, startTime, endTime);

        List<HeartRateResponse> responses = heartRateService
                .getHeartRatesByPeriod(userSeq, startTime, endTime);

        return ResponseEntity.ok(responses);
    }

    /**
     * 당일 데이터 조회
     * GET /api/health/heart-rate/{userSeq}/today
     */
    @GetMapping("/{userSeq}/today")
    public ResponseEntity<List<HeartRateResponse>> getTodayHeartRate(
            @PathVariable int userSeq
    ) {
        log.info("API 호출 - 당일 조회: userSeq={}", userSeq);

        List<HeartRateResponse> responses = heartRateService
                .getTodayHeartRate(userSeq);

        return ResponseEntity.ok(responses);
    }

    /**
     * 이번 주 데이터 조회
     * GET /api/health/heart-rate/{userSeq}/this-week
     */
    @GetMapping("/{userSeq}/this-week")
    public ResponseEntity<List<HeartRateResponse>> getThisWeekHeartRates(
            @PathVariable int userSeq
    ) {
        log.info("API 호출 - 이번 주 조회: userSeq={}", userSeq);

        List<HeartRateResponse> responses = heartRateService
                .getThisWeekHeartRates(userSeq);

        return ResponseEntity.ok(responses);
    }


}
