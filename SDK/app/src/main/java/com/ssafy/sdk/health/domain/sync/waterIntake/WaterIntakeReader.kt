package com.ssafy.sdk.health.domain.sync.waterIntake

import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalDateFilter
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.ssafy.sdk.health.data.model.WaterIntakeData
import com.ssafy.sdk.health.data.model.WaterIntakeGroupedData
import com.ssafy.sdk.health.domain.sync.common.CompositeHealthReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class WaterIntakeReader @Inject constructor(
    private val store: HealthDataStore
) : CompositeHealthReader<WaterIntakeData>() {

    override suspend fun readToday(): WaterIntakeData {
        val startDate = LocalDate.now(KST).atStartOfDay()
        val endDate = startDate.plusDays(1)

        val waterIntakes = readWaterIntakeData(startDate, endDate)
        val total = readTotalWaterIntake(startDate, endDate)
        val goal = readWaterIntakeGoal(LocalDate.now(KST))

        return WaterIntakeData(waterIntakes, goal, total)
    }

    override suspend fun readAll(
        onProgress: ((done: Int, total: Int) -> Unit)?
    ): List<WaterIntakeData> = withContext(Dispatchers.IO) {
        val startDate = LocalDate.of(1970, 1, 1)
        val endDate = LocalDate.now(KST)
        val result = mutableListOf<WaterIntakeData>()

        var current = startDate
        var dayCount = 0
        val totalDays = ChronoUnit.DAYS.between(startDate, endDate).toInt()

        while (current <= endDate) {
            val start = current.atStartOfDay()
            val end = start.plusDays(1)

            val waterIntakes = readWaterIntakeData(start, end)
            val total = readTotalWaterIntake(start, end)
            val goal = readWaterIntakeGoal(current)

            // 데이터가 있는 경우만 추가
            if (waterIntakes.isNotEmpty() || total > 0) {
                result.add(WaterIntakeData(waterIntakes, goal, total))
            }

            dayCount++

            // 30일마다 진행상황 알림
            if (dayCount % 30 == 0) {
                onProgress?.invoke(dayCount, totalDays)
            }

            // API 호출 제한 대응: 매 요청마다 짧은 딜레이
            if (current < endDate) {
                delay(50) // 50ms 대기
            }

            current = current.plusDays(1)
        }

        onProgress?.invoke(totalDays, totalDays)
        result
    }

    private suspend fun readWaterIntakeData(
        start: LocalDateTime,
        end: LocalDateTime
    ): List<WaterIntakeGroupedData> = withContext(Dispatchers.IO) {
        val req = DataTypes.WATER_INTAKE.readDataRequestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(start, end))
            .build()

        val result = store.readData(req)
        result.dataList.map { dp ->
            WaterIntakeGroupedData(
                uid = dp.uid,
                startTime = dp.startTime,
                endTime = dp.endTime,
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