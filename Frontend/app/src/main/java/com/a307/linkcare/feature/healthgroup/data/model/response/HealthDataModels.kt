package com.a307.linkcare.feature.healthgroup.data.model.response

import com.google.gson.annotations.SerializedName

// 일일 건강 데이터 (단일 날짜)
data class DailyHealthDataDto(
    @SerializedName("activitySummary") val activitySummary: ActivitySummaryDto?,
    @SerializedName("heartRate") val heartRate: List<HeartRateDto>?,
    @SerializedName("sleep") val sleep: List<SleepDataDto>?,
    @SerializedName("waterIntake") val waterIntake: WaterIntakeDto?,
    @SerializedName("bloodPressure") val bloodPressure: List<BloodPressureDto>?,
    @SerializedName("exercise") val exercise: ExerciseDto?,
    @SerializedName("step") val step: StepDto?
)

// 전체 건강 데이터 (여러 날짜)
data class AllHealthDataDto(
    @SerializedName("activitySummary") val activitySummary: List<ActivitySummaryDto>?,
    @SerializedName("heartRate") val heartRate: List<HeartRateDto>?,
    @SerializedName("sleep") val sleep: List<SleepDataDto>?,
    @SerializedName("waterIntake") val waterIntake: List<WaterIntakeDto>?,
    @SerializedName("bloodPressure") val bloodPressure: List<BloodPressureDto>?,
    @SerializedName("exercise") val exercise: List<ExerciseDto>?,
    @SerializedName("step") val step: List<StepDto>?
)

data class ActivitySummaryDto(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("deviceType") val deviceType: String,
    @SerializedName("startTime") val startTime: String, // ISO format
    @SerializedName("totalCaloriesBurned") val totalCaloriesBurned: Double?,
    @SerializedName("totalDistance") val totalDistance: Double?
)

data class HeartRateDto(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("deviceType") val deviceType: String,
    @SerializedName("uid") val uid: String,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String,
    @SerializedName("zoneOffset") val zoneOffset: String,
    @SerializedName("dataSource") val dataSource: DataSourceDto,
    @SerializedName("heartRate") val heartRate: Double
)

data class SleepDataDto(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("deviceType") val deviceType: String,
    @SerializedName("uid") val uid: String,
    @SerializedName("zoneOffset") val zoneOffset: String,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String,
    @SerializedName("dataSource") val dataSource: DataSourceDto,
    @SerializedName("duration") val duration: Int,
    @SerializedName("sessions") val sessions: List<SleepSessionDto>
)

data class SleepSessionDto(
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String,
    @SerializedName("duration") val duration: Int
)

data class WaterIntakeDto(
    @SerializedName("waterIntakes") val waterIntakes: List<WaterIntakeGroupedDto>,
    @SerializedName("goal") val goal: Float
)

data class WaterIntakeGroupedDto(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("deviceType") val deviceType: String,
    @SerializedName("uid") val uid: String,
    @SerializedName("zoneOffset") val zoneOffset: String,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String,
    @SerializedName("dataSource") val dataSource: DataSourceDto,
    @SerializedName("amount") val amount: Float
)

data class BloodPressureDto(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("deviceType") val deviceType: String,
    @SerializedName("uid") val uid: String,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("dataSource") val dataSource: DataSourceDto,
    @SerializedName("systolic") val systolic: Float,
    @SerializedName("diastolic") val diastolic: Float,
    @SerializedName("mean") val mean: Float,
    @SerializedName("pulseRate") val pulseRate: Int
)

data class ExerciseDto(
    @SerializedName("exercises") val exercises: List<ExerciseTypeDto>
)

data class ExerciseTypeDto(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("deviceType") val deviceType: String,
    @SerializedName("uid") val uid: String,
    @SerializedName("zoneOffset") val zoneOffset: String,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String,
    @SerializedName("dataSource") val dataSource: DataSourceDto,
    @SerializedName("exerciseType") val exerciseType: String,
    @SerializedName("sessions") val sessions: List<ExerciseSessionDto>
)

data class ExerciseSessionDto(
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String,
    @SerializedName("exerciseType") val exerciseType: String,
    @SerializedName("calories") val calories: Float?,
    @SerializedName("distance") val distance: Float?,
    @SerializedName("duration") val duration: Long?
)

data class StepDto(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("deviceType") val deviceType: String,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String,
    @SerializedName("count") val count: Int,
    @SerializedName("goal") val goal: Int
)

data class DataSourceDto(
    @SerializedName("a") val a: String,
    @SerializedName("b") val b: String
)

