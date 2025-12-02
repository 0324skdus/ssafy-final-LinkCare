package com.a307.linkcare.feature.healthgroup.data.model.response

data class WeeklyGroupGoalResponse(
    val weeklyGroupGoalsSeq: Long,
    val groupSeq: Long,
    val weekStart: String,  // "2024-01-08"
    val goalSteps: Long,
    val goalKcal: Float,
    val goalDuration: Int,  // minutes
    val goalDistance: Float,  // km
    val predictedGrowthRateSteps: Double,
    val predictedGrowthRateKcal: Double,
    val predictedGrowthRateDuration: Double,
    val predictedGrowthRateDistance: Double,
    val selectedMetricType: String? = null  // "STEPS", "KCAL", "DURATION", "DISTANCE"
)
