package com.linkcare.linkcarewear.data.model

data class WorkoutSummary(
    val sessionId: Long = 0L,
    val avgHeartRate: Int,
    val calories: Int,
    val distance: Int,
    val durationSec: Int,
    val startTimestamp: Long = 0L,
    val endTimestamp: Long = 0L
)
