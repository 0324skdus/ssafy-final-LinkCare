package com.ssafy.sdk.health.domain.exercise

import android.util.Log
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.ssafy.sdk.health.data.test.ExerciseData
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

private const val TAG = "ReadExerciseUseCase"


class ReadExerciseUseCase @Inject constructor(
    private val readExerciseTypeUseCase: ReadExerciseTypeUseCase,
    private val readExerciseTotalCaloriesUseCase: ReadExerciseTotalCaloriesUseCase,
    private val readExerciseTotalDurationUseCase: ReadExerciseTotalDurationUseCase
) {

    @Throws(HealthDataException::class)
    suspend operator fun invoke(): ExerciseData {
        Log.i(TAG, "ReadTotalSleepsUseCase invoke()")
        val zoneOffset = ZoneOffset.of("+09:00")
        val startTime = LocalDateTime.now(zoneOffset)
        val now = LocalDateTime.now(zoneOffset)

        val exercises = readExerciseTypeUseCase()
        val totalDuration = readExerciseTotalDurationUseCase()
        val totalCalories = readExerciseTotalCaloriesUseCase()

        Log.i("Exercises: ", "$exercises")
        Log.i("Total Duration: ", "$totalDuration")
        Log.i("Total Calories: ", "$totalCalories")

        return ExerciseData(
            startTime,
            exercises,
            totalDuration,
            totalCalories
        )
    }

}