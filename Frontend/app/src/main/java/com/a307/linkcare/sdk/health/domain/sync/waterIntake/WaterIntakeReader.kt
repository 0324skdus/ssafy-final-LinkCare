package com.a307.linkcare.sdk.health.domain.sync.waterIntake

import com.samsung.android.sdk.health.data.DeviceManager
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalDateFilter
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.a307.linkcare.sdk.health.data.model.WaterIntakeData
import com.a307.linkcare.sdk.health.data.model.WaterIntakeGroupedData
import com.a307.linkcare.sdk.health.domain.sync.common.CompositeHealthReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class WaterIntakeReader @Inject constructor(
    private val store: HealthDataStore,
    private val deviceManager: DeviceManager
) : CompositeHealthReader<WaterIntakeData>(deviceManager) {

    override suspend fun readToday(): WaterIntakeData {
        val startDate = LocalDate.now(KST).atStartOfDay()
        val endDate = startDate.plusDays(1)

        val waterIntakes = readWaterIntakeData(startDate, endDate, deviceManager)
        val total = readTotalWaterIntake(startDate, endDate)
        val goal = readWaterIntakeGoal(LocalDate.now(KST))

        return WaterIntakeData(waterIntakes, goal, total)
    }

    override suspend fun readAll(
        yearsPerChunk: Long,
        onProgress: ((done: Int, total: Int) -> Unit)?,
    ): List<WaterIntakeData> = withContext(Dispatchers.IO) {
        val startDate = LocalDate.now().minusYears(20)
        val endDate = LocalDate.now(KST)
        val result = mutableListOf<WaterIntakeData>()

        val chunkDays = (yearsPerChunk * 365).toLong()
        var current = startDate

        var chunkCount = 0
        val totalChunks = (ChronoUnit.DAYS.between(startDate, endDate) / chunkDays).toInt() + 1

        while (current <= endDate) {
            val chunkEnd = minOf(current.plusDays(chunkDays), endDate)
            val start = current.atStartOfDay()
            val end = chunkEnd.atTime(23, 59, 59)

            // chunk 단위로 데이터 가져오기
            val waterIntakes = readWaterIntakeData(start, end, deviceManager)
            val total = readTotalWaterIntake(start, end)
            val goal = readWaterIntakeGoal(current)

            if (waterIntakes.isNotEmpty() || total > 0) {
                result.add(WaterIntakeData(waterIntakes, goal, total))
            }

            chunkCount++
            onProgress?.invoke(chunkCount, totalChunks)

            delay(100)
            current = chunkEnd.plusDays(1)
        }

        result
    }

    private suspend fun readWaterIntakeData(
        start: LocalDateTime,
        end: LocalDateTime,
        deviceManager: DeviceManager
    ): List<WaterIntakeGroupedData> = withContext(Dispatchers.IO) {
        val req = DataTypes.WATER_INTAKE.readDataRequestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(start, end))
            .build()

        val device = findDevice(deviceManager)

        val result = store.readData(req)
        result.dataList.map { dp ->
            WaterIntakeGroupedData(
                deviceId = device.id,
                deviceType = device.deviceType,
                uid = dp.uid,
                startTime = dp.startTime.atZone(KST).toLocalDateTime(),
                endTime = dp.endTime?.atZone(KST)?.toLocalDateTime(),
                zoneOffset = dp.zoneOffset,
                dataSource = dp.dataSource,
                amount = dp.getValue(DataType.WaterIntakeType.AMOUNT)
            )
        }
    }

    private suspend fun readTotalWaterIntake(
        start: LocalDateTime,
        end: LocalDateTime
    ): Float = withContext(Dispatchers.IO) {
        val req = DataType.WaterIntakeType.TOTAL.requestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(start, end))
            .build()

        val result = store.aggregateData(req)
        result.dataList.firstOrNull()?.value ?: 0f
    }

    private suspend fun readWaterIntakeGoal(date: LocalDate): Float = withContext(Dispatchers.IO) {
        val req = DataType.WaterIntakeGoalType.LAST.requestBuilder
            .setLocalDateFilter(LocalDateFilter.of(date, date.plusDays(1)))
            .build()

        val result = store.aggregateData(req)
        result.dataList.firstOrNull()?.value ?: 0f
    }
}