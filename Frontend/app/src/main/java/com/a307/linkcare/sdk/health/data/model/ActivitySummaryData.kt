package com.a307.linkcare.sdk.health.data.model

import com.samsung.android.sdk.health.data.device.DeviceType
import java.time.Instant
import java.time.LocalDateTime

data class ActivitySummaryData(
    val deviceId: String,
    val deviceType: DeviceType,
    val startTime: LocalDateTime,
    val totalCaloriesBurned: Double,
    val totalDistance: Double
)