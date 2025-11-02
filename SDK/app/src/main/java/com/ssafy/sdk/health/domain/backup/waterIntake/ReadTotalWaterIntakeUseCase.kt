package com.ssafy.sdk.health.domain.waterIntake

import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.jvm.Throws

private const val TAG = "ReadTotalWaterIntakeUseCase"
private val KST: ZoneId = ZoneId.of("Asia/Seoul")

class ReadTotalWaterIntakeUseCase @Inject constructor(
    private val healthDataStore: HealthDataStore
) {

    @Throws(HealthDataException::class)
    suspend operator fun invoke(): Float {
        Log.i(TAG, "readTotalWaterIntake Invoke()")

        return readTotalWaterIntake()
    }

    private suspend fun readTotalWaterIntake(): Float {
        val startDate = LocalDate.now().atStartOfDay()
        val end = startDate.plusDays(1)

        Log.i(TAG, "StartDate : $startDate, Now: $end")

        val readRequest = DataType.WaterIntakeType.TOTAL
            .requestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(startDate, end))
            .build()

        val result = healthDataStore.aggregateData(readRequest)
        var totalWaterIntake = 0f
        result.dataList.forEach { data ->

            Log.i("Total Water Intake = ", "${data.value}")

            data.value?.let { totalWaterIntake = it }
        }

        return totalWaterIntake
    }
}