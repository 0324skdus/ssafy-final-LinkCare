package com.ssafy.linkcare.gpt.controller;

import com.ssafy.linkcare.gpt.dto.HealthSummaryRequest;
import com.ssafy.linkcare.gpt.dto.HealthSummaryResponse;
import com.ssafy.linkcare.gpt.dto.TodayHealthSummaryResponse;
import com.ssafy.linkcare.gpt.dto.WeeklyHeaderResponse;
import com.ssafy.linkcare.gpt.service.GptService;
import com.ssafy.linkcare.group.entity.GroupMember;
import com.ssafy.linkcare.group.repository.GroupMemberRepository;
import com.ssafy.linkcare.group.service.GroupService;
import com.ssafy.linkcare.health.dto.DailyHealthResponse;
import com.ssafy.linkcare.health.dto.HealthStaticsResponse;
import com.ssafy.linkcare.health.dto.TodayHealthSummary;
import com.ssafy.linkcare.health.entity.UserHealthFeedback;
import com.ssafy.linkcare.health.service.HealthService;
import com.ssafy.linkcare.health.util.TimeStampUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/health/gpt")
@RequiredArgsConstructor
@Tag(name = "GPT API", description = "GPT 기반 건강 분석 API")
public class GptController {

    private final GptService gptService;
    private final GroupService groupService;
    private final HealthService healthService;
    private final TimeStampUtil timeStampUtil;
    private final GroupMemberRepository groupMemberRepository;

//    @GetMapping("/health-summary/{userSeq}")
//    @Operation(summary = "개인 건강 한줄평 생성", description = "사용자의 일일 건강 데이터를 분석하여 한줄평과 건강 상태를 제공합니다")
//    public ResponseEntity<HealthSummaryResponse> getHealthSummary(
//            @PathVariable int userSeq
//    ) {
//        DailyHealthResponse dailyHealthResponse = healthService.getTodayHealthData(userSeq);
//        String healthData = dailyHealthResponse.toString();
//
//        HealthSummaryRequest request = gptService.generateHealthSummary(healthData);
//
//        UserHealthFeedback userHealthFeedback = UserHealthFeedback.builder()
//                .
//        return ResponseEntity.ok(response);
//    }

    @GetMapping("/weekly-header/{groupSeq}")
    @Operation(summary = "그룹 주간 헤더 문구 생성", description = "그룹의 지난 주 건강 데이터를 분석하여 이번 주 목표 문구를 제공합니다")
    public ResponseEntity<WeeklyHeaderResponse> getWeeklyHeader(
            @PathVariable Long groupSeq,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate today
    ) {
        log.info("=== 주간 헤더 생성 시작 ===");
        log.info("그룹 ID: {}", groupSeq);
        log.info("조회 기간: {} ~ {}", timeStampUtil.getLastWeekMonday(), timeStampUtil.getLastWeekSunday());

        // 1. 건강 데이터 조회
        List<HealthStaticsResponse> responses = groupService.getLastWeekGroupHealthStats(
                groupSeq,
                timeStampUtil.getLastWeekMonday(),
                timeStampUtil.getLastWeekSunday()
        );
        log.info("조회된 멤버 수: {}명", responses.size());
        log.debug("건강 데이터 원본: {}", responses);

        // 2. GPT 포맷으로 변환
        String groupHealthData = gptService.formatHealthDataForGPT(responses);
        log.info("=== GPT에 전달할 데이터 ===");
        log.info("\n{}", groupHealthData);
        log.info("=== 데이터 끝 ===");

        // 3. GPT 호출
        log.info("GPT API 호출 시작...");
        WeeklyHeaderResponse response = gptService.generateWeeklyHeader(groupHealthData);
        log.info("GPT 생성 결과: {}", response.getHeaderMessage());
        log.info("=== 주간 헤더 생성 완료 ===");

        return ResponseEntity.ok(response);
    }


    @GetMapping("/health-summary/today/{userSeq}")
    @Operation(summary = "개인 건강 데이터별 한줄평 생성", description = "사용자의 일일 건강 데이터를 분석하여 데이터별 한줄평을 제공합니다")
    public ResponseEntity<TodayHealthSummaryResponse> getTodayHealthSummary(@PathVariable int userSeq) {

        log.info("=== 오늘의 건강 한줄평 생성 시작 ===");
        log.info("사용자 ID: {}", userSeq);

        // 1. 오늘의 건강 데이터 조회
        TodayHealthSummary todayHealthData = healthService.getTodayHealthSummary(userSeq);
        log.info("건강 데이터 조회 완료");

        // 2. GPT로 한줄평 생성
        TodayHealthSummaryResponse response = gptService.generateTodayHealthReviews(todayHealthData);
        log.info("한줄평 생성 완료");
        log.info("=== 건강 한줄평 생성 완료 ===");

        return ResponseEntity.ok(response);
    }
}
