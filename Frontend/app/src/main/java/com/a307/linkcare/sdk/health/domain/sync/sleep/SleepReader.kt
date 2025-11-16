package com.a307.linkcare.sdk.health.domain.sync.sleep

import com.samsung.android.sdk.health.data.DeviceManager
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.data.entries.SleepSession
import com.samsung.android.sdk.health.data.device.Device
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.InstantTimeFilter
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.a307.linkcare.sdk.health.data.model.SleepData
import com.a307.linkcare.sdk.health.data.model.SleepSessionData
import com.a307.linkcare.sdk.health.domain.sync.common.SimpleHealthReader
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class SleepReader @Inject constructor(
    store: HealthDataStore,
    deviceManager: DeviceManager
) : SimpleHealthReader<SleepData>(store, deviceManager) {

    override suspend fun buildTodayRequest() =
        DataTypes.SLEEP.readDataRequestBuilder
            .setLocalTimeFilter(
                LocalTimeFilter.of(
                    LocalDate.now(KST).atStartOfDay(),
                    LocalDate.now(KST).atStartOfDay().plusDays(1)
                )
            )
            .build()

    override suspend fun buildRangeRequest(start: Instant, end: Instant) =
        DataTypes.SLEEP.readDataRequestBuilder
            .setInstantTimeFilter(InstantTimeFilter.of(start, end))
            .build()

    override suspend fun mapToData(
        dp: HealthDataPoint,
        deviceManager: DeviceManager
    ): SleepData {
        val rawSessions: List<SleepSession>? =
            dp.getValue(DataType.SleepType.SESSIONS)

        val sessions = rawSessions?.map { rs ->
            SleepSessionData(
                startTime = rs.startTime.atZone(KST).toLocalDateTime(),
                endTime = rs.endTime.atZone(KST).toLocalDateTime(),
                duration = rs.duration.toMillis()
            )
        }
        val device = findDevice(deviceManager)
        return SleepData(
            deviceId = device.id,
            deviceType= device.deviceType,
            uid = dp.uid,
            startTime = dp.startTime.atOffset(dp.zoneOffset).toLocalDateTime(),
            endTime = dp.endTime?.atOffset(dp.zoneOffset)?.toLocalDateTime(),
            zoneOffset = dp.zoneOffset,
            dataSource = dp.dataSource,
            duration = dp.getValue(DataType.SleepType.DURATION)?.toMillis(),
            sessions = sessions
        )
    }

//    private suspend fun findDevice(
//        deviceManager: DeviceManager
//    ): Device {
//        val device = deviceManager.getLocalDevice()
//        return device
//    }
}