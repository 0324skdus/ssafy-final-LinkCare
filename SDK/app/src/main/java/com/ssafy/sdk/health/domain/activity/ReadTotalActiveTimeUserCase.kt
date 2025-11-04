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

private const val TAG = "ReadTotalActiveTimeUserCase"
class ReadTotalActiveTimeUserCase @Inject constructor(
    private val healthDataStore: HealthDataStore
) {
    @Throws(HealthDataException::class)
    suspend operator fun invoke(): Long {
        return readTotalActiveTime()
    }

    @Throws(HealthDataException::class)
    private suspend fun readTotalActiveTime(): Long {
        val startDate = LocalDate.now().atStartOfDay()     // 현재 날짜
//        val now = LocalDateTime.now()       // 현재 시간
        val end = startDate.plusDays(1)

        Log.i(TAG, "StartDate: $startDate; Now: $end")
        val readRequest = DataType.ActivitySummaryType.TOTAL_ACTIVE_TIME
            .requestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(startDate, end))
            .build()
        val result = healthDataStore.aggregateData(readRequest)
        var totalActiveTime: Long = 0L;
        result.dataList.forEach { data ->
            Log.i(TAG, "Total Active TIme: ${data.value?.toMillis()}")
            Log.i(TAG, "data.startDate: ${data.startTime}")
            Log.i(TAG, "data.endTIme: ${data.endTime}")
            data.value?.let { totalActiveTime = it.toMillis()}
        }
        return totalActiveTime
    }
}