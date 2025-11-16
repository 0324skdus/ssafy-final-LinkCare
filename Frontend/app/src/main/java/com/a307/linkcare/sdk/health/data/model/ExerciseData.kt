package com.a307.linkcare.sdk.health.data.model

import com.samsung.android.sdk.health.data.data.DataSource
import com.samsung.android.sdk.health.data.device.DeviceType
import com.samsung.android.sdk.health.data.request.DataType
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

data class ExerciseSessionData (
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val exerciseType: DataType.ExerciseType.PredefinedExerciseType,
    val duration: Long,
    val calories: Float,
    val distance: Float?,
)

data class ExerciseTypeData (
    val deviceId: String,
    val deviceType: DeviceType,
    val uid: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime?,
    val zoneOffset: ZoneOffset?,
    val dataSource: DataSource?,
    val exerciseType: DataType.ExerciseType.PredefinedExerciseType?,
    val sessions: List<ExerciseSessionData>?,
)

data class ExerciseData (
    val exercises: List<ExerciseTypeData>,
    val totalDuration: Long,
    val totalCalories: Float
)

data class ExerciseOnlyData(
    val userSeq: Int,
    val exercises: List<ExerciseData>
)