package com.ssafy.linkcare.health.controller;

import com.ssafy.linkcare.health.dto.BloodPressureStaticsResponse;
import com.ssafy.linkcare.health.service.BloodPressureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "BloodPressure", description = "혈압 데이터 API")
@Slf4j
@RestController
@RequestMapping("/api/health/blood-pressure")
@RequiredArgsConstructor
public class BloodPressureController {

    private final BloodPressureService bloodPressureService;

    @GetMapping("/stats/{userSeq}")
    @Operation(summary = "기간별 혈압 통계 조회", description = "특정 기간의 평균,최고,최저 수축기/이완기 혈압을 조회합니다")
    public ResponseEntity<BloodPressureStaticsResponse> getBloodPressureStats(
            @PathVariable int userSeq,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        BloodPressureStaticsResponse response = bloodPressureService.getBloodPressureStatsByPeriod(userSeq, startDate, endDate);
        return ResponseEntity.ok(response);
    }
}
