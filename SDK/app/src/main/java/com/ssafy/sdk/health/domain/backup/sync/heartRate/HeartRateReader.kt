package com.ssafy.sdk.health.domain.backup.sync.heartRate

import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.InstantTimeFilter
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.ssafy.sdk.health.data.test.HeartRateSeriesData
import com.ssafy.sdk.health.data.test.HeartRatesData
import com.ssafy.sdk.health.domain.backup.sync.common.BaseReader
import com.ssafy.sdk.health.domain.backup.sync.common.Windowing
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant

private const val TAG = "HeartRateReader"

private val KST: ZoneId = ZoneId.of("Asia/Seoul")


class HeartRateReader(
    store: HealthDataStore
) : BaseReader<HeartRatesData>(store) {

    override suspend fun buildTodayRequest() =
        DataTypes.HEART_RATE.readDataRequestBuilder
            .setLocalTimeFilter(
                LocalTimeFilter.of(
                    LocalDate.now(KST).atStartOfDay(),
                    LocalDate.now(KST).atStartOfDay().plusDays(1)
                )
            )
            .build()

    override fun buildRangeRequest(s: Instant, e: Instant) =
        DataTypes.HEART_RATE.readDataRequestBuilder
            .setInstantTimeFilter(InstantTimeFilter.of(s, e))
            .build()

    override fun map(dp: HealthDataPoint): HeartRatesData {
        // SDK 실제 타입에 맞춰 캐스팅/매핑 (네가 정의한 HeartRateSeriesData로 정리)
        val raw: List<HeartRateSeriesData>? =
            dp.getValue(DataType.HeartRateType.SERIES_DATA) as? List<HeartRateSeriesData>?

        val series = raw?.map { hr ->
            HeartRateSeriesData(
                startTime = hr.startTime.atOffset(dp.zoneOffset).toLocalDateTime(),
                endTime   = hr.endTime.atOffset(dp.zoneOffset).toLocalDateTime(),
                heartRate = hr.heartRate,
                min = hr.min,
                max = hr.max
            )
        }

        return HeartRatesData(
            uid = dp.uid,
            startTime = dp.startTime.atOffset(dp.zoneOffset).toLocalDateTime(),
            endTime = dp.endTime?.atOffset(dp.zoneOffset)?.toLocalDateTime(),
            zoneOffset = dp.zoneOffset,
            dataSource = dp.dataSource,
            heartRate = dp.getValue(DataType.HeartRateType.HEART_RATE),
            seriesData = series,
            minHeartRate = dp.getValue(DataType.HeartRateType.MIN_HEART_RATE),
            maxHeartRate = dp.getValue(DataType.HeartRateType.MAX_HEART_RATE)
        )
    }

    suspend fun readAllByYear(
        yearsPerChunk: Long = 1,
        onProgress: ((Int, Int) -> Unit)? = null
    ) = super.readAll(Windowing.ByYears(yearsPerChunk), onProgress)
}

//class HeartRateReader(
//    private val healthDataStore: HealthDataStore
//) {
//
//    // 당일 데이터 수집
//    @Throws(HealthDataException::class)
//    suspend fun readToday(): List<HeartRatesData> = withContext(Dispatchers.IO) {
//        val start = LocalDate.now(KST).atStartOfDay()
//        val end = start.plusDays(1)
//
//        Log.i(TAG, "readToday() start=$start end=$end")
//
//        val req = DataTypes.HEART_RATE.readDataRequestBuilder
//            .setLocalTimeFilter(LocalTimeFilter.of(start,end))
//            .build()
//
//        val result = healthDataStore.readData(req)
//        result.dataList.map { mapDataPoint(it) }
//
//    }
//
//    // 전체 수집 (1년 단위)
//    @Throws(HealthDataException::class)
//    suspend fun readAll(
//        yearsPerChunk: Long = 1L,
//        onProgress: ((done: Int, total: Int) -> Unit) ?= null
//    ): List<HeartRatesData> = withContext(Dispatchers.IO) {
//
//        val windows = instantWindowsLarge(yearsPerChunk).toList()
//        val out = mutableListOf<HeartRatesData>()
//
//        windows.forEachIndexed { idx, (s, e) ->
//            Log.i(TAG, "readAllOnce() window ${idx + 1}/${windows.size}  $s ~ $e")
//
//            // 심박은 InstantTimeFilter 권장 (밀리초 타임스탬프 기반 시리즈/샘플에 안전)
//            val req = DataTypes.HEART_RATE
//                .readDataRequestBuilder
//                .setInstantTimeFilter(InstantTimeFilter.of(s, e))
//                .build()
//
//            val r = healthDataStore.readData(req)
//            r.dataList.forEach { dp ->
//                out += mapDataPoint(dp)
//            }
//
//            onProgress?.invoke(idx + 1, windows.size)
//        }
//
//        out
//
//    }
//
//    private fun mapDataPoint(dataPoint: HealthDataPoint): HeartRatesData {
//        val zoneOffset = dataPoint.zoneOffset
//        val rawSeries: List<HeartRateSeriesData>? =
//            dataPoint.getValue(DataType.HeartRateType.SERIES_DATA) as? List<HeartRateSeriesData>?
//
//
//        val seriesData = rawSeries?.map { hr ->
//            HeartRateSeriesData(
//                startTime = hr.startTime.atOffset(zoneOffset).toLocalDateTime(),
//                endTime   = hr.endTime.atOffset(zoneOffset).toLocalDateTime(),
//                heartRate = hr.heartRate,
//                min       = hr.min,
//                max       = hr.max
//            )
//        }
//        Log.i(TAG, "uid = ${dataPoint.uid}")
//        Log.i(TAG, "startTime = ${dataPoint.startTime.atOffset(dataPoint.zoneOffset).toLocalDateTime()}")
//        Log.i(TAG, "endTime = ${dataPoint.endTime?.atOffset(dataPoint.zoneOffset)?.toLocalDateTime()}")
//        Log.i(TAG, "zoneOffset = ${dataPoint.zoneOffset}")
//        Log.i(TAG, "dataSource = ${dataPoint.dataSource}")
//        Log.i(TAG, "heartRate = ${dataPoint.getValue(DataType.HeartRateType.HEART_RATE)}")
//        Log.i(TAG, "seriesData = $seriesData")
//        Log.i(TAG, "minHeartRate = ${dataPoint.getValue(DataType.HeartRateType.MIN_HEART_RATE)}")
//        Log.i(TAG, "maxHeartRate = ${dataPoint.getValue(DataType.HeartRateType.MAX_HEART_RATE)}")
//
//        return HeartRatesData(
//            uid = dataPoint.uid,
//            startTime = dataPoint.startTime.atOffset(dataPoint.zoneOffset).toLocalDateTime(),
//            endTime = dataPoint.endTime?.atOffset(dataPoint.zoneOffset)?.toLocalDateTime(),
//            zoneOffset = dataPoint.zoneOffset,
//            dataSource = dataPoint.dataSource,
//            heartRate = dataPoint.getValue(DataType.HeartRateType.HEART_RATE),
//            seriesData = seriesData,
//            minHeartRate = dataPoint.getValue(DataType.HeartRateType.MIN_HEART_RATE),
//            maxHeartRate = dataPoint.getValue(DataType.HeartRateType.MAX_HEART_RATE)
//        )
//    }
//
//    // 1년 단위로 수집
//    private fun instantWindowsLarge(yearPerChunk: Long): Sequence<Pair<Instant, Instant>> {
//        val end = Instant.now()
//        var cursor = Instant.EPOCH
//
//        return generateSequence {
//            if (cursor >= end) null else{
//                val to = cursor.atZone(ZoneOffset.UTC).plusYears(yearPerChunk).toInstant()
//                    .let { if(it > end) end else it}
//                (cursor to to).also { cursor = to }
//            }
//        }
//    }
//}