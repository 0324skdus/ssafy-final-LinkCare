package com.a307.linkcare.sdk.health.domain.sync.activitySummary

import com.samsung.android.sdk.health.data.DeviceManager
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.a307.linkcare.sdk.health.data.model.ActivitySummaryData
import com.a307.linkcare.sdk.health.domain.sync.common.CompositeHealthReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class ActivitySummaryReader @Inject constructor(
    private val store: HealthDataStore,
    private val deviceManager: DeviceManager,
): CompositeHealthReader<ActivitySummaryData>(deviceManager) {

    override suspend fun readToday(): ActivitySummaryData {
        val start = LocalDate.now(KST).atStartOfDay()
        val end = start.plusDays(1)

        val totalCaloriesBurned = readTotalCaloriesBurned(start, end)
        val totalDistance = readTotalDistance(start, end)
        val device = findDevice(deviceManager)

        return ActivitySummaryData(
            device.id, device.deviceType,
            start.atZone(KST).toLocalDateTime(), totalCaloriesBurned, totalDistance
        )
    }

    override suspend fun readAll(
        yearsPerChunk: Long,
        onProgress: ((Int, Int) -> Unit)?
    ): List<ActivitySummaryData> = withContext(Dispatchers.IO) {
        val startDate = LocalDate.now().minusYears(1)
        val endDate = LocalDate.now(KST)

        val result = mutableListOf<ActivitySummaryData>()
        val chunkDays = (yearsPerChunk * 365).toLong()
        var current = startDate

        var processedDays = 0
        val totalDays = ChronoUnit.DAYS.between(startDate, endDate).toInt()

        val device = findDevice(deviceManager)

        while (current <= endDate) {
            val chunkEnd = minOf(current.plusDays(chunkDays), endDate)

            // Chunk 내부를 하루씩 순회!
            var dayInChunk = current
            while (dayInChunk <= chunkEnd) {
                val dayStart = dayInChunk.atStartOfDay()
                val dayEnd = dayStart.plusDays(1)

                // 하루치 데이터 조회
                val totalCaloriesBurned = readTotalCaloriesBurned(dayStart, dayEnd)
                val totalDistance = readTotalDistance(dayStart, dayEnd)

                // 데이터가 있는 날만 추가
                if (totalCaloriesBurned > 0.0 || totalDistance > 0.0) {
                    result.add(ActivitySummaryData(
                        deviceId = device.id,
                        deviceType = device.deviceType,
                        startTime = dayStart.atZone(KST).toLocalDateTime(),
                        totalCaloriesBurned = totalCaloriesBurned,
                        totalDistance = totalDistance
                    ))
                }

                dayInChunk = dayInChunk.plusDays(1)
                processedDays++

                // 30일마다 진행상황 알림
                if (processedDays % 30 == 0) {
                    onProgress?.invoke(processedDays, totalDays)
                }
            }

            delay(100) // Chunk 간 딜레이
            current = chunkEnd.plusDays(1)
        }

        onProgress?.invoke(totalDays, totalDays)
        result

    }

    private suspend fun readTotalCaloriesBurned(
        start: LocalDateTime,
        end: LocalDateTime
    ): Double = withContext(Dispatchers.IO){
        val req = DataType.ActivitySummaryType.TOTAL_ACTIVE_CALORIES_BURNED
            .requestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(start, end))
            .build()

        val result = store.aggregateData(req)

        (result.dataList.firstOrNull()?.value ?: 0).toDouble()
    }

    private suspend fun readTotalDistance(
        start: LocalDateTime,
        end: LocalDateTime
    ): Double = withContext(Dispatchers.IO){
        val req = DataType.ActivitySummaryType.TOTAL_DISTANCE
            .requestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(start, end))
            .build()

        val result = store.aggregateData(req)

        (result.dataList.firstOrNull()?.value ?: 0).toDouble()
    }


}
