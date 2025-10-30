package com.ssafy.sdk.health.data.test

import com.samsung.android.sdk.health.data.data.DataSource
import java.time.LocalDateTime
import java.time.ZoneOffset

data class WaterIntakeGroupedData (
    val uid: String,
    val startTime: LocalDateTime?,
    val endTime: LocalDateTime?,
    val zoneOffset: ZoneOffset?,
    val dataSource: DataSource?,
    val amount: Float?
)

data class WaterIntakeData (
    val startTime: LocalDateTime?,
    val waterIntakes: List<WaterIntakeGroupedData>?,
    val goal: Float,
    val total: Float
)