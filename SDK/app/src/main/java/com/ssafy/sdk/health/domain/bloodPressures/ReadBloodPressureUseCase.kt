package com.ssafy.sdk.health.domain.bloodPressures

import android.util.Log
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.ssafy.sdk.health.data.model.BloodPressureData
import com.ssafy.sdk.health.data.model.ExerciseData
import com.ssafy.sdk.health.data.model.SleepData
import com.ssafy.sdk.health.data.model.SleepGoalData
import com.ssafy.sdk.health.domain.exercise.ReadExerciseTotalCaloriesUseCase
import com.ssafy.sdk.health.domain.exercise.ReadExerciseTotalDurationUseCase
import com.ssafy.sdk.health.domain.exercise.ReadExerciseTypeUseCase
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

private const val TAG = "ReadBloodPressureUseCase"


class ReadBloodPressureUseCase @Inject constructor(
    private val readBloodPressureDataUseCase: ReadBloodPressureDataUseCase
) {

    @Throws(HealthDataException::class)
    suspend operator fun invoke(): BloodPressureData {
        Log.i(TAG, "ReadTotalSleepsUseCase invoke()")
        val zoneOffset = ZoneOffset.of("+09:00")
        val startTime = LocalDateTime.now(zoneOffset)
        val now = LocalDateTime.now(zoneOffset)

        val bloodPressures = readBloodPressureDataUseCase()

        Log.i("Start Time: ", "$startTime")
        Log.i("Blood Pressures: ", "$bloodPressures")

        return BloodPressureData(
            startTime,
            bloodPressures
        )
    }

}