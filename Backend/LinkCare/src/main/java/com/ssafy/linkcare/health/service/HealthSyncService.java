package com.ssafy.linkcare.health.service;

import com.ssafy.linkcare.health.dto.AllHealthDataDto;
import com.ssafy.linkcare.health.dto.DailyHealthData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HealthSyncService {

    private final ActivitySummaryService activitySummaryService;
    private final BloodPressureService bloodPressureService;
    private final ExerciseService exerciseService;
    private final HeartRateService heartRateService;
    private final StepService stepService;
    private final WaterIntakeService waterIntakeService;

    // 전체 데이터 동기화 (초기 1회)
    public void syncAllHealthData(AllHealthDataDto dto, int userSeq) {

        activitySummaryService.saveAllActivitySummaryData(dto.getActivitySummary(), userSeq);
        bloodPressureService.saveAllBloodPressureData(dto.getBloodPressure(), userSeq);
        exerciseService.saveAllExerciseData(dto.getExercise(), userSeq);
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
    }
}
