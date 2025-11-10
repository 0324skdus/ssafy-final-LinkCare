package com.ssafy.linkcare.health.controller;

import com.ssafy.linkcare.health.dto.AllHealthDataDto;
import com.ssafy.linkcare.health.dto.DailyHealthData;
import com.ssafy.linkcare.health.dto.DailyHealthResponse;
import com.ssafy.linkcare.health.service.HealthService;
import com.ssafy.linkcare.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthSyncService;

    // 일별 전체 데이터 동기화
    @PostMapping("/users/{userId}/daily")
    public ResponseEntity<ApiResponse<DailyHealthData>> uploadUserDailyHealthData(
            @PathVariable int userId, @RequestBody DailyHealthData data) {

        System.out.println("받은 데이터: " + data);

        healthSyncService.syncDailyHealthData(data, userId);

        return ResponseEntity.ok(
                ApiResponse.success("성공", HttpStatus.OK.value(), data)
        );
    }

    @PostMapping("/users/{userId}/sync-all")
    public ResponseEntity<ApiResponse<AllHealthDataDto>> uploadUserAllHealthData(
            @PathVariable int userId, @RequestBody AllHealthDataDto data) {

        System.out.println("받은 데이터: " + data);

        healthSyncService.syncAllHealthData(data, userId);

        return ResponseEntity.ok(
                ApiResponse.success("성공", HttpStatus.OK.value(), data)
        );
    }

    @GetMapping("/{userSeq}/today")
    public ResponseEntity<DailyHealthResponse> getUserTodayHealthData(
            @PathVariable int userSeq
    ) {
        log.info("API 호출 - 당일 건강 데이터 전체 조회 userSeq = {}", userSeq);

        DailyHealthResponse dailyHealthResponse = healthSyncService.getTodayHealthData(userSeq);

        return ResponseEntity.ok(dailyHealthResponse);
    }
}
