package com.ssafy.sdk.health.domain.activity

import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.jvm.Throws

private const val TAG = "ReadTotalCaloriesBurnedUserCase"
class ReadTotalCaloriesBurnedUserCase @Inject constructor(
    private val healthDataStore: HealthDataStore
) {
    @Throws(HealthDataException::class)
    suspend operator fun invoke(): Double {
        return readTotalCaloriesBurned()
    }

    @Throws(HealthDataException::class)
    private suspend fun readTotalCaloriesBurned(): Double {
        val zone = ZoneId.systemDefault()
        val startDate = LocalDate.now(zone).atStartOfDay()     // 현재 날짜
//        val now = LocalDateTime.now(zone)       // 현재 시간
        val end = startDate.plusDays(1)
        Log.i(TAG, "StartDate: $startDate; Now: $end")
        val readRequest = DataType.ActivitySummaryType.TOTAL_CALORIES_BURNED
            .requestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(startDate, end))
            .build()
        val result = healthDataStore.aggregateData(readRequest)
        var totalCaloriesBurned: Double = 0.toDouble();
        result.dataList.forEach { data ->
            Log.i(TAG, "Total Calories Burned: ${data.value}")
            Log.i(TAG, "data.startDate: ${data.startTime}")
            Log.i(TAG, "data.endTIme: ${data.endTime}")
            data.value?.let { totalCaloriesBurned = it.toDouble()}
        }
        return totalCaloriesBurned
    }
}