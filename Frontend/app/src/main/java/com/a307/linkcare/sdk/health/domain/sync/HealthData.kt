package com.a307.linkcare.sdk.health.domain.sync

import com.a307.linkcare.sdk.health.data.model.ActivitySummaryData
import com.a307.linkcare.sdk.health.data.model.BloodPressureTypeData
import com.a307.linkcare.sdk.health.data.model.ExerciseData
import com.a307.linkcare.sdk.health.data.model.HeartRatesData
import com.a307.linkcare.sdk.health.data.model.SleepData
import com.a307.linkcare.sdk.health.data.model.StepData
import com.a307.linkcare.sdk.health.data.model.WaterIntakeData

/**
 * 하루치 건강 데이터 (일일 동기화 결과)
 */
data class DailyHealthData(
    val heartRate: List<HeartRatesData>?,
    val sleep: List<SleepData>?,
    val waterIntake: WaterIntakeData?,
    val bloodPressure: List<BloodPressureTypeData>?,
    val exercise: ExerciseData?,
    val step: StepData?,
    val activitySummary: ActivitySummaryData?,
)

/**
 * 전체 건강 데이터 (전체 동기화 결과)
 */
data class AllHealthData(
    val heartRate: List<HeartRatesData>,
    val sleep: List<SleepData>,
    val waterIntake: List<WaterIntakeData>,
    val bloodPressure: List<BloodPressureTypeData>,
    val exercise: List<ExerciseData>,
    val step: List<StepData>,
    val activitySummary: List<ActivitySummaryData>,
)
