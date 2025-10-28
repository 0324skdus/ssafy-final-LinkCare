package com.ssafy.sdk.health.data.model

import com.samsung.android.sdk.health.data.data.DataSource
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.Instant

data class HeartRateSeriesData(
    val startTime: Instant,
    val endTime: Instant,
    val heartRate: Float,
    val min: Float,
    val max: Float
)

data class HeartRatesData(
    val uid: String,
    val startTime: Instant,
    val endTime: Instant,
    val zoneOffset: ZoneOffset,
    val dataSource: DataSource,
    val heartRate: Float,
    val seriesData: List<HeartRateSeriesData>?,
    val minHeartRate: Float,
    val maxHeartRate: Float
)