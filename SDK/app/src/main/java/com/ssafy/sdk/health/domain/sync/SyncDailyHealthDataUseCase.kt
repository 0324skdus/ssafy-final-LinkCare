package com.ssafy.sdk.health.domain.sync

import com.ssafy.sdk.health.domain.sync.activitySummary.ActivitySummaryReader
import com.ssafy.sdk.health.domain.sync.bloodPressure.BloodPressureReader
import com.ssafy.sdk.health.domain.sync.exercise.ExerciseReader
import com.ssafy.sdk.health.domain.sync.heartRate.HeartRateReader
import com.ssafy.sdk.health.domain.sync.sleep.SleepReader
import com.ssafy.sdk.health.domain.sync.step.StepReader
import com.ssafy.sdk.health.domain.sync.waterIntake.WaterIntakeReader
import javax.inject.Inject

/**
 * 매일 동기화 UseCase (빠름)
 * 오늘 하루 데이터만 조회
 */
class SyncDailyHealthDataUseCase @Inject constructor(
    private val heartRateReader: HeartRateReader,
    private val sleepReader: SleepReader,
    private val waterIntakeReader: WaterIntakeReader,
    private val bloodPressureReader: BloodPressureReader,
    private val exerciseReader: ExerciseReader,
    private val stepReader: StepReader,
    private val activitySummaryReader: ActivitySummaryReader,
) {
    suspend operator fun invoke(): DailyHealthData {
        return DailyHealthData(
            heartRate = heartRateReader.readToday(),
            sleep = sleepReader.readToday(),
            waterIntake = waterIntakeReader.readToday(),
            bloodPressure = bloodPressureReader.readToday(),
            exercise = exerciseReader.readToday(),
            step = stepReader.readToday(),
            activitySummary = activitySummaryReader.readToday(),
        )
    }
}