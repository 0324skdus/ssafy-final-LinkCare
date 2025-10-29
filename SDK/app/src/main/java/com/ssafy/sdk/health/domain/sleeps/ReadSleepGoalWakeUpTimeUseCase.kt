package com.ssafy.sdk.health.domain.sleeps

import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.LocalDateFilter
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlin.jvm.Throws

private const val TAG = "ReadSleepGoalWakeUpTimeUseCase"

class ReadSleepGoalWakeUpTimeUseCase @Inject constructor(
    private val healthDataStore: HealthDataStore
) {

    @Throws(HealthDataException::class)
    suspend operator fun invoke(): LocalTime {
        Log.i(TAG, "ReadSleepGoalWakeUpTimeUseCase invoke()")
        return readSleepGoal()
    }

    private suspend fun readSleepGoal(): LocalTime {
        val today = LocalDate.now()
        val localDateFilter = LocalDateFilter.of(today.minusDays(6), today)

        val readRequest = DataType.SleepGoalType.LAST_WAKE_UP_TIME
            .requestBuilder
            .setLocalDateFilter(localDateFilter)
            .build()

        val result = healthDataStore.aggregateData(readRequest)
        var wakeUpTime = LocalTime.MIDNIGHT

        result.dataList.forEach { data ->
            Log.i("Wake Up Time Goal: ", "${data.value}")

            data.value?.let { wakeUpTime = it }
        }

        return wakeUpTime
    }
}