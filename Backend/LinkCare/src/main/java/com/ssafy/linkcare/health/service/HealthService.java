package com.ssafy.linkcare.health.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.gpt.dto.TodayHealthSummaryResponse;
import com.ssafy.linkcare.health.dto.*;
import com.ssafy.linkcare.health.entity.ActivitySummary;
import com.ssafy.linkcare.health.util.TimeStampUtil;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthService {

    private final ActivitySummaryService activitySummaryService;
    private final BloodPressureService bloodPressureService;
    private final ExerciseService exerciseService;
    private final HeartRateService heartRateService;
    private final StepService stepService;
    private final WaterIntakeService waterIntakeService;
    private final SleepService sleepService;

    private final UserRepository userRepository;

    private final ObjectMapper objectMapper;
    private TimeStampUtil timeStampUtil;

    // 전체 데이터 동기화 (초기 1회)
    public void syncAllHealthData(AllHealthDataDto dto, int userSeq) {

        activitySummaryService.saveAllActivitySummaryData(dto.getActivitySummary(), userSeq);
        bloodPressureService.saveAllBloodPressureData(dto.getBloodPressure(), userSeq);
        exerciseService.saveAllExerciseData(dto.getExercise(), userSeq);
        sleepService.saveAllSleepData(dto.getSleep(), userSeq);
        heartRateService.saveAllHeartRateData(dto.getHeartRate(), userSeq);
        stepService.saveAllStepData(dto.getStep(), userSeq);
        waterIntakeService.saveAllWaterIntakeData(dto.getWaterIntake(), userSeq);
    }

    // 데일리 데이터 동기화 (매일 1회)
    public void syncDailyHealthData(DailyHealthData dto, int userSeq) {
        activitySummaryService.saveDailyActivitySummaryData(dto.getActivitySummary(), userSeq);
        bloodPressureService.saveDailyBloodPressureData(dto.getBloodPressure(), userSeq);
        exerciseService.saveDailyExerciseData(dto.getExercise(), userSeq);
        heartRateService.saveDailyHeartRateData(dto.getHeartRate(), userSeq);
        stepService.saveDailyStepData(dto.getStep(), userSeq);
        waterIntakeService.saveDailyWaterIntakeData(dto.getWaterIntake(), userSeq);
        sleepService.saveDailySleepData(dto.getSleep(), userSeq);
    }

    // 당일 건강 데이터 조회
    public DailyHealthResponse getTodayHealthData(int userSeq) {
        return DailyHealthResponse.builder().activitySummary(activitySummaryService.getTodayActivitySummary(userSeq))
                .bloodPressure(bloodPressureService.getTodayBloodPressures(userSeq))
                .exercise(exerciseService.getTodayExercises(userSeq))
                .heartRate(heartRateService.getTodayHeartRate(userSeq))
                .step(stepService.getTodayStepCount(userSeq))
                .exercise(exerciseService.getTodayExercises(userSeq))
                .waterIntake(waterIntakeService.getTodayWaterIntakes(userSeq))
                .sleep(sleepService.getTodaySleeps(userSeq))
                .build();
    }


    // 건강 통계 데이터 조회
    public HealthStaticsResponse getHealthStaticsData(int userSeq, LocalDate startDate, LocalDate endDate) {
        return HealthStaticsResponse.builder()
                .activitySummaryStats(activitySummaryService.getActivitySummaryStatsByDate(userSeq, startDate, endDate))
                .exerciseStats(exerciseService.getExerciseStatisticsByDate(userSeq, startDate, endDate))
                .heartRateStats(heartRateService.getHeartRateStatsByDate(userSeq, startDate, endDate))
                .stepStats(stepService.getWeekStepStatisticsByDate(userSeq, startDate, endDate))
                .exerciseStats(exerciseService.getExerciseStatisticsByDate(userSeq, startDate, endDate))
                .waterIntakeStats(waterIntakeService.getWaterIntakeStatisticsByDate(userSeq, startDate, endDate))
                .sleepStats(sleepService.getSleepStatisticsByDate(userSeq,startDate,endDate))
                .build();
    }

    public TodayHealthSummary getTodayHealthSummary(int userSeq) {
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().plusDays(1);

        return TodayHealthSummary.builder()
                .activitySummary(activitySummaryService.getActivitySummaryStatsToday(userSeq))
                .exercise(exerciseService.getTodayExerciseStatistics(userSeq))
                .step(stepService.getTodayStepCount(userSeq))
                .heartRate(heartRateService.getHeartRateStatsByDate(userSeq, start, end))
                .bloodPressure(bloodPressureService.getBloodPressureStatsByPeriod(userSeq, start, end))
                .waterIntake(waterIntakeService.getTodayWaterIntakeStatistics(userSeq))
                .sleep(sleepService.getSleepStatisticsByDate(userSeq, start, end))
                .build();
    }


    // validate
    public User validateUser(int userSeq) {
        return userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    // DailyActivity 데이터 통합 메서드
    private String createDailyActivityDataForGPT(
            ActivitySummaryStaticsResponse activitySummary,
            ExerciseStatisticsResponse exercise,
            int todayStep) {

        try {
            Map<String, Object> dailyActivity = new HashMap<>();
            dailyActivity.put("activitySummary", activitySummary);
            dailyActivity.put("exercise", exercise);
            dailyActivity.put("steps", todayStep);

            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(dailyActivity);
        } catch (JsonProcessingException e) {
            log.error("DailyActivity 데이터 변환 실패", e);
            return "{}";
        }
    }
}
