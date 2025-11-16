package com.a307.linkcare.feature.watch.model

data class WorkoutSummary(
    val sessionId: Long,
    val avgHeartRate: Int,
    val calories: Int,
    val distance: Int,
    val durationSec: Int,
    val startTimestamp: Long = 0L,
    val endTimestamp: Long = 0L
)