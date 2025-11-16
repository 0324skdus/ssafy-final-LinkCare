package com.ssafy.sdk.health.data.model

import com.samsung.android.sdk.health.data.data.DataSource
import com.samsung.android.sdk.health.data.device.DeviceType
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

data class SleepGoalData (
    val lastBedTime: Instant,
    val lastWakeUpTime: Instant
)

data class SleepSessionData (
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val duration: Long?,
//    val stages: List<SleepStageData>
)

data class SleepData (
    val deviceId: String,
    val deviceType: DeviceType,
    val uid: String?,
    val startTime: LocalDateTime?,
    val endTime: LocalDateTime?,
    val zoneOffset: ZoneOffset?,
    val dataSource: DataSource?,
    val sessions: List<SleepSessionData>?,
    val duration: Long?,
)

// 하루 수면 데이터, 목표
//data class SleepData (
//    val sleeps: SleepTypeData,  // 세션처럼 보여주기
////    val totalDuration: Long,    // sleeps 의 duration 전부 더하기 -> 백에서 하면 될 듯?
////    val goal: SleepGoalData     // 매일 수면 목표
//)