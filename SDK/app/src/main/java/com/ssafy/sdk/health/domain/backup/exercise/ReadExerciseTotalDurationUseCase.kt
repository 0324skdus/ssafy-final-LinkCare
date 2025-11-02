package com.ssafy.sdk.health.domain.exercise

import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalDateFilter
import java.time.LocalDate
import javax.inject.Inject
import kotlin.jvm.Throws
import java.time.Duration

private const val TAG = "ReadExerciseTotalDurationUseCase"

class ReadExerciseTotalDurationUseCase @Inject constructor(
    private val healthDataStore: HealthDataStore
) {

    @Throws(HealthDataException::class)
    suspend operator fun invoke(): Long {
        Log.i(TAG, "ReadExerciseTotalDurationUseCase invoke()")
        return readExerciseTotalDuration()
    }

    private suspend fun readExerciseTotalDuration(): Long {
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(1)

        val readRequest = DataType.ExerciseType.TOTAL_DURATION
            .requestBuilder
            .setLocalDateFilter(LocalDateFilter.of(startDate,endDate))
            .build()

        val result = healthDataStore.aggregateData(readRequest)
        var totalDuration = 0L
        result.dataList.forEach { data ->
            Log.i("Total Exercise Duration: ", "${data.value?.toMillis()}")
            data.value?.let { totalDuration = it.toMillis() }
        }

        return totalDuration
    }
}