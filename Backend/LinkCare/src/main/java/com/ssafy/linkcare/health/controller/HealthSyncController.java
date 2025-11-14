package com.ssafy.linkcare.health.controller;

import com.ssafy.linkcare.health.dto.ExerciseDto;
import com.ssafy.linkcare.health.service.ExerciseService;
import com.ssafy.linkcare.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/health/sync")
@RequiredArgsConstructor
public class HealthSyncController {

    private final ExerciseService exerciseService;

    /**
     * 운동 데이터만 동기화 (워치 병합용)
     */
    @PostMapping("/users/{userId}/exercise-sync")
    public ResponseEntity<ApiResponse<Void>> syncExerciseOnly(
            @PathVariable int userId,
            @RequestBody ExerciseDto exerciseDto) {

        exerciseService.saveDailyExerciseDataWithWatchMerge(exerciseDto, userId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, HttpStatus.OK.value(), "운동 데이터 동기화 완료", null, LocalDateTime.now())
        );
    }
}
