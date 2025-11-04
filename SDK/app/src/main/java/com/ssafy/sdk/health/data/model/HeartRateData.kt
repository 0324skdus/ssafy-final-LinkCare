package com.ssafy.sdk.health.data.model

import com.samsung.android.sdk.health.data.data.DataSource
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.Instant
import java.time.LocalDate

data class HeartRateSeriesData(
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val heartRate: Float,
    val min: Float,
    val max: Float
)

data class HeartRatesData(
    val uid: String,
    val startTime: LocalDateTime?,
    val endTime: LocalDateTime?,
    val zoneOffset: ZoneOffset?,
    val dataSource: DataSource?,
    val heartRate: Float?,
    val seriesData: List<HeartRateSeriesData>?,
    val minHeartRate: Float?,
    val maxHeartRate: Float?
)