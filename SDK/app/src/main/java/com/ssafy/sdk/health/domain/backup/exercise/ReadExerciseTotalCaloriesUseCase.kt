package com.ssafy.sdk.health.domain.exercise

import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import java.time.LocalDate
import javax.inject.Inject
import kotlin.jvm.Throws
import java.time.LocalDateTime

private const val TAG = "ReadExerciseTotalCaloriesUseCase"

class ReadExerciseTotalCaloriesUseCase @Inject constructor(
    private val healthDataStore: HealthDataStore
) {

    @Throws(HealthDataException::class)
    suspend operator fun invoke(): Float {
        Log.i(TAG, "ReadExerciseTotalCaloriesUseCase invoke()")
        return readExerciseTotalCalories()
    }

    private suspend fun readExerciseTotalCalories(): Float {
        val startTime = LocalDate.now().atStartOfDay()
        val now = LocalDateTime.now()

        val readRequest = DataType.ExerciseType.TOTAL_CALORIES
            .requestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(startTime, now))
            .build()

        val result = healthDataStore.aggregateData(readRequest)
        var totalCalories = 0f
        result.dataList.forEach { data ->
            Log.i("Total Exercise Calories: ", "${data.value}")
            data.value?.let { totalCalories = it }
        }

        return totalCalories
    }
}