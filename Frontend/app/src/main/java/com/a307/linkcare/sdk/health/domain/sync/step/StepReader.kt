package com.a307.linkcare.sdk.health.domain.sync.step

import com.samsung.android.sdk.health.data.DeviceManager
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.LocalDateFilter
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.a307.linkcare.sdk.health.data.model.StepData
import com.a307.linkcare.sdk.health.domain.sync.common.CompositeHealthReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class StepReader @Inject constructor(
    private val store: HealthDataStore,
    private val deviceManager: DeviceManager
): CompositeHealthReader<StepData>(deviceManager) {

    override suspend fun readToday(): StepData {
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(1)

        val goal = readStepGoal(startDate, endDate)
        val steps = readSteps(startDate.atStartOfDay(), endDate.atStartOfDay())
        val device = findDevice(deviceManager)

        val startTime = startDate.atStartOfDay()
        val endTime = startTime.plusDays(1)
        return StepData(device.id, device.deviceType, startTime, endTime, steps,  goal)
    }

    override suspend fun readAll(
        yearsPerChunk: Long,
        onProgress: ((done: Int, total: Int) -> Unit)?,
    ): List<StepData> = withContext(Dispatchers.IO) {
        val startDate = LocalDate.now().minusYears(1)
        val endDate = LocalDate.now(KST)
        val result = mutableListOf<StepData>()

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
                val totalSteps = readSteps(dayStart, dayEnd)
                val goal = readStepGoal(dayStart.toLocalDate(), dayStart.toLocalDate().plusDays(1))

                // 데이터가 있는 날만 추가
                if (totalSteps > 0L) {
                    result.add(StepData(
                        deviceId = device.id,
                        deviceType = device.deviceType,
                        startTime= dayStart.atZone(KST).toLocalDateTime(),
                        endTime = dayEnd.atZone(KST).toLocalDateTime(),
                        count = totalSteps,
                        goal = goal
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

    private suspend fun readStepGoal(
        start: LocalDate,
        end: LocalDate
    ): Int = withContext(Dispatchers.IO) {
        try {
            val readRequest = DataType.StepsGoalType.LAST
                .requestBuilder
                .setLocalDateFilter(LocalDateFilter.of(start, end))
                .build()
            val result = store.aggregateData(readRequest)

            result.dataList.firstOrNull()?.value ?: 0
        } catch (e: Exception) {
            // steps_goal 권한이 없는 경우 0 반환
            android.util.Log.w("StepReader", "StepGoal 읽기 실패 (권한 없음?): ${e.message}")
            0
        }
    }

    private suspend fun readSteps(
        start: LocalDateTime,
        end: LocalDateTime
    ): Long = withContext(Dispatchers.IO) {
        val req = DataType.StepsType.TOTAL
            .requestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(start, end))
            .build()

        val result = store.aggregateData(req)
        result.dataList.firstOrNull()?.value ?: 0L
    }

//    private suspend fun readGroupedStepsByTimeRange(
//        start: LocalDateTime,
//        end: LocalDateTime,
//        localTimeGroup: LocalTimeGroup,
//    ): ArrayList<GroupedData> = withContext(Dispatchers.IO) {
//        val multiplier = 1
////        val localTimeGroup = LocalTimeGroup.of(LocalTimeGroupUnit.HOURLY, 1)
//        val localTimeFilter = LocalTimeFilter.of(start, end)
//
//        val aggregateRequest = DataType.StepsType.TOTAL
//            .requestBuilder
//            .setLocalTimeFilterWithGroup(localTimeFilter, localTimeGroup)
//            .setLocalTimeFilter(LocalTimeFilter.of(start,end))
//            .build()
//        val result = store.aggregateData(aggregateRequest)
//
//        result.dataList.map { ad ->
//            GroupedData(
//                count= ad.value,
//                startTime= ad.startTime.atZone(ZoneId.systemDefault()).toLocalDateTime()
//            )
//        } as ArrayList<GroupedData>
//    }

}