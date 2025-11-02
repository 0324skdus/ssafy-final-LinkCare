package com.ssafy.sdk.health.data.model

import com.samsung.android.sdk.health.data.data.DataSource
import java.time.LocalDateTime
import java.time.ZoneOffset

data class HeartRatesData(
    val uid: String,
    val startTime: LocalDateTime?,
    val endTime: LocalDateTime?,
    val zoneOffset: ZoneOffset?,
    val dataSource: DataSource?,
    val heartRate: Float?,
    val minHeartRate: Float?,
    val maxHeartRate: Float?
)