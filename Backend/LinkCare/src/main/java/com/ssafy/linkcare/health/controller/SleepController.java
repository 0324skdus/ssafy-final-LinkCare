package com.ssafy.linkcare.health.controller;

import com.ssafy.linkcare.health.dto.SleepResponse;
import com.ssafy.linkcare.health.dto.SleepStatisticsResponse;
import com.ssafy.linkcare.health.service.SleepService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Sleep", description = "수면 데이터 API")
@Slf4j
@RestController
@RequestMapping("/api/health/sleep")
@RequiredArgsConstructor
public class SleepController {

    private final SleepService sleepService;


     @Operation(summary = "당일 수면 데이터 조회",
     description = "오늘 일어난(종료된) 수면 데이터를 조회합니다. endTime 기준으로 조회됩니다.")
     @ApiResponses(value = {
     @ApiResponse(responseCode = "200", description = "조회 성공"),
     @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
     })
     @GetMapping("/{userSeq}/today")
     public ResponseEntity<List<SleepResponse>> getTodaySleeps(
     @Parameter(description = "사용자 시퀀스", required = true) @PathVariable int userSeq
     ){
        log.info("API 호출 - 당일 수면 조회: userSeq={}", userSeq);

        List<SleepResponse> responses = sleepService.getTodaySleeps(userSeq);

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "이번 주 수면 데이터 조회",
            description = "이번 주에 일어난(종료된) 수면 데이터를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userSeq}/this-week")
    public ResponseEntity<List<SleepResponse>> getThisWeekSleeps(
            @PathVariable int userSeq
    ) {
        log.info("API 호출 - 이번 주 수면 조회: userSeq={}", userSeq);

        List<SleepResponse> responses = sleepService.getThisWeekSleeps(userSeq);

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "기간별 수면 데이터 조회 (Timestamp)",
            description = "Unix timestamp 기준으로 기간별 수면 데이터를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userSeq}/period")
    public ResponseEntity<List<SleepResponse>> getSleepsByPeriod(
            @Parameter(description = "사용자 시퀀스", required = true) @PathVariable int userSeq,
            @Parameter(description = "시작 시간 (Unix timestamp)") @RequestParam(required = false) Long startTime,
            @Parameter(description = "종료 시간 (Unix timestamp)") @RequestParam(required = false) Long endTime
    ) {
        log.info("API 호출 - 특정 기간 수면 데이터 조회: userSeq = {}, " +
                "startTime = {}, endTime = {}", userSeq, startTime, endTime);

        List<SleepResponse> responses = sleepService.getSleepsByPeriod(userSeq, startTime, endTime);

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "기간별 수면 데이터 조회 (LocalDate)",
            description = "LocalDate 기준으로 기간별 수면 데이터를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 날짜 형식"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userSeq}/date-period")
    public ResponseEntity<List<SleepResponse>> getSleepsByDate(
            @Parameter(description = "사용자 시퀀스", required = true) @PathVariable int userSeq,
            @Parameter(description = "시작 날짜") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDate startDate,
            @Parameter(description = "종료 날짜") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDate endDate
    ) {
        log.info("API 호출 - 특정 기간 수면 데이터 조회: userSeq = {}, " +
                "startTime = {},  endTime = {}", userSeq, startDate, endDate);

        List<SleepResponse> responses = sleepService.getSleepsByDate(userSeq, startDate, endDate);

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "3주치 수면 데이터 조회",
            description = "최근 3주간의 수면 데이터를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userSeq}/three-weeks")
    public ResponseEntity<List<SleepResponse>> getThreeWeeksSleeps(
            @PathVariable int userSeq
    ) {
        log.info("API 호출 - 3주치 수면 조회: userSeq={}", userSeq);

        List<SleepResponse> responses = sleepService.getThreeWeeksSleeps(userSeq);

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "(TimeStamp) 기간별 수면 통계 조회",
            description = "지정된 기간의 수면 통계(총 수면 시간, 평균, 최대/최소)를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userSeq}/statistics")
    public ResponseEntity<SleepStatisticsResponse> getSleepStatistics(
            @Parameter(description = "사용자 시퀀스", required = true) @PathVariable int userSeq,
            @Parameter(description = "시작 시간 (Unix timestamp)") @RequestParam(required = false) Long startTime,
            @Parameter(description = "종료 시간 (Unix timestamp)") @RequestParam(required = false) Long endTime
    ) {
        log.info("API 호출 - 수면 통계 조회: userSeq={}, startTime={}, endTime={}",
                userSeq, startTime, endTime);

        SleepStatisticsResponse response = sleepService
                .getSleepStatistics(userSeq, startTime, endTime);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "(LocalDate) 기간별 수면 통계 조회",
            description = "지정된 기간의 수면 통계(총 수면 시간, 평균, 최대/최소)를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userSeq}/statistics/date")
    public ResponseEntity<SleepStatisticsResponse> getSleepStatisticsByDate(
            @Parameter(description = "사용자 시퀀스", required = true) @PathVariable int userSeq,
            @Parameter(description = "시작 날짜") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDate startDate,
            @Parameter(description = "종료 날짜") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDate endDate
    ) {
        log.info("API 호출 - (LocalDate) 수면 통계 조회: userSeq={}, startTime={}, endTime={}",
                userSeq, startDate, endDate);

        SleepStatisticsResponse response = sleepService
                .getSleepStatisticsByDate(userSeq, startDate, endDate);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "당일 수면 통계 조회",
            description = "오늘 하루의 수면 통계를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userSeq}/statistics/today")
    public ResponseEntity<SleepStatisticsResponse> getTodaySleepStatistics(
            @PathVariable int userSeq
    ) {
        log.info("API 호출 - 당일 수면 통계 조회: userSeq={}", userSeq);

        SleepStatisticsResponse response = sleepService
                .getTodaySleepStatistics(userSeq);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "이번 주 수면 통계 조회",
            description = "이번 주의 수면 통계를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userSeq}/statistics/this-week")
    public ResponseEntity<SleepStatisticsResponse> getThisWeekSleepStatistics(
            @PathVariable int userSeq
    ) {
        log.info("API 호출 - 이번 주 수면 통계 조회: userSeq={}", userSeq);

        SleepStatisticsResponse response = sleepService
                .getThisWeekSleepStatistics(userSeq);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "기간별 수면 시간 조회",
            description = "LocalDate 기준으로 기간별 수면 시간을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 날짜 형식"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userSeq}/total-duration/date-period")
    public ResponseEntity<List<Long>> getTotalSleepDurationByDate(
            @Parameter(description = "사용자 시퀀스", required = true) @PathVariable int userSeq,
            @Parameter(description = "시작 날짜") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDate startDate,
            @Parameter(description = "종료 날짜") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDate endDate
    ) {
        log.info("API 호출 - 기간별 수면 시간 조회: userSeq = {}, " +
                "startTime = {},  endTime = {}", userSeq, startDate, endDate);

        List<Long> responses = sleepService.getTotalSleepDurationByDate(userSeq, startDate, endDate);

        return ResponseEntity.ok(responses);
    }
}
