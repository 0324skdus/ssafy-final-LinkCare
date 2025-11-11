package com.ssafy.linkcare.health.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.health.dto.AllHealthDataDto;
import com.ssafy.linkcare.health.dto.DailyHealthData;
import com.ssafy.linkcare.health.dto.DailyHealthResponse;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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






    // validate
    public User validateUser(int userSeq) {
        return userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
