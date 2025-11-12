package com.ssafy.linkcare.health.controller;

import com.ssafy.linkcare.health.dto.AllHealthDataDto;
import com.ssafy.linkcare.health.dto.DailyHealthData;
import com.ssafy.linkcare.health.dto.DailyHealthResponse;
import com.ssafy.linkcare.health.dto.TotalActivityStatisticsResponse;
import com.ssafy.linkcare.health.service.HealthService;
import com.ssafy.linkcare.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;

    // 일별 데이터 동기화
    @PostMapping("/users/{userId}/daily")
    public ResponseEntity<ApiResponse<DailyHealthData>> uploadUserDailyHealthData(
            @PathVariable int userId, @RequestBody DailyHealthData data) {

        System.out.println("받은 데이터: " + data);

        healthService.syncDailyHealthData(data, userId);

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

    // 기간 별 칼로리, 걸음, 운동 시간
    @GetMapping("/{userSeq/total-activity/stats")
    public ResponseEntity<TotalActivityStatisticsResponse> getTotalActivityStatistics(
            @PathVariable int userSeq,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        log.info("API 호출 - 기간별 칼로리, 걸음 수, 운동 시간 조회 userSeq = {}", userSeq);

        TotalActivityStatisticsResponse response = healthService.getTotalActivityStatistics(userSeq, startDate, endDate);

        return ResponseEntity.ok(response);
    }

}
