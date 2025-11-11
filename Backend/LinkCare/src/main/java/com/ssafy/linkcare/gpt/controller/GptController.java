package com.ssafy.linkcare.gpt.controller;

import com.ssafy.linkcare.gpt.dto.HealthSummaryResponse;
import com.ssafy.linkcare.gpt.dto.WeeklyHeaderResponse;
import com.ssafy.linkcare.gpt.service.GptService;
import com.ssafy.linkcare.health.dto.DailyHealthResponse;
import com.ssafy.linkcare.health.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/health/gpt")
@RequiredArgsConstructor
@Tag(name = "GPT API", description = "GPT 기반 건강 분석 API")
public class GptController {

    private final GptService gptService;
    private final HealthService healthService;

    @GetMapping("/health-summary/{userSeq}")
    @Operation(summary = "개인 건강 한줄평 생성", description = "사용자의 일일 건강 데이터를 분석하여 한줄평과 건강 상태를 제공합니다")
    public ResponseEntity<HealthSummaryResponse> getHealthSummary(
            @PathVariable int userSeq
    ) {
        DailyHealthResponse dailyHealthResponse = healthService.getTodayHealthData(userSeq);
        String healthData = dailyHealthResponse.toString();

        HealthSummaryResponse response = gptService.generateHealthSummary(healthData);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/weekly-header/{groupSeq}")
    @Operation(summary = "그룹 주간 헤더 문구 생성", description = "그룹의 지난 주 건강 데이터를 분석하여 이번 주 목표 문구를 제공합니다")
    public ResponseEntity<WeeklyHeaderResponse> getWeeklyHeader(
            @PathVariable Long groupSeq,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        // TODO: groupSeq와 기간으로 그룹 건강 데이터 조회
        String groupHealthData = "임시 그룹 데이터"; // 나중에 실제 데이터 조회로 교체

        WeeklyHeaderResponse response = gptService.generateWeeklyHeader(groupHealthData);
        return ResponseEntity.ok(response);
    }
}
