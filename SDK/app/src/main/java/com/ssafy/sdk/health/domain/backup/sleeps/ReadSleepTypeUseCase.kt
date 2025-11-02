package com.ssafy.sdk.health.domain.sleeps

import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.entries.SleepSession
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.ssafy.sdk.health.data.model.SleepSessionData
import com.ssafy.sdk.health.data.model.SleepData
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.jvm.Throws

private const val TAG = "ReadSleepTypeUseCase"

class ReadSleepTypeUseCase @Inject constructor(
    private val healthDataStore: HealthDataStore,
) {

    @Throws(HealthDataException::class)
    suspend operator fun invoke(): List<SleepData> {
        return readSleepType()
    }

    private suspend fun readSleepType(): List<SleepData> {
        val startTime = LocalDate.now().atStartOfDay()
        val now = LocalDateTime.now()

        val readRequest = DataTypes.SLEEP
            .readDataRequestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(startTime, now))
            .build()

        val result = healthDataStore.readData(readRequest)
        val items = result.dataList.map { dataPoint ->

            val rawSessions: List<SleepSession>? =
                dataPoint.getValue(DataType.SleepType.SESSIONS)

            val sessionsData = rawSessions?.map { rs ->
                val rawStages: List<SleepSession.SleepStage>? = rs.stages

                SleepSessionData(
                    startTime = rs.startTime,
                    endTime = rs.endTime,
                    duration = rs.duration.toMillis()
                )
            }

            SleepData(
                uid = dataPoint.uid,
                startTime = dataPoint.startTime,
                endTime = dataPoint.endTime,
                zoneOffset = dataPoint.zoneOffset,
                dataSource = dataPoint.dataSource,
                duration = dataPoint.getValue(DataType.SleepType.DURATION)?.toMillis(),
                sessions = sessionsData
            )
        }

        return items
    }
}