package com.ssafy.sdk.health.domain.bloodPressures

import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.ssafy.sdk.health.data.model.BloodPressureTypeData
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

private const val TAG = "ReadBloodPressureDataUseCase"

class ReadBloodPressureDataUseCase @Inject constructor(
    private val healthDataStore: HealthDataStore
) {

    @Throws(HealthDataException::class)
    suspend operator fun invoke(): List<BloodPressureTypeData> {
        return readBloodPressures()
    }

    private suspend fun readBloodPressures(): List<BloodPressureTypeData> {
        val startTime = LocalDate.now().atStartOfDay()
        val now = LocalDateTime.now()

        val readRequest = DataTypes.BLOOD_PRESSURE
            .readDataRequestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(startTime, now))
            .build()

        val result = healthDataStore.readData(readRequest)

        val items: List<BloodPressureTypeData> =
            result.dataList.map { dataPoint ->

                BloodPressureTypeData(
                    uid = dataPoint.uid,
                    startTime = dataPoint.startTime.atOffset(dataPoint.zoneOffset).toLocalDateTime(),
                    endTime = dataPoint.endTime?.atOffset(dataPoint.zoneOffset)?.toLocalDateTime(),
                    zoneOffset = dataPoint.zoneOffset,
                    dataSource = dataPoint.dataSource,
                    systolic = dataPoint.getValue(DataType.BloodPressureType.SYSTOLIC),
                    diastolic = dataPoint.getValue(DataType.BloodPressureType.DIASTOLIC),
                    mean = dataPoint.getValue(DataType.BloodPressureType.MEAN),
                    pulseRate = dataPoint.getValue(DataType.BloodPressureType.PULSE_RATE),
                    medicationTaken = dataPoint.getValue(DataType.BloodPressureType.MEDICATION_TAKEN)
                )
            }

        return items
    }
}