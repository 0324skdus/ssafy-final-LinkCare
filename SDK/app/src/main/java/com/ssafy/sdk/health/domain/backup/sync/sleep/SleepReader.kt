package com.ssafy.sdk.health.domain.backup.sync.sleep

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
import com.ssafy.sdk.health.data.model.SleepSessionData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant
import java.time.ZoneOffset

private const val TAG = "SleepReader"

private val KST: ZoneId = ZoneId.of("Asia/Seoul")


class SleepReader(
    private val healthDataStore: HealthDataStore
) {

    // 당일 데이터 수집
    @Throws(HealthDataException::class)
    suspend fun readToday(): List<SleepData> = withContext(Dispatchers.IO) {
        val start = LocalDate.now(KST).atStartOfDay()
        val end = start.plusDays(1)

        Log.i(TAG, "readToday() start=$start end=$end")

        val req = DataTypes.SLEEP
            .readDataRequestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(start, end))
            .build()

        val result = healthDataStore.readData(req)
        result.dataList.map { mapDataPoint(it) }
    }

    // 전체 수집 (1년 단위)
    @Throws(HealthDataException::class)
    suspend fun readAll(
        yearsPerChunk: Long = 1L,
        onProgress: ((done: Int, total: Int) -> Unit) ?= null
    ): List<SleepData> = withContext(Dispatchers.IO) {

        val windows = instantWindowsLarge(yearsPerChunk).toList()
        val out = mutableListOf<SleepData>()

        windows.forEachIndexed { idx, (s, e) ->
            Log.i(TAG, "readAllOnce() window ${idx + 1}/${windows.size}  $s ~ $e")

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

    private fun mapDataPoint(dataPoint: HealthDataPoint): SleepData {
        val zoneOffset = dataPoint.zoneOffset
        val rawSessions: List<SleepSession>? =
            dataPoint.getValue(DataType.SleepType.SESSIONS)

        val sessionsData = rawSessions?.map { rs ->
            SleepSessionData(
                startTime = rs.startTime,
                endTime = rs.endTime,
                duration = rs.duration.toMillis(),
            )
        }

        return SleepData(
            uid = dataPoint.uid,
            startTime = dataPoint.startTime,
            endTime = dataPoint.endTime,
            zoneOffset = dataPoint.zoneOffset,
            dataSource = dataPoint.dataSource,
            duration = dataPoint.getValue(DataType.SleepType.DURATION)?.toMillis(),
            sessions = sessionsData
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
}


//class SleepReader(
//    store: HealthDataStore
//) : BaseReader<SleepData>(store) {
//
//    override suspend fun buildTodayRequest() =
//        DataTypes.SLEEP.readDataRequestBuilder
//            .setLocalTimeFilter(
//                LocalTimeFilter.of(
//                    LocalDate.now(KST).atStartOfDay(),
//                    LocalDate.now(KST).atStartOfDay().plusDays(1)
//                )
//            )
//            .build()
//
//    override fun buildRangeRequest(s: Instant, e: Instant) =
//        DataTypes.SLEEP.readDataRequestBuilder
//            .setInstantTimeFilter(InstantTimeFilter.of(s, e))
//            .build()
//
//    override fun map(dp: HealthDataPoint): SleepData {
//        val rawSessions: List<SleepSession>? = dp.getValue(DataType.SleepType.SESSIONS)
//        val sessions = rawSessions?.map { rs ->
//            SleepSessionData(
//                startTime = rs.startTime,
//                endTime = rs.endTime,
//                duration = rs.duration.toMillis()
//            )
//        }
//        return SleepData(
//            uid = dp.uid,
//            startTime = dp.startTime,
//            endTime = dp.endTime,
//            zoneOffset = dp.zoneOffset,
//            dataSource = dp.dataSource,
//            duration = dp.getValue(DataType.SleepType.DURATION)?.toMillis(),
//            sessions = sessions
//        )
//    }
//
//    suspend fun readAllByYear(
//        yearsPerChunk: Long = 1,
//        onProgress: ((Int, Int) -> Unit)? = null
//    ) = super.readAll(Windowing.ByYears(yearsPerChunk), onProgress)
//}
