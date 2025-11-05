package com.ssafy.linkcare.health.dto;

import lombok.Data;

import java.util.List;

@Data
public class DailyHealthData {
    private List<HeartRateDto> heartRate;
//    private List<SleepData> sleep;
//    private WaterIntakeData waterIntake;
//    private List<BloodPressureTypeData> bloodPressure;
//    private ExerciseData exercise;
//    private StepData step;
}
