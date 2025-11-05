package com.ssafy.sdk.health.data.model

import com.samsung.android.sdk.health.data.data.DataSource
import com.samsung.android.sdk.health.data.device.DeviceType
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

data class WaterIntakeGroupedData (
    val deviceId: String,
    val deviceType: DeviceType,
    val uid: String,
    val startTime: LocalDateTime?,
    val endTime: LocalDateTime?,
    val zoneOffset: ZoneOffset?,
    val dataSource: DataSource?,
    val amount: Float?
)

data class WaterIntakeData (
    val waterIntakes: List<WaterIntakeGroupedData>?,
    val goal: Float,
    val total: Float
)