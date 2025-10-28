package com.ssafy.sdk.health.domain.activity

import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.jvm.Throws

private const val TAG = "ReadTotalDistanceUserCase"
class ReadTotalDistanceUserCase @Inject constructor(
    private val healthDataStore: HealthDataStore
) {
    @Throws(HealthDataException::class)
    suspend operator fun invoke(): Double {
        return readTotalDistance()
    }

    @Throws(HealthDataException::class)
    private suspend fun readTotalDistance(): Double {
        val startDate = LocalDate.now().atStartOfDay()     // 현재 날짜
        val now = LocalDateTime.now()       // 현재 시간
        Log.i(TAG, "StartDate: $startDate; Now: $now")
        val readRequest = DataType.ActivitySummaryType.TOTAL_DISTANCE
            .requestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(startDate, now))
            .build()
        val result = healthDataStore.aggregateData(readRequest)
        var totalDistance: Double = 0.toDouble();
        result.dataList.forEach { data ->
            Log.i(TAG, "Total Distance: ${data.value}")
            Log.i(TAG, "data.startDate: ${data.startTime}")
            Log.i(TAG, "data.endTIme: ${data.endTime}")
            data.value?.let { totalDistance = it.toDouble()}
        }
        return totalDistance
    }
}