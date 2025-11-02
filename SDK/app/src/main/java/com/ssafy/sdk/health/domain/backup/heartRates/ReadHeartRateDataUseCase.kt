package com.ssafy.sdk.health.domain.heartRates

import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.ssafy.sdk.health.data.test.HeartRateSeriesData
import com.ssafy.sdk.health.data.test.HeartRatesData
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

private const val TAG = "ReadHeartRateDataUseCase"

class ReadHeartRateDataUseCase @Inject constructor(
    private val healthDataStore: HealthDataStore
) {
    @Throws(HealthDataException::class)
    suspend operator fun invoke(): List<HeartRatesData> {
        Log.i(TAG, "ReadHeartRate invoke()")
        return readHeartRate()
    }

    private suspend fun readHeartRate(): List<HeartRatesData> {
        // 한국 시간 기준
        val zoneOffset = ZoneOffset.of("+09:00")
        val startDate = LocalDate.now(zoneOffset).atStartOfDay()
//        val now = LocalDateTime.now()
        val end = startDate.plusDays(1)
        Log.i(TAG, "StartDate: ${startDate}, End: $${end}")
        val readRequest = DataTypes.HEART_RATE
            .readDataRequestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(startDate, end))
            .build()

        val result = healthDataStore.readData(readRequest)

        val items: List<HeartRatesData> = result.dataList.map { dataPoint ->
            val rawSeries: List<HeartRateSeriesData>? =
                dataPoint.getValue(DataType.HeartRateType.SERIES_DATA) as? List<HeartRateSeriesData>?


            val seriesData = rawSeries?.map { hr ->
                HeartRateSeriesData(
                    startTime = hr.startTime.atOffset(zoneOffset).toLocalDateTime(),
                    endTime   = hr.endTime.atOffset(zoneOffset).toLocalDateTime(),
                    heartRate = hr.heartRate,
                    min       = hr.min,
                    max       = hr.max
                )
            }

            Log.i(TAG, "uid = ${dataPoint.uid}")
            Log.i(TAG, "startTime = ${dataPoint.startTime.atOffset(dataPoint.zoneOffset).toLocalDateTime()}")
            Log.i(TAG, "endTime = ${dataPoint.endTime?.atOffset(dataPoint.zoneOffset)?.toLocalDateTime()}")
            Log.i(TAG, "zoneOffset = ${dataPoint.zoneOffset}")
            Log.i(TAG, "dataSource = ${dataPoint.dataSource}")
            Log.i(TAG, "heartRate = ${dataPoint.getValue(DataType.HeartRateType.HEART_RATE)}")
            Log.i(TAG, "seriesData = $seriesData")
            Log.i(TAG, "minHeartRate = ${dataPoint.getValue(DataType.HeartRateType.MIN_HEART_RATE)}")
            Log.i(TAG, "maxHeartRate = ${dataPoint.getValue(DataType.HeartRateType.MAX_HEART_RATE)}")

            HeartRatesData(
                uid = dataPoint.uid,
                startTime = dataPoint.startTime.atOffset(dataPoint.zoneOffset).toLocalDateTime(),
                endTime = dataPoint.endTime?.atOffset(dataPoint.zoneOffset)?.toLocalDateTime(),
                zoneOffset = dataPoint.zoneOffset,
                dataSource = dataPoint.dataSource,
                heartRate = dataPoint.getValue(DataType.HeartRateType.HEART_RATE),
                seriesData = seriesData,
                minHeartRate = dataPoint.getValue(DataType.HeartRateType.MIN_HEART_RATE),
                maxHeartRate = dataPoint.getValue(DataType.HeartRateType.MAX_HEART_RATE)
            )
        }

        return items
    }

}