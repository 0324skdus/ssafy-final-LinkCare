package com.a307.linkcare.feature.caregroup.data

import com.a307.linkcare.feature.caregroup.*
import com.a307.linkcare.feature.caregroup.domain.*

fun DailyHealthDetailResponse.toHealthToday(): HealthToday {
    return HealthToday(
        bloodPressures = this.bloodPressures.map { it.toBloodPressure() },
        waterIntakes = this.waterIntakes.map { it.toWaterIntake() },
        waterGoalMl = 2000, // TODO: 실제 목표값으로 변경
        sleeps = this.sleeps.map { it.toSleep() },
        dailyActivitySummary = this.dailyActivitySummary?.toDailyActivitySummary(),
        heartRates = this.heartRates.map { it.toHeartRate() }
    )
}

private fun BloodPressureResponse.toBloodPressure() = BloodPressure(
    bloodPressureId = this.bloodPressureId,
    uid = this.uid,
    startTime = this.startTime,
    systolic = this.systolic,
    diastolic = this.diastolic,
    mean = this.mean,
    pulseRate = this.pulseRate
)

private fun WaterIntakeResponse.toWaterIntake() = WaterIntake(
    waterIntakeId = this.waterIntakeId,
    startTime = this.startTime,
    amount = this.amount
)

private fun SleepResponse.toSleep() = Sleep(
    sleepId = this.sleepId,
    startTime = this.startTime,
    endTime = this.endTime,
    duration = this.duration
)

private fun DailyActivitySummaryResponse.toDailyActivitySummary() = DailyActivitySummary(
    exercises = this.exercises
        .distinctBy { it.startTime to it.endTime }  // startTime + endTime 조합으로 중복 제거
        .map { it.toExerciseSession() },
    steps = this.steps
)

private fun ExerciseSessionResponse.toExerciseSession() = ExerciseSession(
    startTime = this.startTime,
    endTime = this.endTime,
    distance = this.distance,
    calories = this.calories,
    meanPulseRate = this.meanPulseRate,
    duration = this.duration
)

private fun HeartRateResponse.toHeartRate() = HeartRate(
    heartRateId = this.heartRateId,
    startTime = this.startTime,
    endTime = this.endTime,
    heartRate = this.heartRate
)
