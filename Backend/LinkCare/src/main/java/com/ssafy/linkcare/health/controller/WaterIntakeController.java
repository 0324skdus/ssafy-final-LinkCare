package com.ssafy.linkcare.health.controller;

import com.ssafy.linkcare.health.dto.WaterIntakeResponse;
import com.ssafy.linkcare.health.dto.WaterIntakeStatisticsResponse;
import com.ssafy.linkcare.health.service.WaterIntakeService;
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

@Tag(name = "Water Intake", description = "물 섭취 데이터 API")
@Slf4j
@RestController
@RequestMapping("/api/health/water-intake")
@RequiredArgsConstructor
public class WaterIntakeController {

    private final WaterIntakeService waterIntakeService;

    @Operation(summary = "당일 물 섭취 데이터 조회",
            description = "오늘 하루의 물 섭취 데이터를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userSeq}/today")
    public ResponseEntity<List<WaterIntakeResponse>> getTodayWaterIntakes(
            @Parameter(description = "사용자 시퀀스", required = true) @PathVariable int userSeq
    ) {
        log.info("API 호출 - 당일 물 섭취 조회: userSeq={}", userSeq);

        List<WaterIntakeResponse> responses = waterIntakeService.getTodayWaterIntakes(userSeq);

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "이번 주 물 섭취 데이터 조회",
            description = "이번 주의 물 섭취 데이터를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userSeq}/this-week")
    public ResponseEntity<List<WaterIntakeResponse>> getThisWeekWaterIntakes(
            @PathVariable int userSeq
    ) {
        log.info("API 호출 - 이번 주 물 섭취 조회: userSeq={}", userSeq);

        List<WaterIntakeResponse> responses = waterIntakeService.getThisWeekWaterIntakes(userSeq);

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "3주치 물 섭취 데이터 조회",
            description = "최근 3주간의 물 섭취 데이터를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userSeq}/three-weeks")
    public ResponseEntity<List<WaterIntakeResponse>> getThreeWeeksWaterIntakes(
            @PathVariable int userSeq
    ) {
        log.info("API 호출 - 3주치 물 섭취 조회: userSeq={}", userSeq);

        List<WaterIntakeResponse> responses = waterIntakeService.getThreeWeeksWaterIntakes(userSeq);

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "기간별 물 섭취 통계 조회",
            description = "지정된 기간의 물 섭취 통계(총량, 평균, 기록 개수)를 조회합니다. 목표 달성률은 제외됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userSeq}/statistics")
    public ResponseEntity<WaterIntakeStatisticsResponse> getWaterIntakeStatistics(
            @Parameter(description = "사용자 시퀀스", required = true) @PathVariable int userSeq,
            @Parameter(description = "시작 시간 (Unix timestamp)") @RequestParam(required = false) Long startTime,
            @Parameter(description = "종료 시간 (Unix timestamp)") @RequestParam(required = false) Long endTime
    ) {
        log.info("API 호출 - 물 섭취 통계 조회: userSeq={}, startTime={}, endTime={}",
                userSeq, startTime, endTime);

        WaterIntakeStatisticsResponse response = waterIntakeService
                .getWaterIntakeStatistics(userSeq, startTime, endTime);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "(LocalDate) 기간별 물 섭취 통계 조회",
            description = "지정된 기간의 물 섭취 통계(총량, 평균, 기록 개수)를 조회합니다. 목표 달성률은 제외됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userSeq}/date-statistics")
    public ResponseEntity<WaterIntakeStatisticsResponse> getWaterIntakeStatisticsByDate(
            @Parameter(description = "사용자 시퀀스", required = true) @PathVariable int userSeq,
            @Parameter(description = "시작 시간 (LocalDate)") @RequestParam(required = false) LocalDate startTime,
            @Parameter(description = "종료 시간 (LocalDate)") @RequestParam(required = false) LocalDate endTime
    ) {
        log.info("API 호출 - (LocalDate) 물 섭취 통계 조회: userSeq={}, startTime={}, endTime={}",
                userSeq, startTime, endTime);

        WaterIntakeStatisticsResponse response = waterIntakeService
                .getWaterIntakeStatisticsByDate(userSeq, startTime, endTime);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "당일 물 섭취 통계 조회",
            description = "오늘 하루의 물 섭취 통계를 조회합니다. 목표 달성률이 포함됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userSeq}/statistics/today")
    public ResponseEntity<WaterIntakeStatisticsResponse> getTodayWaterIntakeStatistics(
            @PathVariable int userSeq
    ) {
        log.info("API 호출 - 당일 물 섭취 통계 조회: userSeq={}", userSeq);

        WaterIntakeStatisticsResponse response = waterIntakeService
                .getTodayWaterIntakeStatistics(userSeq);

        return ResponseEntity.ok(response);
    }


    @Operation(summary = "이번 주 물 섭취 통계 조회",
            description = "이번 주의 물 섭취 통계를 조회합니다. 목표 달성률은 제외됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userSeq}/statistics/this-week")
    public ResponseEntity<WaterIntakeStatisticsResponse> getThisWeekWaterIntakeStatistics(
            @PathVariable int userSeq
    ) {
        log.info("API 호출 - 이번 주 물 섭취 통계 조회: userSeq={}", userSeq);

        WaterIntakeStatisticsResponse response = waterIntakeService
                .getThisWeekWaterIntakeStatistics(userSeq);

        return ResponseEntity.ok(response);
    }
}
