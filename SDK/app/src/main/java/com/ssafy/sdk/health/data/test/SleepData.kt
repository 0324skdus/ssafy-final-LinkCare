package com.ssafy.sdk.health.data.test

import com.samsung.android.sdk.health.data.data.DataSource
import com.samsung.android.sdk.health.data.request.DataType
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

data class SleepStageData (
    val startTime: Instant?,
    val endTime: Instant?,
    val stage: DataType.SleepType.StageType
)

data class SleepSessionData (
    val startTime: Instant?,
    val endTime: Instant?,
    val duration: Long?,
    val stages: List<SleepStageData>
)

data class SleepTypeData (
    val uid: String?,
    val startTime: LocalDateTime?,
    val endTime: LocalDateTime?,
    val zoneOffset: ZoneOffset?,
    val dataSource: DataSource?,
    val duration: Long?,
    val sessions: List<SleepSessionData>?,
    val sleepScore: Int?,
)

data class SleepGoalData (
    val lastBedTime: LocalTime,
    val lastWakeUpTime: LocalTime
)

// 단일 값
data class SleepData (
    val startTime: LocalDateTime?,
    val sleeps: List<SleepTypeData>,
    val totalDuration: Long,
    val goal: SleepGoalData
)