package com.ssafy.sdk.health.domain.waterIntake

import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.LocalDateFilter
import java.time.LocalDate
import javax.inject.Inject
import kotlin.jvm.Throws

private const val TAG = "ReadWaterIntakeGoalUseCase"

class ReadWaterIntakeGoalUseCase @Inject constructor(
    private val healthDataStore: HealthDataStore
) {

    @Throws(HealthDataException::class)
    suspend operator fun invoke(): Float {
        return readWaterIntakeGoal()
    }

    private suspend fun readWaterIntakeGoal(): Float {
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(1)

        Log.i(TAG, "StartDate: $startDate, EndDate: $endDate")
        val readRequest = DataType.WaterIntakeGoalType.LAST
            .requestBuilder
            .setLocalDateFilter(LocalDateFilter.of(startDate, endDate))
            .build()

        val result = healthDataStore.aggregateData(readRequest)
        var waterIntakeGoal = 0f

        result.dataList.forEach { data ->
            Log.i("Water Intake Goal : ", "${data.value}")
            data.value?.let { waterIntakeGoal = it }
        }

        return waterIntakeGoal
    }
}