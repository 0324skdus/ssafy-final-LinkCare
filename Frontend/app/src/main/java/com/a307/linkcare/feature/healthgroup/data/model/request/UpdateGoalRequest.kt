package com.a307.linkcare.feature.healthgroup.data.model.request

data class UpdateGoalRequest(
    val selectedMetricType: String,  // "STEPS", "KCAL", "DURATION", "DISTANCE"
    val goalValue: Long  // 목표값
)