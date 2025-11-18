package com.a307.linkcare.data.model

data class WorkoutSummary(
    val sessionId: Long,
    val avgHeartRate: Int,
    val calories: Float,
    val distance: Float,
    val durationSec: Long,
    val startTimestamp: Long = 0L,
    val endTimestamp: Long = 0L
)
