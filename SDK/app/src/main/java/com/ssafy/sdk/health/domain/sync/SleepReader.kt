package com.ssafy.sdk.health.domain.sync

import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.data.entries.SleepSession
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.InstantTimeFilter
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.ssafy.sdk.health.data.model.SleepData
import com.ssafy.sdk.health.data.model.SleepGoalData
import com.ssafy.sdk.health.data.test.SleepSessionData
import com.ssafy.sdk.health.data.test.SleepStageData
import com.ssafy.sdk.health.data.test.SleepTypeData
import com.ssafy.sdk.health.domain.sleeps.ReadSleepGoalBedTimeUseCase
import com.ssafy.sdk.health.domain.sleeps.ReadSleepGoalWakeUpTimeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import kotlin.jvm.Throws
import java.time.Instant
import java.time.ZoneOffset
import kotlin.let

private const val TAG = "SleepReader"

private val KST: ZoneId = ZoneId.of("Asia/Seoul")

class SleepReader(
    private val healthDataStore: HealthDataStore
) {

    // 당일 데이터 수집
    @Throws(HealthDataException::class)
    suspend fun readToday(): SleepData = withContext(Dispatchers.IO) {
        val start = LocalDate.now(KST).atStartOfDay()
        val end = start.plusDays(1)

        Log.i(TAG, "readToday() start=$start end=$end")


        SleepData(

        )

    }

    // 전체 수집 (1년 단위)
    @Throws(HealthDataException::class)
    suspend fun readAll(
        yearsPerChunk: Long = 1L,
        onProgress: ((done: Int, total: Int) -> Unit) ?= null
    ): List<SleepTypeData> = withContext(Dispatchers.IO) {

        val windows = instantWindowsLarge(yearsPerChunk).toList()
        val out = mutableListOf<SleepTypeData>()

        windows.forEachIndexed { idx, (s, e) ->
            Log.i(TAG, "readAllOnce() window ${idx + 1}/${windows.size}  $s ~ $e")

            // 심박은 InstantTimeFilter 권장 (밀리초 타임스탬프 기반 시리즈/샘플에 안전)
            val req = DataTypes.SLEEP
                .readDataRequestBuilder
                .setInstantTimeFilter(InstantTimeFilter.of(s, e))
                .build()

            val r = healthDataStore.readData(req)
            r.dataList.forEach { dp ->
                out += mapDataPoint(dp)
            }

            onProgress?.invoke(idx + 1, windows.size)
        }

        out

    }

    private fun mapDataPoint(dataPoint: HealthDataPoint): SleepTypeData {
        val zoneOffset = dataPoint.zoneOffset
        val rawSessions: List<SleepSession>? =
            dataPoint.getValue(DataType.SleepType.SESSIONS)

        val sessionsData = rawSessions?.map { rs ->
            val rawStages: List<SleepSession.SleepStage>? = rs.stages

            val stagesData: List<SleepStageData> = rawStages?.map { stageData ->
                SleepStageData(
                    startTime = stageData.startTime,
                    endTime = stageData.endTime,
                    stage = stageData.stage
                )
            }?: emptyList()

            SleepSessionData(
                startTime = rs.startTime,
                endTime = rs.endTime,
                duration = rs.duration.toMillis(),
                stages = stagesData
            )
        }

        return SleepTypeData(
            uid = dataPoint.uid,
            startTime = dataPoint.startTime.atOffset(dataPoint.zoneOffset).toLocalDateTime(),
            endTime = dataPoint.endTime?.atOffset(dataPoint.zoneOffset)?.toLocalDateTime(),
            zoneOffset = dataPoint.zoneOffset,
            dataSource = dataPoint.dataSource,
            duration = dataPoint.getValue(DataType.SleepType.DURATION)?.toMillis(),
            sessions = sessionsData,
            sleepScore = dataPoint.getValue(DataType.SleepType.SLEEP_SCORE)
        )
    }

    // 1년 단위로 수집
    private fun instantWindowsLarge(yearPerChunk: Long): Sequence<Pair<Instant, Instant>> {
        val end = Instant.now()
        var cursor = Instant.EPOCH

        return generateSequence {
            if (cursor >= end) null else{
                val to = cursor.atZone(ZoneOffset.UTC).plusYears(yearPerChunk).toInstant()
                    .let { if(it > end) end else it}
                (cursor to to).also { cursor = to }
            }
        }
    }

    // 수면 목표 수집
    private suspend fun readSleepGoals(): SleepGoalData {
        
        val bedTime = Instant.now()
        val wakeUpTime = Instant.now()

        return SleepGoalData(bedTime, wakeUpTime)
    }
}