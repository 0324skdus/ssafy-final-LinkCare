package com.ssafy.linkcare.health.controller;


import com.ssafy.linkcare.health.dto.ExerciseDto;
import com.ssafy.linkcare.health.dto.ExerciseResponse;
import com.ssafy.linkcare.health.dto.ExerciseStatisticsResponse;
import com.ssafy.linkcare.health.dto.WatchExerciseData;
import com.ssafy.linkcare.health.service.ExerciseService;
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

@Tag(name = "Exercise", description = "운동 데이터 API")
@Slf4j
@RestController
@RequestMapping("/api/health/exercise")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService exerciseService;

    @Operation(summary = "당일 운동 데이터 조회",
            description = "오늘 하루의 운동 데이터를 조회합니다. 데이터가 없으면 빈 리스트를 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userSeq}/today")
    public ResponseEntity<List<ExerciseResponse>> getTodayExercises(
            @Parameter(description = "사용자 시퀀스", required = true) @PathVariable int userSeq
    ) {
        log.info("API 호출 - 당일 운동 조회: userSeq={}", userSeq);

        List<ExerciseResponse> responses = exerciseService.getTodayExercises(userSeq);

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "이번 주 운동 데이터 조회",
            description = "이번 주 월요일부터 현재까지의 운동 데이터를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userSeq}/this-week")
    public ResponseEntity<List<ExerciseResponse>> getThisWeekExercises(
            @PathVariable int userSeq
    ) {
        log.info("API 호출 - 이번 주 운동 조회: userSeq={}", userSeq);

        List<ExerciseResponse> responses = exerciseService.getThisWeekExercises(userSeq);

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "3주치 운동 데이터 조회",
            description = "최근 3주간의 운동 데이터를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userSeq}/three-weeks")
    public ResponseEntity<List<ExerciseResponse>> getThreeWeeksExercises(
            @PathVariable int userSeq
    ) {
        log.info("API 호출 - 3주치 운동 조회: userSeq={}", userSeq);

        List<ExerciseResponse> responses = exerciseService.getThreeWeeksExercises(userSeq);

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "기간별 운동 통계 조회",
            description = "지정된 기간의 운동 통계(총 칼로리, 총 거리, 평균, 운동 횟수)를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userSeq}/statistics")
    public ResponseEntity<ExerciseStatisticsResponse> getExerciseStatistics(
            @Parameter(description = "사용자 시퀀스", required = true) @PathVariable int userSeq,
            @Parameter(description = "시작 시간 (Unix timestamp)") @RequestParam(required = false) Long startTime,
            @Parameter(description = "종료 시간 (Unix timestamp)") @RequestParam(required = false) Long endTime
    ) {
        log.info("API 호출 - 운동 통계 조회: userSeq={}, startTime={}, endTime={}",
                userSeq, startTime, endTime);

        ExerciseStatisticsResponse response = exerciseService
                .getExerciseStatistics(userSeq, startTime, endTime);

        return ResponseEntity.ok(response);
    }

//    @GetMapping("/{userSeq}/statistics/date")
//    public ResponseEntity<ExerciseStatisticsResponse> getExerciseStatisticsByPeriod(
//            @PathVariable int userSeq,
//            @RequestParam(required = false) LocalDate startTime,
//            @RequestParam(required = false) LocalDate endTime
//    ) {
//        log.info("(LocalDate) API 호출 - 운동 통계 조회: userSeq={}, startTime={}, endTime={}",
//                userSeq, startTime, endTime);
//
//        ExerciseStatisticsResponse response = exerciseService
//                .getExerciseStatisticsByDate(userSeq, startTime, endTime);
//
//        return ResponseEntity.ok(response);
//    }

     @Operation(summary = "당일 운동 통계 조회",
     description = "오늘 하루의 운동 통계를 조회합니다.")
     @ApiResponses(value = {
     @ApiResponse(responseCode = "200", description = "조회 성공"),
     @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
     })
    @GetMapping("/{userSeq}/statistics/today")
    public ResponseEntity<ExerciseStatisticsResponse> getTodayExerciseStatistics(
            @PathVariable int userSeq
    ) {
        log.info("API 호출 - 당일 운동 통계 조회: userSeq={}", userSeq);

        ExerciseStatisticsResponse response = exerciseService
                .getTodayExerciseStatistics(userSeq);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "이번 주 운동 통계 조회",
            description = "이번 주의 운동 통계를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userSeq}/statistics/this-week")
    public ResponseEntity<ExerciseStatisticsResponse> getThisWeekExerciseStatistics(
            @PathVariable int userSeq
    ) {
        log.info("API 호출 - 이번 주 운동 통계 조회: userSeq={}", userSeq);

        ExerciseStatisticsResponse response = exerciseService
                .getThisWeekExerciseStatistics(userSeq);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "3주치 운동 통계 조회",
            description = "최근 3주간의 운동 통계를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userSeq}/statistics/three-weeks")
    public ResponseEntity<ExerciseStatisticsResponse> getThreeWeeksExerciseStatistics(
            @PathVariable int userSeq
    ) {
        log.info("API 호출 - 3주치 운동 통계 조회: userSeq={}", userSeq);

        ExerciseStatisticsResponse response = exerciseService
                .getThreeWeeksExerciseStatistics(userSeq);

        return ResponseEntity.ok(response);
    }


    @Operation(summary = "기간별 운동 데이터 조회 (LocalDate)",
            description = "LocalDate 기준으로 기간별 운동 데이터를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 날짜 형식"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userSeq}/period")
    public ResponseEntity<List<ExerciseResponse>> getExerciseByPeriod(
            @PathVariable int userSeq,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        log.info("API 호출 - 기간별 운동 기록 조회: userSeq={}, startTime={}, endTime={}",
                userSeq, startDate, endDate);

        List<ExerciseResponse> response = exerciseService
                .getExercisesByDate(userSeq, startDate, endDate);

        return ResponseEntity.ok(response);
    }

    /**
     * 워치 운동 데이터 저장
     */
    @PostMapping("/users/{userId}/watch")
    @Operation(summary = "워치 운동 데이터 저장",
            description = "워치에서 전송한 운동 데이터를 저장합니다")
    public ResponseEntity<String> uploadWatchExercise(
            @PathVariable int userId,
            @RequestBody WatchExerciseData data) {

        log.info("워치 운동 데이터 수신: userId={}, sessionId={}",
                userId, data.getSessionId());

        exerciseService.saveWatchExercise(data, userId);

        return ResponseEntity.ok().build();
    }

    /**
     * 워치 병합 포함 동기화 엔드포인트
     */
    @PostMapping("/exercise/sync-with-merge")
    public ResponseEntity<Void> syncExerciseWithMerge(
            @RequestBody ExerciseDto exerciseDto,
            @RequestHeader("userSeq") int userSeq) {

        exerciseService.saveDailyExerciseDataWithWatchMerge(exerciseDto, userSeq);
        return ResponseEntity.ok().build();
    }

    /**
     * 일반 동기화 엔드포인트
     */
    @PostMapping("/watch/sync")
    public ResponseEntity<Void> syncExercise(
            @RequestBody WatchExerciseData watchExerciseData,
            @RequestHeader("userSeq") int userSeq) {

        exerciseService.saveWatchExercise(watchExerciseData, userSeq);
        return ResponseEntity.ok().build();
    }
    // "/api/health/exercise/watch/sync" syncExercise
}
