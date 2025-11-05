package com.ssafy.sdk.health.domain.sync.exercise

import com.samsung.android.sdk.health.data.DeviceManager
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.data.entries.ExerciseSession
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.InstantTimeFilter
import com.samsung.android.sdk.health.data.request.LocalDateFilter
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.ssafy.sdk.health.data.model.ExerciseData
import com.ssafy.sdk.health.data.model.ExerciseSessionData
import com.ssafy.sdk.health.data.model.ExerciseTypeData
import com.ssafy.sdk.health.domain.sync.common.CompositeHealthReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

class ExerciseReader @Inject constructor(
    private val store: HealthDataStore,
    private val deviceManager: DeviceManager,
) : CompositeHealthReader<ExerciseData>(deviceManager) {

    override suspend fun readToday(): ExerciseData {
        val startDate = LocalDate.now(KST).atStartOfDay()
        val endDate = startDate.plusDays(1)

        val exercises = readExerciseData(startDate, endDate, deviceManager)
        val totalDuration = readTotalDuration(LocalDate.now(KST))
        val totalCalories = readTotalCalories(startDate, endDate)

        return ExerciseData(
            exercises = exercises,
            totalDuration = totalDuration,
            totalCalories = totalCalories
        )
    }

    override suspend fun readAll(
        yearsPerChunk: Long,
        onProgress: ((done: Int, total: Int) -> Unit)?,
    ): List<ExerciseData> = withContext(Dispatchers.IO) {
        // 전체 기간을 1년 단위로 조회
        val windows = generateYearWindows(1L)
        val dailyExerciseMap = mutableMapOf<LocalDate, MutableList<ExerciseTypeData>>()

        windows.forEachIndexed { index, (start, end) ->
            // 각 구간의 운동 데이터 조회
            val exercises = readExerciseDataByRange(start, end, deviceManager)

            // 날짜별로 그룹화
            exercises.forEach { exercise ->
                val date = exercise.startTime.atZone(exercise.zoneOffset ?: ZoneOffset.UTC).toLocalDate()
                dailyExerciseMap.getOrPut(date) { mutableListOf() }.add(exercise)
            }

            onProgress?.invoke(index + 1, windows.size)

            // API 호출 제한 대응
            if (index < windows.size - 1) {
                delay(100) // 100ms 대기
            }
        }

        // 날짜별로 ExerciseData 생성
        val result = dailyExerciseMap.map { (date, dailyExercises) ->
            val startOfDay = date.atStartOfDay()
            val endOfDay = startOfDay.plusDays(1)

            val totalDuration = readTotalDuration(date)
            val totalCalories = readTotalCalories(startOfDay, endOfDay)

            ExerciseData(
                exercises = dailyExercises,
                totalDuration = totalDuration,
                totalCalories = totalCalories
            )
        }

        result
    }

    private suspend fun readExerciseData(
        start: LocalDateTime,
        end: LocalDateTime,
        deviceManager: DeviceManager
    ): List<ExerciseTypeData> = withContext(Dispatchers.IO) {
        val req = DataTypes.EXERCISE.readDataRequestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(start, end))
            .build()

        val result = store.readData(req)
        result.dataList.map { dp -> mapToExerciseData(dp, deviceManager) }
    }

    private suspend fun readExerciseDataByRange(
        start: Instant,
        end: Instant,
        deviceManager: DeviceManager
    ): List<ExerciseTypeData> = withContext(Dispatchers.IO) {
        val req = DataTypes.EXERCISE.readDataRequestBuilder
            .setInstantTimeFilter(InstantTimeFilter.of(start, end))
            .build()

        val result = store.readData(req)
        result.dataList.map { dp -> mapToExerciseData(dp, deviceManager) }
    }

    private suspend fun readTotalDuration(date: LocalDate): Long = withContext(Dispatchers.IO) {
        val req = DataType.ExerciseType.TOTAL_DURATION.requestBuilder
            .setLocalDateFilter(LocalDateFilter.of(date, date.plusDays(1)))
            .build()

        val result = store.aggregateData(req)
        result.dataList.firstOrNull()?.value?.toMillis() ?: 0L
    }

    private suspend fun readTotalCalories(
        start: LocalDateTime,
        end: LocalDateTime
    ): Float = withContext(Dispatchers.IO) {
        val req = DataType.ExerciseType.TOTAL_CALORIES.requestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(start, end))
            .build()

        val result = store.aggregateData(req)
        result.dataList.firstOrNull()?.value ?: 0f
    }

    private suspend fun mapToExerciseData(dp: HealthDataPoint,
                                  deviceManager: DeviceManager): ExerciseTypeData {
        val rawSessions: List<ExerciseSession>? =
            dp.getValue(DataType.ExerciseType.SESSIONS)

        val sessions = rawSessions?.map { rs ->

            ExerciseSessionData(
                startTime = rs.startTime.atZone(KST).toLocalDateTime(),
                endTime = rs.endTime.atZone(KST).toLocalDateTime(),
                exerciseType = rs.exerciseType,
                duration = rs.duration.toMillis(),
                calories = rs.calories,
                distance = rs.distance
            )
        }

        val device = findDevice(deviceManager)
        return ExerciseTypeData(
            deviceId = device.id,
            deviceType = device.deviceType,
            uid = dp.uid,
            startTime = dp.startTime.atOffset(dp.zoneOffset).toLocalDateTime(),
            endTime = dp.endTime?.atOffset(dp.zoneOffset)?.toLocalDateTime(),
            zoneOffset = dp.zoneOffset,
            dataSource = dp.dataSource,
            exerciseType = dp.getValue(DataType.ExerciseType.EXERCISE_TYPE),
            sessions = sessions
        )
    }

    private fun generateYearWindows(yearsPerChunk: Long): List<Pair<Instant, Instant>> {
        val now = Instant.now()
        var cursor = Instant.now().minus(Duration.ofDays(20 * 365L))
        val windows = mutableListOf<Pair<Instant, Instant>>()

        while (cursor < now) {
            val next = cursor.atZone(ZoneOffset.UTC)
                .plusYears(yearsPerChunk)
                .toInstant()
                .let { if (it > now) now else it }
            windows.add(cursor to next)
            cursor = next
        }

        return windows
    }
}