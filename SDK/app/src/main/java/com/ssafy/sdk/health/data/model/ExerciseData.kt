package com.ssafy.sdk.health.data.model

import com.samsung.android.sdk.health.data.data.DataSource
import com.samsung.android.sdk.health.data.request.DataType
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

data class ExerciseSessionData (
    val startTime: Instant,
    val endTime: Instant,
    val exerciseType: DataType.ExerciseType.PredefinedExerciseType,
    val duration: Long,
    val calories: Float,
    val distance: Float?,
)

data class ExerciseTypeData (
    val uid: String,
    val startTime: Instant,
    val endTime: Instant?,
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