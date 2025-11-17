package com.a307.linkcare.sdk.health.domain.sync.bloodPressure

import com.samsung.android.sdk.health.data.DeviceManager
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.InstantTimeFilter
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.a307.linkcare.sdk.health.data.model.BloodPressureTypeData
import com.a307.linkcare.sdk.health.domain.sync.common.SimpleHealthReader
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class BloodPressureReader @Inject constructor(
    store: HealthDataStore,
    private val deviceManager: DeviceManager,
    ) : SimpleHealthReader<BloodPressureTypeData>(store, deviceManager) {

    override suspend fun buildTodayRequest() =
        DataTypes.BLOOD_PRESSURE.readDataRequestBuilder
            .setLocalTimeFilter(
                LocalTimeFilter.of(
                    LocalDate.now(KST).atStartOfDay(),
                    LocalDate.now(KST).atStartOfDay().plusDays(1)
                )
            )
            .build()

    override suspend fun buildRangeRequest(start: Instant, end: Instant) =
        DataTypes.BLOOD_PRESSURE.readDataRequestBuilder
            .setInstantTimeFilter(InstantTimeFilter.of(start, end))
            .build()

    override suspend fun mapToData(dp: HealthDataPoint, deviceManager: DeviceManager): BloodPressureTypeData {
        val device = findDevice(deviceManager)
        return BloodPressureTypeData(
            deviceId = device.id,
            deviceType = device.deviceType,
            uid = dp.uid,
            startTime = dp.startTime.atOffset(dp.zoneOffset).toLocalDateTime(),
            endTime = dp.endTime?.atOffset(dp.zoneOffset)?.toLocalDateTime(),
            zoneOffset = dp.zoneOffset,
            dataSource = dp.dataSource,
            systolic = dp.getValue(DataType.BloodPressureType.SYSTOLIC),
            diastolic = dp.getValue(DataType.BloodPressureType.DIASTOLIC),
            mean = dp.getValue(DataType.BloodPressureType.MEAN),
            pulseRate = dp.getValue(DataType.BloodPressureType.PULSE_RATE)
        )
    }
}