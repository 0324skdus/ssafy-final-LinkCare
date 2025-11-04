package com.linkcare.linkcarewear.model

data class WorkoutSummary(
    val sessionId: String,
    val avgHeartRate: Int,
    val calories: Int,
    val steps: Int,
    val durationSec: Int
)
