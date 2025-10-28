package com.ssafy.sdk.health.domain.heartRates

import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.InstantTimeFilter
import com.ssafy.sdk.health.data.model.HeartRateSeriesData
import com.ssafy.sdk.health.data.model.HeartRatesData
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

private const val TAG = "ReadHeartRateDataUseCase"

class ReadHeartRateDataUseCase @Inject constructor(
    private val healthDataStore: HealthDataStore
) {
    @Throws(HealthDataException::class)
    suspend operator fun invoke(): HeartRatesData {
        return readHeartRate()
    }

    private suspend fun readHeartRate(): HeartRatesData {
        Log.i(TAG, "ReadHeartRate invoke()")
        val readRequest = DataTypes.HEART_RATE
            .readDataRequestBuilder
//            .setInstantTimeFilter(InstantTimeFilter.)
            .build()

        val result = healthDataStore.readData(readRequest)

        result.dataList.map { dataPoint ->
            val rawSeries: List<HeartRateSeriesData>? =
                dataPoint.getValue(DataType.HeartRateType.SERIES_DATA) as List<HeartRateSeriesData>?


            val seriesData = rawSeries?.map { hr ->
                HeartRateSeriesData(
                    startTime = hr.startTime,
                    endTime   = hr.endTime,
                    heartRate = hr.heartRate,
                    min       = hr.min,
                    max       = hr.max
                )
            }

            HeartRatesData(
                uid = dataPoint.uid,
                startTime = dataPoint.startTime,
                endTime = dataPoint.getValue(DataType.HeartRateType.),
                zoneOffset = dataPoint.zoneOffset,
                dataSource = dataPoint.dataSource,
                heartRate = dataPoint.getValue(),

            )
        }


    }

}