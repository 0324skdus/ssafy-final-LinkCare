package com.a307.linkcare.feature.workout.data.dto

data class WorkoutSummaryRequest(
    //TODO: 형식체크필요, 인증필요한지??
    val sessionId: Long,
    val avgHeartRate: Int,
    val calories: Int,
    val distance: Int,
    val durationSec: Int,
    val startTime: Long,
    val endTime: Long
)
