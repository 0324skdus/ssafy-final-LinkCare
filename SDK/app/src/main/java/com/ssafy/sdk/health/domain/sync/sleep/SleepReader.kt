package com.ssafy.sdk.health.domain.sync.sleep

import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.data.entries.SleepSession
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.InstantTimeFilter
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.ssafy.sdk.health.data.model.SleepData
import com.ssafy.sdk.health.data.model.SleepSessionData
import com.ssafy.sdk.health.domain.sync.common.SimpleHealthReader
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class SleepReader @Inject constructor(
    store: HealthDataStore
) : SimpleHealthReader<SleepData>(store) {

    override fun buildTodayRequest() =
        DataTypes.SLEEP.readDataRequestBuilder
            .setLocalTimeFilter(
                LocalTimeFilter.of(
                    LocalDate.now(KST).atStartOfDay(),
                    LocalDate.now(KST).atStartOfDay().plusDays(1)
                )
            )
            .build()

    override fun buildRangeRequest(start: Instant, end: Instant) =
        DataTypes.SLEEP.readDataRequestBuilder
            .setInstantTimeFilter(InstantTimeFilter.of(start, end))
            .build()

    override fun mapToData(dp: HealthDataPoint): SleepData {
        val rawSessions: List<SleepSession>? =
            dp.getValue(DataType.SleepType.SESSIONS)

        val sessions = rawSessions?.map { rs ->
            SleepSessionData(
                startTime = rs.startTime,
                endTime = rs.endTime,
                duration = rs.duration.toMillis()
            )
        }

        return SleepData(
            uid = dp.uid,
            startTime = dp.startTime,
            endTime = dp.endTime,
            zoneOffset = dp.zoneOffset,
            dataSource = dp.dataSource,
            duration = dp.getValue(DataType.SleepType.DURATION)?.toMillis(),
            sessions = sessions
        )
    }
}