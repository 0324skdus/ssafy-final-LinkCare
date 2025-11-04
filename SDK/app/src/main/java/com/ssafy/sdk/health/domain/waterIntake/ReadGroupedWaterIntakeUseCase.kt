package com.ssafy.sdk.health.domain.waterIntake

import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.ssafy.sdk.health.data.model.WaterIntakeGroupedData
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import kotlin.jvm.Throws

private const val TAG = "ReadTotalWaterIntakeUserCase"

class ReadGroupedWaterIntakeUseCase @Inject constructor(
    private val healthDataStore: HealthDataStore
) {

    @Throws(HealthDataException::class)
    suspend operator fun invoke(): List<WaterIntakeGroupedData> {
        return readTotalWaterIntake()
    }

    private suspend fun readTotalWaterIntake(): List<WaterIntakeGroupedData> {
        val zone = ZoneOffset.systemDefault()
        val startDate = LocalDate.now(zone).atStartOfDay()
        val end = startDate.plusDays(1)

        Log.i(TAG, "StartDate: $${startDate}, End: $end")
        val readRequest = DataTypes.WATER_INTAKE
            .readDataRequestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(startDate, end))
            .build()

        val result = healthDataStore.readData(readRequest)

        val items: List<WaterIntakeGroupedData> = result.dataList.map { dataPoint ->
            Log.i(TAG, "uid : ${dataPoint.uid}")
            Log.i(TAG, "StartTime: ${dataPoint.startTime}")
            Log.i(TAG, "ZoneOffSet: ${dataPoint.zoneOffset}")
            Log.i(TAG, "DataSource: ${dataPoint.dataSource}")
            Log.i(TAG, "Amount: ${dataPoint.getValue(DataType.WaterIntakeType.AMOUNT)}")

            WaterIntakeGroupedData(
                uid = dataPoint.uid,
                startTime = dataPoint.startTime.atOffset(dataPoint.zoneOffset).toLocalDateTime(),
                endTime = dataPoint.endTime?.atOffset(dataPoint.zoneOffset)?.toLocalDateTime(),
                zoneOffset = dataPoint.zoneOffset,
                dataSource = dataPoint.dataSource,
                amount = dataPoint.getValue(DataType.WaterIntakeType.AMOUNT)
            )
        }

        return items
    }

}