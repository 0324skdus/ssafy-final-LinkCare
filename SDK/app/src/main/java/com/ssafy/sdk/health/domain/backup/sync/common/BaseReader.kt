package com.ssafy.sdk.health.domain.backup.sync.common

import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.request.ReadDataRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.collections.map
import kotlin.sequences.toList

abstract class BaseReader<T>(
    protected val store: HealthDataStore
) : HealthReader<T> {

    protected val KST: ZoneId = ZoneId.of("Asia/Seoul")

    // 서브클래스가 차이점만 구현
    protected abstract suspend fun buildTodayRequest(): ReadDataRequest<HealthDataPoint>
    protected abstract fun buildRangeRequest(s: Instant, e: Instant): ReadDataRequest<HealthDataPoint>
    protected abstract fun map(dp: HealthDataPoint): T

    override suspend fun readToday(): List<T> = withContext(Dispatchers.IO) {
        val req = buildTodayRequest()
        val res = store.readData(req)
        res.dataList.map { map(it) }
    }

    override suspend fun readAll(
        windowing: Windowing,
        onProgress: ((done: Int, total: Int) -> Unit)?
    ): List<T> = withContext(Dispatchers.IO) {
        val windows = when (windowing) {
            is Windowing.Single   -> sequenceOf(Instant.EPOCH to Instant.now())
            is Windowing.ByYears  -> windowsByYears(windowing.yearsPerChunk)
//            is Windowing.ByMonths -> windowsByMonths(windowing.monthsPerChunk)
        }.toList()
        val out = ArrayList<T>(1024)

        windows.forEachIndexed { idx, (s, e) ->
            val req = buildRangeRequest(s, e)
            val res = store.readData(req)
            res.dataList.forEach { out += map(it) }
            onProgress?.invoke(idx + 1, windows.size)
        }
        out
    }

    protected fun windowsByYears(yearsPerChunk: Long): Sequence<Pair<Instant, Instant>> {
        val end = Instant.now(); var cursor = Instant.EPOCH
        return generateSequence {
            if (cursor >= end) null else {
                val to = cursor.atZone(ZoneOffset.UTC).plusYears(yearsPerChunk).toInstant()
                    .let { if (it > end) end else it }
                (cursor to to).also { cursor = to }
            }
        }
    }
}
