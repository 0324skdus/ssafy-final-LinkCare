package com.ssafy.sdk.health.data.test

import com.samsung.android.sdk.health.data.data.DataSource
import com.samsung.android.sdk.health.data.request.DataType
import java.time.LocalDateTime
import java.time.ZoneOffset

data class ExerciseLocationData (
    val timestamp: Long,
    val longitude: Long,
    val latitude: Long,
    val altitude: Long?,
    val accuracy: Long?
)

data class ExerciseLogData (
    val timestamp: Long,
    val heartRate: Float?,
//    val cadence: Float?,
    val count: Int?,
//    val power: Float?,
    val speed: Float?
)

data class ExerciseSessionData (
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val exerciseType: DataType.ExerciseType.PredefinedExerciseType,
    val duration: Long,
    val calories: Float,
//    val route: List<ExerciseLocationData>?,
    val log: List<ExerciseLogData>,
    val distance: Float?,
)

data class ExerciseTypeData (
    val uid: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime?,
    val zoneOffset: ZoneOffset?,
    val dataSource: DataSource?,
    val exerciseType: DataType.ExerciseType.PredefinedExerciseType?,
    val sessions: List<ExerciseSessionData>?,
)

data class ExerciseData (
    val startTime: LocalDateTime?,
    val exercises: List<ExerciseTypeData>,
    val totalDuration: Long,
    val totalCalories: Float
)