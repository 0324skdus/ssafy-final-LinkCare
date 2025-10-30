package com.ssafy.sdk.health.domain.exercise

import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.entries.ExerciseLog
import com.samsung.android.sdk.health.data.data.entries.ExerciseSession
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.ssafy.sdk.health.data.test.ExerciseLogData
import com.ssafy.sdk.health.data.test.ExerciseSessionData
import com.ssafy.sdk.health.data.test.ExerciseTypeData
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import kotlin.jvm.Throws

private const val TAG = "ReadExerciseTypeUseCase"

class ReadExerciseTypeUseCase @Inject constructor(
    private val healthDataStore: HealthDataStore
) {

    @Throws(HealthDataException::class)
    suspend operator fun invoke(): List<ExerciseTypeData> {
        return readExerciseTypeData()
    }

    private suspend fun readExerciseTypeData(): List<ExerciseTypeData> {
        val startTime = LocalDate.now().atStartOfDay()
        val now = LocalDateTime.now()

        val readRequest = DataTypes.EXERCISE
            .readDataRequestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(startTime, now))
            .build()

        val result = healthDataStore.readData(readRequest)

        val items = result.dataList.map { dataPoint ->

            val rawSessions: List<ExerciseSession>? =
                dataPoint.getValue(DataType.ExerciseType.SESSIONS)

            val sessionData = rawSessions?.map { rs ->

                val rawLogs: List<ExerciseLog>? = rs.log

                val exerciseLogs: List<ExerciseLogData> = rawLogs?.map { rl ->
                    ExerciseLogData(
                        timestamp = rl.timestamp.toEpochMilli(),
                        heartRate = rl.heartRate,
                        count = rl.count,
                        speed = rl.speed
                    )
                }?: emptyList()

                ExerciseSessionData(
                    startTime = rs.startTime.atOffset(ZoneOffset.of("+09:00")).toLocalDateTime(),
                    endTime = rs.endTime.atOffset(ZoneOffset.of("+09:00")).toLocalDateTime(),
                    exerciseType = rs.exerciseType,
                    duration = rs.duration.toMillis(),
                    calories = rs.calories,
                    log = exerciseLogs,
                    distance = rs.distance
                )
            }

            ExerciseTypeData(
                uid = dataPoint.uid,
                startTime = dataPoint.startTime.atOffset(dataPoint.zoneOffset).toLocalDateTime(),
                endTime = dataPoint.endTime?.atOffset(dataPoint.zoneOffset)?.toLocalDateTime(),
                zoneOffset = dataPoint.zoneOffset,
                dataSource = dataPoint.dataSource,
                exerciseType = dataPoint.getValue(DataType.ExerciseType.EXERCISE_TYPE),
                sessions = sessionData
            )

        }

        return items
    }
}