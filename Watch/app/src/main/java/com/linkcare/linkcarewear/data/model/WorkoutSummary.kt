package com.linkcare.linkcarewear.data.model

data class WorkoutSummary(
    val sessionId: String,
    val avgHeartRate: Int,
    val calories: Int,
    val distance: Int,
    val durationSec: Int
)
