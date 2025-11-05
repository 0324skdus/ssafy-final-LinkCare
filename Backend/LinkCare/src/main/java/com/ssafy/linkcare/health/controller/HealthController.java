package com.ssafy.linkcare.health.controller;

import com.ssafy.linkcare.health.dto.AllHealthDataDto;
import com.ssafy.linkcare.health.dto.DailyHealthData;
import com.ssafy.linkcare.health.service.HealthSyncService;
import com.ssafy.linkcare.health.service.SleepService;
import com.ssafy.linkcare.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthSyncService healthSyncService;

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
}
