package com.ssafy.sdk.health.domain.sleeps

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

private const val TAG = "ReadTotalSleepDurationUseCase"

class ReadTotalSleepDurationUseCase @Inject constructor(
    private val healthDataStore: HealthDataStore
) {

    @Throws(HealthDataException::class)
    suspend operator fun invoke(): Long {
        Log.i(TAG, "ReadTotalSleepDurationUseCase invoke()")
        return readTotalSleepDuration()
    }

    private suspend fun readTotalSleepDuration(): Long {
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(1)

        val readRequest = DataType.SleepType.TOTAL_DURATION
            .requestBuilder
            .setLocalDateFilter(LocalDateFilter.of(startDate,endDate))
            .build()

        val result = healthDataStore.aggregateData(readRequest)
        var totalDuration = 0L
        result.dataList.forEach { data ->
            Log.i("Total Sleep Duration: ", "${data.value?.toMillis()}")
            data.value?.let { totalDuration = it.toMillis() }
        }

        return totalDuration
    }
}