package com.ssafy.linkcare.health.controller;

import com.ssafy.linkcare.health.dto.*;
import com.ssafy.linkcare.health.service.HealthService;
import com.ssafy.linkcare.health.service.HealthSyncService;
import com.ssafy.linkcare.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.core.Local;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Date;

@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;
    private final HealthSyncService healthSyncService;

    // 일별 데이터 동기화
    @PostMapping("/users/{userId}/daily")
    public ResponseEntity<ApiResponse<DailyHealthData>> uploadUserDailyHealthData(
            @PathVariable int userId, @RequestBody DailyHealthData data) {

        System.out.println("받은 데이터: " + data.toString());

        healthSyncService.syncDailyHealthData(data, userId);

        return ResponseEntity.ok(
                ApiResponse.success("성공", HttpStatus.OK.value(), data)
        );
    }

    @PostMapping("/users/{userId}/sync-all")
    public ResponseEntity<ApiResponse<AllHealthDataDto>> uploadUserAllHealthData(
            @PathVariable int userId, @RequestBody AllHealthDataDto data) {

        System.out.println("받은 데이터: " + data);

        healthService.syncAllHealthData(data, userId);

        return ResponseEntity.ok(
                ApiResponse.success("성공", HttpStatus.OK.value(), data)
        );
    }


    @GetMapping("/{userSeq}/today")
    public ResponseEntity<DailyHealthResponse> getUserTodayHealthData(
            @PathVariable int userSeq
    ) {
        log.info("API 호출 - 당일 건강 데이터 전체 조회 userSeq = {}", userSeq);

        DailyHealthResponse dailyHealthResponse = healthService.getTodayHealthData(userSeq);

        return ResponseEntity.ok(dailyHealthResponse);
    }

    @GetMapping("/{userSeq}/today/detail")
    public ResponseEntity<DailyHealthDetailResponse> getUserDailyHealthDetailData(
            @PathVariable int userSeq
    ) {
        log.info("API 호출 - 당일 건강 데이터 detail 전체 조회 userSeq = {}", userSeq);

        DailyHealthDetailResponse dailyHealthDetailResponse = healthService.getDailyHealthDetail(userSeq);

        return ResponseEntity.ok(dailyHealthDetailResponse);
    }

    @GetMapping("/{userSeq}/health/daily/{date}")
    public ResponseEntity<DailyHealthDetailResponse> getUserHealthDetailDataByDate(
            @PathVariable int userSeq,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        log.info("API 호출 - 특정 날짜 건강 데이터 detail 전체 조회 userSeq = {}", userSeq);

        DailyHealthDetailResponse dailyHealthDetailResponse = healthService.getDailyHealthDetailByDate(userSeq, date);

        return ResponseEntity.ok(dailyHealthDetailResponse);
    }

    @GetMapping("/{userSeq}/health/daily-activity/{date}")
    public ResponseEntity<DailyActivitySummaryResponse> getUserDailyActivityDataByDate(
            @PathVariable int userSeq,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        log.info("API 호출 - 특정 날짜 daily activity detail 전체 조회 userSeq = {}", userSeq);

        DailyActivitySummaryResponse dailyActivitySummaryResponse = healthService.getDailyActivitySummaryByDate(userSeq, date);

        return ResponseEntity.ok(dailyActivitySummaryResponse);
    }

    @GetMapping("/{userSeq}/today-activity/detail")
    public ResponseEntity<DailyActivitySummaryResponse> getUserDailyActivitySummaryDetailData(
            @PathVariable int userSeq
    ) {
        log.info("API 호출 - 당일 건강 데이터 detail 전체 조회 userSeq = {}", userSeq);

        DailyActivitySummaryResponse dailyHealthDetailResponse = healthService.getDailyActivitySummary(userSeq);

        return ResponseEntity.ok(dailyHealthDetailResponse);
    }

    // 기간 별 칼로리, 걸음, 운동 시간
    @GetMapping("/{userSeq}/total-activity/stats")
    public ResponseEntity<TotalActivityStatisticsResponse> getTotalActivityStatistics(
            @PathVariable int userSeq,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        log.info("API 호출 - 기간별 칼로리, 걸음 수, 운동 시간 조회 userSeq = {}", userSeq);

        TotalActivityStatisticsResponse response = healthService.getTotalActivityStatistics(userSeq, startDate, endDate);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userSeq}/actual-activity")
    public ResponseEntity<HealthActualActivityResponse> getActualActivity(
            @PathVariable int userSeq,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        log.info("API 호출 - 기간별 칼로리, 걸음 수, 운동 시간, 운동 거리 조회 userSeq = {}", userSeq);

        HealthActualActivityResponse response = healthService.getHealthActualActivity(userSeq, startDate, endDate);

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 그룹의 모든 멤버 데이터 동기화 (그룹 페이지 진입 시)
     */
    @PostMapping("/sync/group/{groupSeq}")
    @Operation(summary = "그룹 데이터 동기화", description = "특정 그룹의 모든 멤버 데이터를 동기화합니다")
    public ResponseEntity<String> syncGroup(@PathVariable Long groupSeq) {
        log.info("그룹 {} 동기화 요청", groupSeq);
        healthService.syncGroupMembersHealthData(groupSeq);
        return ResponseEntity.ok("동기화 완료");
    }

    @GetMapping("/dialogs/{userSeq}/stats/today")
    @Operation(summary = "AI 대사 생성을 위한 오늘 건강 데이터 요약 조회", description = "사용자의 오늘 건강 데이터 요약을 조회합니다")
    public ResponseEntity<TodayStatisticsResponse> getTodayStatsForDialogs(
            @PathVariable int userSeq
    ) {
        log.info("AI 대사 생성용 오늘 건강 데이터 요약 조회 - User: {} ", userSeq);

        TodayStatisticsResponse response = healthService.getTodayStatisticsForDialogs(userSeq);

        return ResponseEntity.ok(response);
    }
}
