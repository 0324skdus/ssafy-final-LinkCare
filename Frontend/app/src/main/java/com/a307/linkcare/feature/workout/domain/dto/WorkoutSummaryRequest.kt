package com.a307.linkcare.feature.workout.domain.dto

data class WorkoutSummaryRequest(
    //TODO:워치 전송 형식 변경할것
    val sessionId: Long,
    val avgHeartRate: Int,
    val calories: Float,
    val distance: Float,
    val durationSec: Long,
    val startTimestamp: Long,
    val endTimestamp: Long
)
