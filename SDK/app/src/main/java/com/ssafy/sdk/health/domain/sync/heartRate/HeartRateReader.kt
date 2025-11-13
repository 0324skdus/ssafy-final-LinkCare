package com.ssafy.sdk.health.domain.sync.heartRate

import com.samsung.android.sdk.health.data.DeviceManager
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.InstantTimeFilter
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.ssafy.sdk.health.data.model.HeartRatesData
import com.ssafy.sdk.health.domain.sync.common.SimpleHealthReader
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class HeartRateReader @Inject constructor(
    store: HealthDataStore,
    deviceManager: DeviceManager
) : SimpleHealthReader<HeartRatesData>(store, deviceManager) {

    override suspend fun buildTodayRequest() =
        DataTypes.HEART_RATE.readDataRequestBuilder
            .setLocalTimeFilter(
                LocalTimeFilter.of(
                    LocalDate.now(KST).atStartOfDay(),
                    LocalDate.now(KST).atStartOfDay().plusDays(1)
                )
            )
            .build()

    override suspend fun buildRangeRequest(start: Instant, end: Instant) =
        DataTypes.HEART_RATE.readDataRequestBuilder
            .setInstantTimeFilter(InstantTimeFilter.of(start, end))
            .build()

    override suspend fun mapToData(dp: HealthDataPoint, deviceManager: DeviceManager): HeartRatesData {
        val device = findDevice(deviceManager)
        return HeartRatesData(
            deviceId = device.id,
            deviceType = device.deviceType,
            uid = dp.uid,
            startTime = dp.startTime.atOffset(dp.zoneOffset).toLocalDateTime(),
            endTime = dp.endTime?.atOffset(dp.zoneOffset)?.toLocalDateTime(),
            zoneOffset = dp.zoneOffset,
            dataSource = dp.dataSource,
            heartRate = dp.getValue(DataType.HeartRateType.HEART_RATE),
            minHeartRate = dp.getValue(DataType.HeartRateType.MIN_HEART_RATE),
            maxHeartRate = dp.getValue(DataType.HeartRateType.MAX_HEART_RATE)
        )
    }
}