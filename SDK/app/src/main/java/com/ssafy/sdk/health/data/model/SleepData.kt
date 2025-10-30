package com.ssafy.sdk.health.data.model

import com.samsung.android.sdk.health.data.data.DataSource
import com.ssafy.sdk.health.data.test.SleepSessionData
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

data class SleepGoalData (
    val lastBedTime: Instant,
    val lastWakeUpTime: Instant
)

data class SleepTypeData (
    val uid: String?,
    val startTime: Instant?,
    val endTime: Instant?,
    val zoneOffset: ZoneOffset?,
    val dataSource: DataSource?,
    val duration: Long?,
)

// 하루 수면 데이터, 목표
data class SleepData (
    val sleeps: SleepTypeData,  // 세션처럼 보여주기
    val totalDuration: Long,    // sleeps 의 duration 전부 더하기
    val goal: SleepGoalData     // 매일 수면 목표
)